package com.ullgren.modern.simple3d.render.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NoEffect}.
 */
public class NoEffectTest {

  @Test
  public void isActive_always_returnsFalse() {
    assertFalse(new NoEffect().isActive());
  }

  @Test
  public void isActive_afterTrigger_stillFalse() {
    NoEffect effect = new NoEffect();
    effect.trigger(50, 50);
    assertFalse(effect.isActive(), "NoEffect must never become active");
  }

  @Test
  public void applyToPixels_doesNotWriteToDestination() {
    int w = 50, h = 50;
    int[] src = new int[w * h];
    int[] dst = new int[w * h];
    for (int i = 0; i < src.length; i++) src[i] = 0xFF0000FF;
    // dst stays all-zero; applyToPixels must not touch it

    new NoEffect().applyToPixels(src, dst, w, h);

    assertEquals(0, dst[0], "NoEffect must not write any pixels");
  }
}
