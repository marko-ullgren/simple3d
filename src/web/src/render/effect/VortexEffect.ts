import type { Effect } from './Effect.js';

export class VortexEffect implements Effect {
  static readonly DURATION_MS = 1200;
  static readonly RADIUS      = 75;
  static readonly MAX_ANGLE   = Math.PI * 1.0;
  private static readonly TICK_MS = 16;

  private cx = 0;
  private cy = 0;
  private angle     = 0;
  private ticksLeft = 0;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly repaint: () => void) {}

  private totalTicks(): number {
    return VortexEffect.DURATION_MS / VortexEffect.TICK_MS;
  }

  trigger(x: number, y: number): void {
    this.cx        = x;
    this.cy        = y;
    this.ticksLeft = this.totalTicks();
    this.angle     = VortexEffect.MAX_ANGLE;
    this.clearTimer();
    this.timerId = setInterval(() => this.tick(), VortexEffect.TICK_MS);
  }

  isActive(): boolean { return this.timerId !== null; }

  stop(): void { this.clearTimer(); }

  applyToPixels(src: Uint8ClampedArray, dst: Uint8ClampedArray, w: number, h: number): void {
    const { cx, cy, angle } = this;
    const R  = VortexEffect.RADIUS;
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

        const t       = d / R;
        const falloff = (1 - t) * (1 - t);
        const rot     = -angle * falloff;
        const cosR    = Math.cos(rot);
        const sinR    = Math.sin(rot);
        const ix      = Math.max(0, Math.min(w - 1, Math.round(cx + ddx * cosR - ddy * sinR)));
        const iy      = Math.max(0, Math.min(h - 1, Math.round(cy + ddx * sinR + ddy * cosR)));
        const dstBase = (oy * w + ox) * 4;
        const srcBase = (iy * w + ix) * 4;

        dst[dstBase]     = src[srcBase];
        dst[dstBase + 1] = src[srcBase + 1];
        dst[dstBase + 2] = src[srcBase + 2];
        dst[dstBase + 3] = src[srcBase + 3];
      }
    }
  }

  private tick(): void {
    this.ticksLeft -= 1;
    this.angle      = VortexEffect.MAX_ANGLE * this.ticksLeft / this.totalTicks();
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
