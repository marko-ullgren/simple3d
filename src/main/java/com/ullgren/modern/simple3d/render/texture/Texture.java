package com.ullgren.modern.simple3d.render.texture;

/**
 * A surface texture that can be applied to rendered geometry.
 * <p>
 * {@link #applyPacked} is called per pixel during scanline rasterisation with interpolated
 * object-space coordinates and the Gouraud shade value. Returns a packed {@code 0xAARRGGBB}
 * integer so no {@link java.awt.Color} objects are allocated on the hot path.
 */
public interface Texture {

  /**
   * Returns the packed ARGB colour for a surface point at object-space position
   * {@code (wx, wy, wz)}, given the base body colour ({@code baseR/G/B}) and
   * the Gouraud shade value {@code shade} in [0, 1].
   */
  int applyPacked(double wx, double wy, double wz,
                  int baseR, int baseG, int baseB,
                  float shade);
}
