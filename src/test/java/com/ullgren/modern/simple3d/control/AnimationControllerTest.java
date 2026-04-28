package com.ullgren.modern.simple3d.control;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.Timer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.BodyLoader;

public class AnimationControllerTest {

  private static final int CX = 200, CY = 200, SENS = 50;

  private Body body;
  private AnimationController ctrl;

  @BeforeEach
  void setUp() {
    body = BodyLoader.load("/com/ullgren/modern/simple3d/cube.body", Color.blue);
    ctrl = new AnimationController(body, () -> {});
  }

  // -------------------------------------------------------------------------
  // applyImpulse — momentum adjustments
  // -------------------------------------------------------------------------

  @Test
  void leftClick_decrementsXZ() throws Exception {
    ctrl.applyImpulse(CX - SENS - 1, CY, CX, CY, SENS);
    assertEquals(-1.0, getMomentumXZ(), 1e-9);
    assertEquals( 0.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void rightClick_incrementsXZ() throws Exception {
    ctrl.applyImpulse(CX + SENS + 1, CY, CX, CY, SENS);
    assertEquals(1.0, getMomentumXZ(), 1e-9);
    assertEquals(0.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void topClick_incrementsYZ() throws Exception {
    ctrl.applyImpulse(CX, CY - SENS - 1, CX, CY, SENS);
    assertEquals(0.0, getMomentumXZ(), 1e-9);
    assertEquals(1.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void bottomClick_decrementsYZ() throws Exception {
    ctrl.applyImpulse(CX, CY + SENS + 1, CX, CY, SENS);
    assertEquals( 0.0, getMomentumXZ(), 1e-9);
    assertEquals(-1.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void deadZoneClick_doesNotChangeMomentum() throws Exception {
    ctrl.applyImpulse(CX, CY, CX, CY, SENS);
    assertEquals(0.0, getMomentumXZ(), 1e-9);
    assertEquals(0.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void diagonalClick_affectsBothAxes() throws Exception {
    ctrl.applyImpulse(CX + SENS + 1, CY - SENS - 1, CX, CY, SENS);
    assertEquals(1.0, getMomentumXZ(), 1e-9);
    assertEquals(1.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void multipleClicks_accumulate() throws Exception {
    ctrl.applyImpulse(CX - SENS - 1, CY, CX, CY, SENS);
    ctrl.applyImpulse(CX - SENS - 1, CY, CX, CY, SENS);
    assertEquals(-2.0, getMomentumXZ(), 1e-9);
  }

  @Test
  void oppositeClicks_cancelAndTimerStops() throws Exception {
    ctrl.applyImpulse(CX - SENS - 1, CY, CX, CY, SENS); // XZ = -1
    ctrl.applyImpulse(CX + SENS + 1, CY, CX, CY, SENS); // XZ = +1 → net 0
    assertFalse(isTimerRunning(), "timer should stop when momenta cancel");
    assertEquals(0.0, getMomentumXZ(), 1e-9);
    assertEquals(0.0, getMomentumYZ(), 1e-9);
  }

  @Test
  void nonZeroImpulse_startsTimer() throws Exception {
    ctrl.applyImpulse(CX - SENS - 1, CY, CX, CY, SENS);
    assertTrue(isTimerRunning(), "timer should start after non-zero impulse");
  }

  // -------------------------------------------------------------------------
  // kickstart
  // -------------------------------------------------------------------------

  @Test
  void kickstart_setsMomentaAndStartsTimer() throws Exception {
    ctrl.kickstart(0.5, 0.3);
    assertEquals(0.5, getMomentumXZ(), 1e-9);
    assertEquals(0.3, getMomentumYZ(), 1e-9);
    assertTrue(isTimerRunning());
  }

  // -------------------------------------------------------------------------
  // tick — rotation and friction
  // -------------------------------------------------------------------------

  @Test
  void tick_rotatesBodyProportionallyToMomentum() throws Exception {
    setMomentumXZ(1.0);
    double xBefore = body.pointAt(0).getX();
    tick();
    assertNotEquals(xBefore, body.pointAt(0).getX(), "body should have rotated");
  }

  @Test
  void tick_appliesFriction() throws Exception {
    double friction = 0.995;
    setMomentumXZ(5.0);
    setMomentumYZ(3.0);
    tick();
    assertEquals(5.0 * friction, getMomentumXZ(), 1e-9);
    assertEquals(3.0 * friction, getMomentumYZ(), 1e-9);
  }

  @Test
  void tick_stopsWhenMomentaDecayBelowThreshold() throws Exception {
    // 0.1005 * 0.995 = 0.099975 < 0.1 → should trigger stop()
    setMomentumXZ(0.1005);
    setMomentumYZ(0.0);
    tick();
    assertFalse(isTimerRunning(), "timer should stop when momentum falls below threshold");
    assertEquals(0.0, getMomentumXZ(), 1e-9);
  }

  @Test
  void tick_callsRepaint() throws Exception {
    int[] calls = {0};
    AnimationController c = new AnimationController(body, () -> calls[0]++);
    setMomentumXZ(c, 1.0);
    tick(c);
    assertEquals(1, calls[0]);
  }

  // -------------------------------------------------------------------------
  // setBody
  // -------------------------------------------------------------------------

  @Test
  void setBody_subsequentTickRotatesNewBodyNotOld() throws Exception {
    Body body2 = BodyLoader.load("/com/ullgren/modern/simple3d/cube.body", Color.red);
    double x0Before  = body.pointAt(0).getX();
    double x0Before2 = body2.pointAt(0).getX();

    ctrl.setBody(body2);
    setMomentumXZ(1.0);
    tick();

    assertEquals(x0Before,   body.pointAt(0).getX(),  1e-9, "old body must not rotate");
    assertNotEquals(x0Before2, body2.pointAt(0).getX(), "new body must rotate");
  }

  // -------------------------------------------------------------------------
  // Reflection helpers
  // -------------------------------------------------------------------------

  private double getMomentumXZ() throws Exception { return getMomentumXZ(ctrl); }
  private double getMomentumYZ() throws Exception { return getMomentumYZ(ctrl); }

  private static double getMomentumXZ(AnimationController c) throws Exception {
    return (double) field("angularMomentumXZ").get(c);
  }
  private static double getMomentumYZ(AnimationController c) throws Exception {
    return (double) field("angularMomentumYZ").get(c);
  }

  private void setMomentumXZ(double v) throws Exception { setMomentumXZ(ctrl, v); }
  private void setMomentumYZ(double v) throws Exception { field("angularMomentumYZ").set(ctrl, v); }

  private static void setMomentumXZ(AnimationController c, double v) throws Exception {
    field("angularMomentumXZ").set(c, v);
  }

  private boolean isTimerRunning() throws Exception {
    Timer t = (Timer) field("timer").get(ctrl);
    return t != null && t.isRunning();
  }

  private void tick() throws Exception { tick(ctrl); }

  private static void tick(AnimationController c) throws Exception {
    Method m = AnimationController.class.getDeclaredMethod("tick");
    m.setAccessible(true);
    m.invoke(c);
  }

  private static Field field(String name) throws Exception {
    Field f = AnimationController.class.getDeclaredField(name);
    f.setAccessible(true);
    return f;
  }
}
