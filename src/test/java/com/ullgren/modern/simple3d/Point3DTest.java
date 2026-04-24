package com.ullgren.modern.simple3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class Point3DTest {

  private static final double DELTA = 1e-9;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  @Test
  public void defaultConstructor_allZero() {
    Point3D p = new Point3D();
    assertEquals(0.0, p.x, DELTA);
    assertEquals(0.0, p.y, DELTA);
    assertEquals(0.0, p.z, DELTA);
  }

  @Test
  public void intConstructor_setsFields() {
    Point3D p = new Point3D(1, 2, 3);
    assertEquals(1.0, p.x, DELTA);
    assertEquals(2.0, p.y, DELTA);
    assertEquals(3.0, p.z, DELTA);
  }

  @Test
  public void doubleConstructor_setsFields() {
    Point3D p = new Point3D(1.5, 2.5, 3.5);
    assertEquals(1.5, p.x, DELTA);
    assertEquals(2.5, p.y, DELTA);
    assertEquals(3.5, p.z, DELTA);
  }

  @Test
  public void copyConstructor_copiesFieldValues() {
    Point3D original = new Point3D(4, 5, 6);
    Point3D copy = new Point3D(original);
    assertEquals(original.x, copy.x, DELTA);
    assertEquals(original.y, copy.y, DELTA);
    assertEquals(original.z, copy.z, DELTA);
  }

  @Test
  public void copyConstructor_isIndependent() {
    Point3D original = new Point3D(4, 5, 6);
    Point3D copy = new Point3D(original);
    copy.x = 99;
    assertEquals(4.0, original.x, DELTA);
  }

  // -------------------------------------------------------------------------
  // toString
  // -------------------------------------------------------------------------

  @Test
  public void toString_containsCoordinates() {
    Point3D p = new Point3D(1, 2, 3);
    String s = p.toString();
    assertTrue(s.contains("1.0"));
    assertTrue(s.contains("2.0"));
    assertTrue(s.contains("3.0"));
  }

  // -------------------------------------------------------------------------
  // rotateXZ
  // -------------------------------------------------------------------------

  @Test
  public void rotateXZ_appliesCorrectMatrix() {
    Point3D p = new Point3D(1.0, 0.0, 0.0);
    p.rotateXZ();
    assertEquals(Point3D.COS,  p.x, DELTA);
    assertEquals(0.0,           p.y, DELTA);
    assertEquals(Point3D.SIN,  p.z, DELTA);
  }

  @Test
  public void rotateXZ_preservesNorm() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double normBefore = p.x * p.x + p.z * p.z;
    p.rotateXZ();
    double normAfter = p.x * p.x + p.z * p.z;
    assertEquals(normBefore, normAfter, DELTA);
  }

  @Test
  public void rotateXZ_doesNotAffectY() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    p.rotateXZ();
    assertEquals(7.0, p.y, DELTA);
  }

  @Test
  public void rotateXZ_roundTrip_120steps() {
    Point3D p = new Point3D(5.0, 3.0, 2.0);
    double x0 = p.x, y0 = p.y, z0 = p.z;
    for (int i = 0; i < 120; i++) p.rotateXZ();
    assertEquals(x0, p.x, 1e-9);
    assertEquals(y0, p.y, 1e-9);
    assertEquals(z0, p.z, 1e-9);
  }

  @Test
  public void rotateXZ_originStaysAtOrigin() {
    Point3D p = new Point3D();
    p.rotateXZ();
    assertEquals(0.0, p.x, DELTA);
    assertEquals(0.0, p.y, DELTA);
    assertEquals(0.0, p.z, DELTA);
  }

  @Test
  public void rotateZX_isInverseOfRotateXZ() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double x0 = p.x, y0 = p.y, z0 = p.z;
    p.rotateXZ();
    p.rotateZX();
    assertEquals(x0, p.x, DELTA);
    assertEquals(y0, p.y, DELTA);
    assertEquals(z0, p.z, DELTA);
  }

  // -------------------------------------------------------------------------
  // rotateYZ
  // -------------------------------------------------------------------------

  @Test
  public void rotateYZ_appliesCorrectMatrix() {
    Point3D p = new Point3D(0.0, 1.0, 0.0);
    p.rotateYZ();
    assertEquals(0.0,           p.x, DELTA);
    assertEquals(Point3D.COS,  p.y, DELTA);
    assertEquals(Point3D.SIN,  p.z, DELTA);
  }

  @Test
  public void rotateYZ_preservesNorm() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double normBefore = p.y * p.y + p.z * p.z;
    p.rotateYZ();
    double normAfter = p.y * p.y + p.z * p.z;
    assertEquals(normBefore, normAfter, DELTA);
  }

  @Test
  public void rotateYZ_doesNotAffectX() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    p.rotateYZ();
    assertEquals(3.0, p.x, DELTA);
  }

  @Test
  public void rotateYZ_roundTrip_120steps() {
    Point3D p = new Point3D(5.0, 3.0, 2.0);
    double x0 = p.x, y0 = p.y, z0 = p.z;
    for (int i = 0; i < 120; i++) p.rotateYZ();
    assertEquals(x0, p.x, 1e-9);
    assertEquals(y0, p.y, 1e-9);
    assertEquals(z0, p.z, 1e-9);
  }

  @Test
  public void rotateYZ_originStaysAtOrigin() {
    Point3D p = new Point3D();
    p.rotateYZ();
    assertEquals(0.0, p.x, DELTA);
    assertEquals(0.0, p.y, DELTA);
    assertEquals(0.0, p.z, DELTA);
  }

  @Test
  public void rotateZY_isInverseOfRotateYZ() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double x0 = p.x, y0 = p.y, z0 = p.z;
    p.rotateYZ();
    p.rotateZY();
    assertEquals(x0, p.x, DELTA);
    assertEquals(y0, p.y, DELTA);
    assertEquals(z0, p.z, DELTA);
  }
}
