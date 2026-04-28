package com.ullgren.modern.simple3d.model;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses {@code .body} shape files from classpath resources and constructs {@link Body} instances.
 * <p>
 * File format:
 * <pre>
 * points
 * x y z
 * ...
 *
 * faces
 * i0 i1 i2 ...
 * ...
 *
 * orientation        (optional)
 * XZ 5              (axis ∈ {XZ, YZ, ZX, ZY}, steps > 0)
 * YZ 5
 * </pre>
 * Lines starting with {@code #} and blank lines are ignored. Sections may appear in any order,
 * except that {@code faces} must follow {@code points} (face indices are validated against the
 * point list as they are parsed).
 */
public final class BodyLoader {

  private enum Axis {
    XZ { @Override void apply(Body b) { b.rotateXZ(); } },
    YZ { @Override void apply(Body b) { b.rotateYZ(); } },
    ZX { @Override void apply(Body b) { b.rotateZX(); } },
    ZY { @Override void apply(Body b) { b.rotateZY(); } };

    abstract void apply(Body b);
  }

  private record OrientationStep(Axis axis, int steps) {}

  private BodyLoader() {}

  /**
   * Loads a {@link Body} from a classpath resource.
   *
   * @param resource path to the {@code .body} resource (e.g. {@code "/com/ullgren/.../mu.body"})
   * @param colour   initial draw colour
   * @return a fully constructed {@link Body} with ambient occlusion baked in
   * @throws UncheckedIOException     if the resource cannot be read
   * @throws IllegalArgumentException if the resource is not found or the file is malformed
   */
  public static Body load(String resource, Color colour) {
    InputStream stream = BodyLoader.class.getResourceAsStream(resource);
    if (stream == null) {
      throw new IllegalArgumentException("Shape resource not found: " + resource);
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8))) {

      List<Point3D>        points      = new ArrayList<>();
      List<int[]>          faces       = new ArrayList<>();
      List<OrientationStep> orientation = new ArrayList<>();
      String section = null;
      String line;
      int lineNum = 0;

      while ((line = reader.readLine()) != null) {
        lineNum++;
        line = line.strip();
        if (line.isEmpty() || line.startsWith("#")) continue;

        if (line.equals("points") || line.equals("faces") || line.equals("orientation")) {
          section = line;
          continue;
        }
        if (section == null) {
          throw new IllegalArgumentException(
              resource + ":" + lineNum + ": data before any section header");
        }

        String[] tokens = line.split("\\s+");
        if (section.equals("points")) {
          if (tokens.length != 3) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": point must have exactly 3 coordinates");
          }
          points.add(new Point3D(
              Double.parseDouble(tokens[0]),
              Double.parseDouble(tokens[1]),
              Double.parseDouble(tokens[2])));
        } else if (section.equals("faces")) {
          if (tokens.length < 3) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": face must have at least 3 indices");
          }
          int[] indices = new int[tokens.length];
          for (int i = 0; i < tokens.length; i++) {
            indices[i] = Integer.parseInt(tokens[i]);
            if (indices[i] < 0 || indices[i] >= points.size()) {
              throw new IllegalArgumentException(
                  resource + ":" + lineNum + ": face index " + indices[i]
                  + " out of range (0.." + (points.size() - 1) + ")");
            }
          }
          faces.add(indices);
        } else { // orientation
          if (tokens.length != 2) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": orientation entry must be '<AXIS> <STEPS>'");
          }
          Axis axis;
          try {
            axis = Axis.valueOf(tokens[0].toUpperCase());
          } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": unknown axis '" + tokens[0]
                + "' — expected one of XZ, YZ, ZX, ZY");
          }
          int steps = Integer.parseInt(tokens[1]);
          if (steps <= 0) {
            throw new IllegalArgumentException(
                resource + ":" + lineNum + ": orientation steps must be positive, got " + steps);
          }
          orientation.add(new OrientationStep(axis, steps));
        }
      }

      if (points.isEmpty()) {
        throw new IllegalArgumentException(resource + ": no points defined");
      }
      if (faces.isEmpty()) {
        throw new IllegalArgumentException(resource + ": no faces defined");
      }
      Body body = new Body(points.toArray(new Point3D[0]), faces.toArray(new int[0][]), colour);
      for (OrientationStep step : orientation) {
        for (int i = 0; i < step.steps(); i++) {
          step.axis().apply(body);
        }
      }
      return body;

    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read shape resource: " + resource, e);
    }
  }
}
