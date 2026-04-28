import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { Body, COLOURS } from '../../src/model/Body.js';
import { Point3D } from '../../src/model/Point3D.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const RES = resolve(__dirname, '../../../../src/main/resources/com/ullgren/modern/simple3d');

function readBody(name: string): string {
  return readFileSync(resolve(RES, name), 'utf-8');
}

// Minimal inline fixtures — avoid needing the filesystem for basic tests.
const TRIANGLE = `
points
0 0 0
100 0 0
0 100 0
faces
0 1 2
`;

const QUAD = `
points
-50 -50 0
 50 -50 0
 50  50 0
-50  50 0
faces
0 1 2 3
`;

// A cube oriented so its front face is visible (z negative = towards viewer).
const CUBE_TEXT = readBody('cube.body');
const MU_TEXT   = readBody('mu.body');

describe('Body — loading', () => {

  it('parses a triangle body correctly', () => {
    const b = Body.fromText(TRIANGLE, COLOURS.blue);
    expect(b.pointCount()).toBe(3);
    expect(b.faceCount()).toBe(1);
    expect(b.faceAt(0)).toEqual([0, 1, 2]);
  });

  it('sets the colour', () => {
    const b = Body.fromText(TRIANGLE, COLOURS.red);
    expect(b.getColour()).toEqual(COLOURS.red);
  });

  it('ignores comment lines and blank lines', () => {
    const text = `
# this is a comment
points
# another comment
0 0 0
100 0 0
0 100 0

faces
0 1 2
`;
    const b = Body.fromText(text, COLOURS.blue);
    expect(b.pointCount()).toBe(3);
    expect(b.faceCount()).toBe(1);
  });

  it('loads mu.body — 36 points, 20 faces', () => {
    const b = Body.fromText(MU_TEXT, COLOURS.blue);
    expect(b.pointCount()).toBe(36);
    expect(b.faceCount()).toBe(20);
  });

  it('loads cube.body — 8 points, 6 faces', () => {
    const text = readBody('cube.body');
    const b = Body.fromText(text, COLOURS.blue);
    expect(b.pointCount()).toBe(8);
    expect(b.faceCount()).toBe(6);
  });
});

describe('Body — parse errors', () => {

  it('throws on data before any section header', () => {
    expect(() => Body.fromText('0 0 0', COLOURS.blue)).toThrow();
  });

  it('throws on point with two coordinates', () => {
    expect(() => Body.fromText('points\n0 0\nfaces\n0 1 2', COLOURS.blue)).toThrow();
  });

  it('throws on point with four coordinates', () => {
    expect(() => Body.fromText('points\n0 0 0 0\nfaces\n0 1 2', COLOURS.blue)).toThrow();
  });

  it('throws on face with fewer than 3 indices', () => {
    expect(() => Body.fromText('points\n0 0 0\n1 0 0\nfaces\n0 1', COLOURS.blue)).toThrow();
  });

  it('throws on face index out of range', () => {
    expect(() =>
      Body.fromText('points\n0 0 0\n1 0 0\n0 1 0\nfaces\n0 1 99', COLOURS.blue),
    ).toThrow();
  });

  it('throws on no points defined', () => {
    expect(() => Body.fromText('points\nfaces\n0 1 2', COLOURS.blue)).toThrow();
  });

  it('throws on no faces defined', () => {
    expect(() => Body.fromText('points\n0 0 0\n1 0 0\n0 1 0\nfaces\n', COLOURS.blue)).toThrow();
  });
});

describe('Body — colour', () => {

  it('setColour updates the colour', () => {
    const b = Body.fromText(TRIANGLE, COLOURS.blue);
    b.setColour(COLOURS.red);
    expect(b.getColour()).toEqual(COLOURS.red);
  });
});

describe('Body — rotation', () => {

  it('rotateXZ changes point positions', () => {
    const b = Body.fromText(QUAD, COLOURS.blue);
    const x0 = b.pointAt(0).x;
    b.rotateXZ();
    expect(b.pointAt(0).x).not.toBeCloseTo(x0, 6);
  });

  it('rotateZY changes point positions', () => {
    const b = Body.fromText(QUAD, COLOURS.blue);
    const z0 = b.pointAt(0).z;
    b.rotateZY();
    // rotateZY touches the z and y components
    expect(b.pointAt(0).z).not.toBeCloseTo(z0, 6);
  });

  it('all no-arg rotation methods complete without throwing', () => {
    const b = Body.fromText(QUAD, COLOURS.blue);
    expect(() => {
      b.rotateXZ(); b.rotateYZ(); b.rotateZX(); b.rotateZY();
    }).not.toThrow();
  });

  it('rotateXZAngle(0) leaves all points unchanged', () => {
    const b = Body.fromText(QUAD, COLOURS.blue);
    const snap = Array.from({ length: b.pointCount() }, (_, i) => ({
      x: b.pointAt(i).x, y: b.pointAt(i).y, z: b.pointAt(i).z,
    }));
    b.rotateXZAngle(0);
    for (let i = 0; i < b.pointCount(); i++) {
      expect(b.pointAt(i).x).toBeCloseTo(snap[i].x, 9);
      expect(b.pointAt(i).y).toBeCloseTo(snap[i].y, 9);
      expect(b.pointAt(i).z).toBeCloseTo(snap[i].z, 9);
    }
  });

  it('rotateXZAngle(ROTATION_ANGLE) matches rotateXZ()', () => {
    const a = Body.fromText(QUAD, COLOURS.blue);
    const b = Body.fromText(QUAD, COLOURS.blue);
    a.rotateXZ();
    b.rotateXZAngle(Point3D.ROTATION_ANGLE);
    for (let i = 0; i < a.pointCount(); i++) {
      expect(a.pointAt(i).x).toBeCloseTo(b.pointAt(i).x, 9);
      expect(a.pointAt(i).z).toBeCloseTo(b.pointAt(i).z, 9);
    }
  });

  it('rotateXZAngle(a) then rotateXZAngle(-a) restores orientation', () => {
    const b = Body.fromText(MU_TEXT, COLOURS.blue);
    const snap = Array.from({ length: b.pointCount() }, (_, i) => ({
      x: b.pointAt(i).x, y: b.pointAt(i).y, z: b.pointAt(i).z,
    }));
    b.rotateXZAngle(Math.PI / 6);
    b.rotateXZAngle(-Math.PI / 6);
    for (let i = 0; i < b.pointCount(); i++) {
      expect(b.pointAt(i).x).toBeCloseTo(snap[i].x, 9);
      expect(b.pointAt(i).z).toBeCloseTo(snap[i].z, 9);
    }
  });

  it('rotateZYAngle(0) leaves all points unchanged', () => {
    const b = Body.fromText(QUAD, COLOURS.blue);
    const snap = Array.from({ length: b.pointCount() }, (_, i) => ({
      x: b.pointAt(i).x, y: b.pointAt(i).y, z: b.pointAt(i).z,
    }));
    b.rotateZYAngle(0);
    for (let i = 0; i < b.pointCount(); i++) {
      expect(b.pointAt(i).x).toBeCloseTo(snap[i].x, 9);
      expect(b.pointAt(i).y).toBeCloseTo(snap[i].y, 9);
      expect(b.pointAt(i).z).toBeCloseTo(snap[i].z, 9);
    }
  });
});

describe('Body — ambient occlusion', () => {

  it('flat triangle has AO = 0 on all vertices', () => {
    const b = Body.fromText(TRIANGLE, COLOURS.red);
    for (let i = 0; i < b.pointCount(); i++) {
      expect(b.getVertexAO(i)).toBeCloseTo(0, 5);
    }
  });

  it('cube corner vertices have AO > 0', () => {
    const b = Body.fromText(CUBE_TEXT, COLOURS.red);
    let anyPositive = false;
    for (let i = 0; i < b.pointCount(); i++) {
      if (b.getVertexAO(i) > 0) { anyPositive = true; break; }
    }
    expect(anyPositive).toBe(true);
  });

  it('all vertex AO values are in [0, 1]', () => {
    const b = Body.fromText(CUBE_TEXT, COLOURS.red);
    for (let i = 0; i < b.pointCount(); i++) {
      const ao = b.getVertexAO(i);
      expect(ao).toBeGreaterThanOrEqual(0);
      expect(ao).toBeLessThanOrEqual(1);
    }
  });
});

describe('Body — orientation section', () => {

  const ORIENTED = `
points
  0   0 0
  0 100 0
100   0 0
faces
0 1 2
orientation
ZY 1
`;

  it('ZY 1 rotates point (0,100,0) by one step', () => {
    const b = Body.fromText(ORIENTED, COLOURS.blue);
    // rotateZY: new_y = 100*COS, new_z = -100*SIN
    expect(b.pointAt(1).y).toBeCloseTo( 100 * Point3D.COS, 9);
    expect(b.pointAt(1).z).toBeCloseTo(-100 * Point3D.SIN, 9);
    expect(b.pointAt(1).x).toBeCloseTo(0, 9);
  });

  it('body without orientation section loads unmodified', () => {
    const b = Body.fromText(TRIANGLE, COLOURS.blue);
    expect(b.pointAt(1).z).toBeCloseTo(0, 9);
  });

  it('unknown axis throws', () => {
    expect(() => Body.fromText(`
points
0 0 0
0 1 0
1 0 0
faces
0 1 2
orientation
XQ 5
`, COLOURS.blue)).toThrow(/unknown axis/i);
  });

  it('zero steps throws', () => {
    expect(() => Body.fromText(`
points
0 0 0
0 1 0
1 0 0
faces
0 1 2
orientation
ZY 0
`, COLOURS.blue)).toThrow(/positive/i);
  });

  it('missing steps token throws', () => {
    expect(() => Body.fromText(`
points
0 0 0
0 1 0
1 0 0
faces
0 1 2
orientation
ZY
`, COLOURS.blue)).toThrow(/AXIS.*STEPS|STEPS.*AXIS/i);
  });
});
