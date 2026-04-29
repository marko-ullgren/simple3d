/**
 * Surface texture applied per pixel during rasterisation.
 */
export interface Texture {
  /**
   * Returns the colour for a pixel at object-space position (wx, wy, wz)
   * as a packed 24-bit integer (0x00RRGGBB).
   */
  applyPacked(
    wx: number, wy: number, wz: number,
    baseR: number, baseG: number, baseB: number,
    shade: number,
  ): number;
}
