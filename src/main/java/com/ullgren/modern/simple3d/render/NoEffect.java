package com.ullgren.modern.simple3d.render;

/**
 * Null-object implementation of {@link Effect}: does nothing.
 * Used when the user selects "No Effect" from the Effect menu.
 */
public class NoEffect implements Effect {

  @Override
  public void trigger(int x, int y) {}

  @Override
  public boolean isActive() { return false; }

  @Override
  public void applyToPixels(int[] src, int[] dst, int w, int h) {}
}
