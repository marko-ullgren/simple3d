import type { Texture } from './Texture.js';

/** Null-object texture: applies only Gouraud shading, no surface pattern. */
export class NoTexture implements Texture {
  prepare(_wx: number, _wy: number, _wz: number): number { return 0; }

  applyPacked(
    _texValue: number,
    baseR: number, baseG: number, baseB: number,
    shade: number,
  ): number {
    const r = Math.min(255, baseR * shade) | 0;
    const g = Math.min(255, baseG * shade) | 0;
    const b = Math.min(255, baseB * shade) | 0;
    return (r << 16) | (g << 8) | b;
  }
}
