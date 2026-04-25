package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A three-dimensional solid body defined by a set of points and polygonal faces.
 * <p>
 * The coordinate origin is at the centre of the drawing area. The positive x-axis points right,
 * the positive y-axis points up, and the positive z-axis points away from the viewer.
 * <p>
 * Use the static factory methods {@link #mu(Color)} and {@link #cube(Color)} to create instances.
 */
public class Body {

  private static final double PROJECTION_FACTOR = 0.001;
  private static final double AMBIENT = 0.15;
  /** Adjacent faces whose normalised normals have a dot product above this value share smooth shading. */
  private static final double SMOOTH_THRESHOLD_COS = Math.cos(Math.toRadians(60.0));

  private final Point3D[] points;
  private final int[][] faces;
  private Color colour;

  /** Cached off-screen buffer; recreated only when the canvas size changes. */
  private BufferedImage canvasImage;
  private int canvasImageW, canvasImageH;

  private Body(Point3D[] points, int[][] faces, Color colour) {
    this.points = points;
    this.faces = faces;
    this.colour = colour;
  }

  public void setColour(Color newColour) {
    this.colour = newColour;
  }

  public Color getColour() {
    return colour;
  }

  // -------------------------------------------------------------------------
  // Drawing
  // -------------------------------------------------------------------------

  /** A scan-line rendered triangle with per-corner Gouraud shading values. */
  private record Triangle(int x0, int y0, int x1, int y1, int x2, int y2,
                           double avgZ, float s0, float s1, float s2) {}

  private record CapPolygon(int[] px, int[] py, double avgZ, Color colour) {
    void draw(Graphics g) {
      g.setColor(colour);
      g.fillPolygon(px, py, px.length);
    }
  }

  /**
   * Draws the body using Gouraud shading for side faces and flat shading for cap faces.
   * <p>
   * Side faces (≤ 4 vertices) are fan-triangulated and rendered with per-vertex shading
   * interpolated via scanline rasterisation into an off-screen buffer. Vertex normals are
   * smoothed across adjacent faces whose normals are within 60° of each other, preserving
   * hard edges between caps and sides.
   * <p>
   * Cap faces (> 4 vertices) are drawn last with flat shading via {@link Graphics#fillPolygon},
   * which handles non-convex polygons correctly.
   *
   * @param canvasW width of the canvas in pixels (used to size the off-screen buffer)
   * @param canvasH height of the canvas in pixels
   */
  public void draw(Graphics g, int centerX, int centerY, double scale, int canvasW, int canvasH) {
    int[] sx = new int[points.length];
    int[] sy = new int[points.length];
    double[] sz = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      double f = 1.0 - PROJECTION_FACTOR * points[i].z;
      sx[i] = (int) (scale * points[i].x * f) + centerX;
      sy[i] = (int) (scale * points[i].y * f) + centerY;
      sz[i] = points[i].z;
    }

    // Pre-compute per-face normals.
    double[][] faceNormals = new double[faces.length][];
    for (int fi = 0; fi < faces.length; fi++) {
      faceNormals[fi] = computeFaceNormal(faces[fi]);
    }

    // Build vertex-to-face adjacency for smooth normal computation.
    @SuppressWarnings("unchecked")
    List<Integer>[] vertexFaces = new List[points.length];
    for (int i = 0; i < points.length; i++) vertexFaces[i] = new ArrayList<>();
    for (int fi = 0; fi < faces.length; fi++) {
      for (int idx : faces[fi]) vertexFaces[idx].add(fi);
    }

    List<Triangle>   sideTris = new ArrayList<>();
    List<CapPolygon> capPolys = new ArrayList<>();

    for (int fi = 0; fi < faces.length; fi++) {
      int[] face = faces[fi];
      double[] fn = faceNormals[fi];
      if (fn[2] >= 0) continue; // back-face cull

      if (face.length > 4) {
        // Cap face: flat shading.
        double fnLen = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1] + fn[2] * fn[2]);
        double shade = Math.max(AMBIENT, fnLen > 0 ? -fn[2] / fnLen : AMBIENT);
        Color faceColour = shadedColour(shade);
        double avgZ = 0;
        for (int idx : face) avgZ += sz[idx];
        avgZ /= face.length;
        int[] px = new int[face.length];
        int[] py = new int[face.length];
        for (int i = 0; i < face.length; i++) { px[i] = sx[face[i]]; py[i] = sy[face[i]]; }
        capPolys.add(new CapPolygon(px, py, avgZ, faceColour));
      } else {
        // Side face: Gouraud shading — compute per-corner smooth shade.
        float[] cs = new float[face.length];
        for (int ci = 0; ci < face.length; ci++) {
          cs[ci] = (float) computeCornerShade(face[ci], fi, faceNormals, vertexFaces);
        }
        for (int i = 1; i < face.length - 1; i++) {
          int a = face[0], b = face[i], c = face[i + 1];
          double avgZ = (sz[a] + sz[b] + sz[c]) / 3.0;
          sideTris.add(new Triangle(sx[a], sy[a], sx[b], sy[b], sx[c], sy[c], avgZ,
              cs[0], cs[i], cs[i + 1]));
        }
      }
    }

    // Render Gouraud triangles into an off-screen buffer (recreated only on resize).
    if (canvasImage == null || canvasImageW != canvasW || canvasImageH != canvasH) {
      canvasImage  = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
      canvasImageW = canvasW;
      canvasImageH = canvasH;
    } else {
      // Clear to transparent so the panel background shows through.
      int[] pix = ((DataBufferInt) canvasImage.getRaster().getDataBuffer()).getData();
      java.util.Arrays.fill(pix, 0);
    }
    int[] pixels = ((DataBufferInt) canvasImage.getRaster().getDataBuffer()).getData();

    sideTris.sort((t1, t2) -> Double.compare(t2.avgZ(), t1.avgZ()));
    for (Triangle t : sideTris) {
      drawGouraudTriangle(pixels, canvasW, canvasH,
          t.x0(), t.y0(), t.s0(),
          t.x1(), t.y1(), t.s1(),
          t.x2(), t.y2(), t.s2());
    }
    g.drawImage(canvasImage, 0, 0, null);

    capPolys.sort((a, b) -> Double.compare(b.avgZ(), a.avgZ()));
    capPolys.forEach(p -> p.draw(g));
  }

  /**
   * Returns the (unnormalised) face normal as [nx, ny, nz] computed from the first three
   * vertices of {@code face} using the cross product.
   */
  private double[] computeFaceNormal(int[] face) {
    double ax = points[face[1]].x - points[face[0]].x;
    double ay = points[face[1]].y - points[face[0]].y;
    double az = points[face[1]].z - points[face[0]].z;
    double bx = points[face[2]].x - points[face[0]].x;
    double by = points[face[2]].y - points[face[0]].y;
    double bz = points[face[2]].z - points[face[0]].z;
    return new double[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
  }

  /**
   * Computes a smooth shading value for vertex {@code vIdx} in the context of face
   * {@code faceIdx}. Adjacent faces whose normals are within {@link #SMOOTH_THRESHOLD_COS}
   * radians of the current face contribute to the average, preserving hard edges.
   */
  private double computeCornerShade(int vIdx, int faceIdx,
      double[][] faceNormals, List<Integer>[] vertexFaces) {
    double[] fn = faceNormals[faceIdx];
    double fnLen = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1] + fn[2] * fn[2]);
    if (fnLen == 0) return AMBIENT;
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
    return len > 0 ? Math.max(AMBIENT, -snz / len) : AMBIENT;
  }

  /**
   * Scanline Gouraud rasteriser. Fills the triangle defined by the three screen-space vertices
   * directly into {@code pixels} (the backing array of a {@code TYPE_INT_ARGB} BufferedImage),
   * interpolating the shade value {@code s} across each horizontal span.
   * All coordinates are clamped to image bounds.
   */
  private void drawGouraudTriangle(int[] pixels, int w, int h,
      int x0, int y0, float s0,
      int x1, int y1, float s1,
      int x2, int y2, float s2) {
    // Sort vertices by y ascending.
    if (y1 < y0) { int t=x0; x0=x1; x1=t; t=y0; y0=y1; y1=t; float ts=s0; s0=s1; s1=ts; }
    if (y2 < y0) { int t=x0; x0=x2; x2=t; t=y0; y0=y2; y2=t; float ts=s0; s0=s2; s2=ts; }
    if (y2 < y1) { int t=x1; x1=x2; x2=t; t=y1; y1=y2; y2=t; float ts=s1; s1=s2; s2=ts; }

    int totalH = y2 - y0;
    if (totalH == 0) return;

    int baseR = colour.getRed(), baseG = colour.getGreen(), baseB = colour.getBlue();

    for (int y = Math.max(0, y0); y <= Math.min(h - 1, y2); y++) {
      float alpha = (float) (y - y0) / totalH;
      // Left edge: always v0 → v2.
      int   xA = x0 + (int) ((x2 - x0) * alpha);
      float sA = s0 + (s2 - s0) * alpha;
      // Right edge: v0 → v1 for first half, v1 → v2 for second half.
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
        float t   = spanW == 0 ? 0.5f : (float) (x - xA) / spanW;
        float shade = sA + (sB - sA) * t;
        int r = Math.min(255, (int) (baseR * shade));
        int gv = Math.min(255, (int) (baseG * shade));
        int b = Math.min(255, (int) (baseB * shade));
        pixels[rowBase + x] = 0xFF000000 | (r << 16) | (gv << 8) | b;
      }
    }
  }

  private Color shadedColour(double shade) {
    return new Color(
        Math.min(255, (int) (colour.getRed()   * shade)),
        Math.min(255, (int) (colour.getGreen() * shade)),
        Math.min(255, (int) (colour.getBlue()  * shade)));
  }

  // -------------------------------------------------------------------------
  // Rotation
  // -------------------------------------------------------------------------

  public void rotateXZ() { for (Point3D p : points) p.rotateXZ(); }
  public void rotateYZ() { for (Point3D p : points) p.rotateYZ(); }
  public void rotateZX() { for (Point3D p : points) p.rotateZX(); }
  public void rotateZY() { for (Point3D p : points) p.rotateZY(); }

  /** Rotates all points by {@code angle} radians in the XZ plane. Negative angles rotate in reverse. */
  public void rotateXZ(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    for (Point3D p : points) {
      double t = p.x;
      p.x = p.x * cos - p.z * sin;
      p.z = t   * sin + p.z * cos;
    }
  }

  /** Rotates all points by {@code angle} radians in the ZY plane. Negative angles rotate in reverse. */
  public void rotateZY(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    for (Point3D p : points) {
      double t = p.z;
      p.z = p.z * cos - p.y * sin;
      p.y = t   * sin + p.y * cos;
    }
  }

  /** For testing only — returns the number of points in this body. */
  int pointCount() { return points.length; }

  /** For testing only — returns the number of faces in this body. */
  int faceCount() { return faces.length; }

  // -------------------------------------------------------------------------
  // Shape loading
  // -------------------------------------------------------------------------

  /**
   * Loads a body from a classpath resource in the {@code .body} file format.
   * <p>
   * Format:
   * <pre>
   * points
   * x y z
   * ...
   *
   * faces
   * i0 i1 i2 ...
   * ...
   * </pre>
   * Lines starting with {@code #} and blank lines are ignored.
   *
   * @throws UncheckedIOException   if the resource cannot be read
   * @throws IllegalArgumentException if the file is malformed
   */
  public static Body loadBody(String resource, Color colour) {
    InputStream stream = Body.class.getResourceAsStream(resource);
    if (stream == null) {
      throw new IllegalArgumentException("Shape resource not found: " + resource);
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8))) {

      List<Point3D> points = new ArrayList<>();
      List<int[]>   faces  = new ArrayList<>();
      String section = null;
      String line;
      int lineNum = 0;

      while ((line = reader.readLine()) != null) {
        lineNum++;
        line = line.strip();
        if (line.isEmpty() || line.startsWith("#")) continue;

        if (line.equals("points") || line.equals("faces")) {
          section = line;
          continue;
        }
        if (section == null) {
          throw new IllegalArgumentException(
              resource + ":" + lineNum + ": data before any section header");
        }

        String[] tokens = line.split("\\s+");
        if (section.equals("points")) {
          if (tokens.length != 3) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": point must have exactly 3 coordinates");
          }
          points.add(new Point3D(
              Double.parseDouble(tokens[0]),
              Double.parseDouble(tokens[1]),
              Double.parseDouble(tokens[2])));
        } else {
          if (tokens.length < 3) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": face must have at least 3 indices");
          }
          int[] indices = new int[tokens.length];
          for (int i = 0; i < tokens.length; i++) {
            indices[i] = Integer.parseInt(tokens[i]);
            if (indices[i] < 0 || indices[i] >= points.size()) {
              throw new IllegalArgumentException(
                  resource + ":" + lineNum + ": face index " + indices[i]
                  + " out of range (0.." + (points.size() - 1) + ")");
            }
          }
          faces.add(indices);
        }
      }

      if (points.isEmpty()) {
        throw new IllegalArgumentException(resource + ": no points defined");
      }
      if (faces.isEmpty()) {
        throw new IllegalArgumentException(resource + ": no faces defined");
      }
      return new Body(points.toArray(new Point3D[0]), faces.toArray(new int[0][]), colour);

    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read shape resource: " + resource, e);
    }
  }
}
