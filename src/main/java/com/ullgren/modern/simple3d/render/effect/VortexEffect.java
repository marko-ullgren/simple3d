package com.ullgren.modern.simple3d.render.effect;

import javax.swing.Timer;

/**
 * Image-space vortex effect.
 * <p>
 * Pixels near the trigger point are rotated around it, creating a swirling vortex that
 * unwinds over {@link #DURATION_MS} milliseconds. The angular displacement is strongest
 * at the centre and falls off quadratically with distance, reaching zero at {@link #RADIUS}.
 * <p>
 * Backward mapping is used: each destination pixel is sampled from a source position
 * rotated in the opposite direction by {@code angle × falloff(d)}.
 */
public class VortexEffect implements Effect {

  /** Duration of the full vortex animation. */
  public static final int   DURATION_MS = 1200;
  /** Outer boundary of the swirl region in pixels. */
  public static final int   RADIUS      = 75;
  /** Peak rotation angle at the very centre of the vortex (radians). */
  public static final float MAX_ANGLE   = (float) (Math.PI * 1.0);
  private static final int  TICK_MS     = 16;

  private int   cx, cy;
  private float angle;
  private int   ticksLeft;

  private Timer timer;

  /**
   * @param repaint callback invoked each tick to request a canvas repaint
   */
  public VortexEffect(Runnable repaint) {
    timer = new Timer(TICK_MS, e -> {
      ticksLeft -= 1;
      angle      = MAX_ANGLE * ticksLeft / (float) totalTicks();
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
    ticksLeft = totalTicks();
    angle     = MAX_ANGLE;
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

        float t       = d / RADIUS;
        float falloff = (1f - t) * (1f - t);   // quadratic — tightest near centre
        float rot     = -angle * falloff;       // negative = backward mapping

        float cosR = (float) Math.cos(rot);
        float sinR = (float) Math.sin(rot);
        int   ix   = Math.max(0, Math.min(w - 1, Math.round(cx + ddx * cosR - ddy * sinR)));
        int   iy   = Math.max(0, Math.min(h - 1, Math.round(cy + ddx * sinR + ddy * cosR)));
        dst[oy * w + ox] = src[iy * w + ix];
      }
    }
  }
}
