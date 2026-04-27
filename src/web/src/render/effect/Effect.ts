export interface Effect {
  trigger(x: number, y: number): void;
  isActive(): boolean;
  applyToPixels(src: Uint8ClampedArray, dst: Uint8ClampedArray, w: number, h: number): void;
  stop(): void;
}
