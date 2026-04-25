package com.ullgren.modern.simple3d.model;

/**
 * A point in three-dimensional space (x, y, z).
 * <p>
 * Rotation methods apply a 2D rotation matrix in the named plane:
 * <pre>
 *   | cos a   sin a |
 *   | -sin a  cos a |
 * </pre>
 * where {@code a = ROTATION_ANGLE} (3 degrees). Methods named {@code rotateAB} rotate in the
 * positive direction; methods named {@code rotateBA} rotate in the reverse direction.
 */
public class Point3D {

  public static final double ROTATION_ANGLE = Math.toRadians(3);
  public static final double SIN = Math.sin(ROTATION_ANGLE);
  public static final double COS = Math.cos(ROTATION_ANGLE);

  private double x, y, z;

  public Point3D() {
    this.x = 0;
    this.y = 0;
    this.z = 0;
  }

  public Point3D(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Point3D(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Point3D(Point3D source) {
    this.x = source.x;
    this.y = source.y;
    this.z = source.z;
  }

  public double getX() { return x; }
  public double getY() { return y; }
  public double getZ() { return z; }

  @Override
  public String toString() {
    return "x: " + this.x + " y: " + this.y + " z: " + this.z;
  }

  public void rotateXZ() {
    double temp = this.x;
    this.x = (this.x * COS) - (this.z * SIN);
    this.z = (temp * SIN) + (this.z * COS);
  }

  public void rotateYZ() {
    double temp = this.y;
    this.y = (this.y * COS) - (this.z * SIN);
    this.z = (temp * SIN) + (this.z * COS);
  }

  public void rotateZX() {
    double temp = this.z;
    this.z = (this.z * COS) - (this.x * SIN);
    this.x = (temp * SIN) + (this.x * COS);
  }

  public void rotateZY() {
    double temp = this.z;
    this.z = (this.z * COS) - (this.y * SIN);
    this.y = (temp * SIN) + (this.y * COS);
  }

  public void rotateXZ(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    double temp = this.x;
    this.x = (this.x * cos) - (this.z * sin);
    this.z = (temp * sin) + (this.z * cos);
  }

  public void rotateYZ(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    double temp = this.y;
    this.y = (this.y * cos) - (this.z * sin);
    this.z = (temp * sin) + (this.z * cos);
  }

  public void rotateZX(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    double temp = this.z;
    this.z = (this.z * cos) - (this.x * sin);
    this.x = (temp * sin) + (this.x * cos);
  }

  public void rotateZY(double angle) {
    double sin = Math.sin(angle), cos = Math.cos(angle);
    double temp = this.z;
    this.z = (this.z * cos) - (this.y * sin);
    this.y = (temp * sin) + (this.y * cos);
  }
}
