import { Body } from '../model/Body.js';
import { Point3D } from '../model/Point3D.js';
import type { Effect } from './effect/Effect.js';

/**
 * Renders a {@link Body} onto a Canvas 2D context using Gouraud shading for side faces
 * and flat shading for cap faces (> 4 vertices).
 *
 * Geometry is rendered at 2× resolution into an off-DOM canvas, then bilinear-downsampled
 * to the target size — same anti-aliasing technique as the Java version.
 *
 * Architecture (per frame):
 *  1. Project all points.
 *  2. Classify visible faces into Gouraud triangles (≤ 4 vertices) and cap polygons (> 4).
 *  3. Rasterise Gouraud triangles directly into an ImageData pixel buffer.
 *  4. putImageData → draw cap polygons via canvas paths (AA enabled).
 *  5. Downsample 2× → 1× geometry canvas (transparent background; stars show through).
 *  6. If elastic effect active: readback geometry pixels, apply displacement, putImageData.
 *  7. Caller composites geometry canvas over the star background.
 */
export class Renderer {
  private static readonly PROJECTION_FACTOR    = 0.001;
  private static readonly AMBIENT              = 0.15;
  private static readonly AO_STRENGTH          = 0.5;
  private static readonly SMOOTH_THRESHOLD_COS = Math.cos(Math.PI / 3); // 60°

  private hiCanvas: HTMLCanvasElement | null = null;
  private geoCanvas: HTMLCanvasElement | null = null;
  private hiImageData: ImageData | null = null;
  private hiW = 0;
  private hiH = 0;
  private geoW = 0;
  private geoH = 0;

  private effect: Effect | null = null;

  setEffect(e: Effect): void { this.effect = e; }

  /**
   * Renders {@code body} and composites the result onto {@code ctx}.
   * Stars should be drawn on {@code ctx} before calling this method;
   * the geometry layer is transparent outside the body.
   */
  render(
    body: Body,
    ctx: CanvasRenderingContext2D,
    centerX: number,
    centerY: number,
    scale: number,
    canvasW: number,
    canvasH: number,
  ): void {
    const hiW     = 2 * canvasW;
    const hiH     = 2 * canvasH;
    const hiScale = 2 * scale;
    const hiCX    = 2 * centerX;
    const hiCY    = 2 * centerY;

    // --- Project points ---
    const n = body.pointCount();
    const sx = new Int32Array(n);
    const sy = new Int32Array(n);
    const sz = new Float64Array(n);
    for (let i = 0; i < n; i++) {
      const p = body.pointAt(i);
      const f = 1.0 - Renderer.PROJECTION_FACTOR * p.z;
      sx[i] = Math.round(hiScale * p.x * f) + hiCX;
      sy[i] = Math.round(hiScale * p.y * f) + hiCY;
      sz[i] = p.z;
    }

    const fc = body.faceCount();

    // Per-face normals (unnormalised; z component < 0 → faces viewer).
    const faceNormals: [number, number, number][] = [];
    for (let fi = 0; fi < fc; fi++) {
      faceNormals.push(this.computeFaceNormal(body, body.faceAt(fi)));
    }

    // Vertex → adjacent face indices for smooth shading.
    const vertexFaces: number[][] = Array.from({ length: n }, () => []);
    for (let fi = 0; fi < fc; fi++) {
      for (const idx of body.faceAt(fi)) vertexFaces[idx].push(fi);
    }

    type Triangle = {
      x0: number; y0: number; x1: number; y1: number; x2: number; y2: number;
      avgZ: number; s0: number; s1: number; s2: number;
    };
    type CapPoly = { px: number[]; py: number[]; avgZ: number; r: number; g: number; b: number };

    const sideTris: Triangle[] = [];
    const capPolys: CapPoly[]  = [];
    const colour = body.getColour();

    for (let fi = 0; fi < fc; fi++) {
      const face = body.faceAt(fi);
      const fn   = faceNormals[fi];
      if (fn[2] >= 0) continue; // back-face cull

      if (face.length > 4) {
        // Cap polygon — flat shading.
        const fnLen = Math.sqrt(fn[0] ** 2 + fn[1] ** 2 + fn[2] ** 2);
        let shade = Math.max(Renderer.AMBIENT, fnLen > 0 ? -fn[2] / fnLen : Renderer.AMBIENT);
        let avgAO = 0;
        for (const idx of face) avgAO += body.getVertexAO(idx);
        avgAO /= face.length;
        const aoAmbient = Renderer.AMBIENT * (1 - Renderer.AO_STRENGTH * avgAO);
        shade = Math.max(aoAmbient, shade);
        let avgZ = 0;
        for (const idx of face) avgZ += sz[idx];
        avgZ /= face.length;
        capPolys.push({
          px: face.map(i => sx[i]),
          py: face.map(i => sy[i]),
          avgZ,
          r: Math.min(255, colour.r * shade),
          g: Math.min(255, colour.g * shade),
          b: Math.min(255, colour.b * shade),
        });
      } else {
        // Side face — Gouraud shading, fan-triangulated.
        const cs = face.map((vIdx, ci) =>
          this.computeCornerShade(body, vIdx, fi, faceNormals, vertexFaces, body.getVertexAO(vIdx), ci),
        );
        for (let i = 1; i < face.length - 1; i++) {
          const a = face[0], b = face[i], c = face[i + 1];
          sideTris.push({
            x0: sx[a], y0: sy[a], x1: sx[b], y1: sy[b], x2: sx[c], y2: sy[c],
            avgZ: (sz[a] + sz[b] + sz[c]) / 3,
            s0: cs[0], s1: cs[i], s2: cs[i + 1],
          });
        }
      }
    }

    // --- Allocate / reuse off-DOM canvases ---
    const hiCtx = this.getHiCtx(hiW, hiH);
    const geoCtx = this.getGeoCtx(canvasW, canvasH);

    // --- Gouraud scanline rasterisation into ImageData ---
    const hiImageData = this.getHiImageData(hiW, hiH, hiCtx);
    hiImageData.data.fill(0); // transparent black

    sideTris.sort((a, b) => b.avgZ - a.avgZ);
    const pixels = hiImageData.data;
    for (const t of sideTris) {
      this.drawGouraudTriangle(pixels, hiW, hiH, colour,
        t.x0, t.y0, t.s0, t.x1, t.y1, t.s1, t.x2, t.y2, t.s2);
    }
    hiCtx.putImageData(hiImageData, 0, 0);

    // --- Cap polygons via canvas paths (drawn on top of Gouraud layer) ---
    capPolys.sort((a, b) => b.avgZ - a.avgZ);
    hiCtx.save();
    hiCtx.imageSmoothingEnabled = false; // paths are AA'd natively
    for (const cap of capPolys) {
      hiCtx.fillStyle = `rgb(${Math.round(cap.r)},${Math.round(cap.g)},${Math.round(cap.b)})`;
      hiCtx.beginPath();
      hiCtx.moveTo(cap.px[0], cap.py[0]);
      for (let i = 1; i < cap.px.length; i++) hiCtx.lineTo(cap.px[i], cap.py[i]);
      hiCtx.closePath();
      hiCtx.fill();
    }
    hiCtx.restore();

    // --- Downsample 2× → 1× (bilinear — this is the AA step) ---
    geoCtx.clearRect(0, 0, canvasW, canvasH);
    geoCtx.imageSmoothingEnabled = true;
    geoCtx.imageSmoothingQuality = 'high';
    geoCtx.drawImage(this.hiCanvas!, 0, 0, canvasW, canvasH);

    // --- Elastic dent (applied only to the geometry layer, not stars) ---
    if (this.effect?.isActive()) {
      const geoImageData = geoCtx.getImageData(0, 0, canvasW, canvasH);
      const src = new Uint8ClampedArray(geoImageData.data);
      this.effect.applyToPixels(src, geoImageData.data, canvasW, canvasH);
      geoCtx.putImageData(geoImageData, 0, 0);
    }

    // --- Composite geometry over star background ---
    ctx.drawImage(this.geoCanvas!, 0, 0);
  }

  private getHiCtx(w: number, h: number): CanvasRenderingContext2D {
    if (!this.hiCanvas || this.hiW !== w || this.hiH !== h) {
      this.hiCanvas    = document.createElement('canvas');
      this.hiCanvas.width  = w;
      this.hiCanvas.height = h;
      this.hiW         = w;
      this.hiH         = h;
      this.hiImageData = null;
    }
    return this.hiCanvas.getContext('2d')!;
  }

  private getGeoCtx(w: number, h: number): CanvasRenderingContext2D {
    if (!this.geoCanvas || this.geoW !== w || this.geoH !== h) {
      this.geoCanvas    = document.createElement('canvas');
      this.geoCanvas.width  = w;
      this.geoCanvas.height = h;
      this.geoW         = w;
      this.geoH         = h;
    }
    return this.geoCanvas.getContext('2d', { willReadFrequently: true })!;
  }

  private getHiImageData(w: number, h: number, ctx: CanvasRenderingContext2D): ImageData {
    if (!this.hiImageData || this.hiImageData.width !== w || this.hiImageData.height !== h) {
      this.hiImageData = ctx.createImageData(w, h);
    }
    return this.hiImageData;
  }

  private computeFaceNormal(body: Body, face: number[]): [number, number, number] {
    const p0 = body.pointAt(face[0]);
    const p1 = body.pointAt(face[1]);
    const p2 = body.pointAt(face[2]);
    const ax = p1.x - p0.x, ay = p1.y - p0.y, az = p1.z - p0.z;
    const bx = p2.x - p0.x, by = p2.y - p0.y, bz = p2.z - p0.z;
    return [ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx];
  }

  private computeCornerShade(
    body: Body,
    vIdx: number,
    faceIdx: number,
    faceNormals: [number, number, number][],
    vertexFaces: number[][],
    ao: number,
    _cornerIdx: number,
  ): number {
    const ambient = Renderer.AMBIENT * (1 - Renderer.AO_STRENGTH * ao);
    const fn  = faceNormals[faceIdx];
    const fnLen = Math.sqrt(fn[0] ** 2 + fn[1] ** 2 + fn[2] ** 2);
    if (fnLen === 0) return ambient;
    const nx = fn[0] / fnLen, ny = fn[1] / fnLen, nz = fn[2] / fnLen;

    let snx = nx, sny = ny, snz = nz;
    for (const fi2 of vertexFaces[vIdx]) {
      if (fi2 === faceIdx) continue;
      const fn2 = faceNormals[fi2];
      const fn2Len = Math.sqrt(fn2[0] ** 2 + fn2[1] ** 2 + fn2[2] ** 2);
      if (fn2Len === 0) continue;
      const dot = nx * (fn2[0] / fn2Len) + ny * (fn2[1] / fn2Len) + nz * (fn2[2] / fn2Len);
      if (dot > Renderer.SMOOTH_THRESHOLD_COS) {
        snx += fn2[0] / fn2Len;
        sny += fn2[1] / fn2Len;
        snz += fn2[2] / fn2Len;
      }
    }
    const len = Math.sqrt(snx * snx + sny * sny + snz * snz);
    return len > 0 ? Math.max(ambient, -snz / len) : ambient;
  }

  /** Scanline Gouraud rasteriser — writes directly to an RGBA Uint8ClampedArray. */
  private drawGouraudTriangle(
    pixels: Uint8ClampedArray,
    w: number,
    h: number,
    colour: { r: number; g: number; b: number },
    x0: number, y0: number, s0: number,
    x1: number, y1: number, s1: number,
    x2: number, y2: number, s2: number,
  ): void {
    // Sort vertices by y ascending.
    if (y1 < y0) { [x0,x1]=[x1,x0]; [y0,y1]=[y1,y0]; [s0,s1]=[s1,s0]; }
    if (y2 < y0) { [x0,x2]=[x2,x0]; [y0,y2]=[y2,y0]; [s0,s2]=[s2,s0]; }
    if (y2 < y1) { [x1,x2]=[x2,x1]; [y1,y2]=[y2,y1]; [s1,s2]=[s2,s1]; }

    const totalH = y2 - y0;
    if (totalH === 0) return;

    const baseR = colour.r, baseG = colour.g, baseB = colour.b;

    for (let y = Math.max(0, y0); y <= Math.min(h - 1, y2); y++) {
      const alpha = (y - y0) / totalH;
      let xA = x0 + (x2 - x0) * alpha;
      let sA = s0 + (s2 - s0) * alpha;
      let xB: number, sB: number;
      if (y <= y1) {
        const segH = y1 - y0;
        const beta = segH === 0 ? 1 : (y - y0) / segH;
        xB = x0 + (x1 - x0) * beta;
        sB = s0 + (s1 - s0) * beta;
      } else {
        const segH = y2 - y1;
        const beta = segH === 0 ? 1 : (y - y1) / segH;
        xB = x1 + (x2 - x1) * beta;
        sB = s1 + (s2 - s1) * beta;
      }
      if (xA > xB) { [xA, xB] = [xB, xA]; [sA, sB] = [sB, sA]; }

      const xAi = Math.round(xA);
      const xBi = Math.round(xB);
      const spanW = xBi - xAi;
      const rowBase = y * w;

      for (let x = Math.max(0, xAi); x <= Math.min(w - 1, xBi); x++) {
        const t     = spanW === 0 ? 0.5 : (x - xAi) / spanW;
        const shade = sA + (sB - sA) * t;
        const base  = (rowBase + x) * 4;
        pixels[base]     = Math.min(255, baseR * shade) | 0;
        pixels[base + 1] = Math.min(255, baseG * shade) | 0;
        pixels[base + 2] = Math.min(255, baseB * shade) | 0;
        pixels[base + 3] = 255;
      }
    }
  }
}
