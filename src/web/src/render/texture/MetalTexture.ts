import type { Texture } from './Texture.js';

/**
 * Procedural metallic surface texture.
 *
 * Metal is characterised by sharp specular highlights tinted by the body's own
 * colour, compressed midtones, and a narrow white sheen at the brightest point.
 * A subtle single-octave noise grain breaks up the surface uniformity.
 *
 * Performance: noise is evaluated once per vertex via {@link prepare} and the
 * result is interpolated linearly across each triangle by the Renderer.
 */
export class MetalTexture implements Texture {

  /** Minimum effective brightness — metal reflects ambient light well but lower than stone for contrast. */
  private static readonly METAL_AMBIENT   = 0.20;
  /** Diffuse scale — reduced to let specular highlights dominate. */
  private static readonly DIFFUSE_SCALE   = 0.45;
  /** Exponent for body-color-tinted specular. */
  private static readonly SPEC_POWER      = 6;
  /** Intensity of body-color specular. */
  private static readonly SPEC_STRENGTH   = 0.55;
  /** Exponent for narrow white highlight sheen. */
  private static readonly WHITE_POWER     = 14;
  /** Intensity of white highlight. */
  private static readonly WHITE_STRENGTH  = 0.15;
  /** Surface grain noise range: [GRAIN_BASE, GRAIN_BASE + GRAIN_RANGE]. */
  private static readonly GRAIN_BASE      = 0.94;
  private static readonly GRAIN_RANGE     = 0.06;
  /** Noise frequency — lower than stone so grain is visible after per-vertex interpolation. */
  private static readonly NOISE_FREQ      = 0.025;

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

  /** Pre-samples surface grain noise at an object-space vertex position. */
  prepare(wx: number, wy: number, wz: number): number {
    const f = MetalTexture.NOISE_FREQ;
    return MetalTexture.valueNoise3D(wx * f, wy * f, wz * f);
  }

  applyPacked(
    texValue: number,
    baseR: number, baseG: number, baseB: number,
    shade: number,
  ): number {
    // Compressed diffuse with ambient floor.
    const metalDiffuse = Math.max(MetalTexture.METAL_AMBIENT, shade * MetalTexture.DIFFUSE_SCALE);

    // Body-color-tinted specular (characteristic of metallic reflection).
    const metalSpec = Math.pow(shade, MetalTexture.SPEC_POWER) * MetalTexture.SPEC_STRENGTH;

    // Narrow white sheen at the brightest highlights for "shine".
    const whiteSpec = Math.pow(shade, MetalTexture.WHITE_POWER) * MetalTexture.WHITE_STRENGTH;

    // Subtle surface grain from interpolated noise.
    const grain = MetalTexture.GRAIN_BASE + texValue * MetalTexture.GRAIN_RANGE;

    const effective = (metalDiffuse + metalSpec) * grain;

    const r = Math.min(255, (baseR * effective + 255 * whiteSpec) | 0);
    const g = Math.min(255, (baseG * effective + 255 * whiteSpec) | 0);
    const b = Math.min(255, (baseB * effective + 255 * whiteSpec) | 0);

    return (r << 16) | (g << 8) | b;
  }

  private static valueNoise3D(x: number, y: number, z: number): number {
    const ix = Math.floor(x);
    const iy = Math.floor(y);
    const iz = Math.floor(z);
    const fx = x - ix, fy = y - iy, fz = z - iz;
    const ux = MetalTexture.fade(fx);
    const uy = MetalTexture.fade(fy);
    const uz = MetalTexture.fade(fz);

    const v000 = MetalTexture.lat(ix,     iy,     iz    );
    const v100 = MetalTexture.lat(ix + 1, iy,     iz    );
    const v010 = MetalTexture.lat(ix,     iy + 1, iz    );
    const v110 = MetalTexture.lat(ix + 1, iy + 1, iz    );
    const v001 = MetalTexture.lat(ix,     iy,     iz + 1);
    const v101 = MetalTexture.lat(ix + 1, iy,     iz + 1);
    const v011 = MetalTexture.lat(ix,     iy + 1, iz + 1);
    const v111 = MetalTexture.lat(ix + 1, iy + 1, iz + 1);

    return MetalTexture.lerp(uz,
      MetalTexture.lerp(uy, MetalTexture.lerp(ux, v000, v100), MetalTexture.lerp(ux, v010, v110)),
      MetalTexture.lerp(uy, MetalTexture.lerp(ux, v001, v101), MetalTexture.lerp(ux, v011, v111)),
    );
  }

  private static lat(ix: number, iy: number, iz: number): number {
    const perm = MetalTexture.PERM;
    return perm[(perm[(perm[ix & 255] + iy) & 255] + iz) & 255] / 255;
  }

  private static fade(t: number): number { return t * t * t * (t * (t * 6 - 15) + 10); }
  private static lerp(t: number, a: number, b: number): number { return a + t * (b - a); }
}
