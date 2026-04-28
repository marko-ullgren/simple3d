// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Renderer } from './Renderer.js';
import { Body, COLOURS } from '../model/Body.js';
import type { Effect } from './effect/Effect.js';

const CUBE_BODY = `
points
-50 -50 -50
 50 -50 -50
 50  50 -50
-50  50 -50
-50 -50  50
 50 -50  50
 50  50  50
-50  50  50
faces
0 1 2 3
4 5 6 7
0 1 5 4
2 3 7 6
1 2 6 5
0 3 7 4
`;

function makeBody(): Body {
  return Body.fromText(CUBE_BODY, COLOURS.blue);
}

/** Creates a fully-featured mock 2D context that won't throw inside Renderer. */
function makeMockCtx(): CanvasRenderingContext2D {
  return {
    createImageData: (w: number, h: number) => ({ width: w, height: h, data: new Uint8ClampedArray(w * h * 4), colorSpace: "srgb" as PredefinedColorSpace } as ImageData),
    getImageData:    (_x: number, _y: number, w: number, h: number) => ({ width: w, height: h, data: new Uint8ClampedArray(w * h * 4), colorSpace: "srgb" as PredefinedColorSpace } as ImageData),
    putImageData:    vi.fn(),
    drawImage:       vi.fn(),
    clearRect:       vi.fn(),
    save:            vi.fn(),
    restore:         vi.fn(),
    beginPath:       vi.fn(),
    moveTo:          vi.fn(),
    lineTo:          vi.fn(),
    closePath:       vi.fn(),
    fill:            vi.fn(),
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high' as ImageSmoothingQuality,
    fillStyle:  '',
    strokeStyle: '',
    lineWidth:   1,
    globalAlpha: 1,
  } as unknown as CanvasRenderingContext2D;
}

describe('Renderer', () => {
  let originalGetContext: typeof HTMLCanvasElement.prototype.getContext;

  beforeEach(() => {
    // Renderer creates its own internal canvases; mock their getContext so they
    // return a functional context rather than null (happy-dom canvas limitation).
    originalGetContext = HTMLCanvasElement.prototype.getContext;
    (HTMLCanvasElement.prototype as any).getContext = () => makeMockCtx();
  });

  afterEach(() => {
    HTMLCanvasElement.prototype.getContext = originalGetContext;
  });

  it('render() does not throw for a visible body', () => {
    const renderer = new Renderer();
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('ctx.drawImage is called to composite the geometry layer', () => {
    const renderer = new Renderer();
    const ctx = makeMockCtx();
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    // Renderer calls ctx.drawImage(geoCanvas, 0, 0) to composite onto the caller's ctx.
    expect(ctx.drawImage).toHaveBeenCalled();
  });

  it('applyToPixels is called when effect is active', () => {
    const renderer = new Renderer();
    const applyToPixels = vi.fn();
    const effect: Effect = {
      trigger:       vi.fn(),
      isActive:      () => true,
      applyToPixels,
      stop:          vi.fn(),
    };
    renderer.setEffect(effect);
    const ctx = makeMockCtx();
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    expect(applyToPixels).toHaveBeenCalled();
  });

  it('applyToPixels is not called when effect is inactive', () => {
    const renderer = new Renderer();
    const applyToPixels = vi.fn();
    const effect: Effect = {
      trigger:       vi.fn(),
      isActive:      () => false,
      applyToPixels,
      stop:          vi.fn(),
    };
    renderer.setEffect(effect);
    const ctx = makeMockCtx();
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    expect(applyToPixels).not.toHaveBeenCalled();
  });

  it('second render with same dimensions does not recreate internal canvases', () => {
    const renderer = new Renderer();
    const ctx = makeMockCtx();
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    const createSpy = vi.spyOn(document, 'createElement');
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    // Renderer caches hiCanvas / geoCanvas — no new createElement calls.
    expect(createSpy.mock.calls.length).toBe(0);
  });

  it('second render with different dimensions recreates internal canvases', () => {
    const renderer = new Renderer();
    const ctx = makeMockCtx();
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    const createSpy = vi.spyOn(document, 'createElement');
    renderer.render(makeBody(), ctx, 75, 75, 1, 150, 150); // different dimensions
    expect(createSpy).toHaveBeenCalledWith('canvas');
  });
});
