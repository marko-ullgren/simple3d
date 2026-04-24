# simple3d

A 3D viewer that renders an interactive rotating solid body. Originally written by Marko Ullgren in January 1998 as a Java 1.1 applet displaying a **wireframe** object, and gently converted to a standalone application in August 2020. The repository now contains two packages side by side: the untouched vintage wireframe original and a fully modernised rewrite that renders a **solid body** with flat shading and hidden-surface removal. The modernisation was done by GitHub Copilot with Marko providing the instructions.

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
- **Solid rendering** with flat shading and hidden-surface removal:
  - Back-face culling discards faces pointing away from the viewer
  - Diffuse + ambient flat shading based on each face's normal vs. the view direction
  - Side faces are fan-triangulated and sorted back-to-front (painter's algorithm)
  - The non-convex cap face is drawn last — geometrically guaranteed to be in front of any side face it overlaps

---

## How to compile

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
- **Body menu** — switch between the MU logo and a cube
- **Colour menu** — change the object colour
