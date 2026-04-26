import { describe, it, expect } from 'vitest';
import { Point3D } from '../../src/model/Point3D.js';

const DELTA = 1e-9;
const near = (a: number, b: number) => Math.abs(a - b) < DELTA;
const expectNear = (a: number, b: number) =>
  expect(Math.abs(a - b)).toBeLessThan(DELTA);

describe('Point3D', () => {

  // --- Constructors ---

  it('default constructor sets all fields to zero', () => {
    const p = new Point3D();
    expectNear(p.x, 0); expectNear(p.y, 0); expectNear(p.z, 0);
  });

  it('constructor sets x y z', () => {
    const p = new Point3D(1.5, 2.5, 3.5);
    expectNear(p.x, 1.5); expectNear(p.y, 2.5); expectNear(p.z, 3.5);
  });

  it('copy via spread is independent', () => {
    const p = new Point3D(4, 5, 6);
    const copy = new Point3D(p.x, p.y, p.z);
    copy.rotateXZ();
    expectNear(p.x, 4); // original unchanged
  });

  // --- rotateXZ ---

  it('rotateXZ applies correct matrix to unit x', () => {
    const p = new Point3D(1, 0, 0);
    p.rotateXZ();
    expectNear(p.x, Point3D.COS);
    expectNear(p.y, 0);
    expectNear(p.z, Point3D.SIN);
  });

  it('rotateXZ preserves XZ norm', () => {
    const p = new Point3D(3, 7, 4);
    const before = p.x ** 2 + p.z ** 2;
    p.rotateXZ();
    expectNear(p.x ** 2 + p.z ** 2, before);
  });

  it('rotateXZ does not affect y', () => {
    const p = new Point3D(3, 7, 4);
    p.rotateXZ();
    expectNear(p.y, 7);
  });

  it('rotateXZ 120-step round-trip restores original', () => {
    const p = new Point3D(5, 3, 2);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    for (let i = 0; i < 120; i++) p.rotateXZ();
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  it('rotateXZ leaves origin at origin', () => {
    const p = new Point3D();
    p.rotateXZ();
    expectNear(p.x, 0); expectNear(p.y, 0); expectNear(p.z, 0);
  });

  it('rotateZX is inverse of rotateXZ', () => {
    const p = new Point3D(3, 7, 4);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    p.rotateXZ(); p.rotateZX();
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  // --- rotateYZ ---

  it('rotateYZ applies correct matrix to unit y', () => {
    const p = new Point3D(0, 1, 0);
    p.rotateYZ();
    expectNear(p.x, 0);
    expectNear(p.y, Point3D.COS);
    expectNear(p.z, Point3D.SIN);
  });

  it('rotateYZ preserves YZ norm', () => {
    const p = new Point3D(3, 7, 4);
    const before = p.y ** 2 + p.z ** 2;
    p.rotateYZ();
    expectNear(p.y ** 2 + p.z ** 2, before);
  });

  it('rotateYZ does not affect x', () => {
    const p = new Point3D(3, 7, 4);
    p.rotateYZ();
    expectNear(p.x, 3);
  });

  it('rotateYZ 120-step round-trip restores original', () => {
    const p = new Point3D(5, 3, 2);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    for (let i = 0; i < 120; i++) p.rotateYZ();
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  it('rotateYZ leaves origin at origin', () => {
    const p = new Point3D();
    p.rotateYZ();
    expectNear(p.x, 0); expectNear(p.y, 0); expectNear(p.z, 0);
  });

  it('rotateZY is inverse of rotateYZ', () => {
    const p = new Point3D(3, 7, 4);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    p.rotateYZ(); p.rotateZY();
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  // --- rotateXZAngle / rotateZXAngle ---

  it('rotateXZAngle(0) does nothing', () => {
    const p = new Point3D(3, 7, 4);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    p.rotateXZAngle(0);
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  it('rotateXZAngle(ROTATION_ANGLE) matches rotateXZ()', () => {
    const a = new Point3D(3, 7, 4);
    const b = new Point3D(3, 7, 4);
    a.rotateXZ();
    b.rotateXZAngle(Point3D.ROTATION_ANGLE);
    expectNear(a.x, b.x); expectNear(a.y, b.y); expectNear(a.z, b.z);
  });

  it('rotateXZAngle(a) then rotateXZAngle(-a) restores original', () => {
    const p = new Point3D(3, 7, 4);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    p.rotateXZAngle(Math.PI / 6);
    p.rotateXZAngle(-Math.PI / 6);
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  // --- rotateYZAngle / rotateZYAngle ---

  it('rotateYZAngle(0) does nothing', () => {
    const p = new Point3D(3, 7, 4);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    p.rotateYZAngle(0);
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  it('rotateYZAngle(ROTATION_ANGLE) matches rotateYZ()', () => {
    const a = new Point3D(3, 7, 4);
    const b = new Point3D(3, 7, 4);
    a.rotateYZ();
    b.rotateYZAngle(Point3D.ROTATION_ANGLE);
    expectNear(a.x, b.x); expectNear(a.y, b.y); expectNear(a.z, b.z);
  });

  it('rotateYZAngle(a) then rotateYZAngle(-a) restores original', () => {
    const p = new Point3D(3, 7, 4);
    const [x0, y0, z0] = [p.x, p.y, p.z];
    p.rotateYZAngle(Math.PI / 6);
    p.rotateYZAngle(-Math.PI / 6);
    expectNear(p.x, x0); expectNear(p.y, y0); expectNear(p.z, z0);
  });

  // --- rotateZXAngle / rotateZYAngle ---

  it('rotateZXAngle(ROTATION_ANGLE) matches rotateZX()', () => {
    const a = new Point3D(3, 7, 4);
    const b = new Point3D(3, 7, 4);
    a.rotateZX();
    b.rotateZXAngle(Point3D.ROTATION_ANGLE);
    expectNear(a.x, b.x); expectNear(a.y, b.y); expectNear(a.z, b.z);
  });

  it('rotateZYAngle(ROTATION_ANGLE) matches rotateZY()', () => {
    const a = new Point3D(3, 7, 4);
    const b = new Point3D(3, 7, 4);
    a.rotateZY();
    b.rotateZYAngle(Point3D.ROTATION_ANGLE);
    expectNear(a.x, b.x); expectNear(a.y, b.y); expectNear(a.z, b.z);
  });
});
