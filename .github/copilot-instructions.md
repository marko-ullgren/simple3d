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

- **`com.ullgren.vintage.simple3d`** — original 1998 Java 1.1 code, intentionally unmodified. Uses deprecated AWT APIs throughout.
- **`com.ullgren.modern.simple3d`** — modernized equivalent targeting Java 21. Same logic, same Finnish identifiers, no deprecated APIs.

### Class roles (identical in both packages)

- **`Simple3D`** — main `Frame`. Owns a `javax.swing.Timer` (40 ms interval) that fires on the EDT: each tick advances the rotation by applying `kaanna*()` calls proportional to `Lxz`/`Lyz` (angular momentum), then calls `repaint()`. Mouse clicks relative to the window centre adjust `Lxz`/`Lyz` and start/stop the timer. Menu items use individual `ActionListener` lambdas.
- **`Kappale`** — a 3D body defined by a `Piste[]` array and an `int[][2]` edge list (`yhd`). `piirra()` applies a simple perspective projection (`PROJEKTIOKERROIN = 0.001`) and darkens edges whose both endpoints have `z > 60`. Rotation delegates to the underlying `Piste` objects.
- **`Piste`** — a 3D point. Holds `x, y, z` as `double`. Four rotation methods (`kaannaXZ`, `kaannaYZ`, `kaannaZX`, `kaannaZY`) each apply a 2D rotation matrix at a fixed ~3° step (`aste = 0.017 * 3 ≈ 0.051 rad`). `sini` and `kosini` are pre-computed instance fields.

Two built-in shapes are defined in `Simple3D`:
- **MU** — 36-point extrusion (the letters "MU"), 55 edges
- **Cube** — 8-point cube, 12 edges

## Key Conventions

### Finnish naming — intentionally preserved
All identifiers and comments use Finnish. Do not rename them. Key vocabulary:
| Finnish | English |
|---------|---------|
| `piste` / `Piste` | point |
| `kappale` / `Kappale` | body / object |
| `viiva` / `viivat` | line / lines |
| `piirra` | draw |
| `kaanna*` | rotate (axis suffix: XZ, ZX, YZ, ZY) |
| `vari` | colour |
| `tummavari` | darker colour (used for far edges) |
| `yhd` | connections / edge list |
| `aste` | angle (in radians) |
| `leveys` / `korkeus` | width / height |
| `Lxz` / `Lyz` | angular momentum around respective axes |

### Deprecated APIs — vintage package only
The `com.ullgren.vintage.simple3d` package deliberately uses the deprecated Java 1.1 AWT event model (`java.awt.Event`, `mouseDown()`, `action()`, `resize()`, `size()`). Do **not** modernise that package.

The `com.ullgren.modern.simple3d` package uses:
- `MouseAdapter` / `ActionListener` lambdas on `MenuItem` instead of `mouseDown()` / `action()`
- `setSize()` / `getSize()` instead of `resize()` / `size()`
- `javax.swing.Timer` (40 ms, EDT-safe) instead of a background `Thread` + `Runnable`
- `WindowAdapter.windowClosing()` → `destroy()` for proper window close
- `setResizable(false)` since `kx`/`ky` are fixed constants

### Coordinate system
Origin at the window centre (`kx = LEVEYS/2`, `ky = KORKEUS/2`). X is positive-right, Y is positive-up, Z is positive-away-from-viewer. Perspective is applied in `Kappale.piirra()` by scaling each projected coordinate by `(1 - PROJEKTIOKERROIN * z)`.

### Shape definition pattern
New shapes are added as two private static methods in `Simple3D`:
- `<name>Pisteet()` returning `Piste[]`
- `<name>Viivat()` returning `int[][2]` (pairs of indices into the points array)

Then wire them up in the `action()` menu handler.
