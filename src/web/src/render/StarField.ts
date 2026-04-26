/**
 * Static starfield background of 200 stars.
 *
 * Stars use normalised (0–1) coordinates so they scale with the canvas.
 * Layout is deterministic via a port of Java's java.util.Random(seed=42),
 * producing identical star positions to the Java version.
 */
export class StarField {
  private static readonly COUNT = 200;

  private readonly starX:      Float32Array;
  private readonly starY:      Float32Array;
  private readonly starRadius: Float32Array;
  private readonly starAlpha:  Float32Array;

  constructor() {
    const n = StarField.COUNT;
    this.starX      = new Float32Array(n);
    this.starY      = new Float32Array(n);
    this.starRadius = new Float32Array(n);
    this.starAlpha  = new Float32Array(n);

    const rng = new JavaRandom(42);
    for (let i = 0; i < n; i++) {
      this.starX[i]      = rng.nextFloat();
      this.starY[i]      = rng.nextFloat();
      this.starRadius[i] = 0.5 + rng.nextFloat() * rng.nextFloat() * 2.0;
      this.starAlpha[i]  = 0.4 + rng.nextFloat() * 0.6;
    }
  }

  draw(ctx: CanvasRenderingContext2D, w: number, h: number): void {
    for (let i = 0; i < StarField.COUNT; i++) {
      const alpha = this.starAlpha[i];
      const x = this.starX[i]  * w;
      const y = this.starY[i]  * h;
      const d = Math.max(1, Math.round(this.starRadius[i] * 2));

      ctx.fillStyle = `rgba(255,255,255,${alpha.toFixed(3)})`;
      if (d <= 1) {
        ctx.fillRect(Math.round(x), Math.round(y), 1, 1);
      } else {
        ctx.beginPath();
        ctx.arc(x, y, d / 2, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }
}

/**
 * Port of java.util.Random's linear congruential generator.
 * Uses BigInt internally for exact 48-bit arithmetic.
 */
class JavaRandom {
  private seed: bigint;

  constructor(seed: number) {
    this.seed = (BigInt(seed) ^ 0x5DEECE66Dn) & ((1n << 48n) - 1n);
  }

  private next(bits: number): number {
    this.seed = (this.seed * 0x5DEECE66Dn + 0xBn) & ((1n << 48n) - 1n);
    return Number(this.seed >> BigInt(48 - bits));
  }

  nextFloat(): number {
    return this.next(24) / (1 << 24);
  }
}
