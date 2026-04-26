/**
 * A point in three-dimensional space.
 *
 * Rotation methods apply a 2D rotation matrix in the named plane:
 *   | cos a   sin a |
 *   | -sin a  cos a |
 * where a = ROTATION_ANGLE (3°).  rotateAB rotates in the positive direction;
 * rotateBA rotates in reverse.
 */
export class Point3D {
  static readonly ROTATION_ANGLE = Math.PI / 180 * 3;
  static readonly SIN = Math.sin(Point3D.ROTATION_ANGLE);
  static readonly COS = Math.cos(Point3D.ROTATION_ANGLE);

  x: number;
  y: number;
  z: number;

  constructor(x = 0, y = 0, z = 0) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  rotateXZ(): void {
    const tmp = this.x;
    this.x = this.x * Point3D.COS - this.z * Point3D.SIN;
    this.z = tmp          * Point3D.SIN + this.z * Point3D.COS;
  }

  rotateYZ(): void {
    const tmp = this.y;
    this.y = this.y * Point3D.COS - this.z * Point3D.SIN;
    this.z = tmp          * Point3D.SIN + this.z * Point3D.COS;
  }

  rotateZX(): void {
    const tmp = this.z;
    this.z = this.z * Point3D.COS - this.x * Point3D.SIN;
    this.x = tmp          * Point3D.SIN + this.x * Point3D.COS;
  }

  rotateZY(): void {
    const tmp = this.z;
    this.z = this.z * Point3D.COS - this.y * Point3D.SIN;
    this.y = tmp          * Point3D.SIN + this.y * Point3D.COS;
  }

  rotateXZAngle(angle: number): void {
    const sin = Math.sin(angle), cos = Math.cos(angle);
    const tmp = this.x;
    this.x = this.x * cos - this.z * sin;
    this.z = tmp    * sin + this.z * cos;
  }

  rotateYZAngle(angle: number): void {
    const sin = Math.sin(angle), cos = Math.cos(angle);
    const tmp = this.y;
    this.y = this.y * cos - this.z * sin;
    this.z = tmp    * sin + this.z * cos;
  }

  rotateZXAngle(angle: number): void {
    const sin = Math.sin(angle), cos = Math.cos(angle);
    const tmp = this.z;
    this.z = this.z * cos - this.x * sin;
    this.x = tmp    * sin + this.x * cos;
  }

  rotateZYAngle(angle: number): void {
    const sin = Math.sin(angle), cos = Math.cos(angle);
    const tmp = this.z;
    this.z = this.z * cos - this.y * sin;
    this.y = tmp    * sin + this.y * cos;
  }
}
