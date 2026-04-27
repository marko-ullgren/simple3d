import type { Effect } from './Effect.js';

export class RippleEffect implements Effect {
  static readonly DURATION_MS = 1500;
  static readonly RADIUS      = 75;
  static readonly WAVELENGTH  = 35;
  static readonly AMPLITUDE   = 10;
  static readonly SPEED       = 2.5;
  private static readonly TICK_MS = 16;

  private cx = 0;
  private cy = 0;
  private phase     = 0;
  private strength  = 0;
  private ticksLeft = 0;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly repaint: () => void) {}

  private totalTicks(): number {
    return RippleEffect.DURATION_MS / RippleEffect.TICK_MS;
  }

  trigger(x: number, y: number): void {
    this.cx        = x;
    this.cy        = y;
    this.phase     = 0;
    this.ticksLeft = this.totalTicks();
    this.strength  = 1;
    this.clearTimer();
    this.timerId = setInterval(() => this.tick(), RippleEffect.TICK_MS);
  }

  isActive(): boolean { return this.timerId !== null; }

  stop(): void { this.clearTimer(); }

  applyToPixels(src: Uint8ClampedArray, dst: Uint8ClampedArray, w: number, h: number): void {
    const { cx, cy, phase, strength } = this;
    const R  = RippleEffect.RADIUS;
    const WL = RippleEffect.WAVELENGTH;
    const A  = RippleEffect.AMPLITUDE;
    const x0 = Math.max(0, cx - R);
    const x1 = Math.min(w - 1, cx + R);
    const y0 = Math.max(0, cy - R);
    const y1 = Math.min(h - 1, cy + R);

    for (let oy = y0; oy <= y1; oy++) {
      for (let ox = x0; ox <= x1; ox++) {
        const ddx = ox - cx;
        const ddy = oy - cy;
        const d   = Math.sqrt(ddx * ddx + ddy * ddy);
        if (d === 0 || d >= R) continue;

        const t            = d / R;
        const falloff      = 1 - t;
        const displacement = A * strength * falloff
          * Math.sin(2 * Math.PI * (d / WL - phase));
        const sourceDist   = d - displacement;
        const dstBase      = (oy * w + ox) * 4;

        if (sourceDist <= 0) {
          const srcBase = dstBase;
          dst[dstBase]     = src[srcBase];
          dst[dstBase + 1] = src[srcBase + 1];
          dst[dstBase + 2] = src[srcBase + 2];
          dst[dstBase + 3] = src[srcBase + 3];
          continue;
        }

        const scale   = sourceDist / d;
        const ix      = Math.max(0, Math.min(w - 1, Math.round(cx + ddx * scale)));
        const iy      = Math.max(0, Math.min(h - 1, Math.round(cy + ddy * scale)));
        const srcBase = (iy * w + ix) * 4;

        dst[dstBase]     = src[srcBase];
        dst[dstBase + 1] = src[srcBase + 1];
        dst[dstBase + 2] = src[srcBase + 2];
        dst[dstBase + 3] = src[srcBase + 3];
      }
    }
  }

  private tick(): void {
    this.phase     += RippleEffect.SPEED * (RippleEffect.TICK_MS / 1000);
    this.ticksLeft -= 1;
    this.strength   = Math.max(0, this.ticksLeft / this.totalTicks());
    if (this.ticksLeft <= 0) {
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
