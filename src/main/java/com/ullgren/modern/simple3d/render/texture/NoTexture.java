package com.ullgren.modern.simple3d.render.texture;

/**
 * Null-object {@link Texture}: applies only Gouraud shading with no textural variation.
 */
public final class NoTexture implements Texture {

  @Override
  public int applyPacked(double wx, double wy, double wz,
                         int baseR, int baseG, int baseB,
                         float shade) {
    int r = Math.min(255, (int) (baseR * shade));
    int g = Math.min(255, (int) (baseG * shade));
    int b = Math.min(255, (int) (baseB * shade));
    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }
}
