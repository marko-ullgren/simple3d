import { Body } from '../model/Body.js';
import { Point3D } from '../model/Point3D.js';

/**
 * Controls the rotation animation of a {@link Body}.
 *
 * Click impulses adjust the angular momentum on the XZ and ZY axes.
 * A setInterval timer applies the accumulated momentum each tick and decays it
 * via friction until the body comes to rest.
 */
export class AnimationController {
  private static readonly FRICTION        = 0.995;
  private static readonly STOP_THRESHOLD  = 0.1;
  private static readonly TICK_MS         = 40;

  private angularMomentumXZ = 0;
  private angularMomentumYZ = 0;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private body: Body,
    private readonly repaint: () => void,
  ) {}

  setBody(body: Body): void {
    this.body = body;
  }

  /** Seeds the controller with initial momentum and starts the animation immediately. */
  kickstart(xz: number, yz: number): void {
    this.angularMomentumXZ = xz;
    this.angularMomentumYZ = yz;
    this.start();
  }

  /**
   * Applies a rotational impulse based on where the user clicked relative to the
   * canvas centre.  Clicks within the {@code sensitivity} dead-zone have no effect.
   */
  applyImpulse(
    mouseX: number, mouseY: number,
    centerX: number, centerY: number,
    sensitivity: number,
  ): void {
    const wasIdle = this.isIdle();

    if (mouseX < centerX - sensitivity) this.angularMomentumXZ -= 1;
    if (mouseX > centerX + sensitivity) this.angularMomentumXZ += 1;
    if (mouseY < centerY - sensitivity) this.angularMomentumYZ += 1;
    if (mouseY > centerY + sensitivity) this.angularMomentumYZ -= 1;

    if (this.isIdle()) {
      this.stop();
    } else if (wasIdle) {
      this.start();
    }
  }

  private start(): void {
    if (this.timerId !== null) return;
    this.timerId = setInterval(() => this.tick(), AnimationController.TICK_MS);
  }

  private stop(): void {
    this.angularMomentumXZ = 0;
    this.angularMomentumYZ = 0;
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  private tick(): void {
    this.body.rotateXZAngle(this.angularMomentumXZ * Point3D.ROTATION_ANGLE);
    this.body.rotateZYAngle(this.angularMomentumYZ * Point3D.ROTATION_ANGLE);
    this.angularMomentumXZ *= AnimationController.FRICTION;
    this.angularMomentumYZ *= AnimationController.FRICTION;
    if (this.isIdle()) this.stop();
    this.repaint();
  }

  private isIdle(): boolean {
    return (
      Math.abs(this.angularMomentumXZ) < AnimationController.STOP_THRESHOLD &&
      Math.abs(this.angularMomentumYZ) < AnimationController.STOP_THRESHOLD
    );
  }
}
