package com.ullgren.modern.simple3d.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.BodyLoader;
/**
 * Behavioural tests for {@link Renderer}.
 * <p>
 * All tests work at the pixel level via {@link BufferedImage} — there are no public accessors to
 * the shading pipeline itself.  Tests are split into three groups:
 * <ul>
 *   <li><b>Projection</b> — scale and centre-offset affect where and how many pixels are drawn</li>
 *   <li><b>Shading</b> — face orientation affects pixel brightness (diffuse shading is active)</li>
 *   <li><b>Renderer state</b> — buffer reuse, elastic-effect wiring, and edge cases</li>
 * </ul>
 */
public class RendererTest {

  private static final String RES = "/com/ullgren/modern/simple3d/";

  // ---------------------------------------------------------------------------
  // Smoke tests — rendering known bodies must not throw
  // ---------------------------------------------------------------------------

  @Test
  public void render_withTriangleFace_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.blue);
    assertDoesNotThrow(() -> render(body, 200, 200, 1.0, 400, 400));
  }

  @Test
  public void render_withQuadFace_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_quad.body", Color.blue);
    assertDoesNotThrow(() -> render(body, 200, 200, 1.0, 400, 400));
  }

  @Test
  public void render_withCapPolygon_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_cap.body", Color.blue);
    assertDoesNotThrow(() -> render(body, 200, 200, 1.0, 400, 400));
  }

  @Test
  public void render_muBody_doesNotThrow() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    assertDoesNotThrow(() -> render(body, 200, 200, 1.0, 400, 400));
  }

  @Test
  public void render_cubeBody_doesNotThrow() {
    Body body = BodyLoader.load(RES + "cube.body", Color.blue);
    assertDoesNotThrow(() -> render(body, 200, 200, 1.0, 400, 400));
  }

  // ---------------------------------------------------------------------------
  // Back-face culling
  // ---------------------------------------------------------------------------

  @Test
  public void render_backFacedBody_producesNoPixels() {
    Body body = BodyLoader.load(RES + "test_backfacing.body", Color.white);
    BufferedImage img = render(body, 200, 200, 1.0, 400, 400);
    for (int y = 0; y < 400; y++)
      for (int x = 0; x < 400; x++)
        assertEquals(0, img.getRGB(x, y), "Expected no pixels drawn for back-facing body");
  }

  @Test
  public void render_frontFacedBody_producesNonBlackPixels() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.white);
    assertTrue(countNonZeroPixels(render(body, 200, 200, 1.0, 400, 400)) > 0,
        "Expected some pixels to be drawn");
  }

  // ---------------------------------------------------------------------------
  // Renderer state and reuse
  // ---------------------------------------------------------------------------

  @Test
  public void renderer_consecutiveRenders_produceSameOutput() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Renderer renderer = new Renderer();
    long first  = pixelSum(renderWith(renderer, body, 200, 200, 1.0, 400, 400));
    long second = pixelSum(renderWith(renderer, body, 200, 200, 1.0, 400, 400));
    assertEquals(first, second,
        "Same renderer, same body, same canvas size should produce identical output");
  }

  @Test
  public void renderer_afterColourChange_reflectsNewColour() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Renderer renderer = new Renderer();
    long blue = pixelSum(renderWith(renderer, body, 200, 200, 1.0, 400, 400));
    body.setColour(Color.red);
    long red = pixelSum(renderWith(renderer, body, 200, 200, 1.0, 400, 400));
    assertNotEquals(blue, red, "Render output should change after colour change");
  }

  @Test
  public void renderer_afterBodySwap_reflectsNewBody() {
    Renderer renderer = new Renderer();
    Body mu = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) mu.rotateZY();
    Body cube = BodyLoader.load(RES + "cube.body", Color.blue);
    long muPixels   = pixelSum(renderWith(renderer, mu,   200, 200, 1.0, 400, 400));
    long cubePixels = pixelSum(renderWith(renderer, cube, 200, 200, 1.0, 400, 400));
    assertNotEquals(muPixels, cubePixels, "MU and cube should produce different renders");
  }

  @Test
  public void renderer_afterSizeChange_doesNotThrow() {
    Body body = BodyLoader.load(RES + "mu.body", Color.blue);
    for (int i = 0; i < 60; i++) body.rotateZY();
    Renderer renderer = new Renderer();
    renderer.render(body, newGraphics(400, 400), 200, 200, 1.0, 400, 400);
    assertDoesNotThrow(() ->
        renderer.render(body, newGraphics(600, 500), 300, 250, 1.0, 600, 500));
  }

  // ---------------------------------------------------------------------------
  // Projection
  // ---------------------------------------------------------------------------

  /**
   * A larger {@code scale} value projects vertices further from the centre, covering a greater
   * canvas area.  This directly exercises {@code hiScale = 2.0 * scale}.
   */
  @Test
  public void render_largerScale_fillsMorePixels() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.white);
    int small = countNonZeroPixels(render(body, 200, 200, 0.5, 400, 400));
    int large  = countNonZeroPixels(render(body, 200, 200, 3.0, 400, 400));
    assertTrue(large > small,
        "scale=3.0 should fill more canvas pixels than scale=0.5 (small=" + small + ", large=" + large + ")");
  }

  /**
   * Moving the render centre shifts where the body appears on screen.  The centroid of drawn
   * pixels (weighted mean X) should closely track the centre offset.
   */
  @Test
  public void render_centreOffset_shiftsBodyPosition() {
    // test_cap.body is centred at the origin, so the centroid of drawn pixels should be
    // very close to the render centre regardless of canvas position.
    Body body = BodyLoader.load(RES + "test_cap.body", Color.white);
    double centroidLeft  = meanNonZeroX(render(body, 100, 200, 1.0, 400, 400));
    double centroidRight = meanNonZeroX(render(body, 300, 200, 1.0, 400, 400));
    assertTrue(centroidLeft < centroidRight,
        "centroid of body at centreX=100 should be left of centroid at centreX=300 "
            + "(left=" + centroidLeft + ", right=" + centroidRight + ")");
  }

  /**
   * Projecting a body whose vertices all land outside the canvas should produce no drawn pixels
   * and must not throw (e.g. from array index out of bounds).
   */
  @Test
  public void render_bodyOutsideCanvas_producesNoPixels() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.white);
    BufferedImage img = render(body, -500, -500, 1.0, 400, 400);
    assertEquals(0, countNonZeroPixels(img),
        "body projected entirely off-screen should produce no drawn pixels");
  }

  /**
   * A canvas of 1×1 pixel is the absolute minimum.  The renderer must not throw even if most
   * geometry projects outside the single pixel.
   */
  @Test
  public void render_minimalCanvas_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.blue);
    assertDoesNotThrow(() ->
        new Renderer().render(body, newGraphics(1, 1), 0, 0, 1.0, 1, 1));
  }

  /**
   * {@code scale=0} collapses all vertices onto the centre point — a degenerate but legal call
   * that must not throw.
   */
  @Test
  public void render_scaleZero_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.blue);
    assertDoesNotThrow(() ->
        new Renderer().render(body, newGraphics(400, 400), 200, 200, 0.0, 400, 400));
  }

  // ---------------------------------------------------------------------------
  // Shading — side faces (Gouraud)
  // ---------------------------------------------------------------------------

  /**
   * A face whose normal is aligned with the viewer direction (diffuse=1) should be rendered
   * notably brighter than the same face tilted 75° (diffuse≈0.26).
   * <p>
   * We compare <em>average brightness of non-zero pixels</em> rather than total pixel sum to
   * decouple shading from foreshortened projected area.
   */
  @Test
  public void render_sideFace_directFacingBrighter_thanGlancingFace() {
    Body direct   = BodyLoader.load(RES + "test_triangle.body", Color.white);
    Body glancing = BodyLoader.load(RES + "test_triangle.body", Color.white);
    glancing.rotateXZ(Math.toRadians(75));

    double avgDirect   = meanBrightness(render(direct,   200, 200, 1.0, 400, 400));
    double avgGlancing = meanBrightness(render(glancing, 200, 200, 1.0, 400, 400));

    assertTrue(avgDirect > avgGlancing,
        "direct-facing side face should be brighter than 75°-glancing face "
            + "(direct=" + avgDirect + ", glancing=" + avgGlancing + ")");
  }

  // ---------------------------------------------------------------------------
  // Shading — cap faces (flat)
  // ---------------------------------------------------------------------------

  /**
   * Same shading contract as for side faces, but exercising the flat-shading cap branch
   * ({@code face.length > 4}).  The hexagonal cap in test_cap.body faces the viewer directly
   * at 0° and is tilted 75° for the glancing variant.
   */
  @Test
  public void render_capFace_directFacingBrighter_thanGlancingFace() {
    Body direct   = BodyLoader.load(RES + "test_cap.body", Color.white);
    Body glancing = BodyLoader.load(RES + "test_cap.body", Color.white);
    glancing.rotateXZ(Math.toRadians(75));

    double avgDirect   = meanBrightness(render(direct,   200, 200, 1.0, 400, 400));
    double avgGlancing = meanBrightness(render(glancing, 200, 200, 1.0, 400, 400));

    assertTrue(avgDirect > avgGlancing,
        "direct-facing cap face should be brighter than 75°-glancing cap "
            + "(direct=" + avgDirect + ", glancing=" + avgGlancing + ")");
  }

  // ---------------------------------------------------------------------------
  // Renderer state
  // ---------------------------------------------------------------------------

  /**
   * The off-screen buffer must be cleared ({@code Arrays.fill(pix, 0)}) between renders.
   * If it isn't, pixels from a previously drawn front-facing body would bleed into a
   * subsequent all-culled render.
   */
  @Test
  public void render_bufferClearedBetweenRenders() {
    Renderer renderer = new Renderer();
    Body front = BodyLoader.load(RES + "test_triangle.body",   Color.white);
    Body back  = BodyLoader.load(RES + "test_backfacing.body", Color.white);

    BufferedImage firstRender = renderWith(renderer, front, 200, 200, 1.0, 400, 400);
    assertTrue(countNonZeroPixels(firstRender) > 0,
        "front-facing body should produce drawn pixels (precondition)");

    BufferedImage secondRender = renderWith(renderer, back, 200, 200, 1.0, 400, 400);
    assertEquals(0, countNonZeroPixels(secondRender),
        "back-facing body rendered by the same Renderer instance should produce no pixels "
            + "(hi-res buffer must be cleared between renders)");
  }

  /**
   * Attaching an {@link ElasticEffect} and rendering while it is active must not throw.
   * This exercises the {@code if (effect != null && effect.isActive())} branch.
   */
  @Test
  public void render_withActiveElasticEffect_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.blue);
    Renderer renderer = new Renderer();
    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(220, 220);
    renderer.setEffect(effect);
    assertDoesNotThrow(() ->
        renderer.render(body, newGraphics(400, 400), 200, 200, 1.0, 400, 400));
  }

  /**
   * Removing the effect (setting {@code null}) after it was active must not throw and must
   * leave the renderer in a valid state for subsequent renders.
   */
  @Test
  public void render_removingEffect_doesNotThrow() {
    Body body = BodyLoader.load(RES + "test_triangle.body", Color.blue);
    Renderer renderer = new Renderer();
    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(200, 200);
    renderer.setEffect(effect);
    renderer.render(body, newGraphics(400, 400), 200, 200, 1.0, 400, 400);

    renderer.setEffect(null);
    assertDoesNotThrow(() ->
        renderer.render(body, newGraphics(400, 400), 200, 200, 1.0, 400, 400));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static BufferedImage render(Body body, int cx, int cy, double scale, int w, int h) {
    return renderWith(new Renderer(), body, cx, cy, scale, w, h);
  }

  private static BufferedImage renderWith(Renderer renderer, Body body,
      int cx, int cy, double scale, int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    renderer.render(body, img.createGraphics(), cx, cy, scale, w, h);
    return img;
  }

  private static Graphics newGraphics(int w, int h) {
    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).createGraphics();
  }

  /** Sum of all ARGB pixel values, used to detect colour or content changes. */
  private static long pixelSum(BufferedImage img) {
    long sum = 0;
    for (int y = 0; y < img.getHeight(); y++)
      for (int x = 0; x < img.getWidth(); x++)
        sum += img.getRGB(x, y) & 0xFFFFFFFFL;
    return sum;
  }

  /** Number of pixels whose ARGB value is not 0 (i.e., something was drawn). */
  private static int countNonZeroPixels(BufferedImage img) {
    int count = 0;
    for (int y = 0; y < img.getHeight(); y++)
      for (int x = 0; x < img.getWidth(); x++)
        if (img.getRGB(x, y) != 0) count++;
    return count;
  }

  /** Weighted mean X-coordinate of drawn (non-zero) pixels. Returns 0 if nothing was drawn. */
  private static double meanNonZeroX(BufferedImage img) {
    long sumX = 0, count = 0;
    for (int y = 0; y < img.getHeight(); y++)
      for (int x = 0; x < img.getWidth(); x++)
        if (img.getRGB(x, y) != 0) { sumX += x; count++; }
    return count == 0 ? 0.0 : (double) sumX / count;
  }

  /**
   * Average brightness of drawn (non-zero) pixels, computed as the mean of all R, G, B channel
   * values across non-zero pixels.  Using only non-zero pixels avoids diluting the average with
   * the black background, keeping the metric independent of projected area.
   */
  private static double meanBrightness(BufferedImage img) {
    long channelSum = 0;
    int count = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        int rgb = img.getRGB(x, y);
        if (rgb != 0) {
          channelSum += (rgb >> 16) & 0xFF; // R
          channelSum += (rgb >>  8) & 0xFF; // G
          channelSum +=  rgb        & 0xFF; // B
          count++;
        }
      }
    }
    return count == 0 ? 0.0 : (double) channelSum / (count * 3);
  }
}
