import { describe, it, expect } from 'vitest';
import { NoEffect } from './NoEffect.js';

function makePixels(w: number, h: number, fill = 0): Uint8ClampedArray {
  const arr = new Uint8ClampedArray(w * h * 4);
  if (fill !== 0) arr.fill(fill);
  return arr;
}

describe('NoEffect', () => {
  it('isActive always returns false', () => {
    const e = new NoEffect();
    expect(e.isActive()).toBe(false);
  });

  it('isActive remains false after trigger', () => {
    const e = new NoEffect();
    e.trigger(50, 50);
    expect(e.isActive()).toBe(false);
  });

  it('applyToPixels does not modify dst', () => {
    const w = 10, h = 10;
    const src = makePixels(w, h, 0xAB);
    const dst = makePixels(w, h, 0xCD);
    const original = new Uint8ClampedArray(dst);
    const e = new NoEffect();
    e.applyToPixels(src, dst, w, h);
    expect(dst).toEqual(original);
  });

  it('stop does not throw', () => {
    const e = new NoEffect();
    expect(() => e.stop()).not.toThrow();
  });
});
