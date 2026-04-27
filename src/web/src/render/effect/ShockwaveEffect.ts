import type { Effect } from './Effect.js';

export class ShockwaveEffect implements Effect {
  static readonly EXPAND_SPEED = 250;
  static readonly WAVE_WIDTH   = 20;
  static readonly STRENGTH     = 8;
  static readonly MAX_RADIUS   = 400;
  private static readonly TICK_MS = 16;

  private cx     = 0;
  private cy     = 0;
  private radius = 0;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly repaint: () => void) {}

  trigger(x: number, y: number): void {
    this.cx     = x;
    this.cy     = y;
    this.radius = ShockwaveEffect.WAVE_WIDTH;
    this.clearTimer();
    this.timerId = setInterval(() => this.tick(), ShockwaveEffect.TICK_MS);
  }

  isActive(): boolean { return this.timerId !== null; }

  stop(): void { this.clearTimer(); }

  applyToPixels(src: Uint8ClampedArray, dst: Uint8ClampedArray, w: number, h: number): void {
    const { cx, cy, radius } = this;
    const WW     = ShockwaveEffect.WAVE_WIDTH;
    const outerR = Math.ceil(radius + WW);
    const x0 = Math.max(0, cx - outerR);
    const x1 = Math.min(w - 1, cx + outerR);
    const y0 = Math.max(0, cy - outerR);
    const y1 = Math.min(h - 1, cy + outerR);

    for (let oy = y0; oy <= y1; oy++) {
      for (let ox = x0; ox <= x1; ox++) {
        const ddx        = ox - cx;
        const ddy        = oy - cy;
        const d          = Math.sqrt(ddx * ddx + ddy * ddy);
        const distToRing = Math.abs(d - radius);
        if (d === 0 || distToRing >= WW) continue;

        const t            = distToRing / WW;
        const bell         = (1 - t) * (1 - t);
        const displacement = ShockwaveEffect.STRENGTH * bell;
        const sourceDist   = Math.max(0, d - displacement);
        const dstBase      = (oy * w + ox) * 4;

        if (sourceDist === 0) {
          const srcBase = (cy * w + cx) * 4;
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
    this.radius += ShockwaveEffect.EXPAND_SPEED * (ShockwaveEffect.TICK_MS / 1000);
    if (this.radius > ShockwaveEffect.MAX_RADIUS) {
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
