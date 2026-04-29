// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Renderer } from './Renderer.js';
import { Body, COLOURS } from '../model/Body.js';
import type { Effect } from './effect/Effect.js';
import { StoneTexture } from './texture/StoneTexture.js';
import { NoTexture } from './texture/NoTexture.js';

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

/** A body with a non-convex cap face (> 4 vertices) to exercise ear-clip. */
const CAP_BODY = `
points
  0  60  0
-20  20  0
-60   0  0
-20 -20  0
  0 -60  0
 20 -20  0
 60   0  0
 20  20  0
  0  60  1
-20  20  1
-60   0  1
-20 -20  1
  0 -60  1
 20 -20  1
 60   0  1
 20  20  1
faces
0 1 2 3 4 5 6 7
8 9 10 11 12 13 14 15
0 8 9 1
1 9 10 2
2 10 11 3
3 11 12 4
4 12 13 5
5 13 14 6
6 14 15 7
7 15 8 0
`;

function makeBody(): Body {
  return Body.fromText(CUBE_BODY, COLOURS.blue);
}

function makeCapBody(): Body {
  return Body.fromText(CAP_BODY, COLOURS.red);
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
    stroke:          vi.fn(),
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

  it('setTexture(StoneTexture) renders without throwing', () => {
    const renderer = new Renderer();
    renderer.setTexture(new StoneTexture());
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('setTexture(NoTexture) renders without throwing', () => {
    const renderer = new Renderer();
    renderer.setTexture(new NoTexture());
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('wireframe mode renders without throwing', () => {
    const renderer = new Renderer();
    renderer.setWireframeMode(true);
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('wireframe mode calls stroke() on the internal canvas', () => {
    const sharedCtx = makeMockCtx();
    (HTMLCanvasElement.prototype as any).getContext = () => sharedCtx;
    const renderer = new Renderer();
    renderer.setWireframeMode(true);
    renderer.render(makeBody(), sharedCtx, 50, 50, 1, 100, 100);
    expect(sharedCtx.stroke).toHaveBeenCalled();
  });

  it('wireframe mode calls clearRect on the hi-res canvas', () => {
    const sharedCtx = makeMockCtx();
    (HTMLCanvasElement.prototype as any).getContext = () => sharedCtx;
    const renderer = new Renderer();
    renderer.setWireframeMode(true);
    renderer.render(makeBody(), sharedCtx, 50, 50, 1, 100, 100);
    expect(sharedCtx.clearRect).toHaveBeenCalled();
  });

  it('switching from filled to wireframe and back does not throw', () => {
    const renderer = new Renderer();
    const ctx = makeMockCtx();
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    renderer.setWireframeMode(true);
    renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100);
    renderer.setWireframeMode(false);
    renderer.setTexture(new StoneTexture());
    expect(() => renderer.render(makeBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('cap body (> 4 vertex faces) renders without throwing in filled mode', () => {
    const renderer = new Renderer();
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeCapBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('cap body (> 4 vertex faces) renders without throwing with stone texture', () => {
    const renderer = new Renderer();
    renderer.setTexture(new StoneTexture());
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeCapBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });

  it('cap body renders without throwing in wireframe mode', () => {
    const renderer = new Renderer();
    renderer.setWireframeMode(true);
    const ctx = makeMockCtx();
    expect(() => renderer.render(makeCapBody(), ctx, 50, 50, 1, 100, 100)).not.toThrow();
  });
});
