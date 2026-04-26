import { Point3D } from './Point3D.js';

export type Colour = { r: number; g: number; b: number };

export const COLOURS: Record<string, Colour> = {
  blue:  { r: 0,   g: 0,   b: 255 },
  red:   { r: 255, g: 0,   b: 0   },
  green: { r: 0,   g: 255, b: 0   },
};

/**
 * A three-dimensional solid body defined by points and polygonal faces.
 *
 * Use {@link Body.load} to create instances. Use {@link Renderer} to draw them.
 * Coordinate convention: x right, y up (same as Java version), z away from viewer.
 */
export class Body {
  private readonly points: Point3D[];
  private readonly faces: number[][];
  private colour: Colour;
  /** Per-vertex ambient occlusion, baked once after loading. */
  private readonly vertexAO: Float32Array;

  private constructor(points: Point3D[], faces: number[][], colour: Colour, vertexAO: Float32Array) {
    this.points   = points;
    this.faces    = faces;
    this.colour   = colour;
    this.vertexAO = vertexAO;
  }

  getColour(): Colour { return this.colour; }
  setColour(c: Colour): void { this.colour = c; }

  pointCount(): number { return this.points.length; }
  faceCount():  number { return this.faces.length; }
  pointAt(i: number): Point3D  { return this.points[i]; }
  faceAt(i: number):  number[] { return this.faces[i]; }
  getVertexAO(i: number): number { return this.vertexAO[i]; }

  // --- Rotation (mirrors Java API) ---

  rotateXZ(): void { for (const p of this.points) p.rotateXZ(); }
  rotateYZ(): void { for (const p of this.points) p.rotateYZ(); }
  rotateZX(): void { for (const p of this.points) p.rotateZX(); }
  rotateZY(): void { for (const p of this.points) p.rotateZY(); }

  rotateXZAngle(a: number): void { for (const p of this.points) p.rotateXZAngle(a); }
  rotateYZAngle(a: number): void { for (const p of this.points) p.rotateYZAngle(a); }
  rotateZXAngle(a: number): void { for (const p of this.points) p.rotateZXAngle(a); }
  rotateZYAngle(a: number): void { for (const p of this.points) p.rotateZYAngle(a); }

  // --- Loading ---

  /**
   * Loads a body from a URL pointing to a .body file.
   * Format: `points` section (x y z per line) then `faces` section (space-separated indices).
   * Lines starting with # and blank lines are ignored.
   */
  static async load(url: string, colour: Colour): Promise<Body> {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to load body file: ${url} (${response.status})`);
    }
    const text = await response.text();
    return Body.parse(text, colour, url);
  }

  static fromText(text: string, colour: Colour): Body {
    return Body.parse(text, colour, '<inline>');
  }

  private static parse(text: string, colour: Colour, sourceHint: string): Body {
    const points: Point3D[] = [];
    const faces:  number[][] = [];
    let section: 'points' | 'faces' | null = null;
    let lineNum = 0;

    for (const raw of text.split('\n')) {
      lineNum++;
      const line = raw.trim();
      if (!line || line.startsWith('#')) continue;

      if (line === 'points' || line === 'faces') {
        section = line;
        continue;
      }
      if (section === null) {
        throw new Error(`${sourceHint}:${lineNum}: data before any section header`);
      }

      const tokens = line.split(/\s+/);
      if (section === 'points') {
        if (tokens.length !== 3) {
          throw new Error(`${sourceHint}:${lineNum}: point must have exactly 3 coordinates`);
        }
        points.push(new Point3D(+tokens[0], +tokens[1], +tokens[2]));
      } else {
        if (tokens.length < 3) {
          throw new Error(`${sourceHint}:${lineNum}: face must have at least 3 indices`);
        }
        faces.push(tokens.map(t => {
          const idx = parseInt(t, 10);
          if (idx < 0 || idx >= points.length) {
            throw new Error(`${sourceHint}:${lineNum}: face index ${idx} out of range`);
          }
          return idx;
        }));
      }
    }

    if (points.length === 0) throw new Error(`${sourceHint}: no points defined`);
    if (faces.length === 0)  throw new Error(`${sourceHint}: no faces defined`);

    const vertexAO = Body.computeAO(points, faces);
    return new Body(points, faces, colour, vertexAO);
  }

  /**
   * Bakes per-vertex ambient occlusion.
   * AO = 1 − |mean of adjacent normalised face normals|.
   * Rotation-invariant: relative face angles don't change as the body spins.
   */
  private static computeAO(points: Point3D[], faces: number[][]): Float32Array {
    const fc = faces.length;
    const n  = points.length;

    // Per-face normalised normals.
    const fn: [number, number, number][] = [];
    for (const face of faces) {
      const p0 = points[face[0]], p1 = points[face[1]], p2 = points[face[2]];
      const ax = p1.x - p0.x, ay = p1.y - p0.y, az = p1.z - p0.z;
      const bx = p2.x - p0.x, by = p2.y - p0.y, bz = p2.z - p0.z;
      const nx = ay * bz - az * by;
      const ny = az * bx - ax * bz;
      const nz = ax * by - ay * bx;
      const len = Math.sqrt(nx * nx + ny * ny + nz * nz);
      fn.push(len > 0 ? [nx / len, ny / len, nz / len] : [0, 0, 0]);
    }

    // Vertex → adjacent faces.
    const adj: number[][] = Array.from({ length: n }, () => []);
    for (let fi = 0; fi < fc; fi++) {
      for (const idx of faces[fi]) adj[idx].push(fi);
    }

    const ao = new Float32Array(n);
    for (let v = 0; v < n; v++) {
      if (adj[v].length === 0) continue;
      let sx = 0, sy = 0, sz = 0;
      for (const fi of adj[v]) { sx += fn[fi][0]; sy += fn[fi][1]; sz += fn[fi][2]; }
      const meanLen = Math.sqrt(sx * sx + sy * sy + sz * sz) / adj[v].length;
      ao[v] = Math.max(0, 1 - meanLen);
    }
    return ao;
  }
}
