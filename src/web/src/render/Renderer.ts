import { Body } from '../model/Body.js';
import { Point3D } from '../model/Point3D.js';
import type { Effect } from './effect/Effect.js';
import type { Texture } from './texture/Texture.js';
import { NoTexture } from './texture/NoTexture.js';

/**
 * Renders a {@link Body} onto a Canvas 2D context using Gouraud shading for side faces
 * and flat shading for cap faces (> 4 vertices).
 *
 * Geometry is rendered at 2× resolution into an off-DOM canvas, then bilinear-downsampled
 * to the target size — same anti-aliasing technique as the Java version.
 *
 * Architecture (per frame):
 *  1. Project all points.
 *  2. Classify visible faces; ear-clip cap faces (> 4 verts) into triangles.
 *  3. Rasterise all triangles directly into an ImageData pixel buffer,
 *     delegating per-pixel colour to the active Texture.
 *  4. putImageData → downsample 2× → 1× geometry canvas.
 *  5. If elastic effect active: readback geometry pixels, apply displacement, putImageData.
 *  6. Caller composites geometry canvas over the star background.
 *  7. In wireframe mode, steps 3-4 are replaced by edge strokes on the hi-res canvas.
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
  /** Surface texture applied per pixel during rasterisation. */
  private texture: Texture = new NoTexture();
  /** When true, renders edges only (wireframe) instead of filled surfaces. */
  private wireframeMode = false;

  /**
   * Per-body cache: maps a Body to its precomputed per-vertex texture values and
   * cap-face ear-clip triangulations. Both are computed from object-space coordinates
   * that never change after load, so they are stable across all frames.
   * Keyed by body+texture pair; invalidated when setTexture() is called.
   */
  private bodyTexCache = new WeakMap<Body, { texture: Texture; vertexTv: Float64Array }>();
  private earClipCache = new WeakMap<Body, Map<number, [number, number, number][]>>();

  setEffect(e: Effect): void { this.effect = e; }
  /** Attaches a surface texture; clears the per-body texture cache. */
  setTexture(t: Texture): void {
    this.texture = t;
    this.bodyTexCache = new WeakMap(); // invalidate: vertexTv depends on the texture
  }
  /** When true, renders as wireframe edges; textures are ignored. */
  setWireframeMode(b: boolean): void { this.wireframeMode = b; }

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

    // --- Allocate / reuse off-DOM canvases ---
    const hiCtx  = this.getHiCtx(hiW, hiH);
    const geoCtx = this.getGeoCtx(canvasW, canvasH);

    if (this.wireframeMode) {
      hiCtx.clearRect(0, 0, hiW, hiH);
      this.renderWireframe(body, sx, sy, sz, hiCtx);
    } else {
      this.renderFilled(body, sx, sy, sz, hiW, hiH, hiCtx);
    }

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

  /** Filled (Gouraud + texture) rendering path. All faces are scanline-rasterised. */
  private renderFilled(
    body: Body,
    sx: Int32Array, sy: Int32Array, sz: Float64Array,
    hiW: number, hiH: number,
    hiCtx: CanvasRenderingContext2D,
  ): void {
    const n  = body.pointCount();
    const fc = body.faceCount();

    type Triangle = {
      x0: number; y0: number; x1: number; y1: number; x2: number; y2: number;
      avgZ: number; s0: number; s1: number; s2: number;
      tv0: number; tv1: number; tv2: number;
    };

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

    // Per-vertex texture values — computed once from object-space coords and cached.
    // Stable across rotation: texCoords are baked at load time and never mutate.
    let cached = this.bodyTexCache.get(body);
    if (!cached || cached.texture !== this.texture) {
      const vertexTv = new Float64Array(n);
      for (let i = 0; i < n; i++) {
        const tc = body.getTexCoord(i);
        vertexTv[i] = this.texture.prepare(tc[0], tc[1], tc[2]);
      }
      cached = { texture: this.texture, vertexTv };
      this.bodyTexCache.set(body, cached);
    }
    const vertexTv = cached.vertexTv;

    // Object-space XY coordinates for ear-clip: constant across frames → stable triangulation.
    // Cap-face triangulations are also cached since they are frame-invariant.
    let faceTriCache = this.earClipCache.get(body);
    if (!faceTriCache) {
      faceTriCache = new Map();
      this.earClipCache.set(body, faceTriCache);
    }
    const texX = new Float64Array(n);
    const texY = new Float64Array(n);
    for (let i = 0; i < n; i++) {
      const tc = body.getTexCoord(i);
      texX[i] = tc[0];
      texY[i] = tc[1];
    }

    const allTris: Triangle[] = [];
    const colour = body.getColour();

    for (let fi = 0; fi < fc; fi++) {
      const face = body.faceAt(fi);
      const fn   = faceNormals[fi];
      if (fn[2] >= 0) continue; // back-face cull

      if (face.length > 4) {
        // Cap face — flat shading for all triangles.
        const fnLen = Math.sqrt(fn[0] ** 2 + fn[1] ** 2 + fn[2] ** 2);
        let shade = Math.max(Renderer.AMBIENT, fnLen > 0 ? -fn[2] / fnLen : Renderer.AMBIENT);
        let avgAO = 0;
        for (const idx of face) avgAO += body.getVertexAO(idx);
        avgAO /= face.length;
        const aoAmbient = Renderer.AMBIENT * (1 - Renderer.AO_STRENGTH * avgAO);
        shade = Math.max(aoAmbient, shade);

        // Use cached triangulation (computed in object space — frame-invariant).
        let capTris = faceTriCache.get(fi);
        if (!capTris) {
          capTris = Renderer.earClip(face, texX, texY);
          faceTriCache.set(fi, capTris);
        }
        for (const [li0, li1, li2] of capTris) {
          const a = face[li0], b = face[li1], c = face[li2];
          allTris.push({
            x0: sx[a], y0: sy[a], x1: sx[b], y1: sy[b], x2: sx[c], y2: sy[c],
            avgZ: (sz[a] + sz[b] + sz[c]) / 3,
            s0: shade, s1: shade, s2: shade,
            tv0: vertexTv[a], tv1: vertexTv[b], tv2: vertexTv[c],
          });
        }
      } else {
        // Side face — Gouraud shading, fan-triangulated.
        const cs = face.map((vIdx, ci) =>
          this.computeCornerShade(body, vIdx, fi, faceNormals, vertexFaces, body.getVertexAO(vIdx), ci),
        );
        for (let i = 1; i < face.length - 1; i++) {
          const a = face[0], b = face[i], c = face[i + 1];
          allTris.push({
            x0: sx[a], y0: sy[a], x1: sx[b], y1: sy[b], x2: sx[c], y2: sy[c],
            avgZ: (sz[a] + sz[b] + sz[c]) / 3,
            s0: cs[0], s1: cs[i], s2: cs[i + 1],
            tv0: vertexTv[a], tv1: vertexTv[b], tv2: vertexTv[c],
          });
        }
      }
    }

    // --- Scanline rasterisation into ImageData ---
    const hiImageData = this.getHiImageData(hiW, hiH, hiCtx);
    hiImageData.data.fill(0); // transparent black

    allTris.sort((a, b) => b.avgZ - a.avgZ);
    const pixels = hiImageData.data;
    for (const t of allTris) {
      this.drawGouraudTriangle(pixels, hiW, hiH, colour,
        t.x0, t.y0, t.s0, t.tv0,
        t.x1, t.y1, t.s1, t.tv1,
        t.x2, t.y2, t.s2, t.tv2);
    }
    hiCtx.putImageData(hiImageData, 0, 0);
  }

  /**
   * Draws all unique edges of {@code body} as anti-aliased lines.
   * Front edges (avgZ <= 0) are drawn in the body colour; back edges are drawn darker,
   * reproducing the vintage app's depth-cueing effect.
   */
  private renderWireframe(
    body: Body,
    sx: Int32Array, sy: Int32Array, sz: Float64Array,
    hiCtx: CanvasRenderingContext2D,
  ): void {
    const colour = body.getColour();
    const dark = (v: number) => Math.floor(v * 0.7);
    const dimR = dark(dark(colour.r));
    const dimG = dark(dark(colour.g));
    const dimB = dark(dark(colour.b));
    const frontStyle = `rgb(${colour.r},${colour.g},${colour.b})`;
    const dimStyle   = `rgb(${dimR},${dimG},${dimB})`;

    hiCtx.save();
    hiCtx.lineWidth = 2;
    const drawn = new Set<string>();
    const fc = body.faceCount();
    for (let fi = 0; fi < fc; fi++) {
      const face = body.faceAt(fi);
      for (let i = 0; i < face.length; i++) {
        const a = face[i], b = face[(i + 1) % face.length];
        const key = `${Math.min(a, b)},${Math.max(a, b)}`;
        if (drawn.has(key)) continue;
        drawn.add(key);
        const avgZ = (sz[a] + sz[b]) / 2;
        hiCtx.strokeStyle = avgZ > 0 ? dimStyle : frontStyle;
        hiCtx.beginPath();
        hiCtx.moveTo(sx[a], sy[a]);
        hiCtx.lineTo(sx[b], sy[b]);
        hiCtx.stroke();
      }
    }
    hiCtx.restore();
  }

  /**
   * Ear-clips a simple (possibly non-convex) polygon into triangles.
   * Pass object-space coordinates (not screen-space) so the triangulation
   * is deterministic and stable across frames regardless of body rotation.
   * Returns face-local index triplets.
   */
  private static earClip(face: number[], px: ArrayLike<number>, py: ArrayLike<number>): [number, number, number][] {
    const n = face.length;
    // Signed area (×2) to determine CCW vs CW winding.
    let area2 = 0;
    for (let i = 0; i < n; i++) {
      const j = (i + 1) % n;
      area2 += px[face[i]] * py[face[j]] - px[face[j]] * py[face[i]];
    }
    const isCCW = area2 > 0;

    const rem: number[] = Array.from({ length: n }, (_, i) => i); // face-local indices
    const result: [number, number, number][] = [];
    let safety = n * n + n;

    while (rem.length > 3 && safety-- > 0) {
      const rn = rem.length;
      let earFound = false;
      for (let i = 0; i < rn; i++) {
        const fi0 = rem[(i - 1 + rn) % rn];
        const fi1 = rem[i];
        const fi2 = rem[(i + 1) % rn];
        const ax = px[face[fi0]], ay = py[face[fi0]];
        const bx = px[face[fi1]], by = py[face[fi1]];
        const cx = px[face[fi2]], cy = py[face[fi2]];

        const cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        const convex = isCCW ? cross >= 0 : cross <= 0;
        if (!convex) continue;

        let isEar = true;
        for (let j = 0; j < rn && isEar; j++) {
          if (j === (i - 1 + rn) % rn || j === i || j === (i + 1) % rn) continue;
          const fj = rem[j];
          if (Renderer.pointInTriangle2D(ax, ay, bx, by, cx, cy, px[face[fj]], py[face[fj]])) {
            isEar = false;
          }
        }
        if (isEar) {
          result.push([fi0, fi1, fi2]);
          rem.splice(i, 1);
          earFound = true;
          break;
        }
      }
      if (!earFound) {
        // Degenerate polygon — force-clip to avoid infinite loop.
        result.push([rem[0], rem[1], rem[2]]);
        rem.splice(1, 1);
      }
    }
    if (rem.length === 3) {
      result.push([rem[0], rem[1], rem[2]]);
    }
    return result;
  }

  private static pointInTriangle2D(
    ax: number, ay: number, bx: number, by: number, cx: number, cy: number,
    px: number, py: number,
  ): boolean {
    const d1 = Renderer.edgeSign(px, py, ax, ay, bx, by);
    const d2 = Renderer.edgeSign(px, py, bx, by, cx, cy);
    const d3 = Renderer.edgeSign(px, py, cx, cy, ax, ay);
    const hasNeg = d1 < 0 || d2 < 0 || d3 < 0;
    const hasPos = d1 > 0 || d2 > 0 || d3 > 0;
    return !(hasNeg && hasPos);
  }

  private static edgeSign(
    px: number, py: number, x1: number, y1: number, x2: number, y2: number,
  ): number {
    return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
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
    x0: number, y0: number, s0: number, tv0: number,
    x1: number, y1: number, s1: number, tv1: number,
    x2: number, y2: number, s2: number, tv2: number,
  ): void {
    // Sort vertices by y ascending.
    if (y1 < y0) {
      [x0, x1] = [x1, x0]; [y0, y1] = [y1, y0]; [s0, s1] = [s1, s0]; [tv0, tv1] = [tv1, tv0];
    }
    if (y2 < y0) {
      [x0, x2] = [x2, x0]; [y0, y2] = [y2, y0]; [s0, s2] = [s2, s0]; [tv0, tv2] = [tv2, tv0];
    }
    if (y2 < y1) {
      [x1, x2] = [x2, x1]; [y1, y2] = [y2, y1]; [s1, s2] = [s2, s1]; [tv1, tv2] = [tv2, tv1];
    }

    const totalH = y2 - y0;
    if (totalH === 0) return;

    const baseR = colour.r, baseG = colour.g, baseB = colour.b;

    for (let y = Math.max(0, y0); y <= Math.min(h - 1, y2); y++) {
      const alpha = (y - y0) / totalH;
      let xA  = x0  + (x2  - x0)  * alpha;
      let sA  = s0  + (s2  - s0)  * alpha;
      let tvA = tv0 + (tv2 - tv0) * alpha;
      let xB: number, sB: number, tvB: number;
      if (y <= y1) {
        const segH = y1 - y0;
        const beta = segH === 0 ? 1 : (y - y0) / segH;
        xB  = x0  + (x1  - x0)  * beta;
        sB  = s0  + (s1  - s0)  * beta;
        tvB = tv0 + (tv1 - tv0) * beta;
      } else {
        const segH = y2 - y1;
        const beta = segH === 0 ? 1 : (y - y1) / segH;
        xB  = x1  + (x2  - x1)  * beta;
        sB  = s1  + (s2  - s1)  * beta;
        tvB = tv1 + (tv2 - tv1) * beta;
      }
      if (xA > xB) {
        [xA, xB] = [xB, xA]; [sA, sB] = [sB, sA]; [tvA, tvB] = [tvB, tvA];
      }

      const xAi = Math.round(xA);
      const xBi = Math.round(xB);
      const spanW = xBi - xAi;
      const rowBase = y * w;

      for (let x = Math.max(0, xAi); x <= Math.min(w - 1, xBi); x++) {
        const t     = spanW === 0 ? 0.5 : (x - xAi) / spanW;
        const shade = sA  + (sB  - sA)  * t;
        const tv    = tvA + (tvB - tvA) * t;
        const packed = this.texture.applyPacked(tv, baseR, baseG, baseB, shade);
        const base   = (rowBase + x) * 4;
        pixels[base]     = (packed >> 16) & 0xFF;
        pixels[base + 1] = (packed >>  8) & 0xFF;
        pixels[base + 2] =  packed        & 0xFF;
        pixels[base + 3] = 255;
      }
    }
  }
}
