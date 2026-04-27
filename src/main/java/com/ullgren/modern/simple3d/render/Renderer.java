package com.ullgren.modern.simple3d.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.Point3D;
import com.ullgren.modern.simple3d.render.effect.Effect;

/**
 * Renders a {@link Body} onto a {@link Graphics} context using Gouraud shading for side faces
 * and flat shading for cap faces.
 * <p>
 * Side faces (≤ 4 vertices) are fan-triangulated and rasterised with per-vertex shading
 * interpolated via scanline rasterisation into an off-screen buffer. Vertex normals are
 * smoothed across adjacent faces whose normals are within 60° of each other, preserving
 * hard edges between caps and sides.
 * <p>
 * Cap faces (&gt; 4 vertices) are drawn last with flat shading via {@link Graphics#fillPolygon},
 * which handles non-convex polygons correctly.
 * <p>
 * Geometry is rendered at 2× resolution and bilinear-downsampled to the display buffer,
 * giving hardware-quality anti-aliasing on all edges at no extra API cost.
 * <p>
 * A {@code Renderer} instance is stateful: it caches off-screen {@link BufferedImage} buffers
 * that are only reallocated when the canvas size changes. Create one instance per canvas and
 * reuse it.
 */
public class Renderer {

  private static final double PROJECTION_FACTOR    = 0.001;
  private static final double AMBIENT              = 0.15;
  /** How strongly baked AO darkens the ambient floor (0 = off, 1 = full). */
  private static final double AO_STRENGTH          = 0.5;
  /** Adjacent faces whose normalised normals dot-product exceeds this share smooth shading. */
  private static final double SMOOTH_THRESHOLD_COS = Math.cos(Math.toRadians(60.0));

  /** Cached off-screen buffer; recreated only when the canvas size changes. */
  private BufferedImage canvasImage;
  private int           canvasImageW, canvasImageH;
  /** 2× resolution buffer; geometry is rendered here then downsampled to {@link #canvasImage}. */
  private BufferedImage hiResImage;
  private int           hiResImageW, hiResImageH;
  /** Scratch buffer for the effect pixel-displacement pass. */
  private int[]  tempBuffer;
  /** Optional image-space effect applied after all geometry is rendered. */
  private Effect effect;

  /** A scan-line rendered triangle with per-corner Gouraud shading values. */
  private record Triangle(int x0, int y0, int x1, int y1, int x2, int y2,
                           double avgZ, float s0, float s1, float s2) {}

  private record CapPolygon(int[] px, int[] py, double avgZ, Color colour) {
    void draw(Graphics2D g) {
      g.setColor(colour);
      g.fillPolygon(px, py, px.length);
    }
  }

  /** Attaches an effect that will be applied each frame after geometry rendering. */
  public void setEffect(Effect effect) {
    this.effect = effect;
  }

  /**
   * Renders {@code body} onto {@code g}.
   *
   * @param body    the body to render
   * @param g       Graphics context to draw into
   * @param centerX x-coordinate of the origin in screen space
   * @param centerY y-coordinate of the origin in screen space
   * @param scale   zoom scale factor
   * @param canvasW canvas width in pixels (used to size the off-screen buffer)
   * @param canvasH canvas height in pixels
   */
  public void render(Body body, Graphics g,
                     int centerX, int centerY, double scale,
                     int canvasW, int canvasH) {
    // All geometry is rendered at 2× resolution for anti-aliasing.
    int    hiW     = 2 * canvasW;
    int    hiH     = 2 * canvasH;
    double hiScale = 2.0 * scale;
    int    hiCX    = 2 * centerX;
    int    hiCY    = 2 * centerY;

    int n = body.pointCount();
    int[] sx = new int[n];
    int[] sy = new int[n];
    double[] sz = new double[n];
    for (int i = 0; i < n; i++) {
      Point3D p = body.pointAt(i);
      double f = 1.0 - PROJECTION_FACTOR * p.getZ();
      sx[i] = (int) (hiScale * p.getX() * f) + hiCX;
      sy[i] = (int) (hiScale * p.getY() * f) + hiCY;
      sz[i] = p.getZ();
    }

    int fc = body.faceCount();

    // Pre-compute per-face normals.
    double[][] faceNormals = new double[fc][];
    for (int fi = 0; fi < fc; fi++) {
      faceNormals[fi] = computeFaceNormal(body, body.faceAt(fi));
    }

    // Build vertex-to-face adjacency for smooth normal computation.
    @SuppressWarnings("unchecked")
    List<Integer>[] vertexFaces = new List[n];
    for (int i = 0; i < n; i++) vertexFaces[i] = new ArrayList<>();
    for (int fi = 0; fi < fc; fi++) {
      for (int idx : body.faceAt(fi)) vertexFaces[idx].add(fi);
    }

    List<Triangle>   sideTris = new ArrayList<>();
    List<CapPolygon> capPolys = new ArrayList<>();
    Color colour = body.getColour();

    for (int fi = 0; fi < fc; fi++) {
      int[] face = body.faceAt(fi);
      double[] fn = faceNormals[fi];
      if (fn[2] >= 0) continue; // back-face cull

      if (face.length > 4) {
        double fnLen = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1] + fn[2] * fn[2]);
        double shade = Math.max(AMBIENT, fnLen > 0 ? -fn[2] / fnLen : AMBIENT);
        // Apply average AO of the cap's vertices to the flat ambient floor.
        double avgAO = 0;
        for (int idx : face) avgAO += body.getVertexAO(idx);
        avgAO /= face.length;
        double aoAmbient = AMBIENT * (1.0 - AO_STRENGTH * avgAO);
        shade = Math.max(aoAmbient, shade);
        Color faceColour = shadedColour(colour, shade);
        double avgZ = 0;
        for (int idx : face) avgZ += sz[idx];
        avgZ /= face.length;
        int[] px = new int[face.length];
        int[] py = new int[face.length];
        for (int i = 0; i < face.length; i++) { px[i] = sx[face[i]]; py[i] = sy[face[i]]; }
        capPolys.add(new CapPolygon(px, py, avgZ, faceColour));
      } else {
        float[] cs = new float[face.length];
        for (int ci = 0; ci < face.length; ci++) {
          cs[ci] = (float) computeCornerShade(body, face[ci], fi, faceNormals, vertexFaces,
              body.getVertexAO(face[ci]));
        }
        for (int i = 1; i < face.length - 1; i++) {
          int a = face[0], b = face[i], c = face[i + 1];
          double avgZ = (sz[a] + sz[b] + sz[c]) / 3.0;
          sideTris.add(new Triangle(sx[a], sy[a], sx[b], sy[b], sx[c], sy[c], avgZ,
              cs[0], cs[i], cs[i + 1]));
        }
      }
    }

    // Render Gouraud triangles into the 2× hi-res buffer (recreated only on resize).
    if (hiResImage == null || hiResImageW != hiW || hiResImageH != hiH) {
      hiResImage  = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_ARGB);
      hiResImageW = hiW;
      hiResImageH = hiH;
    } else {
      int[] pix = ((DataBufferInt) hiResImage.getRaster().getDataBuffer()).getData();
      Arrays.fill(pix, 0);
    }
    int[] hiPixels = ((DataBufferInt) hiResImage.getRaster().getDataBuffer()).getData();

    sideTris.sort((t1, t2) -> Double.compare(t2.avgZ(), t1.avgZ()));
    for (Triangle t : sideTris) {
      drawGouraudTriangle(hiPixels, hiW, hiH, colour,
          t.x0(), t.y0(), t.s0(),
          t.x1(), t.y1(), t.s1(),
          t.x2(), t.y2(), t.s2());
    }

    // Draw cap polygons into the hi-res buffer with AA enabled.
    capPolys.sort((a, b) -> Double.compare(b.avgZ(), a.avgZ()));
    Graphics2D hiG = hiResImage.createGraphics();
    hiG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    capPolys.forEach(p -> p.draw(hiG));
    hiG.dispose();

    // Downsample 2× → 1× with bilinear interpolation — this is the AA step.
    if (canvasImage == null || canvasImageW != canvasW || canvasImageH != canvasH) {
      canvasImage  = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
      canvasImageW = canvasW;
      canvasImageH = canvasH;
    }
    int[] pixels = ((DataBufferInt) canvasImage.getRaster().getDataBuffer()).getData();
    Graphics2D downG = canvasImage.createGraphics();
    downG.setComposite(java.awt.AlphaComposite.Src);
    downG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    downG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    downG.drawImage(hiResImage, 0, 0, canvasW, canvasH, null);
    downG.dispose();

    // Apply image-space elastic dent if active.
    if (effect != null && effect.isActive()) {
      int len = canvasW * canvasH;
      if (tempBuffer == null || tempBuffer.length != len) {
        tempBuffer = new int[len];
      }
      System.arraycopy(pixels, 0, tempBuffer, 0, len);
      effect.applyToPixels(tempBuffer, pixels, canvasW, canvasH);
    }

    g.drawImage(canvasImage, 0, 0, null);
  }

  /**
   * Returns the (unnormalised) face normal as [nx, ny, nz] computed from the first three
   * vertices of {@code face} using the cross product.
   */
  private double[] computeFaceNormal(Body body, int[] face) {
    Point3D p0 = body.pointAt(face[0]);
    Point3D p1 = body.pointAt(face[1]);
    Point3D p2 = body.pointAt(face[2]);
    double ax = p1.getX() - p0.getX(), ay = p1.getY() - p0.getY(), az = p1.getZ() - p0.getZ();
    double bx = p2.getX() - p0.getX(), by = p2.getY() - p0.getY(), bz = p2.getZ() - p0.getZ();
    return new double[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
  }

  /**
   * Computes a smooth shading value for vertex {@code vIdx} in face {@code faceIdx}.
   * Adjacent faces whose normals are within {@link #SMOOTH_THRESHOLD_COS} of the current face
   * contribute to the average, preserving hard edges.
   */
  private double computeCornerShade(Body body, int vIdx, int faceIdx,
      double[][] faceNormals, List<Integer>[] vertexFaces, float ao) {
    double ambient = AMBIENT * (1.0 - AO_STRENGTH * ao);
    double[] fn = faceNormals[faceIdx];
    double fnLen = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1] + fn[2] * fn[2]);
    if (fnLen == 0) return ambient;
    double nx = fn[0] / fnLen, ny = fn[1] / fnLen, nz = fn[2] / fnLen;

    double snx = nx, sny = ny, snz = nz;
    for (int fi2 : vertexFaces[vIdx]) {
      if (fi2 == faceIdx) continue;
      double[] fn2 = faceNormals[fi2];
      double fn2Len = Math.sqrt(fn2[0] * fn2[0] + fn2[1] * fn2[1] + fn2[2] * fn2[2]);
      if (fn2Len == 0) continue;
      double dot = nx * (fn2[0] / fn2Len) + ny * (fn2[1] / fn2Len) + nz * (fn2[2] / fn2Len);
      if (dot > SMOOTH_THRESHOLD_COS) {
        snx += fn2[0] / fn2Len;
        sny += fn2[1] / fn2Len;
        snz += fn2[2] / fn2Len;
      }
    }
    double len = Math.sqrt(snx * snx + sny * sny + snz * snz);
    return len > 0 ? Math.max(ambient, -snz / len) : ambient;
  }

  /**
   * Scanline Gouraud rasteriser. Fills the triangle defined by the three screen-space vertices
   * directly into {@code pixels} (the backing array of a {@code TYPE_INT_ARGB} BufferedImage),
   * interpolating the shade value {@code s} across each horizontal span.
   */
  private void drawGouraudTriangle(int[] pixels, int w, int h, Color colour,
      int x0, int y0, float s0,
      int x1, int y1, float s1,
      int x2, int y2, float s2) {
    if (y1 < y0) { int t=x0; x0=x1; x1=t; t=y0; y0=y1; y1=t; float ts=s0; s0=s1; s1=ts; }
    if (y2 < y0) { int t=x0; x0=x2; x2=t; t=y0; y0=y2; y2=t; float ts=s0; s0=s2; s2=ts; }
    if (y2 < y1) { int t=x1; x1=x2; x2=t; t=y1; y1=y2; y2=t; float ts=s1; s1=s2; s2=ts; }

    int totalH = y2 - y0;
    if (totalH == 0) return;

    int baseR = colour.getRed(), baseG = colour.getGreen(), baseB = colour.getBlue();

    for (int y = Math.max(0, y0); y <= Math.min(h - 1, y2); y++) {
      float alpha = (float) (y - y0) / totalH;
      int   xA = x0 + (int) ((x2 - x0) * alpha);
      float sA = s0 + (s2 - s0) * alpha;
      int   xB;
      float sB;
      if (y <= y1) {
        int segH = y1 - y0;
        float beta = segH == 0 ? 1f : (float) (y - y0) / segH;
        xB = x0 + (int) ((x1 - x0) * beta);
        sB = s0 + (s1 - s0) * beta;
      } else {
        int segH = y2 - y1;
        float beta = segH == 0 ? 1f : (float) (y - y1) / segH;
        xB = x1 + (int) ((x2 - x1) * beta);
        sB = s1 + (s2 - s1) * beta;
      }
      if (xA > xB) { int t=xA; xA=xB; xB=t; float ts=sA; sA=sB; sB=ts; }

      int spanW = xB - xA;
      int rowBase = y * w;
      for (int x = Math.max(0, xA); x <= Math.min(w - 1, xB); x++) {
        float t     = spanW == 0 ? 0.5f : (float) (x - xA) / spanW;
        float shade = sA + (sB - sA) * t;
        int r = Math.min(255, (int) (baseR * shade));
        int gv = Math.min(255, (int) (baseG * shade));
        int b = Math.min(255, (int) (baseB * shade));
        pixels[rowBase + x] = 0xFF000000 | (r << 16) | (gv << 8) | b;
      }
    }
  }

  private Color shadedColour(Color colour, double shade) {
    return new Color(
        Math.min(255, (int) (colour.getRed()   * shade)),
        Math.min(255, (int) (colour.getGreen() * shade)),
        Math.min(255, (int) (colour.getBlue()  * shade)));
  }
}
