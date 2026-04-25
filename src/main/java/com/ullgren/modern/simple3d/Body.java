package com.ullgren.modern.simple3d;

import java.awt.Color;
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
 * Use {@link #loadBody(String, Color)} to create instances. Use {@link Renderer} to draw them.
 */
public class Body {

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
  // Package-private geometry accessors (used by Renderer)
  // -------------------------------------------------------------------------

  int pointCount()          { return points.length; }
  int faceCount()           { return faces.length; }
  Point3D pointAt(int i)    { return points[i]; }
  int[]   faceAt(int i)     { return faces[i]; }

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

  /** Rotates all points by {@code angle} radians in the YZ plane. Negative angles rotate in reverse. */
  public void rotateYZ(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    for (Point3D p : points) {
      double t = p.y;
      p.y = p.y * cos - p.z * sin;
      p.z = t   * sin + p.z * cos;
    }
  }

  /** Rotates all points by {@code angle} radians in the ZX plane. Negative angles rotate in reverse. */
  public void rotateZX(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    for (Point3D p : points) {
      double t = p.z;
      p.z = p.z * cos - p.x * sin;
      p.x = t   * sin + p.x * cos;
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
