# simple3d

A 3D viewer that renders an interactive rotating solid body. Originally written by Marko Ullgren in January 1998 as a Java 1.1 applet displaying a **wireframe** object, and gently converted to a standalone application in August 2020. 

The repository now contains two Java packages side by side: the untouched vintage wireframe original and a **fully modernised rewrite** that renders a solid body with flat shading and hidden-surface removal. The modernisation was done by with the help of GitHub Copilot.

The repository also contains a TypeScript port of the modern Java rewrite.

**Try the TypeScript port in your browser → https://marko-ullgren.github.io/simple3d/**

---

## Java Packages

### `com.ullgren.vintage.simple3d` — the 1998 original

A preserved piece of 1990s Java. Deliberately left as-is, including deprecated AWT APIs, Finnish variable names, and the classic inability to close the window without killing the process. Run it to feel the era.

### `com.ullgren.modern.simple3d` — the modernised rewrite

A complete rewrite targeting Java 21, keeping the same geometry and interaction model but replacing almost everything else:

- **Swing** instead of AWT (`JFrame`, `JPanel`, `JMenuBar`, `Timer`)
- **EDT-safe initialisation** via `EventQueue.invokeLater()`
- **Proper window close** — the × button works
- **English identifiers** throughout (`Point3D`, `Body`, `Simple3D`, `rotateXZ`, …) replacing the original Finnish names
- **Resizable window** with content that scales proportionally
- **Solid rendering** with Gouraud shading and hidden-surface removal:
  - Back-face culling discards faces pointing away from the viewer
  - Diffuse + ambient Gouraud shading with smooth normals across adjacent faces
  - Per-vertex ambient occlusion baked at load time — concave corners appear naturally darker
  - All faces are triangulated (ear-clip algorithm for non-convex caps) and sorted back-to-front (painter's algorithm)
  - 2× supersampling anti-aliasing for smooth edges
- **Surface textures** — selectable via the Texture menu:
  - *None* (default) — smooth Gouraud shading, no texture
  - *Stone* — procedural three-octave 3D value noise sampled at object-space coordinates; the pattern is baked at load time and stays fixed to the surface during rotation
  - *Metal* — polished metallic surface with body-color-tinted specular highlights, a narrow white sheen, a wide soft glare bloom, and subtle single-octave surface grain noise
  - *Wireframe* — draws only edges, dark for far edges and bright for near ones, reproducing the look of the 1998 original
- **Free colour picker** — choose any colour via `JColorChooser` (replaces the fixed Blue/Red/Green presets)
- **Image-space effects** triggered on mouse click — four selectable via the Effect menu:
  - *Elastic Dent* (default) — spring-damper compression that bounces back
  - *Ripple* — concentric sine-wave rings that radiate outward and fade
  - *Vortex* — swirling rotation around the click point that unwinds
  - *Shockwave* — a single expanding ring of radial displacement
  - *No Effect* — disables the click effect
- **Starfield background** — 200 stars of varying size and brightness give the illusion of the object floating in space
- **Seven built-in shapes**: MU logo (default), Cube, Tetrahedron, Octahedron, Icosahedron, Torus, and Pyramid

---

## Web app (`src/web/`)

A faithful TypeScript port of the modern Java app that runs entirely in the browser — no server, no plugins.

**Live:** https://marko-ullgren.github.io/simple3d/

### What's inside

| File | Role |
|---|---|
| `src/model/Point3D.ts` | 3D point with the same rotation math as the Java version |
| `src/model/Body.ts` | Async `.body` file loader (`fetch`); same ambient-occlusion bake |
| `src/render/StarField.ts` | Deterministic star layout via a BigInt port of Java's `Random(42)` |
| `src/render/effect/Effect.ts` | `Effect` interface (`trigger`, `isActive`, `applyToPixels`, `stop`) |
| `src/render/effect/ElasticEffect.ts` | Spring-damper dent; `setInterval`-based; RGBA pixel displacement |
| `src/render/effect/RippleEffect.ts` | Expanding sine-wave rings that fade over 1.5 s |
| `src/render/effect/VortexEffect.ts` | Swirling rotation that unwinds over 1.2 s |
| `src/render/effect/ShockwaveEffect.ts` | Single expanding ring of radial displacement |
| `src/render/effect/NoEffect.ts` | Null-object — disables the click effect |
| `src/render/Renderer.ts` | Gouraud scanline rasteriser into `ImageData`; ear-clip triangulation for non-convex cap faces; per-pixel texture via `Texture.applyPacked()`; wireframe mode; 2× SSAA |
| `src/render/texture/Texture.ts` | `Texture` interface — `applyPacked(wx, wy, wz, baseR, baseG, baseB, shade)` returns packed RGB |
| `src/render/texture/NoTexture.ts` | Null-object texture: Gouraud shading only, no surface pattern |
| `src/render/texture/StoneTexture.ts` | Procedural three-octave 3D value noise sampled at object-space coords baked at load time |
| `src/render/texture/MetalTexture.ts` | Polished metallic surface — body-color specular, white sheen, glare bloom, single-octave grain |
| `src/control/AnimationController.ts` | Angular momentum + friction decay |
| `src/main.ts` | Wires everything; `requestAnimationFrame` render loop; `<select>` menus |

Shape files are shared with the Java app — `vite-plugin-static-copy` pulls them from `src/main/resources/` at build time; no duplication in version control.

### Run locally

Node 22 is required (use `nvm`):

```bash
cd src/web
nvm use        # picks Node 22 from .nvmrc
npm install
npm run dev    # → http://localhost:5173
```

### Tests

112 unit tests cover the model, control, effect, and rendering layers:

```bash
cd src/web
nvm use
npm install
npm test        # run once
npm run test:watch  # re-run on file changes
```

Tests are co-located with source files (`*.test.ts`). Effect tests (`render/effect/*.test.ts`) verify pixel-level displacement logic directly against `Uint8ClampedArray` buffers — no browser or canvas required. Renderer tests cover the filled, stone-texture, metal-texture, and wireframe rendering paths including ear-clip triangulation of non-convex cap faces.

### Deployment

Every push to `master` that touches `src/web/` or the `.body` shape files triggers a GitHub Actions workflow (`.github/workflows/deploy-pages.yml`) that:

1. Installs dependencies with `npm ci`
2. Runs `npm run build` (TypeScript compile + Vite bundle)
3. Publishes `src/web/dist/` to GitHub Pages

The live URL updates within about a minute of merging.

---
## Java app (`src/main/`)
### Compile locally
```
mvn compile
```

### How to run tests (`src/test/`)

Tests cover `Point3D`, `Body`, `Renderer`, and all five effect implementations. Effect tests verify pixel-level displacement directly against `int[]` buffers — no display required:

```
mvn test
```

### How to run

**Modern app (Java 21, solid rendering):**
```
mvn exec:exec@modern
```

**Vintage app (Java 1.1 style):**
```
mvn exec:java@vintage
```

## Controls (both Web and Java apps)

- **Click** in a quadrant to spin the object in that direction; click again to add more momentum or in the opposite quadrant to slow it down
- **Scroll wheel** to zoom in and out (zoom range: 0.1× – 10×)
- **Body menu** — switch between seven shapes: MU logo (default), Cube, Tetrahedron, Octahedron, Icosahedron, Torus, and Pyramid
- **Colour picker** — choose any object colour (free colour picker on web, `JColorChooser` dialog in Java)
- **Effect menu** — choose the click effect: Elastic Dent (default), Ripple, Vortex, Shockwave, or No Effect
- **Texture menu** — choose the surface appearance: None (default), Stone, Metal, or Wireframe
