package com.ullgren.modern.simple3d.control;

import java.awt.Color;
import java.util.function.Consumer;
import javax.swing.Timer;

import com.ullgren.modern.simple3d.model.Body;

/**
 * Drives a scale-based transition between two {@link Body} shapes.
 * <p>
 * When {@link #morphTo} is called the currently active body shrinks to zero scale over
 * the first half of {@link #MORPH_DURATION_MS}, then the target body grows back to full
 * scale over the second half.  A smoothstep easing curve is applied to both phases.
 * <p>
 * If a new morph is requested while one is already running the transition is interrupted:
 * the scale snaps to zero, the pending body is discarded, and the new target immediately
 * begins its grow phase.
 * <p>
 * Create one instance per canvas.  Wire body-menu actions to {@link #morphTo}.
 * Call {@link #setColour} from colour-menu actions so that both the active and any
 * pending target body stay in sync.
 */
public class MorphController {

  /** Total duration of a shape transition in milliseconds. Adjust here to change speed. */
  public static final int MORPH_DURATION_MS = 1000;

  private static final int TICK_MS = 16; // ~60 fps update rate

  private Body   activeBody;
  private Body   targetBody;
  private double morphScale = 1.0;
  private long   startNanos;
  private boolean morphing = false;
  private boolean swapped  = false;
  private Timer  timer;

  private final Consumer<Body> onBodySwap;
  private final Runnable       repaint;

  /**
   * @param initial    the body to display before any morph begins
   * @param onBodySwap callback fired at the midpoint of each transition with the new active body;
   *                   wire this to {@link AnimationController#setBody}
   * @param repaint    callback to trigger a canvas repaint each morph tick
   */
  public MorphController(Body initial, Consumer<Body> onBodySwap, Runnable repaint) {
    this.activeBody  = initial;
    this.onBodySwap  = onBodySwap;
    this.repaint     = repaint;
  }

  /** The body that should be rendered and rotated this frame. */
  public Body getActiveBody() { return activeBody; }

  /**
   * Geometry morph factor in [0..1].
   * <p>
   * 1.0 = body at its full shape; 0.0 = body fully collapsed to a sphere of its average
   * vertex radius (the mid-morph intermediate state).  Pass this to
   * {@link com.ullgren.modern.simple3d.render.Renderer#render} to drive the elastic deformation.
   */
  public double getMorphFactor() { return morphScale; }

  /**
   * Start transitioning to {@code target}.
   * <p>
   * If a morph is already in progress it is interrupted: scale snaps to zero,
   * the new body is swapped in immediately, and the grow phase begins at once.
   */
  public void morphTo(Body target) {
    if (timer != null) timer.stop();

    if (morphing) {
      // Interrupted mid-morph: snap to zero, swap to new target immediately, start grow.
      morphScale  = 0.0;
      activeBody  = target;
      targetBody  = null;
      onBodySwap.accept(activeBody);
      swapped    = true;
      // Place the clock at exactly the halfway point so the grow phase proceeds normally.
      startNanos = System.nanoTime() - (MORPH_DURATION_MS / 2L * 1_000_000L);
    } else {
      // Fresh morph.
      targetBody = target;
      swapped    = false;
      startNanos = System.nanoTime();
      morphing   = true;
    }

    timer = new Timer(TICK_MS, e -> tick());
    timer.start();
  }

  /**
   * Applies {@code colour} to the active body and to any pending target body so that
   * colour-menu changes remain in effect across a transition.
   */
  public void setColour(Color colour) {
    activeBody.setColour(colour);
    if (targetBody != null) targetBody.setColour(colour);
  }

  private void tick() {
    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
    double t = Math.min(1.0, (double) elapsedMs / MORPH_DURATION_MS);

    if (!swapped) {
      if (t <= 0.5) {
        // Phase 1: shrink active body.
        morphScale = smoothstep(1.0, 0.0, t / 0.5);
      } else {
        // Crossed midpoint: swap bodies then grow.
        morphScale = 0.0;
        activeBody = targetBody;
        targetBody = null;
        onBodySwap.accept(activeBody);
        swapped    = true;
        morphScale = smoothstep(0.0, 1.0, (t - 0.5) / 0.5);
      }
    } else {
      // Phase 2: grow new body.
      morphScale = smoothstep(0.0, 1.0, (t - 0.5) / 0.5);
    }

    if (t >= 1.0) {
      morphScale = 1.0;
      morphing   = false;
      timer.stop();
    }

    repaint.run();
  }

  /** Smoothstep (Hermite) interpolation with clamped input. */
  private static double smoothstep(double from, double to, double t) {
    t = Math.max(0.0, Math.min(1.0, t));
    double s = t * t * (3 - 2 * t);
    return from + (to - from) * s;
  }
}
