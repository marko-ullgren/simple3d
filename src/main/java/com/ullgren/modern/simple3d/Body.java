package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;
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

  /** Creates the MU logo body with the given colour. */
  public static Body mu(Color colour) {
    return new Body(muPoints(), muFaces(), colour);
  }

  /** Creates a cube body with the given colour. */
  public static Body cube(Color colour) {
    return new Body(cubePoints(), cubeFaces(), colour);
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

  // -------------------------------------------------------------------------
  // Shape definitions
  // -------------------------------------------------------------------------

  private static Point3D[] muPoints() {
    Point3D[] p = new Point3D[36];
    p[0]  = new Point3D(-150, -90, 25);
    p[1]  = new Point3D( -90, -90, 25);
    p[2]  = new Point3D( -90,  30, 25);
    p[3]  = new Point3D( -60,   0, 25);
    p[4]  = new Point3D( -30,  30, 25);
    p[5]  = new Point3D( -30, -60, 25);
    p[6]  = new Point3D(   0, -90, 25);
    p[7]  = new Point3D(  90, -90, 25);
    p[8]  = new Point3D( 120, -60, 25);
    p[9]  = new Point3D( 120,  90, 25);
    p[10] = new Point3D(  60,  90, 25);
    p[11] = new Point3D(  60, -30, 25);
    p[12] = new Point3D(  30, -30, 25);
    p[13] = new Point3D(  30,  90, 25);
    p[14] = new Point3D( -30,  90, 25);
    p[15] = new Point3D( -60,  60, 25);
    p[16] = new Point3D( -90,  90, 25);
    p[17] = new Point3D(-150,  90, 25);
    for (int i = 18; i < 36; i++) {
      p[i] = new Point3D(p[i - 18]);
      p[i].z = -25;
    }
    return p;
  }

  /**
   * Face winding is chosen so that the cross-product of the first two edge vectors
   * yields an outward-facing normal.
   * <ul>
   *   <li>Back face  (z=+25): normal +z, away from viewer</li>
   *   <li>Front face (z=-25): normal -z, toward viewer</li>
   *   <li>18 side quads connecting corresponding front/back edges</li>
   * </ul>
   */
  private static int[][] muFaces() {
    int[][] faces = new int[20][];
    faces[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
    faces[1] = new int[]{18, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19};
    for (int i = 0; i < 17; i++) {
      faces[2 + i] = new int[]{i + 1, i, i + 18, i + 19};
    }
    faces[19] = new int[]{0, 17, 35, 18};
    return faces;
  }

  private static Point3D[] cubePoints() {
    return new Point3D[]{
        new Point3D(-90,  90, -90),
        new Point3D( 90,  90, -90),
        new Point3D( 90, -90, -90),
        new Point3D(-90, -90, -90),
        new Point3D(-90,  90,  90),
        new Point3D( 90,  90,  90),
        new Point3D( 90, -90,  90),
        new Point3D(-90, -90,  90)
    };
  }

  /**
   * Winding gives outward-facing normals for each face.
   */
  private static int[][] cubeFaces() {
    return new int[][]{
        {0, 1, 2, 3},  // front  (z=-90, normal -z)
        {4, 7, 6, 5},  // back   (z=+90, normal +z)
        {0, 3, 7, 4},  // left   (x=-90, normal -x)
        {1, 5, 6, 2},  // right  (x=+90, normal +x)
        {0, 4, 5, 1},  // top    (y=+90, normal +y)
        {2, 6, 7, 3}   // bottom (y=-90, normal -y)
    };
  }
}
