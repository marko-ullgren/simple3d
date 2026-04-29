import type { Texture } from './Texture.js';

/**
 * Procedural stone surface texture based on three-octave 3D value noise.
 *
 * The noise is sampled at object-space coordinates so the pattern stays fixed
 * to the surface as the body rotates. Each sample drives a greyscale stone base
 * (cool grey) with subtle tonal shifts, blended at 85% stone / 15% body colour.
 */
export class StoneTexture implements Texture {

  private static readonly FREQ = [0.013, 0.031, 0.077];
  private static readonly AMP  = [0.55,  0.30,  0.15 ];

  /** Deterministic 256-entry permutation table, doubled to 512 (LCG seed = 137). */
  private static readonly PERM: Uint8Array = (() => {
    const base = new Uint8Array(256);
    let rng = 137;
    for (let i = 0; i < 256; i++) {
      rng = ((rng * 1664525 + 1013904223) >>> 0);
      base[i] = rng & 0xFF;
    }
    const perm = new Uint8Array(512);
    for (let i = 0; i < 512; i++) perm[i] = base[i & 255];
    return perm;
  })();

  applyPacked(
    wx: number, wy: number, wz: number,
    baseR: number, baseG: number, baseB: number,
    shade: number,
  ): number {
    const n = StoneTexture.stoneNoise(wx, wy, wz);

    const stoneR = 98  + (n * 58) | 0;
    const stoneG = 95  + (n * 52) | 0;
    const stoneB = 92  + (n * 44) | 0;

    // Tint toward body colour at 15%.
    const r = (stoneR * 0.85 + baseR * 0.15) | 0;
    const g = (stoneG * 0.85 + baseG * 0.15) | 0;
    const b = (stoneB * 0.85 + baseB * 0.15) | 0;

    return (Math.min(255, (r * shade) | 0) << 16)
         | (Math.min(255, (g * shade) | 0) << 8)
         |  Math.min(255, (b * shade) | 0);
  }

  private static stoneNoise(x: number, y: number, z: number): number {
    let total = 0;
    const freq = StoneTexture.FREQ;
    const amp  = StoneTexture.AMP;
    for (let i = 0; i < freq.length; i++) {
      total += StoneTexture.valueNoise3D(x * freq[i], y * freq[i], z * freq[i]) * amp[i];
    }
    return Math.max(0, Math.min(1, total));
  }

  private static valueNoise3D(x: number, y: number, z: number): number {
    const ix = Math.floor(x);
    const iy = Math.floor(y);
    const iz = Math.floor(z);
    const fx = x - ix, fy = y - iy, fz = z - iz;
    const ux = StoneTexture.fade(fx);
    const uy = StoneTexture.fade(fy);
    const uz = StoneTexture.fade(fz);

    const v000 = StoneTexture.lat(ix,     iy,     iz    );
    const v100 = StoneTexture.lat(ix + 1, iy,     iz    );
    const v010 = StoneTexture.lat(ix,     iy + 1, iz    );
    const v110 = StoneTexture.lat(ix + 1, iy + 1, iz    );
    const v001 = StoneTexture.lat(ix,     iy,     iz + 1);
    const v101 = StoneTexture.lat(ix + 1, iy,     iz + 1);
    const v011 = StoneTexture.lat(ix,     iy + 1, iz + 1);
    const v111 = StoneTexture.lat(ix + 1, iy + 1, iz + 1);

    return StoneTexture.lerp(uz,
      StoneTexture.lerp(uy, StoneTexture.lerp(ux, v000, v100), StoneTexture.lerp(ux, v010, v110)),
      StoneTexture.lerp(uy, StoneTexture.lerp(ux, v001, v101), StoneTexture.lerp(ux, v011, v111)),
    );
  }

  private static lat(ix: number, iy: number, iz: number): number {
    const perm = StoneTexture.PERM;
    return perm[(perm[(perm[ix & 255] + iy) & 255] + iz) & 255] / 255;
  }

  private static fade(t: number): number { return t * t * t * (t * (t * 6 - 15) + 10); }
  private static lerp(t: number, a: number, b: number): number { return a + t * (b - a); }
}
