package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

/**
 * A static starfield background of {@value #STAR_COUNT} stars.
 * <p>
 * Stars are stored as normalised (0–1) coordinates so they scale correctly when the canvas
 * is resized. Each star has an independently chosen size and brightness.
 * The layout is deterministic (seed 42).
 */
public class StarField {

  private static final int STAR_COUNT = 200;

  private final float[] starX      = new float[STAR_COUNT];
  private final float[] starY      = new float[STAR_COUNT];
  private final float[] starRadius = new float[STAR_COUNT];
  private final float[] starAlpha  = new float[STAR_COUNT];

  public StarField() {
    Random rng = new Random(42);
    for (int i = 0; i < STAR_COUNT; i++) {
      starX[i]      = rng.nextFloat();
      starY[i]      = rng.nextFloat();
      // Most stars are tiny (radius 0.5–1.5 px); a few are larger
      starRadius[i] = 0.5f + rng.nextFloat() * rng.nextFloat() * 2.0f;
      starAlpha[i]  = 0.4f + rng.nextFloat() * 0.6f;
    }
  }

  /**
   * Draws the star field onto {@code g} for a canvas of size {@code w} × {@code h}.
   */
  public void draw(Graphics g, int w, int h) {
    for (int i = 0; i < STAR_COUNT; i++) {
      int alpha = (int) (starAlpha[i] * 255);
      g.setColor(new Color(255, 255, 255, alpha));
      int x = (int) (starX[i] * w);
      int y = (int) (starY[i] * h);
      int d = Math.max(1, Math.round(starRadius[i] * 2));
      if (d <= 1) {
        g.drawLine(x, y, x, y);
      } else {
        g.fillOval(x, y, d, d);
      }
    }
  }
}
