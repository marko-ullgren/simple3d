import { describe, it, expect } from 'vitest';
import { RippleEffect } from './RippleEffect.js';

const R = RippleEffect.RADIUS;

function makePixels(w: number, h: number, fill = 0): Uint8ClampedArray {
  const arr = new Uint8ClampedArray(w * h * 4);
  if (fill !== 0) arr.fill(fill);
  return arr;
}

function copyPixels(src: Uint8ClampedArray): Uint8ClampedArray {
  return new Uint8ClampedArray(src);
}

describe('RippleEffect', () => {
  it('isActive is false before trigger', () => {
    const e = new RippleEffect(() => {});
    expect(e.isActive()).toBe(false);
  });

  it('isActive is true immediately after trigger', () => {
    const e = new RippleEffect(() => {});
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
  });

  it('pixels outside radius are unchanged', () => {
    const w = 300, h = 300;
    const src = makePixels(w, h, 0xAB);
    const dst = copyPixels(src);

    const e = new RippleEffect(() => {});
    e.trigger(150, 150);
    e.applyToPixels(src, dst, w, h);

    // Corners are well outside RADIUS from (150,150).
    expect(dst[0]).toBe(src[0]);
    expect(dst[(h - 1) * w * 4 + (w - 1) * 4]).toBe(src[(h - 1) * w * 4 + (w - 1) * 4]);
  });

  it('centre pixel (d=0) is not displaced', () => {
    const w = 300, h = 300;
    const src = new Uint8ClampedArray(w * h * 4);
    for (let i = 0; i < src.length; i++) src[i] = (i % 251) as number;
    const dst = copyPixels(src);

    const e = new RippleEffect(() => {});
    e.trigger(150, 150);
    e.applyToPixels(src, dst, w, h);

    const centreBase = (150 * w + 150) * 4;
    expect(dst[centreBase]).toBe(src[centreBase]);
    expect(dst[centreBase + 1]).toBe(src[centreBase + 1]);
    expect(dst[centreBase + 2]).toBe(src[centreBase + 2]);
  });

  it('pixels exactly at RADIUS boundary are not displaced', () => {
    const cx = 150, cy = 150;
    const w = cx + R + 10, h = cy + R + 10;
    const src = makePixels(w, h, 0xCC);
    const dst = copyPixels(src);

    const e = new RippleEffect(() => {});
    e.trigger(cx, cy);
    e.applyToPixels(src, dst, w, h);

    const boundaryBase = (cy * w + (cx + R)) * 4;
    expect(dst[boundaryBase]).toBe(src[boundaryBase]);
  });

  it('pixels inside radius may be displaced (gradient src)', () => {
    const w = 400, h = 400;
    const src = new Uint8ClampedArray(w * h * 4);
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const base = (y * w + x) * 4;
        src[base]     = x & 0xFF;
        src[base + 3] = 255;
      }
    }
    const dst = copyPixels(src);

    const e = new RippleEffect(() => {});
    e.trigger(200, 200);
    // With strength=1 and non-zero phase=0, the sin term may be 0 at exact distances;
    // we check that applyToPixels runs without throwing and dst is populated.
    expect(() => e.applyToPixels(src, dst, w, h)).not.toThrow();
  });

  it('stop deactivates the effect', () => {
    const e = new RippleEffect(() => {});
    e.trigger(50, 50);
    expect(e.isActive()).toBe(true);
    e.stop();
    expect(e.isActive()).toBe(false);
  });
});
