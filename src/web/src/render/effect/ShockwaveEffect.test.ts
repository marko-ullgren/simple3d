import { describe, it, expect } from 'vitest';
import { ShockwaveEffect } from './ShockwaveEffect.js';

const WW = ShockwaveEffect.WAVE_WIDTH;

function makePixels(w: number, h: number, fill = 0): Uint8ClampedArray {
  const arr = new Uint8ClampedArray(w * h * 4);
  if (fill !== 0) arr.fill(fill);
  return arr;
}

function copyPixels(src: Uint8ClampedArray): Uint8ClampedArray {
  return new Uint8ClampedArray(src);
}

describe('ShockwaveEffect', () => {
  it('isActive is false before trigger', () => {
    const e = new ShockwaveEffect(() => {});
    expect(e.isActive()).toBe(false);
  });

  it('isActive is true immediately after trigger', () => {
    const e = new ShockwaveEffect(() => {});
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
  });

  it('pixels far from the ring are unchanged', () => {
    const w = 300, h = 300;
    const src = makePixels(w, h, 0xAB);
    const dst = copyPixels(src);

    const e = new ShockwaveEffect(() => {});
    e.trigger(150, 150);
    // radius starts at WAVE_WIDTH (~20), so pixels at corners are untouched.
    e.applyToPixels(src, dst, w, h);

    expect(dst[0]).toBe(src[0]);
    expect(dst[(h - 1) * w * 4 + (w - 1) * 4]).toBe(src[(h - 1) * w * 4 + (w - 1) * 4]);
  });

  it('centre pixel (d=0) is not displaced', () => {
    const w = 300, h = 300;
    const src = new Uint8ClampedArray(w * h * 4);
    for (let i = 0; i < src.length; i++) src[i] = (i % 251) as number;
    const dst = copyPixels(src);

    const e = new ShockwaveEffect(() => {});
    e.trigger(150, 150);
    e.applyToPixels(src, dst, w, h);

    const centreBase = (150 * w + 150) * 4;
    expect(dst[centreBase]).toBe(src[centreBase]);
  });

  it('pixels on the ring are displaced (gradient src)', () => {
    const w = 400, h = 400;
    const cx = 200, cy = 200;
    const src = new Uint8ClampedArray(w * h * 4);
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const base = (y * w + x) * 4;
        src[base]     = x & 0xFF;
        src[base + 3] = 255;
      }
    }
    const dst = copyPixels(src);

    const e = new ShockwaveEffect(() => {});
    e.trigger(cx, cy);

    // Place a test pixel exactly on the ring: distance = radius = WAVE_WIDTH, so distToRing=0.
    // WAVE_WIDTH is the initial radius, so pixels at distance ~WAVE_WIDTH from centre are on the ring.
    e.applyToPixels(src, dst, w, h);

    // At (cx + WW, cy): d ≈ WW, distToRing ≈ 0, bell ≈ 1 → should be displaced.
    const testBase = (cy * w + (cx + Math.round(WW))) * 4;
    const origR  = src[testBase];
    const displR = dst[testBase];
    expect(displR).not.toBe(origR);
  });

  it('stop deactivates the effect', () => {
    const e = new ShockwaveEffect(() => {});
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
    e.stop();
    expect(e.isActive()).toBe(false);
  });
});
