package com.ullgren.modern.simple3d;

import org.junit.Test;
import static org.junit.Assert.*;

import com.ullgren.modern.simple3d.model.Point3D;

public class Point3DTest {

  private static final double DELTA = 1e-9;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  @Test
  public void defaultConstructor_allZero() {
    Point3D p = new Point3D();
    assertEquals(0.0, p.getX(), DELTA);
    assertEquals(0.0, p.getY(), DELTA);
    assertEquals(0.0, p.getZ(), DELTA);
  }

  @Test
  public void intConstructor_setsFields() {
    Point3D p = new Point3D(1, 2, 3);
    assertEquals(1.0, p.getX(), DELTA);
    assertEquals(2.0, p.getY(), DELTA);
    assertEquals(3.0, p.getZ(), DELTA);
  }

  @Test
  public void doubleConstructor_setsFields() {
    Point3D p = new Point3D(1.5, 2.5, 3.5);
    assertEquals(1.5, p.getX(), DELTA);
    assertEquals(2.5, p.getY(), DELTA);
    assertEquals(3.5, p.getZ(), DELTA);
  }

  @Test
  public void copyConstructor_copiesFieldValues() {
    Point3D original = new Point3D(4, 5, 6);
    Point3D copy = new Point3D(original);
    assertEquals(original.getX(), copy.getX(), DELTA);
    assertEquals(original.getY(), copy.getY(), DELTA);
    assertEquals(original.getZ(), copy.getZ(), DELTA);
  }

  @Test
  public void copyConstructor_isIndependent() {
    Point3D original = new Point3D(4, 5, 6);
    Point3D copy = new Point3D(original);
    copy.rotateXZ();
    assertEquals(4.0, original.getX(), DELTA);
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
    assertEquals(Point3D.COS,  p.getX(), DELTA);
    assertEquals(0.0,           p.getY(), DELTA);
    assertEquals(Point3D.SIN,  p.getZ(), DELTA);
  }

  @Test
  public void rotateXZ_preservesNorm() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double normBefore = p.getX() * p.getX() + p.getZ() * p.getZ();
    p.rotateXZ();
    double normAfter = p.getX() * p.getX() + p.getZ() * p.getZ();
    assertEquals(normBefore, normAfter, DELTA);
  }

  @Test
  public void rotateXZ_doesNotAffectY() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    p.rotateXZ();
    assertEquals(7.0, p.getY(), DELTA);
  }

  @Test
  public void rotateXZ_roundTrip_120steps() {
    Point3D p = new Point3D(5.0, 3.0, 2.0);
    double x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
    for (int i = 0; i < 120; i++) p.rotateXZ();
    assertEquals(x0, p.getX(), 1e-9);
    assertEquals(y0, p.getY(), 1e-9);
    assertEquals(z0, p.getZ(), 1e-9);
  }

  @Test
  public void rotateXZ_originStaysAtOrigin() {
    Point3D p = new Point3D();
    p.rotateXZ();
    assertEquals(0.0, p.getX(), DELTA);
    assertEquals(0.0, p.getY(), DELTA);
    assertEquals(0.0, p.getZ(), DELTA);
  }

  @Test
  public void rotateZX_isInverseOfRotateXZ() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
    p.rotateXZ();
    p.rotateZX();
    assertEquals(x0, p.getX(), DELTA);
    assertEquals(y0, p.getY(), DELTA);
    assertEquals(z0, p.getZ(), DELTA);
  }

  // -------------------------------------------------------------------------
  // rotateYZ
  // -------------------------------------------------------------------------

  @Test
  public void rotateYZ_appliesCorrectMatrix() {
    Point3D p = new Point3D(0.0, 1.0, 0.0);
    p.rotateYZ();
    assertEquals(0.0,           p.getX(), DELTA);
    assertEquals(Point3D.COS,  p.getY(), DELTA);
    assertEquals(Point3D.SIN,  p.getZ(), DELTA);
  }

  @Test
  public void rotateYZ_preservesNorm() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double normBefore = p.getY() * p.getY() + p.getZ() * p.getZ();
    p.rotateYZ();
    double normAfter = p.getY() * p.getY() + p.getZ() * p.getZ();
    assertEquals(normBefore, normAfter, DELTA);
  }

  @Test
  public void rotateYZ_doesNotAffectX() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    p.rotateYZ();
    assertEquals(3.0, p.getX(), DELTA);
  }

  @Test
  public void rotateYZ_roundTrip_120steps() {
    Point3D p = new Point3D(5.0, 3.0, 2.0);
    double x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
    for (int i = 0; i < 120; i++) p.rotateYZ();
    assertEquals(x0, p.getX(), 1e-9);
    assertEquals(y0, p.getY(), 1e-9);
    assertEquals(z0, p.getZ(), 1e-9);
  }

  @Test
  public void rotateYZ_originStaysAtOrigin() {
    Point3D p = new Point3D();
    p.rotateYZ();
    assertEquals(0.0, p.getX(), DELTA);
    assertEquals(0.0, p.getY(), DELTA);
    assertEquals(0.0, p.getZ(), DELTA);
  }

  @Test
  public void rotateZY_isInverseOfRotateYZ() {
    Point3D p = new Point3D(3.0, 7.0, 4.0);
    double x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
    p.rotateYZ();
    p.rotateZY();
    assertEquals(x0, p.getX(), DELTA);
    assertEquals(y0, p.getY(), DELTA);
    assertEquals(z0, p.getZ(), DELTA);
  }
}
