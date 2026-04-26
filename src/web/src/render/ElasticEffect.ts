/**
 * Image-space elastic dent effect.
 *
 * Calling {@link dent} triggers a spring-damper animation that radially
 * displaces pixels around the click point.  The effect owns its own 60 fps
 * interval timer and stops automatically once the spring has settled.
 *
 * Call {@link applyToPixels} each frame while {@link isActive} returns true to
 * blend the effect into the rendered geometry layer.
 */
export class ElasticEffect {
  private static readonly STIFFNESS   = 350;
  private static readonly DAMPING     = 22;
  /** Initial inward displacement in pixels. */
  private static readonly INIT_DISP   = 10;
  /** Pixel radius of the influence region. */
  static readonly RADIUS = 60;
  private static readonly STOP_THRESH = 0.3;
  private static readonly TICK_MS     = 16;

  private cx = 0;
  private cy = 0;
  private displacement = 0;
  private velocity     = 0;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly repaint: () => void) {}

  /** Triggers a dent centred on {@code (x, y)}.  Restarts if already running. */
  dent(x: number, y: number): void {
    this.cx           = x;
    this.cy           = y;
    this.displacement = ElasticEffect.INIT_DISP;
    this.velocity     = 0;
    this.clearTimer();
    this.timerId = setInterval(() => this.tick(), ElasticEffect.TICK_MS);
  }

  isActive(): boolean {
    return this.timerId !== null;
  }

  /**
   * Applies radial displacement from {@code src} into {@code dst} (RGBA Uint8ClampedArray).
   * Pixels outside {@link RADIUS} are copied unchanged.  For interior pixels the backward-
   * mapping reads from {@code src} at a position displaced outward by
   * `displacement × 4t(1−t)²` pixels (cubic profile, t = d/RADIUS).
   */
  applyToPixels(
    src: Uint8ClampedArray,
    dst: Uint8ClampedArray,
    w: number,
    h: number,
  ): void {
    const { cx, cy, displacement } = this;
    const R = ElasticEffect.RADIUS;
    const x0 = Math.max(0,     cx - R);
    const x1 = Math.min(w - 1, cx + R);
    const y0 = Math.max(0,     cy - R);
    const y1 = Math.min(h - 1, cy + R);

    for (let oy = y0; oy <= y1; oy++) {
      for (let ox = x0; ox <= x1; ox++) {
        const ddx = ox - cx;
        const ddy = oy - cy;
        const d   = Math.sqrt(ddx * ddx + ddy * ddy);
        if (d === 0 || d >= R) continue;

        const t          = d / R;
        const mag        = displacement * 4 * t * (1 - t) * (1 - t);
        const sourceDist = d + mag;
        const dstBase    = (oy * w + ox) * 4;

        if (sourceDist <= 0) {
          // Clamped to original pixel.
          const srcBase = dstBase;
          dst[dstBase]     = src[srcBase];
          dst[dstBase + 1] = src[srcBase + 1];
          dst[dstBase + 2] = src[srcBase + 2];
          dst[dstBase + 3] = src[srcBase + 3];
          continue;
        }

        const scale = sourceDist / d;
        const ix    = Math.max(0, Math.min(w - 1, Math.round(cx + ddx * scale)));
        const iy    = Math.max(0, Math.min(h - 1, Math.round(cy + ddy * scale)));
        const srcBase = (iy * w + ix) * 4;

        dst[dstBase]     = src[srcBase];
        dst[dstBase + 1] = src[srcBase + 1];
        dst[dstBase + 2] = src[srcBase + 2];
        dst[dstBase + 3] = src[srcBase + 3];
      }
    }
  }

  private tick(): void {
    const dt  = ElasticEffect.TICK_MS / 1000;
    const acc = -ElasticEffect.STIFFNESS * this.displacement
              - ElasticEffect.DAMPING    * this.velocity;
    this.velocity     += acc * dt;
    this.displacement += this.velocity * dt;

    if (
      Math.abs(this.displacement) < ElasticEffect.STOP_THRESH &&
      Math.abs(this.velocity)     < ElasticEffect.STOP_THRESH
    ) {
      this.displacement = 0;
      this.velocity     = 0;
      this.clearTimer();
    }
    this.repaint();
  }

  private clearTimer(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
