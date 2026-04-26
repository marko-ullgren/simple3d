# simple3d

A 3D viewer that renders an interactive rotating solid body. Originally written by Marko Ullgren in January 1998 as a Java 1.1 applet displaying a **wireframe** object, and gently converted to a standalone application in August 2020. The repository now contains two packages side by side: the untouched vintage wireframe original and a fully modernised rewrite that renders a **solid body** with flat shading and hidden-surface removal. The modernisation was done by GitHub Copilot with Marko providing the instructions.

**Try it in your browser → https://marko-ullgren.github.io/simple3d/**

---

## Packages

### `com.ullgren.vintage.simple3d` — the 1998 original

A preserved piece of 1990s Java. Deliberately left as-is, including deprecated AWT APIs, Finnish variable names, and the classic inability to close the window without killing the process. Run it to feel the era.

### `com.ullgren.modern.simple3d` — the modernised rewrite

A from-scratch rewrite targeting Java 21, keeping the same geometry and interaction model but replacing everything else:

- **Swing** instead of AWT (`JFrame`, `JPanel`, `JMenuBar`, `Timer`)
- **EDT-safe initialisation** via `EventQueue.invokeLater()`
- **Proper window close** — the × button works
- **English identifiers** throughout (`Point3D`, `Body`, `Simple3D`, `rotateXZ`, …) replacing the original Finnish names
- **Resizable window** with content that scales proportionally
- **Solid rendering** with Gouraud shading and hidden-surface removal:
  - Back-face culling discards faces pointing away from the viewer
  - Diffuse + ambient Gouraud shading with smooth normals across adjacent faces
  - Per-vertex ambient occlusion baked at load time — concave corners appear naturally darker
  - Side faces are fan-triangulated and sorted back-to-front (painter's algorithm)
  - The non-convex cap face is drawn last — geometrically guaranteed to be in front of any side face it overlaps
  - 2× supersampling anti-aliasing for smooth edges
- **Elastic dent effect** — clicking the shape leaves a soft spring-damper dent that bounces back
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
| `src/render/ElasticEffect.ts` | Spring-damper dent; `setInterval`-based; RGBA pixel displacement |
| `src/render/Renderer.ts` | Gouraud scanline rasteriser into `ImageData`; cap polygons via Canvas 2D paths; 2× SSAA |
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

### Deployment

Every push to `master` that touches `src/web/` or the `.body` shape files triggers a GitHub Actions workflow (`.github/workflows/deploy-pages.yml`) that:

1. Installs dependencies with `npm ci`
2. Runs `npm run build` (TypeScript compile + Vite bundle)
3. Publishes `src/web/dist/` to GitHub Pages

The live URL updates within about a minute of merging.

---

```
mvn compile
```

## How to run tests

Tests cover `Point3D` (constructors, rotation invariants, round-trips) and `Body` (loading, error handling, draw branch coverage, rotation):

```
mvn test
```

> **Note:** If `mvn test` fails because `maven-surefire-plugin` dependencies are not cached locally, run the tests directly using the JVM that Maven uses:
> ```
> JAVA=$(mvn --version 2>/dev/null | grep "runtime:" | sed 's/.*runtime: //')
> mvn test-compile -q && \
> $JAVA/bin/java \
>   -cp "target/classes:target/test-classes:\
> $HOME/.m2/repository/junit/junit/4.11/junit-4.11.jar:\
> $HOME/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar" \
>   org.junit.runner.JUnitCore \
>   com.ullgren.modern.simple3d.Point3DTest \
>   com.ullgren.modern.simple3d.BodyTest
> ```

## How to run

**Modern app (Java 21, solid rendering):**
```
mvn exec:exec@modern
```

**Vintage app (Java 1.1 style):**
```
mvn exec:java@vintage
```

## Controls

- **Click** in a quadrant to spin the object in that direction; click again to add more momentum or in the opposite quadrant to slow it down
- **Scroll wheel** to zoom in and out (zoom range: 0.1× – 10×)
- **Body menu** — switch between seven shapes: MU logo (default), Cube, Tetrahedron, Octahedron, Icosahedron, Torus, and Pyramid
- **Colour menu** — change the object colour
