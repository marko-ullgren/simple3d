import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AnimationController } from '../../src/control/AnimationController.js';
import { Body, COLOURS } from '../../src/model/Body.js';

const QUAD = `
points
-50 -50 0
 50 -50 0
 50  50 0
-50  50 0
faces
0 1 2 3
`;

function makeBody(): Body {
  return Body.fromText(QUAD, COLOURS.blue);
}

describe('AnimationController', () => {

  beforeEach(() => { vi.useFakeTimers(); });
  afterEach(() => { vi.useRealTimers(); });

  it('repaint is not called before any impulse', () => {
    const repaint = vi.fn();
    new AnimationController(makeBody(), repaint);
    vi.advanceTimersByTime(500);
    expect(repaint).not.toHaveBeenCalled();
  });

  it('applyImpulse within dead zone does not start animation', () => {
    const repaint = vi.fn();
    const ctrl = new AnimationController(makeBody(), repaint);
    // Click right at centre — within SENSITIVITY=50 dead zone on both axes.
    ctrl.applyImpulse(200, 200, 200, 200, 50);
    vi.advanceTimersByTime(200);
    expect(repaint).not.toHaveBeenCalled();
  });

  it('applyImpulse to the left starts animation and calls repaint', () => {
    const repaint = vi.fn();
    const ctrl = new AnimationController(makeBody(), repaint);
    ctrl.applyImpulse(100, 200, 200, 200, 50); // left of centre
    vi.advanceTimersByTime(120); // three 40ms ticks
    expect(repaint.mock.calls.length).toBeGreaterThanOrEqual(2);
  });

  it('applyImpulse to the right starts animation', () => {
    const repaint = vi.fn();
    const ctrl = new AnimationController(makeBody(), repaint);
    ctrl.applyImpulse(300, 200, 200, 200, 50); // right of centre
    vi.advanceTimersByTime(80);
    expect(repaint.mock.calls.length).toBeGreaterThanOrEqual(1);
  });

  it('applyImpulse above centre starts animation', () => {
    const repaint = vi.fn();
    const ctrl = new AnimationController(makeBody(), repaint);
    ctrl.applyImpulse(200, 100, 200, 200, 50); // above centre
    vi.advanceTimersByTime(80);
    expect(repaint.mock.calls.length).toBeGreaterThanOrEqual(1);
  });

  it('body points change after animation ticks', () => {
    const body = makeBody();
    const snap = Array.from({ length: body.pointCount() }, (_, i) => body.pointAt(i).x);
    const ctrl = new AnimationController(body, () => {});
    ctrl.applyImpulse(100, 200, 200, 200, 50);
    vi.advanceTimersByTime(40); // one tick
    const changed = snap.some((x, i) => Math.abs(x - body.pointAt(i).x) > 1e-10);
    expect(changed).toBe(true);
  });

  it('setBody swaps the body being animated', () => {
    const body1 = makeBody();
    const body2 = makeBody();
    const snap2 = Array.from({ length: body2.pointCount() }, (_, i) => body2.pointAt(i).x);
    const ctrl = new AnimationController(body1, () => {});
    ctrl.setBody(body2);
    ctrl.applyImpulse(100, 200, 200, 200, 50);
    vi.advanceTimersByTime(40);
    // body1 must NOT have been rotated.
    const body1Changed = snap2.some((_, i) =>
      Math.abs(body1.pointAt(i).x - snap2[i]) > 1e-10,
    );
    expect(body1Changed).toBe(false);
    // body2 MUST have been rotated.
    const body2Changed = snap2.some((x, i) =>
      Math.abs(body2.pointAt(i).x - x) > 1e-10,
    );
    expect(body2Changed).toBe(true);
  });

  it('kickstart sets initial momentum and starts animation', () => {
    const repaint = vi.fn();
    const ctrl = new AnimationController(makeBody(), repaint);
    ctrl.kickstart(0.5, 0.3);
    vi.advanceTimersByTime(40); // one tick
    expect(repaint.mock.calls.length).toBeGreaterThanOrEqual(1);
  });

  it('opposite impulses cancel and animation stops', () => {
    const repaint = vi.fn();
    const ctrl = new AnimationController(makeBody(), repaint);
    // Left impulse, then equal right impulse → net momentum = 0 → stops.
    ctrl.applyImpulse(100, 200, 200, 200, 50); // XZ -= 1
    ctrl.applyImpulse(300, 200, 200, 200, 50); // XZ += 1
    repaint.mockClear();
    vi.advanceTimersByTime(200);
    expect(repaint).not.toHaveBeenCalled();
  });
});
