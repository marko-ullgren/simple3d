package com.ullgren.modern.simple3d.render.effect;

import javax.swing.Timer;

/**
 * Image-space ripple effect.
 * <p>
 * Concentric sine-wave rings radiate outward from the trigger point, fading over
 * {@link #DURATION_MS} milliseconds. Each pixel inside {@link #RADIUS} is displaced
 * radially by {@code AMPLITUDE × strength × falloff(d) × sin(2π × (d/WAVELENGTH - phase))},
 * where {@code phase} advances each tick to make rings appear to move outward.
 * <p>
 * A radial {@code falloff = 1 - d/RADIUS} smoothly fades the distortion at the edge.
 */
public class RippleEffect implements Effect {

  /** Duration of the full ripple animation. */
  public static final int   DURATION_MS = 1500;
  /** Outer boundary of the effect region in pixels. */
  public static final int   RADIUS      = 75;
  /** Distance in pixels between successive wave crests. */
  public static final float WAVELENGTH  = 35f;
  /** Peak radial displacement of a pixel in pixels. */
  public static final float AMPLITUDE   = 10f;
  /** Phase advance per second — how many full wave cycles pass the origin per second. */
  public static final float SPEED       = 2.5f;
  private static final int  TICK_MS     = 16;

  private int   cx, cy;
  private float phase;
  private float strength;
  private int   ticksLeft;

  private Timer timer;

  /**
   * @param repaint callback invoked each tick to request a canvas repaint
   */
  public RippleEffect(Runnable repaint) {
    timer = new Timer(TICK_MS, e -> {
      phase     += SPEED * (TICK_MS / 1000f);
      ticksLeft -= 1;
      strength   = Math.max(0f, (float) ticksLeft / totalTicks());
      if (ticksLeft <= 0) {
        timer.stop();
      }
      repaint.run();
    });
  }

  private int totalTicks() {
    return DURATION_MS / TICK_MS;
  }

  @Override
  public void trigger(int x, int y) {
    cx        = x;
    cy        = y;
    phase     = 0f;
    ticksLeft = totalTicks();
    strength  = 1f;
    timer.stop();
    timer.start();
  }

  @Override
  public boolean isActive() {
    return timer.isRunning();
  }

  @Override
  public void stop() {
    timer.stop();
  }

  @Override
  public void applyToPixels(int[] src, int[] dst, int w, int h) {
    int x0 = Math.max(0,     cx - RADIUS);
    int x1 = Math.min(w - 1, cx + RADIUS);
    int y0 = Math.max(0,     cy - RADIUS);
    int y1 = Math.min(h - 1, cy + RADIUS);

    for (int oy = y0; oy <= y1; oy++) {
      for (int ox = x0; ox <= x1; ox++) {
        float ddx = ox - cx;
        float ddy = oy - cy;
        float d   = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        if (d == 0 || d >= RADIUS) continue;

        float t           = d / RADIUS;
        float falloff     = 1f - t;
        float displacement = AMPLITUDE * strength * falloff
            * (float) Math.sin(2 * Math.PI * (d / WAVELENGTH - phase));

        // Backward mapping: sourceDist = d - displacement.
        // Positive displacement pulls source from smaller radius → outward push.
        float sourceDist = d - displacement;
        if (sourceDist <= 0) {
          dst[oy * w + ox] = src[oy * w + ox];
          continue;
        }
        float scale = sourceDist / d;
        int   ix    = Math.max(0, Math.min(w - 1, Math.round(cx + ddx * scale)));
        int   iy    = Math.max(0, Math.min(h - 1, Math.round(cy + ddy * scale)));
        dst[oy * w + ox] = src[iy * w + ix];
      }
    }
  }
}
