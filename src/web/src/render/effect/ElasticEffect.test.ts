import { describe, it, expect, vi } from 'vitest';
import { ElasticEffect } from './ElasticEffect.js';

const R = ElasticEffect.RADIUS;

function makePixels(w: number, h: number, fill = 0): Uint8ClampedArray {
  const arr = new Uint8ClampedArray(w * h * 4);
  if (fill !== 0) arr.fill(fill);
  return arr;
}

function copyPixels(src: Uint8ClampedArray): Uint8ClampedArray {
  return new Uint8ClampedArray(src);
}

describe('ElasticEffect', () => {

  it('isActive is false before first trigger', () => {
    const e = new ElasticEffect(() => {});
    expect(e.isActive()).toBe(false);
  });

  it('isActive is true immediately after trigger', () => {
    const e = new ElasticEffect(() => {});
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
  });

  it('pixels outside radius are unchanged', () => {
    const w = 200, h = 200;
    const src = makePixels(w, h, 0xAB);
    const dst = copyPixels(src);

    const e = new ElasticEffect(() => {});
    e.trigger(100, 100);
    e.applyToPixels(src, dst, w, h);

    // Corners are well outside RADIUS from (100,100).
    expect(dst[0]).toBe(src[0]);
    expect(dst[(h - 1) * w * 4 + (w - 1) * 4]).toBe(src[(h - 1) * w * 4 + (w - 1) * 4]);
  });

  it('pixels inside radius are displaced when displacement > 0', () => {
    const w = 300, h = 300;
    // Gradient: each pixel's red channel encodes its x position.
    const src = new Uint8ClampedArray(w * h * 4);
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const base = (y * w + x) * 4;
        src[base]     = x & 0xFF; // R encodes x
        src[base + 3] = 255;
      }
    }
    const dst = copyPixels(src);

    const e = new ElasticEffect(() => {});
    e.trigger(150, 150);
    e.applyToPixels(src, dst, w, h);

    // At (170, 150) — 20px from centre, well inside RADIUS — should be displaced.
    const origR = src[(150 * w + 170) * 4];
    const displR = dst[(150 * w + 170) * 4];
    expect(displR).not.toBe(origR);
  });

  it('centre pixel (d=0) is not displaced', () => {
    const w = 200, h = 200;
    const src = new Uint8ClampedArray(w * h * 4);
    for (let i = 0; i < src.length; i++) src[i] = (i % 251) as number; // varied values
    const dst = copyPixels(src);

    const e = new ElasticEffect(() => {});
    e.trigger(100, 100);
    e.applyToPixels(src, dst, w, h);

    const centreBase = (100 * w + 100) * 4;
    expect(dst[centreBase]).toBe(src[centreBase]);
    expect(dst[centreBase + 1]).toBe(src[centreBase + 1]);
    expect(dst[centreBase + 2]).toBe(src[centreBase + 2]);
  });

  it('pixels exactly at RADIUS boundary are not displaced', () => {
    const cx = 100, cy = 100;
    const w = cx + R + 10, h = cy + R + 10;
    const src = makePixels(w, h, 0xCC);
    const dst = copyPixels(src);

    const e = new ElasticEffect(() => {});
    e.trigger(cx, cy);
    e.applyToPixels(src, dst, w, h);

    // Point at exactly (cx + R, cy) — d == R, excluded by d >= R check.
    const boundaryBase = (cy * w + (cx + R)) * 4;
    expect(dst[boundaryBase]).toBe(src[boundaryBase]);
  });

  it('restarting trigger while active resets displacement', () => {
    const repaintCalls: number[] = [];
    const e = new ElasticEffect(() => repaintCalls.push(1));
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
    // Second trigger should not throw and should remain active.
    e.trigger(60, 60);
    expect(e.isActive()).toBe(true);
  });

  it('stop() makes isActive return false', () => {
    const e = new ElasticEffect(() => {});
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
    e.stop();
    expect(e.isActive()).toBe(false);
  });

  it('stop() before trigger is safe', () => {
    const e = new ElasticEffect(() => {});
    expect(() => e.stop()).not.toThrow();
    expect(e.isActive()).toBe(false);
  });

  it('tick() calls repaint and eventually stops', () => {
    vi.useFakeTimers();
    const repaint = vi.fn();
    const e = new ElasticEffect(repaint);
    e.trigger(50, 50);
    vi.advanceTimersByTime(2000); // let spring settle
    expect(repaint.mock.calls.length).toBeGreaterThan(0);
    expect(e.isActive()).toBe(false); // stopped after settling
    vi.useRealTimers();
  });

  it('applyToPixels: large negative displacement triggers sourceDist<=0 branch', () => {
    const W = 20, H = 20;
    const src = new Uint8ClampedArray(W * H * 4);
    const dst = new Uint8ClampedArray(W * H * 4);
    for (let i = 0; i < src.length; i += 4) {
      src[i] = 200; src[i+1] = 100; src[i+2] = 50; src[i+3] = 255;
    }
    const e = new ElasticEffect(() => {});
    e.trigger(10, 10); // centre at (10,10)
    (e as any).displacement = -100; // large negative → sourceDist <= 0 near centre
    expect(() => e.applyToPixels(src, dst, W, H)).not.toThrow();
    // Pixels near centre should have been copied from src (identity mapping at sourceDist<=0).
    // pixel at (10, 9): ddx=0, ddy=-1, d=1
    // t=1/60, mag = -100 * 4 * (1/60) * (59/60)^2 ≈ -6.46 → sourceDist = 1 + (-6.46) ≤ 0 → identity
    const adjPixel = (9 * W + 10) * 4;
    expect(dst[adjPixel]).toBe(src[adjPixel]);
  });
});
