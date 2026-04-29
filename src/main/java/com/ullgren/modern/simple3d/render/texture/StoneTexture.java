package com.ullgren.modern.simple3d.render.texture;

/**
 * Procedural stone surface texture based on three-octave 3D value noise.
 * <p>
 * The noise is sampled at object-space coordinates so the pattern stays fixed to the surface
 * as the body rotates. Each sample drives:
 * <ul>
 *   <li>A greyscale stone base (cool grey, RGB ≈ 120–145) with subtle warm/cool tonal shifts.</li>
 *   <li>The Gouraud {@code shade} value is multiplied on top, preserving lighting.</li>
 * </ul>
 */
public final class StoneTexture implements Texture {

  // Noise frequency and amplitude per octave.
  private static final double[] FREQ = { 0.013, 0.031, 0.077 };
  private static final double[] AMP  = { 0.55,  0.30,  0.15  };

  // Pre-computed permutation table for value noise (256-entry, doubled to avoid wrapping).
  private static final int[] PERM = buildPerm();

  // ------------------------------------------------------------------
  // Texture interface
  // ------------------------------------------------------------------

  @Override
  public int applyPacked(double wx, double wy, double wz,
                         int baseR, int baseG, int baseB,
                         float shade) {
    double n = stoneNoise(wx, wy, wz); // [0, 1]

    // Stone base: slightly wider contrast range for a stronger appearance.
    int stoneR = 98  + (int) (n * 58);
    int stoneG = 95  + (int) (n * 52);
    int stoneB = 92  + (int) (n * 44);

    // Tint toward body colour at 15% so the original colour is still faintly visible.
    int r = (int) (stoneR * 0.85 + baseR * 0.15);
    int g = (int) (stoneG * 0.85 + baseG * 0.15);
    int b = (int) (stoneB * 0.85 + baseB * 0.15);

    // Apply Gouraud lighting.
    r = Math.min(255, (int) (r * shade));
    g = Math.min(255, (int) (g * shade));
    b = Math.min(255, (int) (b * shade));

    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  // ------------------------------------------------------------------
  // 3D value noise
  // ------------------------------------------------------------------

  /**
   * Returns a smooth noise value in [0, 1] at position {@code (x, y, z)}.
   * Three octaves are summed (fBm) for a natural stone appearance.
   */
  private static double stoneNoise(double x, double y, double z) {
    double total = 0.0;
    for (int i = 0; i < FREQ.length; i++) {
      total += valueNoise3D(x * FREQ[i], y * FREQ[i], z * FREQ[i]) * AMP[i];
    }
    return Math.max(0.0, Math.min(1.0, total));
  }

  /**
   * Single-octave 3D value noise with trilinear interpolation and smoothstep easing.
   * Returns a value in [0, 1].
   */
  private static double valueNoise3D(double x, double y, double z) {
    int ix = (int) Math.floor(x);
    int iy = (int) Math.floor(y);
    int iz = (int) Math.floor(z);

    double fx = x - ix;
    double fy = y - iy;
    double fz = z - iz;

    double ux = fade(fx);
    double uy = fade(fy);
    double uz = fade(fz);

    double v000 = latticeValue(ix,     iy,     iz    );
    double v100 = latticeValue(ix + 1, iy,     iz    );
    double v010 = latticeValue(ix,     iy + 1, iz    );
    double v110 = latticeValue(ix + 1, iy + 1, iz    );
    double v001 = latticeValue(ix,     iy,     iz + 1);
    double v101 = latticeValue(ix + 1, iy,     iz + 1);
    double v011 = latticeValue(ix,     iy + 1, iz + 1);
    double v111 = latticeValue(ix + 1, iy + 1, iz + 1);

    return lerp(uz,
        lerp(uy, lerp(ux, v000, v100), lerp(ux, v010, v110)),
        lerp(uy, lerp(ux, v001, v101), lerp(ux, v011, v111)));
  }

  private static double latticeValue(int ix, int iy, int iz) {
    int h = PERM[(PERM[(PERM[ix & 255] + iy) & 255] + iz) & 255];
    return h / 255.0;
  }

  private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
  private static double lerp(double t, double a, double b) { return a + t * (b - a); }

  /** Builds a deterministic 256-entry permutation table (LCG seed = 137). */
  private static int[] buildPerm() {
    int[] base = new int[256];
    long rng = 137L;
    for (int i = 0; i < 256; i++) {
      rng = (rng * 1664525L + 1013904223L) & 0xFFFFFFFFL;
      base[i] = (int) (rng & 0xFF);
    }
    int[] perm = new int[512];
    for (int i = 0; i < 512; i++) perm[i] = base[i & 255];
    return perm;
  }
}
