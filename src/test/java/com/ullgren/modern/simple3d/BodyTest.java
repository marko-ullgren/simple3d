package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class BodyTest {

  private static final String RES = "/com/ullgren/modern/simple3d/";

  // -------------------------------------------------------------------------
  // Loading — valid files
  // -------------------------------------------------------------------------

  @Test
  public void loadBody_triangle_createsBody() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.red);
    assertEquals(3, body.pointCount());
    assertEquals(1, body.faceCount());
  }

  @Test
  public void loadBody_setsColour() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.green);
    assertEquals(Color.green, body.getColour());
  }

  @Test
  public void loadBody_commentsAndBlanksIgnored() {
    Body body = Body.loadBody(RES + "test_triangle_with_comments.body", Color.blue);
    assertEquals(3, body.pointCount());
    assertEquals(1, body.faceCount());
  }

  @Test
  public void loadMuBody_has36PointsAnd20Faces() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    assertEquals(36, body.pointCount());
    assertEquals(20, body.faceCount());
  }

  @Test
  public void loadCubeBody_has8PointsAnd6Faces() {
    Body body = Body.loadBody(RES + "cube.body", Color.blue);
    assertEquals(8, body.pointCount());
    assertEquals(6, body.faceCount());
  }

  // -------------------------------------------------------------------------
  // Loading — invalid files
  // -------------------------------------------------------------------------

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_missingResource_throwsIllegalArgument() {
    Body.loadBody("/nonexistent.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_dataBeforeSection_throwsIllegalArgument() {
    Body.loadBody(RES + "test_data_before_section.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_pointWithTwoCoords_throwsIllegalArgument() {
    Body.loadBody(RES + "test_malformed_point_two_coords.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_pointWithFourCoords_throwsIllegalArgument() {
    Body.loadBody(RES + "test_malformed_point_four_coords.body", Color.blue);
  }

  @Test(expected = NumberFormatException.class)
  public void loadBody_pointWithNonNumericCoord_throwsNumberFormatException() {
    Body.loadBody(RES + "test_malformed_point_nonnumeric.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_faceWithTwoIndices_throwsIllegalArgument() {
    Body.loadBody(RES + "test_malformed_face_two_indices.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_faceWithNegativeIndex_throwsIllegalArgument() {
    Body.loadBody(RES + "test_malformed_face_negative_index.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_faceIndexOutOfBounds_throwsIllegalArgument() {
    Body.loadBody(RES + "test_malformed_face_oob.body", Color.blue);
  }

  @Test(expected = NumberFormatException.class)
  public void loadBody_faceWithNonNumericIndex_throwsNumberFormatException() {
    Body.loadBody(RES + "test_malformed_face_nonnumeric.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_noPointsDefined_throwsIllegalArgument() {
    Body.loadBody(RES + "test_no_points.body", Color.blue);
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadBody_noFacesDefined_throwsIllegalArgument() {
    Body.loadBody(RES + "test_no_faces.body", Color.blue);
  }

  // -------------------------------------------------------------------------
  // Colour
  // -------------------------------------------------------------------------

  @Test
  public void setColour_updatesColour() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.blue);
    body.setColour(Color.red);
    assertEquals(Color.red, body.getColour());
  }

  // -------------------------------------------------------------------------
  // Drawing — branch coverage
  // -------------------------------------------------------------------------

  @Test
  public void draw_withTriangleFace_doesNotThrow() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    body.draw(g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void draw_withQuadFace_doesNotThrow() {
    Body body = Body.loadBody(RES + "test_quad.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    body.draw(g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void draw_withCapPolygon_doesNotThrow() {
    Body body = Body.loadBody(RES + "test_cap.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    body.draw(g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void draw_backFacedBody_producesNoPixels() {
    Body body = Body.loadBody(RES + "test_backfacing.body", Color.white);
    BufferedImage img = newImage(400, 400);
    body.draw(img.createGraphics(), 200, 200, 1.0, 400, 400);
    // All pixels should remain transparent/black — nothing was drawn
    for (int y = 0; y < 400; y++) {
      for (int x = 0; x < 400; x++) {
        assertEquals("Expected no pixels drawn for back-facing body", 0, img.getRGB(x, y));
      }
    }
  }

  @Test
  public void draw_frontFacedBody_producesNonBlackPixels() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.white);
    BufferedImage img = newImage(400, 400);
    body.draw(img.createGraphics(), 200, 200, 1.0, 400, 400);
    assertTrue("Expected some pixels to be drawn", hasNonZeroPixel(img));
  }

  @Test
  public void draw_muBody_doesNotThrow() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Graphics g = newGraphics(400, 400);
    body.draw(g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void draw_cubeBody_doesNotThrow() {
    Body body = Body.loadBody(RES + "cube.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    body.draw(g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  // -------------------------------------------------------------------------
  // Rotation
  // -------------------------------------------------------------------------

  @Test
  public void rotateXZ_changesOrientation() {
    Body before = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) before.rotateZY();

    Body after = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) after.rotateZY();
    for (int i = 0; i < 15; i++) after.rotateXZ(); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZY_changesOrientation() {
    Body before = Body.loadBody(RES + "mu.body", Color.blue);
    Body after  = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 15; i++) after.rotateZY(); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZY_roundTrip_120steps_restoresOrientation() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    BufferedImage before = drawToImage(body);
    for (int i = 0; i < 120; i++) body.rotateZY();
    for (int i = 0; i < 120; i++) body.rotateYZ();
    BufferedImage after = drawToImage(body);

    // Allow 1% tolerance for floating-point drift over 240 accumulated rotations
    long sumBefore = pixelSum(before);
    long sumAfter  = pixelSum(after);
    double diff = Math.abs(sumBefore - sumAfter) / (double) Math.max(1, sumBefore);
    assertTrue("Round-trip pixel sums differ by more than 1%: " + diff, diff < 0.01);
  }

  @Test
  public void rotateAllMethods_doNotThrow() {
    Body body = Body.loadBody(RES + "cube.body", Color.blue);
    body.rotateXZ();
    body.rotateYZ();
    body.rotateZX();
    body.rotateZY();
  }

  // -------------------------------------------------------------------------
  // Continuous rotation (double-angle overloads)
  // -------------------------------------------------------------------------

  @Test
  public void rotateXZ_doubleAngle_zeroIsIdentity() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateXZ(0.0);
    long after = pixelSum(drawToImage(body));

    assertEquals("rotateXZ(0) should leave the body unchanged", before, after);
  }

  @Test
  public void rotateXZ_doubleAngle_changesOrientation() {
    Body before = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) before.rotateZY();

    Body after = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) after.rotateZY();
    after.rotateXZ(Math.PI / 4); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateXZ_doubleAngle_negativeAngleIsInverse() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateXZ(Math.PI / 6);
    body.rotateXZ(-Math.PI / 6);
    long after = pixelSum(drawToImage(body));

    assertEquals("rotateXZ(a) followed by rotateXZ(-a) should restore orientation",
        before, after);
  }

  @Test
  public void rotateXZ_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withDouble.rotateZY();
    withDouble.rotateXZ(Point3D.ROTATION_ANGLE);

    Body withNoArg = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withNoArg.rotateZY();
    withNoArg.rotateXZ();

    assertEquals("rotateXZ(ROTATION_ANGLE) and rotateXZ() should produce identical results",
        pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)));
  }

  @Test
  public void rotateZY_doubleAngle_zeroIsIdentity() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateZY(0.0);
    long after = pixelSum(drawToImage(body));

    assertEquals("rotateZY(0) should leave the body unchanged", before, after);
  }

  @Test
  public void rotateZY_doubleAngle_changesOrientation() {
    Body before = Body.loadBody(RES + "mu.body", Color.blue);

    Body after = Body.loadBody(RES + "mu.body", Color.blue);
    after.rotateZY(Math.PI / 4); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZY_doubleAngle_negativeAngleIsInverse() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateZY(Math.PI / 6);
    body.rotateZY(-Math.PI / 6);
    long after = pixelSum(drawToImage(body));

    assertEquals("rotateZY(a) followed by rotateZY(-a) should restore orientation",
        before, after);
  }

  @Test
  public void rotateZY_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withDouble.rotateZY();
    withDouble.rotateZY(Point3D.ROTATION_ANGLE);

    Body withNoArg = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withNoArg.rotateZY();
    withNoArg.rotateZY();

    assertEquals("rotateZY(ROTATION_ANGLE) and rotateZY() should produce identical results",
        pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static BufferedImage newImage(int w, int h) {
    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  }

  private static Graphics newGraphics(int w, int h) {
    return newImage(w, h).createGraphics();
  }

  private static BufferedImage drawToImage(Body body) {
    BufferedImage img = newImage(400, 400);
    body.draw(img.createGraphics(), 200, 200, 1.0, 400, 400);
    return img;
  }

  private static boolean hasNonZeroPixel(BufferedImage img) {
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (img.getRGB(x, y) != 0) return true;
      }
    }
    return false;
  }

  private static long pixelSum(BufferedImage img) {
    long sum = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        sum += img.getRGB(x, y) & 0xFFFFFFFFL;
      }
    }
    return sum;
  }

  private static void assertDifferentPixels(Body a, Body b) {
    long sumA = pixelSum(drawToImage(a));
    long sumB = pixelSum(drawToImage(b));
    assertNotEquals("Expected bodies at different orientations to produce different renders",
        sumA, sumB);
  }
}
