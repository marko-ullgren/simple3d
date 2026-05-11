# Copilot Instructions for simple3d

> **⚠️ The vintage Java package (`com.ullgren.vintage.simple3d`) must not be touched.**
> It is a preserved 1998 artifact. Do not modify, modernise, refactor, or rename anything in it — even to fix warnings or deprecations.

## Build, Test & Run

### Java (root directory, Maven, Java 21)

```bash
mvn compile                # compile both packages
mvn test                   # run all Java tests (JUnit 5)
mvn test -Dtest=Point3DTest            # run a single test class
mvn test -Dtest=Point3DTest#testRotateXZ  # run a single test method
mvn exec:java@vintage      # run vintage wireframe version
mvn exec:exec@modern       # run modern solid-rendering version (spawns own JVM)
mvn package                # build executable JAR (vintage main)
```

### Web (src/web/, Node 22, Vite + Vitest)

```bash
cd src/web
nvm use                    # picks Node 22 from .nvmrc
npm ci                     # install dependencies (use ci, not install)
npm test                   # run all web tests once (vitest)
npm run test:watch         # re-run tests on file changes
npx vitest run src/model/Point3D.test.ts  # run a single test file
npm run dev                # local dev server → http://localhost:5173
npm run build              # production build (tsc + vite)
```

### CI

GitHub Actions (`.github/workflows/ci.yml`) runs both `mvn test` and `npm test` on every push/PR. Both must pass.

## Architecture

Three implementations of the same 3D viewer:

| Implementation | Location | Language | Rendering |
|---|---|---|---|
| Vintage | `src/main/…/vintage/simple3d/` | Java 1.1 AWT | Wireframe only |
| Modern | `src/main/…/modern/simple3d/` | Java 21 Swing | Solid + Gouraud shading |
| Web | `src/web/src/` | TypeScript | Solid + Gouraud shading (Canvas) |

The modern Java and web apps share the same architecture and `.body` shape files (web pulls them from `src/main/resources/` at build time via `vite-plugin-static-copy`).

### Modern/Web class structure

```
model/      Point3D, Body, BodyLoader     — geometry and file loading
render/     Renderer, StarField           — rasterisation and background
render/texture/   Texture interface + NoTexture, StoneTexture, MetalTexture
render/effect/    Effect interface + Elastic, Ripple, Vortex, Shockwave, NoEffect
control/    AnimationController           — angular momentum + friction decay
Simple3D / main.ts                        — app entry, wires everything together
```

### Vintage package (3 classes only)

- `Simple3D` — main Frame, event handling, shape data
- `Kappale` — wireframe body (points + edge list)
- `Piste` — 3D point with rotation methods

### `.body` file format

Shape geometry lives in `src/main/resources/com/ullgren/modern/simple3d/*.body`. Format:
```
# comments
points
x y z       ← one line per vertex (doubles, space-separated)
...
faces
i j k ...   ← vertex indices forming a face (one face per line, any polygon)
...
```
New shapes: create a `.body` file, add it to `bodies.list`, and wire it into the Body menu.

## Key Conventions

### Do NOT modify the vintage package
`com.ullgren.vintage.simple3d` is a preserved 1998 artifact. It deliberately uses deprecated Java 1.1 APIs (`java.awt.Event`, `mouseDown()`, `action()`, `resize()`). Never modernise, refactor, or rename anything in it.

### Finnish naming in vintage, English in modern/web
The vintage package uses Finnish identifiers (see vocabulary below). The modern Java and web packages use English (`Point3D`, `Body`, `rotateXZ`, etc.).

| Finnish | English |
|---------|---------|
| `piste` / `Piste` | point |
| `kappale` / `Kappale` | body / object |
| `piirra` | draw |
| `kaanna*` | rotate (XZ, ZX, YZ, ZY) |
| `vari` / `tummavari` | colour / darker colour |
| `yhd` | edge list |
| `Lxz` / `Lyz` | angular momentum |

### Coordinate system
Origin at window centre. X right, Y up, Z away from viewer.

### Feature parity
The modern Java and web apps should stay functionally equivalent. When adding a feature to one, implement it in the other too (or note it as a follow-up).

### Branching workflow
```bash
git checkout master && git pull && git checkout -b feature/<name>
```
