package com.ullgren.modern.simple3d.model;

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
  private final int[][]   faces;
  private Color   colour;
  /** Per-vertex ambient occlusion factors, baked once at load time. */
  private float[] vertexAO;

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

  public int     pointCount()       { return points.length; }
  public int     faceCount()        { return faces.length; }
  public Point3D pointAt(int i)     { return points[i]; }
  public int[]   faceAt(int i)      { return faces[i]; }

  /**
   * Returns the baked ambient-occlusion factor for vertex {@code i}.
   * 0 = fully lit (flat surface); approaching 1 = heavily occluded (tight concave corner).
   */
  public float getVertexAO(int i)   { return vertexAO == null ? 0f : vertexAO[i]; }

  // -------------------------------------------------------------------------
  // Rotation
  // -------------------------------------------------------------------------

  public void rotateXZ() { for (Point3D p : points) p.rotateXZ(); }
  public void rotateYZ() { for (Point3D p : points) p.rotateYZ(); }
  public void rotateZX() { for (Point3D p : points) p.rotateZX(); }
  public void rotateZY() { for (Point3D p : points) p.rotateZY(); }

  /** Rotates all points by {@code angle} radians in the XZ plane. Negative angles rotate in reverse. */
  public void rotateXZ(double angle) {
    for (Point3D p : points) p.rotateXZ(angle);
  }

  /** Rotates all points by {@code angle} radians in the YZ plane. Negative angles rotate in reverse. */
  public void rotateYZ(double angle) {
    for (Point3D p : points) p.rotateYZ(angle);
  }

  /** Rotates all points by {@code angle} radians in the ZX plane. Negative angles rotate in reverse. */
  public void rotateZX(double angle) {
    for (Point3D p : points) p.rotateZX(angle);
  }

  /** Rotates all points by {@code angle} radians in the ZY plane. Negative angles rotate in reverse. */
  public void rotateZY(double angle) {
    for (Point3D p : points) p.rotateZY(angle);
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
      Body body = new Body(points.toArray(new Point3D[0]), faces.toArray(new int[0][]), colour);
      body.precomputeAO();
      return body;

    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read shape resource: " + resource, e);
    }
  }

  // -------------------------------------------------------------------------
  // Ambient occlusion — baked once at load time
  // -------------------------------------------------------------------------

  /**
   * Bakes per-vertex ambient occlusion by measuring how much adjacent face normals diverge.
   * <p>
   * For each vertex the normalised face normals of all adjacent faces are averaged. When all
   * normals agree (flat surface) the mean vector has length 1 → AO = 0. When normals diverge
   * (concave corner or crease) the mean vector shrinks → AO approaches 1.
   * <p>
   * Rotation-invariant: the relative angles between faces don't change as the body spins.
   */
  private void precomputeAO() {
    int n  = points.length;
    int fc = faces.length;

    // Compute normalised per-face normals from the first two edges of each face.
    double[][] fn = new double[fc][3];
    for (int fi = 0; fi < fc; fi++) {
      int[] face = faces[fi];
      double ax = points[face[1]].getX() - points[face[0]].getX();
      double ay = points[face[1]].getY() - points[face[0]].getY();
      double az = points[face[1]].getZ() - points[face[0]].getZ();
      double bx = points[face[2]].getX() - points[face[0]].getX();
      double by = points[face[2]].getY() - points[face[0]].getY();
      double bz = points[face[2]].getZ() - points[face[0]].getZ();
      double nx = ay * bz - az * by;
      double ny = az * bx - ax * bz;
      double nz = ax * by - ay * bx;
      double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (len > 0) { fn[fi][0] = nx / len; fn[fi][1] = ny / len; fn[fi][2] = nz / len; }
    }

    // Build vertex-to-face adjacency.
    @SuppressWarnings("unchecked")
    List<Integer>[] adjFaces = new List[n];
    for (int i = 0; i < n; i++) adjFaces[i] = new ArrayList<>();
    for (int fi = 0; fi < fc; fi++) {
      for (int idx : faces[fi]) adjFaces[idx].add(fi);
    }

    // AO = 1 − |mean(adjacent normalised face normals)|.
    vertexAO = new float[n];
    for (int v = 0; v < n; v++) {
      List<Integer> adj = adjFaces[v];
      if (adj.isEmpty()) continue;
      double sx = 0, sy = 0, sz = 0;
      for (int fi : adj) { sx += fn[fi][0]; sy += fn[fi][1]; sz += fn[fi][2]; }
      double meanLen = Math.sqrt(sx * sx + sy * sy + sz * sz) / adj.size();
      vertexAO[v] = (float) Math.max(0.0, 1.0 - meanLen);
    }
  }
}
