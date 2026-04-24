package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;
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

  private static final double PROJECTION_FACTOR = 0.0010;
  private static final double AMBIENT = 0.15;

  private final Point3D[] points;
  private final int[][] faces;
  private Color colour;

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

  private record Triangle(int x0, int y0, int x1, int y1, int x2, int y2,
                           double avgZ, Color colour) {
    void draw(Graphics g) {
      g.setColor(colour);
      g.fillPolygon(new int[]{x0, x1, x2}, new int[]{y0, y1, y2}, 3);
    }
  }

  private record CapPolygon(int[] px, int[] py, double avgZ, Color colour) {
    void draw(Graphics g) {
      g.setColor(colour);
      g.fillPolygon(px, py, px.length);
    }
  }

  /**
   * Draws the body using back-face culling and the painter's algorithm with flat shading.
   * Convex side faces are fan-triangulated and sorted back-to-front. Non-convex cap faces
   * (more than 4 vertices) are drawn last — geometrically correct for extruded solids because
   * the cap shares its front edge with every adjacent side face and is always at least as close
   * to the viewer as any side face it overlaps.
   */
  public void draw(Graphics g, int centerX, int centerY, double scale) {
    int[] sx = new int[points.length];
    int[] sy = new int[points.length];
    double[] sz = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      double f = 1.0 - PROJECTION_FACTOR * points[i].z;
      sx[i] = (int) (scale * points[i].x * f) + centerX;
      sy[i] = (int) (scale * points[i].y * f) + centerY;
      sz[i] = points[i].z;
    }

    List<Triangle>  sideTris = new ArrayList<>();
    List<CapPolygon> capPolys = new ArrayList<>();

    for (int[] face : faces) {
      double ax = points[face[1]].x - points[face[0]].x;
      double ay = points[face[1]].y - points[face[0]].y;
      double az = points[face[1]].z - points[face[0]].z;
      double bx = points[face[2]].x - points[face[0]].x;
      double by = points[face[2]].y - points[face[0]].y;
      double bz = points[face[2]].z - points[face[0]].z;
      double nx = ay * bz - az * by;
      double ny = az * bx - ax * bz;
      double nz = ax * by - ay * bx;

      if (nz >= 0) continue; // back-face cull

      double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
      double shade = Math.max(AMBIENT, nLen > 0 ? -nz / nLen : AMBIENT);
      Color faceColour = shadedColour(shade);

      if (face.length > 4) {
        double avgZ = 0;
        for (int idx : face) avgZ += sz[idx];
        avgZ /= face.length;
        int[] px = new int[face.length];
        int[] py = new int[face.length];
        for (int i = 0; i < face.length; i++) { px[i] = sx[face[i]]; py[i] = sy[face[i]]; }
        capPolys.add(new CapPolygon(px, py, avgZ, faceColour));
      } else {
        for (int i = 1; i < face.length - 1; i++) {
          int a = face[0], b = face[i], c = face[i + 1];
          double avgZ = (sz[a] + sz[b] + sz[c]) / 3.0;
          sideTris.add(new Triangle(sx[a], sy[a], sx[b], sy[b], sx[c], sy[c], avgZ, faceColour));
        }
      }
    }

    sideTris.sort((t1, t2) -> Double.compare(t2.avgZ(), t1.avgZ()));
    sideTris.forEach(t -> t.draw(g));

    capPolys.sort((a, b) -> Double.compare(b.avgZ(), a.avgZ()));
    capPolys.forEach(p -> p.draw(g));
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
