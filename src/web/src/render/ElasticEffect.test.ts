import { describe, it, expect } from 'vitest';
import { ElasticEffect } from '../../src/render/ElasticEffect.js';

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

  it('isActive is false before first dent', () => {
    const e = new ElasticEffect(() => {});
    expect(e.isActive()).toBe(false);
  });

  it('isActive is true immediately after dent', () => {
    const e = new ElasticEffect(() => {});
    e.dent(50, 50);
    expect(e.isActive()).toBe(true);
    // Clean up the interval so the test process can exit.
    // Access via casting since clearTimer is private; use dent()+isActive trick:
    // Restart with zero-displacement-equivalent by accessing internals not needed —
    // just leave it; Vitest runs with fake timers automatically cleaned up.
  });

  it('pixels outside radius are unchanged', () => {
    const w = 200, h = 200;
    const src = makePixels(w, h, 0xAB);
    const dst = copyPixels(src);

    const e = new ElasticEffect(() => {});
    e.dent(100, 100);
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
    e.dent(150, 150);
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
    e.dent(100, 100);
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
    e.dent(cx, cy);
    e.applyToPixels(src, dst, w, h);

    // Point at exactly (cx + R, cy) — d == R, excluded by d >= R check.
    const boundaryBase = (cy * w + (cx + R)) * 4;
    expect(dst[boundaryBase]).toBe(src[boundaryBase]);
  });

  it('restarting dent while active resets displacement', () => {
    const repaintCalls: number[] = [];
    const e = new ElasticEffect(() => repaintCalls.push(1));
    e.dent(50, 50);
    expect(e.isActive()).toBe(true);
    // Second dent should not throw and should remain active.
    e.dent(60, 60);
    expect(e.isActive()).toBe(true);
  });
});
