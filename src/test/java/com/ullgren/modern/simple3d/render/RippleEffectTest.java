package com.ullgren.modern.simple3d.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RippleEffect}.
 * <p>
 * Tests operate directly on {@code int[]} pixel arrays — no AWT or Renderer involved.
 */
public class RippleEffectTest {

  @Test
  public void isActive_beforeTrigger_isFalse() {
    assertFalse(new RippleEffect(() -> {}).isActive());
  }

  @Test
  public void isActive_afterTrigger_isTrue() {
    RippleEffect effect = new RippleEffect(() -> {});
    effect.trigger(50, 50);
    assertTrue(effect.isActive(), "Should be active immediately after trigger");
  }

  @Test
  public void applyToPixels_outsideRadius_pixelsUnchanged() {
    int w = 400, h = 400;
    int[] src = new int[w * h];
    int[] dst = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF0000FF;
    System.arraycopy(src, 0, dst, 0, src.length);

    RippleEffect effect = new RippleEffect(() -> {});
    effect.trigger(200, 200);
    effect.applyToPixels(src, dst, w, h);

    // Corners are well outside RADIUS from centre (200, 200)
    assertEquals(src[0],                         dst[0],                         "Top-left corner must be unchanged");
    assertEquals(src[(h - 1) * w + (w - 1)],     dst[(h - 1) * w + (w - 1)],    "Bottom-right corner must be unchanged");
  }

  @Test
  public void applyToPixels_insideRadius_pixelsDisplaced() {
    int w = 300, h = 300;
    int[] src = new int[w * h];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        src[y * w + x] = 0xFF000000 | (x & 0xFF);
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    RippleEffect effect = new RippleEffect(() -> {});
    effect.trigger(150, 150);
    effect.applyToPixels(src, dst, w, h);

    // Check a pixel 30 px from centre — well inside the influence radius
    boolean anyDisplaced = false;
    for (int x = 155; x < 185; x++) {
      if ((src[150 * w + x] & 0xFF) != (dst[150 * w + x] & 0xFF)) {
        anyDisplaced = true;
        break;
      }
    }
    assertTrue(anyDisplaced, "At least one pixel inside the radius should be displaced");
  }

  @Test
  public void applyToPixels_centrePixel_notDisplaced() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF000000 | i;
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    RippleEffect effect = new RippleEffect(() -> {});
    effect.trigger(100, 100);
    effect.applyToPixels(src, dst, w, h);

    assertEquals(src[100 * w + 100], dst[100 * w + 100],
        "Centre pixel (d=0) must not be displaced");
  }
}
