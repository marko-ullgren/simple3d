/**
 * Surface texture applied during rasterisation.
 *
 * Rendering is split into two steps to avoid expensive per-pixel work:
 *  1. {@link prepare} — called once per vertex; returns a single scalar that
 *     encodes whatever the texture needs (e.g. noise value). The Renderer
 *     interpolates this value linearly across each triangle scanline, the same
 *     way it already interpolates the Gouraud shading value.
 *  2. {@link applyPacked} — called per pixel with the interpolated scalar;
 *     returns the final colour as a packed 24-bit integer (0x00RRGGBB).
 */
export interface Texture {
  /**
   * Pre-samples the texture at object-space vertex position {@code (wx, wy, wz)}.
   * The returned value is interpolated by the Renderer and passed to {@link applyPacked}.
   */
  prepare(wx: number, wy: number, wz: number): number;

  /**
   * Computes the final pixel colour from the interpolated {@code texValue} and
   * the Gouraud {@code shade} factor. Returns a packed 24-bit integer (0x00RRGGBB).
   */
  applyPacked(
    texValue: number,
    baseR: number, baseG: number, baseB: number,
    shade: number,
  ): number;
}
