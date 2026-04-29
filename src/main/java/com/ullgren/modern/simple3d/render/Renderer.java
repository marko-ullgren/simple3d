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
import com.ullgren.modern.simple3d.render.texture.NoTexture;
import com.ullgren.modern.simple3d.render.texture.Texture;

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
  /** Surface texture applied per pixel during rasterisation. */
  private Texture texture = new NoTexture();
  /** When {@code true}, renders edges only (wireframe) instead of filled surfaces. */
  private boolean wireframeMode = false;

  /** A scan-line rendered triangle with per-corner Gouraud shading and object-space tex coords. */
  private record Triangle(int x0, int y0, int x1, int y1, int x2, int y2,
                           double avgZ, float s0, float s1, float s2,
                           float wx0, float wy0, float wz0,
                           float wx1, float wy1, float wz1,
                           float wx2, float wy2, float wz2) {}

  /** Attaches an effect that will be applied each frame after geometry rendering. */
  public void setEffect(Effect effect) {
    this.effect = effect;
  }

  /** Attaches a surface texture applied during rasterisation. */
  public void setTexture(Texture texture) {
    this.texture = texture;
  }

  /**
   * When {@code true} the body is rendered as a wireframe (edges only), like the vintage app.
   * Textures are ignored in wireframe mode.
   */
  public void setWireframeMode(boolean wireframeMode) {
    this.wireframeMode = wireframeMode;
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

    // Ensure hi-res buffer exists and is cleared.
    if (hiResImage == null || hiResImageW != hiW || hiResImageH != hiH) {
      hiResImage  = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_ARGB);
      hiResImageW = hiW;
      hiResImageH = hiH;
    } else {
      Arrays.fill(((DataBufferInt) hiResImage.getRaster().getDataBuffer()).getData(), 0);
    }

    if (wireframeMode) {
      Graphics2D hiG = hiResImage.createGraphics();
      hiG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      renderWireframe(body, sx, sy, sz, body.getColour(), hiG);
      hiG.dispose();
    } else {
      renderFilled(body, sx, sy, sz, hiW, hiH);
    }

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
   * Fan-triangulates a convex (or simply fan-triangulatable) polygon.
   * Returns face-local index triplets {@code [i0, i1, i2]}.
   */
  private static List<int[]> fanTriangulate(int[] face) {
    List<int[]> result = new ArrayList<>(face.length - 2);
    for (int i = 1; i < face.length - 1; i++) {
      result.add(new int[]{0, i, i + 1});
    }
    return result;
  }

  /**
   * Ear-clips a simple (possibly non-convex) polygon into triangles.
   * Triangulation is performed in screen space so no 3D projection is needed.
   * Returns face-local index triplets {@code [i0, i1, i2]}.
   */
  private static List<int[]> earClip(int[] face, int[] sx, int[] sy) {
    int n = face.length;
    // Compute signed area (2×) to determine screen-space winding (CW vs CCW).
    long area2 = 0;
    for (int i = 0; i < n; i++) {
      int j = (i + 1) % n;
      area2 += (long) sx[face[i]] * sy[face[j]] - (long) sx[face[j]] * sy[face[i]];
    }
    boolean isCCW = area2 > 0;

    List<Integer> rem = new ArrayList<>(n);
    for (int i = 0; i < n; i++) rem.add(i); // face-local indices

    List<int[]> result = new ArrayList<>(n - 2);
    int safety = n * n + n;

    while (rem.size() > 3 && safety-- > 0) {
      int rn = rem.size();
      boolean earFound = false;
      for (int i = 0; i < rn; i++) {
        int fi0 = rem.get((i - 1 + rn) % rn);
        int fi1 = rem.get(i);
        int fi2 = rem.get((i + 1) % rn);
        int ax = sx[face[fi0]], ay = sy[face[fi0]];
        int bx = sx[face[fi1]], by = sy[face[fi1]];
        int cx = sx[face[fi2]], cy = sy[face[fi2]];

        long cross = (long) (bx - ax) * (cy - ay) - (long) (by - ay) * (cx - ax);
        boolean convex = isCCW ? (cross >= 0) : (cross <= 0);
        if (!convex) continue;

        boolean isEar = true;
        for (int j = 0; j < rn && isEar; j++) {
          if (j == (i - 1 + rn) % rn || j == i || j == (i + 1) % rn) continue;
          int fj = rem.get(j);
          int px = sx[face[fj]], py = sy[face[fj]];
          if (pointInTriangle2D(ax, ay, bx, by, cx, cy, px, py)) isEar = false;
        }
        if (isEar) {
          result.add(new int[]{fi0, fi1, fi2});
          rem.remove(i);
          earFound = true;
          break;
        }
      }
      if (!earFound) {
        // Degenerate polygon — force-clip to avoid infinite loop.
        result.add(new int[]{rem.get(0), rem.get(1), rem.get(2)});
        rem.remove(1);
      }
    }
    if (rem.size() == 3) {
      result.add(new int[]{rem.get(0), rem.get(1), rem.get(2)});
    }
    return result;
  }

  private static boolean pointInTriangle2D(
      int ax, int ay, int bx, int by, int cx, int cy, int px, int py) {
    long d1 = edgeSign(px, py, ax, ay, bx, by);
    long d2 = edgeSign(px, py, bx, by, cx, cy);
    long d3 = edgeSign(px, py, cx, cy, ax, ay);
    boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
    boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
    return !(hasNeg && hasPos);
  }

  private static long edgeSign(int px, int py, int x1, int y1, int x2, int y2) {
    return (long) (px - x2) * (y1 - y2) - (long) (x1 - x2) * (py - y2);
  }

  /**
   * Draws all unique edges of {@code body} as anti-aliased lines.
   * Front edges (avgZ <= 0) are drawn in the body colour; back edges are drawn darker,
   * reproducing the vintage app's depth-cueing effect.
   */
  private void renderWireframe(Body body, int[] sx, int[] sy, double[] sz,
                               Color colour, Graphics2D g) {
    Color dimColour = colour.darker().darker();
    g.setStroke(new java.awt.BasicStroke(2f));

    java.util.HashSet<Long> drawn = new java.util.HashSet<>();
    int fc = body.faceCount();
    for (int fi = 0; fi < fc; fi++) {
      int[] face = body.faceAt(fi);
      for (int i = 0; i < face.length; i++) {
        int a = face[i], b = face[(i + 1) % face.length];
        long key = a < b ? ((long) a << 32) | b : ((long) b << 32) | a;
        if (!drawn.add(key)) continue;
        double avgZ = (sz[a] + sz[b]) / 2.0;
        g.setColor(avgZ > 0 ? dimColour : colour);
        g.drawLine(sx[a], sy[a], sx[b], sy[b]);
      }
    }
  }

  /** Filled (Gouraud + texture) rendering path, invoked when not in wireframe mode. */
  private void renderFilled(Body body, int[] sx, int[] sy, double[] sz, int hiW, int hiH) {
    int n  = body.pointCount();
    int fc = body.faceCount();

    double[][] faceNormals = new double[fc][];
    for (int fi = 0; fi < fc; fi++) {
      faceNormals[fi] = computeFaceNormal(body, body.faceAt(fi));
    }

    @SuppressWarnings("unchecked")
    List<Integer>[] vertexFaces = new List[n];
    for (int i = 0; i < n; i++) vertexFaces[i] = new ArrayList<>();
    for (int fi = 0; fi < fc; fi++) {
      for (int idx : body.faceAt(fi)) vertexFaces[idx].add(fi);
    }

    List<Triangle> sideTris = new ArrayList<>();
    Color colour = body.getColour();

    for (int fi = 0; fi < fc; fi++) {
      int[] face = body.faceAt(fi);
      double[] fn = faceNormals[fi];
      if (fn[2] >= 0) continue; // back-face cull

      float[] cs = new float[face.length];
      if (face.length > 4) {
        double fnLen = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1] + fn[2] * fn[2]);
        double shade = Math.max(AMBIENT, fnLen > 0 ? -fn[2] / fnLen : AMBIENT);
        double avgAO = 0;
        for (int idx : face) avgAO += body.getVertexAO(idx);
        avgAO /= face.length;
        shade = Math.max(AMBIENT * (1.0 - AO_STRENGTH * avgAO), shade);
        Arrays.fill(cs, (float) shade);
      } else {
        for (int ci = 0; ci < face.length; ci++) {
          cs[ci] = (float) computeCornerShade(body, face[ci], fi, faceNormals, vertexFaces,
              body.getVertexAO(face[ci]));
        }
      }

      List<int[]> tris = (face.length > 4) ? earClip(face, sx, sy) : fanTriangulate(face);
      for (int[] tri : tris) {
        int a = face[tri[0]], b = face[tri[1]], c = face[tri[2]];
        double avgZ = (sz[a] + sz[b] + sz[c]) / 3.0;
        double[] ta = body.getTexCoord(a);
        double[] tb = body.getTexCoord(b);
        double[] tc = body.getTexCoord(c);
        sideTris.add(new Triangle(sx[a], sy[a], sx[b], sy[b], sx[c], sy[c], avgZ,
            cs[tri[0]], cs[tri[1]], cs[tri[2]],
            (float) ta[0], (float) ta[1], (float) ta[2],
            (float) tb[0], (float) tb[1], (float) tb[2],
            (float) tc[0], (float) tc[1], (float) tc[2]));
      }
    }

    int[] hiPixels = ((DataBufferInt) hiResImage.getRaster().getDataBuffer()).getData();
    sideTris.sort((t1, t2) -> Double.compare(t2.avgZ(), t1.avgZ()));
    for (Triangle t : sideTris) {
      drawGouraudTriangle(hiPixels, hiW, hiH, colour,
          t.x0(), t.y0(), t.s0(), t.wx0(), t.wy0(), t.wz0(),
          t.x1(), t.y1(), t.s1(), t.wx1(), t.wy1(), t.wz1(),
          t.x2(), t.y2(), t.s2(), t.wx2(), t.wy2(), t.wz2());
    }
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
   * interpolating shade and object-space texture coordinates across each horizontal span.
   */
  private void drawGouraudTriangle(int[] pixels, int w, int h, Color colour,
      int x0, int y0, float s0, float wx0, float wy0, float wz0,
      int x1, int y1, float s1, float wx1, float wy1, float wz1,
      int x2, int y2, float s2, float wx2, float wy2, float wz2) {
    // Sort vertices by Y (ascending).
    if (y1 < y0) {
      int ti=x0; x0=x1; x1=ti; ti=y0; y0=y1; y1=ti;
      float ts=s0; s0=s1; s1=ts;
      ts=wx0; wx0=wx1; wx1=ts; ts=wy0; wy0=wy1; wy1=ts; ts=wz0; wz0=wz1; wz1=ts;
    }
    if (y2 < y0) {
      int ti=x0; x0=x2; x2=ti; ti=y0; y0=y2; y2=ti;
      float ts=s0; s0=s2; s2=ts;
      ts=wx0; wx0=wx2; wx2=ts; ts=wy0; wy0=wy2; wy2=ts; ts=wz0; wz0=wz2; wz2=ts;
    }
    if (y2 < y1) {
      int ti=x1; x1=x2; x2=ti; ti=y1; y1=y2; y2=ti;
      float ts=s1; s1=s2; s2=ts;
      ts=wx1; wx1=wx2; wx2=ts; ts=wy1; wy1=wy2; wy2=ts; ts=wz1; wz1=wz2; wz2=ts;
    }

    int totalH = y2 - y0;
    if (totalH == 0) return;

    int baseR = colour.getRed(), baseG = colour.getGreen(), baseB = colour.getBlue();

    for (int y = Math.max(0, y0); y <= Math.min(h - 1, y2); y++) {
      float alpha = (float) (y - y0) / totalH;
      int   xA  = x0  + (int) ((x2  - x0)  * alpha);
      float sA  = s0  + (s2  - s0)  * alpha;
      float wxA = wx0 + (wx2 - wx0) * alpha;
      float wyA = wy0 + (wy2 - wy0) * alpha;
      float wzA = wz0 + (wz2 - wz0) * alpha;
      int   xB;
      float sB, wxB, wyB, wzB;
      if (y <= y1) {
        int segH = y1 - y0;
        float beta = segH == 0 ? 1f : (float) (y - y0) / segH;
        xB  = x0  + (int) ((x1  - x0)  * beta);
        sB  = s0  + (s1  - s0)  * beta;
        wxB = wx0 + (wx1 - wx0) * beta;
        wyB = wy0 + (wy1 - wy0) * beta;
        wzB = wz0 + (wz1 - wz0) * beta;
      } else {
        int segH = y2 - y1;
        float beta = segH == 0 ? 1f : (float) (y - y1) / segH;
        xB  = x1  + (int) ((x2  - x1)  * beta);
        sB  = s1  + (s2  - s1)  * beta;
        wxB = wx1 + (wx2 - wx1) * beta;
        wyB = wy1 + (wy2 - wy1) * beta;
        wzB = wz1 + (wz2 - wz1) * beta;
      }
      if (xA > xB) {
        int ti=xA; xA=xB; xB=ti;
        float ts=sA; sA=sB; sB=ts;
        ts=wxA; wxA=wxB; wxB=ts;
        ts=wyA; wyA=wyB; wyB=ts;
        ts=wzA; wzA=wzB; wzB=ts;
      }

      int spanW = xB - xA;
      int rowBase = y * w;
      for (int x = Math.max(0, xA); x <= Math.min(w - 1, xB); x++) {
        float t     = spanW == 0 ? 0.5f : (float) (x - xA) / spanW;
        float shade = sA  + (sB  - sA)  * t;
        float wx    = wxA + (wxB - wxA) * t;
        float wy    = wyA + (wyB - wyA) * t;
        float wz    = wzA + (wzB - wzA) * t;
        pixels[rowBase + x] = texture.applyPacked(wx, wy, wz, baseR, baseG, baseB, shade);
      }
    }
  }
}
