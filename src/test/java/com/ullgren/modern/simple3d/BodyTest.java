package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.BodyLoader;
import com.ullgren.modern.simple3d.model.Point3D;
import com.ullgren.modern.simple3d.render.Renderer;

public class BodyTest {

  private static final String RES = "/com/ullgren/modern/simple3d/";

  // -------------------------------------------------------------------------
  // Loading — valid files
  // -------------------------------------------------------------------------

  @Test
  public void loadBody_triangle_createsBody() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.red);
    assertEquals(3, body.pointCount());
    assertEquals(1, body.faceCount());
  }

  @Test
  public void loadBody_setsColour() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.green);
    assertEquals(Color.green, body.getColour());
  }

  @Test
  public void loadBody_commentsAndBlanksIgnored() {
    Body body = BodyLoader.load(RES + "test_triangle_with_comments.body", Color.blue);
    assertEquals(3, body.pointCount());
    assertEquals(1, body.faceCount());
  }

  @Test
  public void loadMuBody_has36PointsAnd20Faces() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    assertEquals(36, body.pointCount());
    assertEquals(20, body.faceCount());
  }

  @Test
  public void loadCubeBody_has8PointsAnd6Faces() {
    Body body = BodyLoader.load(RES + "cube.body", Color.blue);
    assertEquals(8, body.pointCount());
    assertEquals(6, body.faceCount());
  }

  // -------------------------------------------------------------------------
  // Loading — invalid files
  // -------------------------------------------------------------------------

  @Test
  public void loadBody_missingResource_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load("/nonexistent.body", Color.blue));  }

  @Test
  public void loadBody_dataBeforeSection_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_data_before_section.body", Color.blue));  }

  @Test
  public void loadBody_pointWithTwoCoords_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_malformed_point_two_coords.body", Color.blue));  }

  @Test
  public void loadBody_pointWithFourCoords_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_malformed_point_four_coords.body", Color.blue));  }

  @Test
  public void loadBody_pointWithNonNumericCoord_throwsNumberFormatException() {
    assertThrows(NumberFormatException.class, () -> BodyLoader.load(RES + "test_malformed_point_nonnumeric.body", Color.blue));  }

  @Test
  public void loadBody_faceWithTwoIndices_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_malformed_face_two_indices.body", Color.blue));  }

  @Test
  public void loadBody_faceWithNegativeIndex_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_malformed_face_negative_index.body", Color.blue));  }

  @Test
  public void loadBody_faceIndexOutOfBounds_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_malformed_face_oob.body", Color.blue));  }

  @Test
  public void loadBody_faceWithNonNumericIndex_throwsNumberFormatException() {
    assertThrows(NumberFormatException.class, () -> BodyLoader.load(RES + "test_malformed_face_nonnumeric.body", Color.blue));  }

  @Test
  public void loadBody_noPointsDefined_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_no_points.body", Color.blue));  }

  @Test
  public void loadBody_noFacesDefined_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> BodyLoader.load(RES + "test_no_faces.body", Color.blue));  }

  // -------------------------------------------------------------------------
  // Colour
  // -------------------------------------------------------------------------

  @Test
  public void setColour_updatesColour() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.blue);
    body.setColour(Color.red);
    assertEquals(Color.red, body.getColour());
  }

  // -------------------------------------------------------------------------
  // Rotation
  // -------------------------------------------------------------------------

  @Test
  public void rotateXZ_changesOrientation() {
    Body before = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) before.rotateZY();

    Body after = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) after.rotateZY();
    for (int i = 0; i < 15; i++) after.rotateXZ(); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZY_changesOrientation() {
    Body before = BodyLoader.load(RES + "mu.body", Color.blue);
    Body after  = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 15; i++) after.rotateZY(); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZY_roundTrip_120steps_restoresOrientation() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    BufferedImage before = drawToImage(body);
    for (int i = 0; i < 120; i++) body.rotateZY();
    for (int i = 0; i < 120; i++) body.rotateYZ();
    BufferedImage after = drawToImage(body);

    // Allow 1% tolerance for floating-point drift over 240 accumulated rotations
    long sumBefore = pixelSum(before);
    long sumAfter  = pixelSum(after);
    double diff = Math.abs(sumBefore - sumAfter) / (double) Math.max(1, sumBefore);
    assertTrue(diff < 0.01, "Round-trip pixel sums differ by more than 1%: " + diff);
  }

  @Test
  public void rotateAllMethods_doNotThrow() {
    Body body = BodyLoader.load(RES + "cube.body", Color.blue);
    body.rotateXZ();
    body.rotateYZ();
    body.rotateZX();
    body.rotateZY();
  }

  @Test
  public void rotateYZ_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateYZ(0.0);
    assertEquals(before, pixelSum(drawToImage(body)));
  }

  @Test
  public void rotateYZ_doubleAngle_changesOrientation() {
    Body before = BodyLoader.load(RES + "mu.body", Color.blue);
    Body after  = BodyLoader.load(RES + "mu.body", Color.blue);
    after.rotateYZ(Math.PI / 4);
    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateYZ_doubleAngle_negativeAngleIsInverse() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateYZ(Math.PI / 6);
    body.rotateYZ(-Math.PI / 6);
    assertEquals(before, pixelSum(drawToImage(body)),
        "rotateYZ(a) then rotateYZ(-a) should restore orientation");
  }

  @Test
  public void rotateYZ_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = BodyLoader.load(RES + "mu.body", Color.blue);
    withDouble.rotateYZ(Point3D.ROTATION_ANGLE);
    Body withNoArg = BodyLoader.load(RES + "mu.body", Color.blue);
    withNoArg.rotateYZ();
    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateYZ(ROTATION_ANGLE) and rotateYZ() should produce identical results");
  }

  @Test
  public void rotateZX_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateZX(0.0);
    assertEquals(before, pixelSum(drawToImage(body)));
  }

  @Test
  public void rotateZX_doubleAngle_changesOrientation() {
    Body before = BodyLoader.load(RES + "mu.body", Color.blue);
    Body after  = BodyLoader.load(RES + "mu.body", Color.blue);
    after.rotateZX(Math.PI / 4);
    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZX_doubleAngle_negativeAngleIsInverse() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateZX(Math.PI / 6);
    body.rotateZX(-Math.PI / 6);
    long after = pixelSum(drawToImage(body));
    double diff = Math.abs(before - after) / (double) Math.max(1, before);
    assertTrue(diff < 0.01,
        "rotateZX(a) then rotateZX(-a) should restore orientation (diff=" + diff + ")");
  }

  @Test
  public void rotateZX_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = BodyLoader.load(RES + "mu.body", Color.blue);
    withDouble.rotateZX(Point3D.ROTATION_ANGLE);
    Body withNoArg = BodyLoader.load(RES + "mu.body", Color.blue);
    withNoArg.rotateZX();
    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateZX(ROTATION_ANGLE) and rotateZX() should produce identical results");
  }

  // -------------------------------------------------------------------------
  // Continuous rotation (double-angle overloads)
  // -------------------------------------------------------------------------

  @Test
  public void rotateXZ_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateXZ(0.0);
    long after = pixelSum(drawToImage(body));

    assertEquals(before, after, "rotateXZ(0) should leave the body unchanged");
  }

  @Test
  public void rotateXZ_doubleAngle_changesOrientation() {
    Body before = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) before.rotateZY();

    Body after = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) after.rotateZY();
    after.rotateXZ(Math.PI / 4); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateXZ_doubleAngle_negativeAngleIsInverse() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateXZ(Math.PI / 6);
    body.rotateXZ(-Math.PI / 6);
    long after = pixelSum(drawToImage(body));

    assertEquals(before, after,
        "rotateXZ(a) followed by rotateXZ(-a) should restore orientation");
  }

  @Test
  public void rotateXZ_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withDouble.rotateZY();
    withDouble.rotateXZ(Point3D.ROTATION_ANGLE);

    Body withNoArg = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withNoArg.rotateZY();
    withNoArg.rotateXZ();

    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateXZ(ROTATION_ANGLE) and rotateXZ() should produce identical results");
  }

  @Test
  public void rotateZY_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateZY(0.0);
    long after = pixelSum(drawToImage(body));

    assertEquals(before, after, "rotateZY(0) should leave the body unchanged");
  }

  @Test
  public void rotateZY_doubleAngle_changesOrientation() {
    Body before = BodyLoader.load(RES + "mu.body", Color.blue);

    Body after = BodyLoader.load(RES + "mu.body", Color.blue);
    after.rotateZY(Math.PI / 4); // 45°

    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZY_doubleAngle_negativeAngleIsInverse() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateZY(Math.PI / 6);
    body.rotateZY(-Math.PI / 6);
    long after = pixelSum(drawToImage(body));

    assertEquals(before, after,
        "rotateZY(a) followed by rotateZY(-a) should restore orientation");
  }

  @Test
  public void rotateZY_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withDouble.rotateZY();
    withDouble.rotateZY(Point3D.ROTATION_ANGLE);

    Body withNoArg = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withNoArg.rotateZY();
    withNoArg.rotateZY();

    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateZY(ROTATION_ANGLE) and rotateZY() should produce identical results");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static BufferedImage newImage(int w, int h) {
    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  }

  private static BufferedImage drawToImage(Body body) {
    BufferedImage img = newImage(400, 400);
    new Renderer().render(body, img.createGraphics(), 200, 200, 1.0, 400, 400);
    return img;
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
    assertNotEquals(sumA, sumB,
        "Expected bodies at different orientations to produce different renders");
  }

  // -------------------------------------------------------------------------
  // Ambient Occlusion
  // -------------------------------------------------------------------------

  @Test
  public void ambientOcclusion_flatTriangle_isZero() {
    // A single flat face: all three vertex normals agree → AO should be 0.
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.red);
    for (int i = 0; i < body.pointCount(); i++) {
      assertEquals(0.0f, body.getVertexAO(i), 1e-6f,
          "flat triangle vertex " + i + " should have AO = 0");
    }
  }

  @Test
  public void ambientOcclusion_cubeCorner_isPositive() {
    // A cube has 3 perpendicular faces meeting at each corner.
    // Adjacent face normals diverge → mean normal length < 1 → AO > 0.
    Body body = BodyLoader.load(RES + "test_cube.body", Color.red);
    boolean anyPositive = false;
    for (int i = 0; i < body.pointCount(); i++) {
      if (body.getVertexAO(i) > 0f) { anyPositive = true; break; }
    }
    assertTrue(anyPositive, "cube corner vertices should have AO > 0");
  }

  @Test
  public void ambientOcclusion_allValuesInRange() {
    Body body = BodyLoader.load(RES + "test_cube.body", Color.red);
    for (int i = 0; i < body.pointCount(); i++) {
      float ao = body.getVertexAO(i);
      assertTrue(ao >= 0f, "AO[" + i + "] must be >= 0");
      assertTrue(ao <= 1.0f, "AO[" + i + "] must be <= 1");
    }
  }
}
