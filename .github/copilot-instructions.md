# Copilot Instructions for simple3d

## Build & Run Commands

```bash
mvn compile                # compile both packages
mvn exec:java              # run vintage version
mvn exec:exec@modern       # run modern version (spawns its own JVM — required for macOS close button)
mvn package                # build executable JAR (vintage main)
mvn test                   # run tests (none currently exist)
```

There are no tests in the codebase. The `junit` dependency is declared but no test sources exist under `src/test/`.

## Architecture

Two parallel implementations live side by side:

- **`com.ullgren.vintage.simple3d`** — original 1998 Java 1.1 code, intentionally unmodified. Renders a wireframe. Uses deprecated AWT APIs and Finnish identifiers throughout.
- **`com.ullgren.modern.simple3d`** — modernized equivalent targeting Java 21. Renders a solid body with flat shading and hidden-surface removal. All identifiers are in English. Open PR on `feature/modern-app`.

Do **not** modify `com.ullgren.vintage.simple3d` under any circumstances.

---

## Vintage package (`com.ullgren.vintage.simple3d`)

Uses the deprecated Java 1.1 AWT event model (`java.awt.Event`, `mouseDown()`, `action()`, `resize()`, `size()`). Finnish identifiers throughout. Three classes:

- **`Simple3D`** — main AWT `Frame`. Timer-driven rotation loop via a background `Thread`. Mouse clicks adjust angular momentum (`Lxz`/`Lyz`) and start/stop the thread.
- **`Kappale`** — 3D body: a `Piste[]` array and `int[][2]` edge list (`yhd`). `piirra()` projects and draws wireframe edges, darkening those far from the viewer.
- **`Piste`** — 3D point with `x, y, z` as `double`. Four rotation methods (`kaannaXZ`, `kaannaYZ`, `kaannaZX`, `kaannaZY`) at a fixed ~3° step.

Shapes (MU extrusion and cube) are defined as `*Pisteet()` / `*Viivat()` static methods in `Simple3D`.

---

## Modern package (`com.ullgren.modern.simple3d`)

Java 21, Swing, English identifiers, solid rendering. Three classes:

### `Simple3D`
- Plain class (does **not** extend `JFrame`). Holds a `private final JFrame frame`.
- `main()` calls `EventQueue.invokeLater(() -> new Simple3D().init())`.
- `init()` orchestrates setup by calling `buildBody()`, `buildCanvas()`, `buildMenuBar()`.
- `buildCanvas()` returns a `JPanel` with `paintComponent`, a `ComponentAdapter` for resize scaling, and a `MouseAdapter` for click-driven rotation.
- `buildMenuBar()` wires Body and Colour menus with `ActionListener` lambdas.
- `startAnimation()` / `stopAnimation()` control a `javax.swing.Timer` (40 ms, EDT-safe).
- `angularMomentumXZ` / `angularMomentumYZ`: integer rotation speed per tick.
- `canvasWidth` / `canvasHeight`: track current canvas size for scaling. Guarded against zero on first layout event.
- Window scales proportionally: `scale = min(canvasWidth, canvasHeight) / min(WIDTH, HEIGHT)`.

### `Body`
- Private constructor. Use factory methods `Body.mu(Color)` or `Body.cube(Color)`.
- Holds `final Point3D[] points` and `final int[][] faces` (face winding consistent: cross-product of first two edge vectors = outward normal).
- Shape data (points and face index arrays) defined as private static methods inside `Body`.
- `draw(Graphics g, int centerX, int centerY, double scale)` — solid rendering:
  1. Projects all points with perspective: `f = 1 - PROJECTION_FACTOR * z`
  2. **Back-face culls** faces where `nz >= 0` (normal's z ≥ 0 → facing away from viewer).
  3. Computes **flat shading**: `shade = max(AMBIENT=0.15, -nz/|n|)`.
  4. Routes faces to two draw queues:
     - **`sideTris`**: convex faces (≤4 vertices) fan-triangulated → sorted back-to-front, drawn first.
     - **`capPolys`**: non-convex cap faces (>4 vertices) kept whole → drawn last (always correct for extruded solids; `fillPolygon` handles concave polygons).
- `rotateXZ/YZ/ZX/ZY()` delegate to all points.

### `Point3D`
- Package-private fields `double x, y, z`.
- Static constants `ROTATION_ANGLE = toRadians(3)`, `SIN`, `COS`.
- Four rotation methods: `rotateXZ`, `rotateYZ`, `rotateZX`, `rotateZY`.

---

## Key Technical Notes

### `exec:exec` vs `exec:java` on macOS
`exec:java` runs inside Maven's JVM — macOS won't dispatch native window-close events to it, so the × button doesn't work. `exec:exec` spawns a child JVM which macOS treats as a standalone GUI process. The `pom.xml` uses `exec:exec@modern` for the modern app with `${java.home}/bin/java${script.extension}`; OS profiles set `script.extension` to `.exe` on Windows or empty on Unix.

### `Component.WIDTH/HEIGHT` shadowing inside anonymous JPanels
Inside any anonymous `JPanel` subclass, bare `WIDTH` and `HEIGHT` resolve to `ImageObserver` constants (1 and 2), **not** the enclosing class fields. Always qualify as `Simple3D.WIDTH` / `Simple3D.HEIGHT`.

### Coordinate system
Origin at canvas centre. X positive-right, Y positive-up, Z positive-away-from-viewer. Perspective applied as `(1 - PROJECTION_FACTOR * z)` per point before projecting to screen.

### Adding new shapes (modern package)
Add private static `<name>Points()` and `<name>Faces()` methods to `Body`. Add a factory method `Body.<name>(Color)`. Wire up in `buildMenuBar()` in `Simple3D`. Face winding must yield outward normals via the cross-product rule.
