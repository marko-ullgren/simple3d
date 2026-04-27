package com.ullgren.modern.simple3d.render.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ShockwaveEffect}.
 * <p>
 * Tests operate directly on {@code int[]} pixel arrays — no AWT or Renderer involved.
 */
public class ShockwaveEffectTest {

  @Test
  public void isActive_beforeTrigger_isFalse() {
    assertFalse(new ShockwaveEffect(() -> {}).isActive());
  }

  @Test
  public void isActive_afterTrigger_isTrue() {
    ShockwaveEffect effect = new ShockwaveEffect(() -> {});
    effect.trigger(50, 50);
    assertTrue(effect.isActive(), "Should be active immediately after trigger");
  }

  @Test
  public void applyToPixels_atRingRadius_pixelDisplaced() {
    // At trigger time the ring is at radius = WAVE_WIDTH.
    // Pixels within one belt-width of the ring should be displaced.
    int w = 200, h = 200;
    int[] src = new int[w * h];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        src[y * w + x] = 0xFF000000 | (x & 0xFF);
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    ShockwaveEffect effect = new ShockwaveEffect(() -> {});
    effect.trigger(100, 100);
    effect.applyToPixels(src, dst, w, h);

    // At trigger time the ring sits at d = WAVE_WIDTH from centre.
    // The pixel exactly at the ring centre has bell = 1 → maximum displacement.
    int ringX = Math.round(100 + ShockwaveEffect.WAVE_WIDTH);
    assertNotEquals(src[100 * w + ringX], dst[100 * w + ringX],
        "Pixel at the ring centre should be displaced");
  }

  @Test
  public void applyToPixels_farFromRing_pixelsUnchanged() {
    int w = 400, h = 400;
    int[] src = new int[w * h];
    int[] dst = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF0000FF;
    System.arraycopy(src, 0, dst, 0, src.length);

    ShockwaveEffect effect = new ShockwaveEffect(() -> {});
    effect.trigger(200, 200);
    effect.applyToPixels(src, dst, w, h);

    // Corners are far from any ring radius — must be unchanged
    assertEquals(src[0],                         dst[0],                         "Top-left corner must be unchanged");
    assertEquals(src[(h - 1) * w + (w - 1)],     dst[(h - 1) * w + (w - 1)],    "Bottom-right corner must be unchanged");
  }

  @Test
  public void applyToPixels_centrePixel_notDisplaced() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF000000 | i;
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    ShockwaveEffect effect = new ShockwaveEffect(() -> {});
    effect.trigger(100, 100);
    effect.applyToPixels(src, dst, w, h);

    // d=0 is skipped; the ring starts at WAVE_WIDTH, so the centre is always outside the belt
    assertEquals(src[100 * w + 100], dst[100 * w + 100],
        "Centre pixel must not be displaced");
  }
}
