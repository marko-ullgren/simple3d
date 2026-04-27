package com.ullgren.modern.simple3d.render;

import javax.swing.Timer;

/**
 * Image-space elastic dent effect.
 * <p>
 * When {@link #trigger(int, int)} is called the effect applies a radial pixel-displacement to
 * the already-rendered frame via {@link #applyToPixels}. A spring-damper drives the
 * displacement scalar from an initial inward compression (positive) through a small outward
 * overshoot (negative) back to zero, creating the feel of touching a soft, elastic object.
 * <p>
 * The effect owns its own 60 fps Swing {@link Timer} and stops automatically once the spring
 * has settled. It is entirely in screen-space, so it works regardless of mesh vertex density.
 */
public class ElasticEffect implements Effect {

  private static final float STIFFNESS   = 350f;
  private static final float DAMPING     = 22f;
  /** Initial inward displacement in pixels. */
  private static final float INIT_DISP   = 10f;
  /** Pixel radius of the influence region. */
  public static final int RADIUS = 60;
  private static final float STOP_THRESH = 0.3f;
  private static final int   TICK_MS     = 16;

  private int   cx, cy;
  private float displacement;
  private float velocity;

  private Timer timer;

  /**
   * @param repaint callback invoked each tick to trigger a canvas repaint
   */
  public ElasticEffect(Runnable repaint) {
    timer = new Timer(TICK_MS, e -> {
      float dt  = TICK_MS / 1000f;
      float acc = -STIFFNESS * displacement - DAMPING * velocity;
      velocity    += acc * dt;
      displacement += velocity * dt;
      if (Math.abs(displacement) < STOP_THRESH && Math.abs(velocity) < STOP_THRESH) {
        displacement = 0;
        velocity     = 0;
        timer.stop();
      }
      repaint.run();
    });
  }

  /**
   * Triggers a dent centred on the given screen coordinates.
   * Restarts the animation if already in progress.
   */
  @Override
  public void trigger(int x, int y) {
    cx           = x;
    cy           = y;
    displacement = INIT_DISP;
    velocity     = 0;
    timer.stop();
    timer.start();
  }

  /** Returns {@code true} while the spring is still in motion. */
  @Override
  public boolean isActive() {
    return timer.isRunning();
  }

  /**
   * Applies the radial displacement from {@code src} into {@code dst}.
   * Pixels outside {@link #RADIUS} are copied unchanged. For pixels inside the radius the
   * backward-mapping samples from {@code src} at a position displaced outward by
   * {@code displacement × sin(π × d / RADIUS)} pixels, compressing the image toward the
   * centre (positive displacement) or expanding it outward (negative overshoot).
   *
   * @param src source pixel array (original frame, must not be {@code dst})
   * @param dst destination pixel array (written into; same dimensions as {@code src})
   * @param w   image width in pixels
   * @param h   image height in pixels
   */
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

        float t           = d / RADIUS;              // 0..1
        // Cubic profile: peaks at t=1/3, zero at t=0 and t=1 — tighter round shape.
        float mag         = displacement * 4f * t * (1f - t) * (1f - t);
        float sourceDist  = d + mag;
        if (sourceDist <= 0) {
          // Overshoot puts the sample at or past centre — clamp to original.
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
