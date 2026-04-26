package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.Point3D;
import com.ullgren.modern.simple3d.render.ElasticEffect;
import com.ullgren.modern.simple3d.render.Renderer;

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

  @Test
  public void loadBody_missingResource_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody("/nonexistent.body", Color.blue));
  }

  @Test
  public void loadBody_dataBeforeSection_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_data_before_section.body", Color.blue));
  }

  @Test
  public void loadBody_pointWithTwoCoords_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_malformed_point_two_coords.body", Color.blue));
  }

  @Test
  public void loadBody_pointWithFourCoords_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_malformed_point_four_coords.body", Color.blue));
  }

  @Test
  public void loadBody_pointWithNonNumericCoord_throwsNumberFormatException() {
    assertThrows(NumberFormatException.class, () -> Body.loadBody(RES + "test_malformed_point_nonnumeric.body", Color.blue));
  }

  @Test
  public void loadBody_faceWithTwoIndices_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_malformed_face_two_indices.body", Color.blue));
  }

  @Test
  public void loadBody_faceWithNegativeIndex_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_malformed_face_negative_index.body", Color.blue));
  }

  @Test
  public void loadBody_faceIndexOutOfBounds_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_malformed_face_oob.body", Color.blue));
  }

  @Test
  public void loadBody_faceWithNonNumericIndex_throwsNumberFormatException() {
    assertThrows(NumberFormatException.class, () -> Body.loadBody(RES + "test_malformed_face_nonnumeric.body", Color.blue));
  }

  @Test
  public void loadBody_noPointsDefined_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_no_points.body", Color.blue));
  }

  @Test
  public void loadBody_noFacesDefined_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> Body.loadBody(RES + "test_no_faces.body", Color.blue));
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
  public void render_withTriangleFace_doesNotThrow() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    new Renderer().render(body, g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void render_withQuadFace_doesNotThrow() {
    Body body = Body.loadBody(RES + "test_quad.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    new Renderer().render(body, g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void render_withCapPolygon_doesNotThrow() {
    Body body = Body.loadBody(RES + "test_cap.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    new Renderer().render(body, g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void render_backFacedBody_producesNoPixels() {
    Body body = Body.loadBody(RES + "test_backfacing.body", Color.white);
    BufferedImage img = newImage(400, 400);
    new Renderer().render(body, img.createGraphics(), 200, 200, 1.0, 400, 400);
    // All pixels should remain transparent/black — nothing was drawn
    for (int y = 0; y < 400; y++) {
      for (int x = 0; x < 400; x++) {
        assertEquals(0, img.getRGB(x, y), "Expected no pixels drawn for back-facing body");
      }
    }
  }

  @Test
  public void render_frontFacedBody_producesNonBlackPixels() {
    Body body = Body.loadBody(RES + "test_triangle.body", Color.white);
    BufferedImage img = newImage(400, 400);
    new Renderer().render(body, img.createGraphics(), 200, 200, 1.0, 400, 400);
    assertTrue(hasNonZeroPixel(img), "Expected some pixels to be drawn");
  }

  @Test
  public void render_muBody_doesNotThrow() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Graphics g = newGraphics(400, 400);
    new Renderer().render(body, g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  @Test
  public void render_cubeBody_doesNotThrow() {
    Body body = Body.loadBody(RES + "cube.body", Color.blue);
    Graphics g = newGraphics(400, 400);
    new Renderer().render(body, g, 200, 200, 1.0, 400, 400);
    g.dispose();
  }

  // -------------------------------------------------------------------------
  // Renderer — state and reuse
  // -------------------------------------------------------------------------

  @Test
  public void renderer_consecutiveRenders_produceSameOutput() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Renderer renderer = new Renderer();
    BufferedImage img = newImage(400, 400);
    renderer.render(body, img.createGraphics(), 200, 200, 1.0, 400, 400);
    long first = pixelSum(img);
    img = newImage(400, 400);
    renderer.render(body, img.createGraphics(), 200, 200, 1.0, 400, 400);
    long second = pixelSum(img);
    assertEquals(first, second,
        "Same renderer, same body, same size should produce identical output");
  }

  @Test
  public void renderer_afterColourChange_reflectsNewColour() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Renderer renderer = new Renderer();
    long blue = pixelSum(renderWith(renderer, body, 400, 400));
    body.setColour(Color.red);
    long red = pixelSum(renderWith(renderer, body, 400, 400));
    assertNotEquals(blue, red, "Render output should change after colour change");
  }

  @Test
  public void renderer_afterBodySwap_reflectsNewBody() {
    Renderer renderer = new Renderer();
    Body mu = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) mu.rotateZY();
    Body cube = Body.loadBody(RES + "cube.body", Color.blue);
    long muPixels   = pixelSum(renderWith(renderer, mu,   400, 400));
    long cubePixels = pixelSum(renderWith(renderer, cube, 400, 400));
    assertNotEquals(muPixels, cubePixels, "MU and cube should produce different renders");
  }

  @Test
  public void renderer_afterSizeChange_doesNotThrow() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Renderer renderer = new Renderer();
    renderer.render(body, newGraphics(400, 400), 200, 200, 1.0, 400, 400);
    renderer.render(body, newGraphics(600, 500), 300, 250, 1.0, 600, 500);
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
    assertTrue(diff < 0.01, "Round-trip pixel sums differ by more than 1%: " + diff);
  }

  @Test
  public void rotateAllMethods_doNotThrow() {
    Body body = Body.loadBody(RES + "cube.body", Color.blue);
    body.rotateXZ();
    body.rotateYZ();
    body.rotateZX();
    body.rotateZY();
  }

  @Test
  public void rotateYZ_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateYZ(0.0);
    assertEquals(before, pixelSum(drawToImage(body)));
  }

  @Test
  public void rotateYZ_doubleAngle_changesOrientation() {
    Body before = Body.loadBody(RES + "mu.body", Color.blue);
    Body after  = Body.loadBody(RES + "mu.body", Color.blue);
    after.rotateYZ(Math.PI / 4);
    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateYZ_doubleAngle_negativeAngleIsInverse() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateYZ(Math.PI / 6);
    body.rotateYZ(-Math.PI / 6);
    assertEquals(before, pixelSum(drawToImage(body)),
        "rotateYZ(a) then rotateYZ(-a) should restore orientation");
  }

  @Test
  public void rotateYZ_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = Body.loadBody(RES + "mu.body", Color.blue);
    withDouble.rotateYZ(Point3D.ROTATION_ANGLE);
    Body withNoArg = Body.loadBody(RES + "mu.body", Color.blue);
    withNoArg.rotateYZ();
    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateYZ(ROTATION_ANGLE) and rotateYZ() should produce identical results");
  }

  @Test
  public void rotateZX_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    long before = pixelSum(drawToImage(body));
    body.rotateZX(0.0);
    assertEquals(before, pixelSum(drawToImage(body)));
  }

  @Test
  public void rotateZX_doubleAngle_changesOrientation() {
    Body before = Body.loadBody(RES + "mu.body", Color.blue);
    Body after  = Body.loadBody(RES + "mu.body", Color.blue);
    after.rotateZX(Math.PI / 4);
    assertDifferentPixels(before, after);
  }

  @Test
  public void rotateZX_doubleAngle_negativeAngleIsInverse() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
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
    Body withDouble = Body.loadBody(RES + "mu.body", Color.blue);
    withDouble.rotateZX(Point3D.ROTATION_ANGLE);
    Body withNoArg = Body.loadBody(RES + "mu.body", Color.blue);
    withNoArg.rotateZX();
    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateZX(ROTATION_ANGLE) and rotateZX() should produce identical results");
  }

  // -------------------------------------------------------------------------
  // Continuous rotation (double-angle overloads)
  // -------------------------------------------------------------------------

  @Test
  public void rotateXZ_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateXZ(0.0);
    long after = pixelSum(drawToImage(body));

    assertEquals(before, after, "rotateXZ(0) should leave the body unchanged");
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

    assertEquals(before, after,
        "rotateXZ(a) followed by rotateXZ(-a) should restore orientation");
  }

  @Test
  public void rotateXZ_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withDouble.rotateZY();
    withDouble.rotateXZ(Point3D.ROTATION_ANGLE);

    Body withNoArg = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withNoArg.rotateZY();
    withNoArg.rotateXZ();

    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateXZ(ROTATION_ANGLE) and rotateXZ() should produce identical results");
  }

  @Test
  public void rotateZY_doubleAngle_rotatingByZeroDegreesDoesNothing() {
    Body body = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();

    long before = pixelSum(drawToImage(body));
    body.rotateZY(0.0);
    long after = pixelSum(drawToImage(body));

    assertEquals(before, after, "rotateZY(0) should leave the body unchanged");
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

    assertEquals(before, after,
        "rotateZY(a) followed by rotateZY(-a) should restore orientation");
  }

  @Test
  public void rotateZY_doubleAngle_matchesNoArgAtRotationAngle() {
    Body withDouble = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withDouble.rotateZY();
    withDouble.rotateZY(Point3D.ROTATION_ANGLE);

    Body withNoArg = Body.loadBody(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) withNoArg.rotateZY();
    withNoArg.rotateZY();

    assertEquals(pixelSum(drawToImage(withDouble)), pixelSum(drawToImage(withNoArg)),
        "rotateZY(ROTATION_ANGLE) and rotateZY() should produce identical results");
  }

  // -------------------------------------------------------------------------
  // ElasticEffect — pixel displacement
  // -------------------------------------------------------------------------

  @Test
  public void elasticEffect_outsideRadius_pixelsUnchanged() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    int[] dst = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF0000FF; // blue
    System.arraycopy(src, 0, dst, 0, src.length);

    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(100, 100);
    effect.applyToPixels(src, dst, w, h);

    // Corners are well outside RADIUS=80 from centre (100,100)
    assertEquals(src[0], dst[0], "Top-left corner should be unchanged");
    assertEquals(src[(h - 1) * w + (w - 1)], dst[(h - 1) * w + (w - 1)],
        "Bottom-right corner should be unchanged");
  }

  @Test
  public void elasticEffect_insideRadius_pixelsDisplaced() {
    int w = 300, h = 300;
    // Gradient source: each pixel encodes its x position so displacement is visible
    int[] src = new int[w * h];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        src[y * w + x] = 0xFF000000 | (x & 0xFF);
      }
    }
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(150, 150);
    effect.applyToPixels(src, dst, w, h);

    // At (150+20, 150) — distance 20px, well inside RADIUS=38 — pixel should be displaced
    int originalBlue = src[150 * w + 170] & 0xFF;
    int displacedBlue = dst[150 * w + 170] & 0xFF;
    assertNotEquals(originalBlue, displacedBlue, "Pixel inside radius should be displaced");
  }

  @Test
  public void elasticEffect_centrePixel_notDisplaced() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF000000 | i;
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(100, 100);
    effect.applyToPixels(src, dst, w, h);

    // d==0 at the click centre — the loop skips it, leaving dst unchanged
    assertEquals(src[100 * w + 100], dst[100 * w + 100], "Centre pixel must not be displaced");
  }

  @Test
  public void elasticEffect_isActive_afterDent() {
    ElasticEffect effect = new ElasticEffect(() -> {});
    assertFalse(effect.isActive(), "Should be inactive before first dent");
    effect.dent(50, 50);
    assertTrue(effect.isActive(), "Should be active immediately after dent");
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
    new Renderer().render(body, img.createGraphics(), 200, 200, 1.0, 400, 400);
    return img;
  }

  private static BufferedImage renderWith(Renderer renderer, Body body, int w, int h) {
    BufferedImage img = newImage(w, h);
    renderer.render(body, img.createGraphics(), w / 2, h / 2, 1.0, w, h);
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
    assertNotEquals(sumA, sumB,
        "Expected bodies at different orientations to produce different renders");
  }

  // -------------------------------------------------------------------------
  // Ambient Occlusion
  // -------------------------------------------------------------------------

  @Test
  public void ambientOcclusion_flatTriangle_isZero() {
    // A single flat face: all three vertex normals agree → AO should be 0.
    Body body = Body.loadBody(RES + "test_triangle.body", Color.red);
    for (int i = 0; i < body.pointCount(); i++) {
      assertEquals(0.0f, body.getVertexAO(i), 1e-6f,
          "flat triangle vertex " + i + " should have AO = 0");
    }
  }

  @Test
  public void ambientOcclusion_cubeCorner_isPositive() {
    // A cube has 3 perpendicular faces meeting at each corner.
    // Adjacent face normals diverge → mean normal length < 1 → AO > 0.
    Body body = Body.loadBody(RES + "test_cube.body", Color.red);
    boolean anyPositive = false;
    for (int i = 0; i < body.pointCount(); i++) {
      if (body.getVertexAO(i) > 0f) { anyPositive = true; break; }
    }
    assertTrue(anyPositive, "cube corner vertices should have AO > 0");
  }

  @Test
  public void ambientOcclusion_allValuesInRange() {
    Body body = Body.loadBody(RES + "test_cube.body", Color.red);
    for (int i = 0; i < body.pointCount(); i++) {
      float ao = body.getVertexAO(i);
      assertTrue(ao >= 0f, "AO[" + i + "] must be >= 0");
      assertTrue(ao <= 1.0f, "AO[" + i + "] must be <= 1");
    }
  }
}
