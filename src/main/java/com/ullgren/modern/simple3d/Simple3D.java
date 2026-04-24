package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Simple3D renders a three-dimensional wireframe body that rotates on screen. Clicking different
 * parts of the canvas changes the rotation direction. Body shape and colour can be changed via the
 * menus.
 * <p>
 * Originally written in January 1998 by Marko Ullgren. Minimally refactored and published to
 * GitHub in August 2020. Modernized to Java 21 Swing in 2025.
 * <p>
 * (c) Marko Ullgren 1997-1998
 */
public class Simple3D extends JFrame {

  static final int WIDTH = 350;
  static final int HEIGHT = 375;

  static final int CENTER_X = WIDTH / 2;
  static final int CENTER_Y = HEIGHT / 2;

  /** Minimum pixel distance from centre required for a click to affect rotation. */
  static final int SENSITIVITY = 50;

  private Body body;
  private int angularMomentumXZ, angularMomentumYZ;

  private Timer animationTimer;
  private JPanel canvas;

  public static void main(String[] args) {
    EventQueue.invokeLater(() -> new Simple3D().init());
  }

  public void init() {
    body = new Body(muPoints(), muEdges(), Color.blue);
    for (int i = 0; i < 60; i++) {
      body.rotateZY();
    }

    this.setTitle("A Simple 3D application (c) Marko Ullgren 1998");
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setResizable(false);

    JMenuBar menuBar = new JMenuBar();
    this.setJMenuBar(menuBar);

    JMenu bodyMenu = new JMenu("Body");
    menuBar.add(bodyMenu);
    JMenuItem muItem = new JMenuItem("MU");
    JMenuItem cubeItem = new JMenuItem("Cube");
    JMenuItem quitItem = new JMenuItem("Quit");
    bodyMenu.add(muItem);
    bodyMenu.add(cubeItem);
    bodyMenu.addSeparator();
    bodyMenu.add(quitItem);

    JMenu colourMenu = new JMenu("Colour");
    menuBar.add(colourMenu);
    JMenuItem blueItem = new JMenuItem("Blue");
    JMenuItem redItem = new JMenuItem("Red");
    JMenuItem greenItem = new JMenuItem("Green");
    colourMenu.add(blueItem);
    colourMenu.add(redItem);
    colourMenu.add(greenItem);

    blueItem.addActionListener(e -> { body.setColour(Color.blue); canvas.repaint(); });
    redItem.addActionListener(e -> { body.setColour(Color.red); canvas.repaint(); });
    greenItem.addActionListener(e -> { body.setColour(Color.green); canvas.repaint(); });

    muItem.addActionListener(e -> {
      body = new Body(muPoints(), muEdges(), body.getColour());
      for (int i = 0; i < 60; i++) body.rotateZY();
      canvas.repaint();
    });
    cubeItem.addActionListener(e -> {
      body = new Body(cubePoints(), cubeEdges(), body.getColour());
      canvas.repaint();
    });
    quitItem.addActionListener(e -> System.exit(0));

    canvas = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        body.draw(g, CENTER_X, CENTER_Y);
      }
    };
    canvas.setBackground(Color.black);
    canvas.setForeground(Color.white);
    canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));

    canvas.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        boolean wasIdle = (angularMomentumXZ == 0 && angularMomentumYZ == 0);

        if (e.getX() < CENTER_X - SENSITIVITY) angularMomentumXZ--;
        if (e.getX() > CENTER_X + SENSITIVITY) angularMomentumXZ++;
        if (e.getY() < CENTER_Y - SENSITIVITY) angularMomentumYZ++;
        if (e.getY() > CENTER_Y + SENSITIVITY) angularMomentumYZ--;

        if (angularMomentumYZ == 0 && angularMomentumXZ == 0) {
          stopAnimation();
        } else if (wasIdle) {
          startAnimation();
        }
      }
    });

    this.setContentPane(canvas);
    this.pack();
    this.setVisible(true);
  }

  private void startAnimation() {
    if (animationTimer == null) {
      animationTimer = new Timer(40, e -> {
        for (int i = 0; i < angularMomentumXZ; i++) body.rotateXZ();
        for (int i = 0; i < angularMomentumYZ; i++) body.rotateZY();
        if (angularMomentumXZ < 0) for (int i = 0; i > angularMomentumXZ; i--) body.rotateZX();
        if (angularMomentumYZ < 0) for (int i = 0; i > angularMomentumYZ; i--) body.rotateYZ();
        canvas.repaint();
      });
    }
    animationTimer.start();
  }

  private void stopAnimation() {
    angularMomentumXZ = angularMomentumYZ = 0;
    if (animationTimer != null) {
      animationTimer.stop();
    }
  }

  private static Point3D[] muPoints() {
    Point3D[] points = new Point3D[36];

    points[0] = new Point3D(-150, -90, 25);
    points[1] = new Point3D(-90, -90, 25);
    points[2] = new Point3D(-90, 30, 25);
    points[3] = new Point3D(-60, 0, 25);
    points[4] = new Point3D(-30, 30, 25);
    points[5] = new Point3D(-30, -60, 25);
    points[6] = new Point3D(0, -90, 25);
    points[7] = new Point3D(90, -90, 25);
    points[8] = new Point3D(120, -60, 25);
    points[9] = new Point3D(120, 90, 25);
    points[10] = new Point3D(60, 90, 25);
    points[11] = new Point3D(60, -30, 25);
    points[12] = new Point3D(30, -30, 25);
    points[13] = new Point3D(30, 90, 25);
    points[14] = new Point3D(-30, 90, 25);
    points[15] = new Point3D(-60, 60, 25);
    points[16] = new Point3D(-90, 90, 25);
    points[17] = new Point3D(-150, 90, 25);
    for (int i = 18; i < 36; i++) {
      points[i] = new Point3D(points[i - 18]);
      points[i].z = -25;
    }
    return points;
  }

  private static int[][] muEdges() {
    int[][] edges = new int[55][2];

    for (int i = 0; i < 17; i++) {
      edges[i][0] = i;
      edges[i][1] = i + 1;
    }
    for (int i = 18; i < 35; i++) {
      edges[i][0] = i;
      edges[i][1] = i + 1;
    }
    for (int i = 35; i < 53; i++) {
      edges[i][0] = i - 35;
      edges[i][1] = i - 17;
    }
    edges[53][0] = 17;
    edges[53][1] = 0;
    edges[54][0] = 35;
    edges[54][1] = 18;

    return edges;
  }

  private static Point3D[] cubePoints() {
    Point3D[] points = new Point3D[8];

    points[0] = new Point3D(-90, 90, -90);
    points[1] = new Point3D(90, 90, -90);
    points[2] = new Point3D(90, -90, -90);
    points[3] = new Point3D(-90, -90, -90);
    points[4] = new Point3D(-90, 90, 90);
    points[5] = new Point3D(90, 90, 90);
    points[6] = new Point3D(90, -90, 90);
    points[7] = new Point3D(-90, -90, 90);

    return points;
  }

  private static int[][] cubeEdges() {
    return new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}};
  }

}
