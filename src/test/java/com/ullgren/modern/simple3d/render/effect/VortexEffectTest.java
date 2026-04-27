package com.ullgren.modern.simple3d.render.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VortexEffect}.
 * <p>
 * Tests operate directly on {@code int[]} pixel arrays — no AWT or Renderer involved.
 */
public class VortexEffectTest {

  @Test
  public void isActive_beforeTrigger_isFalse() {
    assertFalse(new VortexEffect(() -> {}).isActive());
  }

  @Test
  public void isActive_afterTrigger_isTrue() {
    VortexEffect effect = new VortexEffect(() -> {});
    effect.trigger(50, 50);
    assertTrue(effect.isActive(), "Should be active immediately after trigger");
  }

  @Test
  public void applyToPixels_outsideRadius_pixelsUnchanged() {
    int w = 400, h = 400;
    int[] src = new int[w * h];
    int[] dst = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF00FF00;
    System.arraycopy(src, 0, dst, 0, src.length);

    VortexEffect effect = new VortexEffect(() -> {});
    effect.trigger(200, 200);
    effect.applyToPixels(src, dst, w, h);

    assertEquals(src[0],                         dst[0],                         "Top-left corner must be unchanged");
    assertEquals(src[(h - 1) * w + (w - 1)],     dst[(h - 1) * w + (w - 1)],    "Bottom-right corner must be unchanged");
  }

  @Test
  public void applyToPixels_insideRadius_pixelsSampled() {
    // Encode position: unique colour per pixel so any non-identity sampling is detectable
    int w = 300, h = 300;
    int[] src = new int[w * h];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        src[y * w + x] = 0xFF000000 | ((x & 0xFF) << 8) | (y & 0xFF);
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    VortexEffect effect = new VortexEffect(() -> {});
    effect.trigger(150, 150);
    effect.applyToPixels(src, dst, w, h);

    // A pixel 30 px from centre (on the x-axis) should have been rotated,
    // sampling from a different (x, y) than (180, 150).
    assertNotEquals(src[150 * w + 180], dst[150 * w + 180],
        "Pixel inside the vortex radius should be sampled from a rotated position");
  }

  @Test
  public void applyToPixels_centrePixel_notDisplaced() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF000000 | i;
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    VortexEffect effect = new VortexEffect(() -> {});
    effect.trigger(100, 100);
    effect.applyToPixels(src, dst, w, h);

    assertEquals(src[100 * w + 100], dst[100 * w + 100],
        "Centre pixel (d=0) must not be displaced");
  }
}
