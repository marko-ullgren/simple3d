package com.ullgren.modern.simple3d.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ElasticEffect}.
 * <p>
 * Tests operate directly on the {@code int[]} pixel arrays via
 * {@link ElasticEffect#applyToPixels} — no {@link Renderer} or AWT involved.
 */
public class ElasticEffectTest {

  @Test
  public void isActive_beforeDent_isFalse() {
    ElasticEffect effect = new ElasticEffect(() -> {});
    assertFalse(effect.isActive(), "Should be inactive before first dent");
  }

  @Test
  public void isActive_afterDent_isTrue() {
    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(50, 50);
    assertTrue(effect.isActive(), "Should be active immediately after dent");
  }

  @Test
  public void applyToPixels_outsideRadius_pixelsUnchanged() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    int[] dst = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF0000FF; // opaque blue
    System.arraycopy(src, 0, dst, 0, src.length);

    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(100, 100);
    effect.applyToPixels(src, dst, w, h);

    // Corners are well outside RADIUS from the dent centre (100, 100)
    assertEquals(src[0],                          dst[0],
        "Top-left corner should be unchanged");
    assertEquals(src[(h - 1) * w + (w - 1)],      dst[(h - 1) * w + (w - 1)],
        "Bottom-right corner should be unchanged");
  }

  @Test
  public void applyToPixels_insideRadius_pixelsDisplaced() {
    int w = 300, h = 300;
    // Each pixel encodes its x-position so any radial displacement is detectable
    int[] src = new int[w * h];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        src[y * w + x] = 0xFF000000 | (x & 0xFF);
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(150, 150);
    effect.applyToPixels(src, dst, w, h);

    // (150+20, 150) is 20 px from the centre — well inside the influence radius
    int originalBlue  = src[150 * w + 170] & 0xFF;
    int displacedBlue = dst[150 * w + 170] & 0xFF;
    assertNotEquals(originalBlue, displacedBlue,
        "Pixel inside the radius should be displaced to a different source pixel");
  }

  @Test
  public void applyToPixels_centrePixel_notDisplaced() {
    int w = 200, h = 200;
    int[] src = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF000000 | i;
    int[] dst = new int[w * h];
    System.arraycopy(src, 0, dst, 0, src.length);

    ElasticEffect effect = new ElasticEffect(() -> {});
    effect.dent(100, 100);
    effect.applyToPixels(src, dst, w, h);

    // distance == 0 at the dent centre — the loop skips it, leaving dst unchanged
    assertEquals(src[100 * w + 100], dst[100 * w + 100],
        "Centre pixel must not be displaced (distance = 0)");
  }
}
