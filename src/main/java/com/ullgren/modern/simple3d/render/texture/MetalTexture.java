package com.ullgren.modern.simple3d.render.texture;

/**
 * Procedural metallic surface texture.
 * <p>
 * Metal is characterised by sharp specular highlights tinted by the body's own
 * colour, compressed midtones, and a narrow white sheen at the brightest point.
 * A subtle single-octave noise grain breaks up the surface uniformity.
 * <p>
 * Unlike the web version (which pre-samples noise per vertex), Java computes the
 * noise per pixel — the JIT makes this cheap enough and produces finer grain detail.
 */
public final class MetalTexture implements Texture {

  /** Minimum effective brightness — metal reflects ambient light well but lower than stone for contrast. */
  private static final double METAL_AMBIENT   = 0.20;
  /** Diffuse scale — reduced to let specular highlights dominate. */
  private static final double DIFFUSE_SCALE   = 0.45;
  /** Exponent for body-color-tinted specular. */
  private static final int    SPEC_POWER      = 6;
  /** Intensity of body-color specular. */
  private static final double SPEC_STRENGTH   = 0.55;
  /** Exponent for narrow white highlight sheen. */
  private static final int    WHITE_POWER     = 14;
  /** Intensity of white highlight. */
  private static final double WHITE_STRENGTH  = 0.20;
  /** Exponent for wide soft glare bloom around the specular peak. */
  private static final int    GLARE_POWER     = 4;
  /** Intensity of soft glare bloom. */
  private static final double GLARE_STRENGTH  = 0.18;
  /** Surface grain noise range: [GRAIN_BASE, GRAIN_BASE + GRAIN_RANGE]. */
  private static final double GRAIN_BASE      = 0.94;
  private static final double GRAIN_RANGE     = 0.06;
  /** Noise frequency — single octave for subtle metallic grain. */
  private static final double NOISE_FREQ      = 0.025;

  // Pre-computed permutation table for value noise (256-entry, doubled to avoid wrapping).
  private static final int[] PERM = buildPerm();

  @Override
  public int applyPacked(double wx, double wy, double wz,
                          int baseR, int baseG, int baseB,
                          float shade) {
    // Subtle surface grain from single-octave noise.
    double noise = valueNoise3D(wx * NOISE_FREQ, wy * NOISE_FREQ, wz * NOISE_FREQ);

    // Compressed diffuse with ambient floor.
    double metalDiffuse = Math.max(METAL_AMBIENT, shade * DIFFUSE_SCALE);

    // Body-color-tinted specular (characteristic of metallic reflection).
    double metalSpec = Math.pow(shade, SPEC_POWER) * SPEC_STRENGTH;

    // Narrow white sheen at the brightest highlights for "shine".
    double whiteSpec = Math.pow(shade, WHITE_POWER) * WHITE_STRENGTH;

    // Wide soft glare bloom — creates the characteristic polished-metal glow.
    double glare = Math.pow(shade, GLARE_POWER) * GLARE_STRENGTH;

    // Surface grain.
    double grain = GRAIN_BASE + noise * GRAIN_RANGE;

    double effective = (metalDiffuse + metalSpec) * grain;
    double whiteTotal = whiteSpec + glare;

    int r = Math.min(255, (int) (baseR * effective + 255 * whiteTotal));
    int g = Math.min(255, (int) (baseG * effective + 255 * whiteTotal));
    int b = Math.min(255, (int) (baseB * effective + 255 * whiteTotal));

    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  // ------------------------------------------------------------------
  // 3D value noise (single octave)
  // ------------------------------------------------------------------

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
