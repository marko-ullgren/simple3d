package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;

/**
 * A three-dimensional wireframe body defined by a set of points and edges.
 * <p>
 * The coordinate origin is at the centre of the drawing area. The positive x-axis points right,
 * the positive y-axis points up, and the positive z-axis points away from the viewer.
 */
public class Body {

  public static final double PROJECTION_FACTOR = 0.0010;

  private Point3D[] points;
  /** Each element is a pair of indices into {@code points} defining one line segment. */
  private int[][] edges;
  private Color colour;
  private Color darkColour;

  public Body(Point3D[] points, int[][] edges, Color colour) {
    this.points = points;
    this.edges = edges;
    this.colour = colour;
    this.darkColour = this.colour.darker();
  }

  public Body(Body source) {
    this.points = source.points;
    this.edges = source.edges;
    this.colour = source.colour;
    this.darkColour = source.darkColour;
  }

  public void setColour(Color newColour) {
    this.colour = newColour;
    this.darkColour = this.colour.darker();
  }

  /**
   * Draws the body onto the given Graphics context.
   *
   * @param g       Graphics context to draw on
   * @param centerX x-coordinate of the canvas centre
   * @param centerY y-coordinate of the canvas centre
   * @param scale   scale factor relative to the default window size
   */
  public void draw(Graphics g, int centerX, int centerY, double scale) {
    for (int i = 0; i < edges.length; i++) {
      g.setColor(colour);
      double x0 = points[edges[i][0]].x;
      double x1 = points[edges[i][1]].x;
      double y0 = points[edges[i][0]].y;
      double y1 = points[edges[i][1]].y;

      // Perspective projection: distant points converge toward the origin
      x0 = scale * (x0 - (x0 * PROJECTION_FACTOR * points[edges[i][0]].z)) + centerX;
      x1 = scale * (x1 - (x1 * PROJECTION_FACTOR * points[edges[i][1]].z)) + centerX;
      y0 = scale * (y0 - (y0 * PROJECTION_FACTOR * points[edges[i][0]].z)) + centerY;
      y1 = scale * (y1 - (y1 * PROJECTION_FACTOR * points[edges[i][1]].z)) + centerY;

      // Darken edges that are further away (behind the body)
      if (points[edges[i][0]].z > 60 && points[edges[i][1]].z > 60) {
        g.setColor(darkColour);
      }

      g.drawLine((int) x0, (int) y0, (int) x1, (int) y1);
    }
  }

  public void rotateXZ() {
    for (Point3D point : points) point.rotateXZ();
  }

  public void rotateYZ() {
    for (Point3D point : points) point.rotateYZ();
  }

  public void rotateZX() {
    for (Point3D point : points) point.rotateZX();
  }

  public void rotateZY() {
    for (Point3D point : points) point.rotateZY();
  }

  public Color getColour() {
    return this.colour;
  }

}
