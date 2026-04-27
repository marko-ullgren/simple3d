package com.ullgren.modern.simple3d.render;

import javax.swing.Timer;

/**
 * Image-space shockwave effect.
 * <p>
 * A single ring of strong radial displacement expands outward from the trigger point at
 * {@link #EXPAND_SPEED} pixels per second, producing the look of a pressure wave. The
 * ring starts at radius {@link #WAVE_WIDTH} (not zero) so the bell profile is zero at the
 * trigger point on the first tick — this avoids a singularity at the centre.
 * <p>
 * The displacement profile is a squared falloff bell centred on the ring:
 * {@code bell = (1 - |d - radius| / WAVE_WIDTH)²} for {@code |d - radius| < WAVE_WIDTH}.
 * Backward mapping pulls source pixels from {@code sourceDist = max(0, d - displacement)}
 * to simulate outward compression ahead of the wave.
 * <p>
 * The effect stops automatically once the ring radius exceeds {@link #MAX_RADIUS}.
 */
public class ShockwaveEffect implements Effect {

  /** Expansion speed of the ring in pixels per second. */
  public static final float EXPAND_SPEED = 250f;
  /** Half-width of the displacement belt around the ring in pixels. */
  public static final float WAVE_WIDTH   = 20f;
  /** Peak radial displacement at the ring centre in pixels. */
  public static final float STRENGTH     = 8f;
  /** Radius at which the effect is considered complete. */
  public static final float MAX_RADIUS   = 400f;
  private static final int  TICK_MS      = 16;

  private int   cx, cy;
  /** Current ring radius. Starts at WAVE_WIDTH to keep the bell zero at the trigger point. */
  private float radius;

  private Timer timer;

  /**
   * @param repaint callback invoked each tick to request a canvas repaint
   */
  public ShockwaveEffect(Runnable repaint) {
    timer = new Timer(TICK_MS, e -> {
      radius += EXPAND_SPEED * (TICK_MS / 1000f);
      if (radius > MAX_RADIUS) {
        timer.stop();
      }
      repaint.run();
    });
  }

  @Override
  public void trigger(int x, int y) {
    cx     = x;
    cy     = y;
    radius = WAVE_WIDTH;  // start one belt-width out so centre bell = 0
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
    // Only process pixels that could be within the wave belt.
    int outerR = (int) Math.ceil(radius + WAVE_WIDTH);
    int x0 = Math.max(0,     cx - outerR);
    int x1 = Math.min(w - 1, cx + outerR);
    int y0 = Math.max(0,     cy - outerR);
    int y1 = Math.min(h - 1, cy + outerR);

    for (int oy = y0; oy <= y1; oy++) {
      for (int ox = x0; ox <= x1; ox++) {
        float ddx       = ox - cx;
        float ddy       = oy - cy;
        float d         = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        float distToRing = Math.abs(d - radius);
        if (d == 0 || distToRing >= WAVE_WIDTH) continue;

        float t            = distToRing / WAVE_WIDTH;
        float bell         = (1f - t) * (1f - t);
        float displacement = STRENGTH * bell;

        // Backward mapping: sample from inward of the wave.
        float sourceDist = Math.max(0f, d - displacement);
        if (sourceDist == 0) {
          dst[oy * w + ox] = src[cy * w + cx];
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
