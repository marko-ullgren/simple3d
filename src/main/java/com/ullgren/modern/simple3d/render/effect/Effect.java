package com.ullgren.modern.simple3d.render.effect;

/**
 * Image-space post-processing effect applied to the already-rendered frame each tick.
 * <p>
 * The Renderer calls {@link #isActive()} before every frame; if it returns {@code true} the
 * Renderer copies the current canvas into a scratch buffer and passes both to
 * {@link #applyToPixels} so the effect can perform its pixel manipulation.
 * <p>
 * {@link #trigger(int, int)} is the user-facing entry point: it starts (or restarts) the
 * effect centred on the given canvas coordinates.
 */
public interface Effect {

  /**
   * Starts or restarts the effect, anchored at the given canvas position.
   *
   * @param x horizontal canvas coordinate of the trigger point
   * @param y vertical canvas coordinate of the trigger point
   */
  void trigger(int x, int y);

  /**
   * Returns {@code true} while the effect has work to do this frame.
   * The Renderer skips the {@link #applyToPixels} pass entirely when this is {@code false}.
   */
  boolean isActive();

  /**
   * Reads pixels from {@code src} (the unmodified rendered frame) and writes the manipulated
   * result into {@code dst}.
   *
   * @param src source pixel array ({@code TYPE_INT_ARGB}; must not be the same array as {@code dst})
   * @param dst destination pixel array (same length as {@code src})
   * @param w   image width in pixels
   * @param h   image height in pixels
   */
  void applyToPixels(int[] src, int[] dst, int w, int h);

  /**
   * Stops the effect immediately, releasing any internal animation timer.
   * Called before the active effect is replaced by another one.
   * The default implementation is a no-op; stateful effects should override it.
   */
  default void stop() {}
}
