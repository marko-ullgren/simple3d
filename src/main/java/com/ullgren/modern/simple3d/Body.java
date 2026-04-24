package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;

/**
 * A three-dimensional solid body defined by a set of points and polygonal faces.
 * <p>
 * The coordinate origin is at the centre of the drawing area. The positive x-axis points right,
 * the positive y-axis points up, and the positive z-axis points away from the viewer.
 */
public class Body {

  public static final double PROJECTION_FACTOR = 0.0010;

  /** Minimum ambient light level so back faces are never completely black. */
  private static final double AMBIENT = 0.15;

  private Point3D[] points;
  /**
   * Each element is an ordered list of point indices defining one polygonal face.
   * Winding must be consistent so that the cross-product of the first two edge vectors
   * yields an outward-facing normal.
   */
  private int[][] faces;
  private Color colour;

  public Body(Point3D[] points, int[][] faces, Color colour) {
    this.points = points;
    this.faces = faces;
    this.colour = colour;
  }

  public Body(Body source) {
    this.points = source.points;
    this.faces = source.faces;
    this.colour = source.colour;
  }

  public void setColour(Color newColour) {
    this.colour = newColour;
  }

  /**
   * Draws the body using the painter's algorithm with flat shading.
   * Faces are sorted back-to-front and filled with a diffuse+ambient colour
   * derived from their angle to the viewer.
   *
   * @param g       Graphics context to draw on
   * @param centerX x-coordinate of the canvas centre
   * @param centerY y-coordinate of the canvas centre
   * @param scale   scale factor relative to the default window size
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

    // Two draw queues:
    //  - sideTris: convex quads fan-triangulated for fine-grained z-sorting
    //  - capPolys: non-convex cap polygons drawn AFTER all side triangles
    // Drawing caps last is geometrically correct for extruded solids: the cap face
    // shares its front edge with every adjacent side face, so it is always at least
    // as close to the viewer as any overlapping side face.
    java.util.List<double[]> sideTris = new java.util.ArrayList<>();
    java.util.List<double[]> capPolys = new java.util.ArrayList<>();

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
      double r  = colour.getRed()   * shade;
      double gr = colour.getGreen() * shade;
      double bl = colour.getBlue()  * shade;

      if (face.length > 4) {
        // Non-convex cap polygon: kept whole so fillPolygon handles concave shape
        double avgZ = 0;
        for (int idx : face) avgZ += sz[idx];
        avgZ /= face.length;
        double[] item = new double[face.length * 2 + 4];
        for (int i = 0; i < face.length; i++) {
          item[i * 2]     = sx[face[i]];
          item[i * 2 + 1] = sy[face[i]];
        }
        int base = face.length * 2;
        item[base] = avgZ; item[base + 1] = r; item[base + 2] = gr; item[base + 3] = bl;
        capPolys.add(item);
      } else {
        // Convex face (quad): fan-triangulate for fine-grained depth sorting
        for (int i = 1; i < face.length - 1; i++) {
          int a = face[0], b = face[i], c = face[i + 1];
          double avgZ = (sz[a] + sz[b] + sz[c]) / 3.0;
          sideTris.add(new double[]{sx[a], sy[a], sx[b], sy[b], sx[c], sy[c], avgZ, r, gr, bl});
        }
      }
    }

    // 1. Draw side triangles back-to-front
    sideTris.sort((t1, t2) -> Double.compare(t2[6], t1[6]));
    for (double[] t : sideTris) {
      g.setColor(new Color(Math.min(255, (int) t[7]), Math.min(255, (int) t[8]), Math.min(255, (int) t[9])));
      g.fillPolygon(new int[]{(int) t[0], (int) t[2], (int) t[4]},
                    new int[]{(int) t[1], (int) t[3], (int) t[5]}, 3);
    }

    // 2. Draw cap polygons last — always on top of any side face they overlap
    capPolys.sort((a, b) -> Double.compare(b[b.length - 4], a[a.length - 4]));
    for (double[] item : capPolys) {
      int n = (item.length - 4) / 2;
      int base = n * 2;
      g.setColor(new Color(Math.min(255, (int) item[base + 1]), Math.min(255, (int) item[base + 2]), Math.min(255, (int) item[base + 3])));
      int[] polyX = new int[n], polyY = new int[n];
      for (int i = 0; i < n; i++) { polyX[i] = (int) item[i * 2]; polyY[i] = (int) item[i * 2 + 1]; }
      g.fillPolygon(polyX, polyY, n);
    }
  }

  public void rotateXZ() { for (Point3D p : points) p.rotateXZ(); }
  public void rotateYZ() { for (Point3D p : points) p.rotateYZ(); }
  public void rotateZX() { for (Point3D p : points) p.rotateZX(); }
  public void rotateZY() { for (Point3D p : points) p.rotateZY(); }

  public Color getColour() { return colour; }

}

