package com.ullgren.modern.simple3d.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A three-dimensional solid body defined by a set of points and polygonal faces.
 * <p>
 * The coordinate origin is at the centre of the drawing area. The positive x-axis points right,
 * the positive y-axis points up, and the positive z-axis points away from the viewer.
 * <p>
 * Use {@link BodyLoader#load(String, Color)} to create instances. Use {@link Renderer} to draw them.
 */
public class Body {

  private final Point3D[] points;
  private final int[][]   faces;
  private Color   colour;
  /** Per-vertex ambient occlusion factors, baked once at construction time. */
  private final float[] vertexAO;
  /**
   * Object-space coordinates used for texture sampling.
   * Initialised from the point positions at construction time and <em>re-baked</em> by
   * {@link BodyLoader} after all orientation steps have been applied, so that the coords
   * are stable during animation.
   */
  private final double[][] texCoords;

  /**
   * Package-private constructor used by {@link BodyLoader}.
   * Bakes ambient occlusion immediately so the invariant {@code vertexAO.length == points.length}
   * always holds.
   */
  Body(Point3D[] points, int[][] faces, Color colour) {
    this.points    = points;
    this.faces     = faces;
    this.colour    = colour;
    this.vertexAO  = computeAO(points, faces);
    this.texCoords = new double[points.length][3];
    snapshotTexCoords();
  }

  /**
   * Snapshots the current point positions into {@link #texCoords}.
   * Called by {@link BodyLoader} after orientation steps so the texture origin is the
   * post-orientation rest pose, not the pre-rotation construction state.
   */
  void bakeTextureCoords() {
    snapshotTexCoords();
  }

  private void snapshotTexCoords() {
    for (int i = 0; i < points.length; i++) {
      texCoords[i][0] = points[i].getX();
      texCoords[i][1] = points[i].getY();
      texCoords[i][2] = points[i].getZ();
    }
  }

  /**
   * Returns the object-space texture coordinate for vertex {@code i} as {@code [x, y, z]}.
   * These coordinates are fixed at load time and do not change during animation.
   */
  public double[] getTexCoord(int i) {
    return texCoords[i];
  }

  public void setColour(Color newColour) {
    this.colour = newColour;
  }

  public Color getColour() {
    return colour;
  }

  // -------------------------------------------------------------------------
  // Geometry accessors (used by Renderer)
  // -------------------------------------------------------------------------

  public int     pointCount()       { return points.length; }
  public int     faceCount()        { return faces.length; }
  public Point3D pointAt(int i)     { return points[i]; }
  public int[]   faceAt(int i)      { return faces[i]; }

  /**
   * Returns the baked ambient-occlusion factor for vertex {@code i}.
   * 0 = fully lit (flat surface); approaching 1 = heavily occluded (tight concave corner).
   */
  public float getVertexAO(int i)   { return vertexAO[i]; }

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
  // Ambient occlusion — baked once at construction time
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
  private static float[] computeAO(Point3D[] points, int[][] faces) {
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
    float[] ao = new float[n];
    for (int v = 0; v < n; v++) {
      List<Integer> adj = adjFaces[v];
      if (adj.isEmpty()) continue;
      double sx = 0, sy = 0, sz = 0;
      for (int fi : adj) { sx += fn[fi][0]; sy += fn[fi][1]; sz += fn[fi][2]; }
      double meanLen = Math.sqrt(sx * sx + sy * sy + sz * sz) / adj.size();
      ao[v] = (float) Math.max(0.0, 1.0 - meanLen);
    }
    return ao;
  }
}
