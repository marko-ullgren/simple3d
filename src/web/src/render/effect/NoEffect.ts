import type { Effect } from './Effect.js';

export class NoEffect implements Effect {
  trigger(_x: number, _y: number): void {}
  isActive(): boolean { return false; }
  applyToPixels(_src: Uint8ClampedArray, _dst: Uint8ClampedArray, _w: number, _h: number): void {}
  stop(): void {}
}
