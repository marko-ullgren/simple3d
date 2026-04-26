package com.ullgren.modern.simple3d.control;

import javax.swing.Timer;

import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.Point3D;

/**
 * Controls the rotation animation of a {@link Body}.
 * <p>
 * Click impulses adjust the angular momentum on the XZ and ZY axes. A Swing {@link Timer}
 * applies the accumulated momentum to the body each tick and decays it via friction until
 * the body comes to rest.
 * <p>
 * Create one instance per canvas. Call {@link #setBody(Body)} whenever the active body changes.
 * Wire mouse clicks to {@link #applyImpulse(int, int, int, int, int)}.
 */
public class AnimationController {

  private static final double FRICTION       = 0.995;
  private static final double STOP_THRESHOLD = 0.1;
  private static final int    TICK_MS        = 40;

  private double angularMomentumXZ;
  private double angularMomentumYZ;
  private Body   body;
  private Timer  timer;

  private final Runnable repaint;

  /**
   * @param body    initial body to animate
   * @param repaint callback invoked each frame to trigger a canvas repaint
   */
  public AnimationController(Body body, Runnable repaint) {
    this.body    = body;
    this.repaint = repaint;
  }

  /** Replaces the body being animated (called when the user switches shapes). */
  public void setBody(Body body) {
    this.body = body;
  }

  /**
   * Seeds the controller with an initial angular momentum and starts the animation immediately.
   * Intended to give the object a slow idle rotation when the app first opens.
   *
   * @param xz initial momentum on the XZ axis
   * @param yz initial momentum on the ZY axis
   */
  public void kickstart(double xz, double yz) {
    angularMomentumXZ = xz;
    angularMomentumYZ = yz;
    start();
  }

  /**
   * Applies a rotational impulse based on where the user clicked relative to the canvas centre.
   * Clicks outside the {@code sensitivity} dead-zone increment the angular momentum on the
   * corresponding axis; the animation starts or stops accordingly.
   *
   * @param mouseX      click x in canvas coordinates
   * @param mouseY      click y in canvas coordinates
   * @param centerX     canvas centre x
   * @param centerY     canvas centre y
   * @param sensitivity dead-zone radius in pixels; clicks within this distance have no effect
   */
  public void applyImpulse(int mouseX, int mouseY, int centerX, int centerY, int sensitivity) {
    boolean wasIdle = isIdle();

    if (mouseX < centerX - sensitivity) angularMomentumXZ -= 1.0;
    if (mouseX > centerX + sensitivity) angularMomentumXZ += 1.0;
    if (mouseY < centerY - sensitivity) angularMomentumYZ += 1.0;
    if (mouseY > centerY + sensitivity) angularMomentumYZ -= 1.0;

    if (isIdle()) {
      stop();
    } else if (wasIdle) {
      start();
    }
  }

  private void start() {
    if (timer == null) {
      timer = new Timer(TICK_MS, e -> tick());
    }
    timer.start();
  }

  private void stop() {
    angularMomentumXZ = angularMomentumYZ = 0;
    if (timer != null) timer.stop();
  }

  private void tick() {
    body.rotateXZ(angularMomentumXZ * Point3D.ROTATION_ANGLE);
    body.rotateZY(angularMomentumYZ * Point3D.ROTATION_ANGLE);
    angularMomentumXZ *= FRICTION;
    angularMomentumYZ *= FRICTION;
    if (isIdle()) stop();
    repaint.run();
  }

  private boolean isIdle() {
    return Math.abs(angularMomentumXZ) < STOP_THRESHOLD
        && Math.abs(angularMomentumYZ) < STOP_THRESHOLD;
  }
}
