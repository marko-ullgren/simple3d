package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 * Simple3D renders a three-dimensional solid body that rotates on screen. Clicking different
 * parts of the canvas changes the rotation direction. Body shape and colour can be changed via
 * the menus.
 * <p>
 * Originally written in January 1998 by Marko Ullgren. Minimally refactored and published to
 * GitHub in August 2020. Modernized to Java 21 Swing in 2025.
 * <p>
 * (c) Marko Ullgren 1997-2026
 */
public class Simple3D {

  static final int WIDTH  = 350;
  static final int HEIGHT = 375;

  /** Minimum pixel distance from centre required for a click to affect rotation. */
  static final int SENSITIVITY = 50;

  private final JFrame  frame    = new JFrame();
  private final StarField starField = new StarField();
  private final Renderer  renderer  = new Renderer();

  private Body   body;
  private JPanel canvas;
  private int    canvasWidth  = WIDTH;
  private int    canvasHeight = HEIGHT;
  private double zoom = 1.0;

  private AnimationController animationController;

  public static void main(String[] args) {
    EventQueue.invokeLater(() -> new Simple3D().init());
  }

  public void init() {
    body   = buildBody();
    canvas = buildCanvas();
    animationController = new AnimationController(body, canvas::repaint);

    frame.setTitle("A Simple 3D application (c) Marko Ullgren 1998-2026");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setMinimumSize(new Dimension(150, 150));
    frame.setJMenuBar(buildMenuBar());
    frame.setContentPane(canvas);
    frame.pack();
    frame.setVisible(true);
  }

  private Body buildBody() {
    Body b = Body.loadBody("/com/ullgren/modern/simple3d/mu.body", Color.blue);
    for (int i = 0; i < 60; i++) b.rotateZY();
    return b;
  }

  private JMenuBar buildMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    JMenu bodyMenu = new JMenu("Body");
    JMenuItem muItem   = new JMenuItem("MU");
    JMenuItem cubeItem = new JMenuItem("Cube");
    JMenuItem quitItem = new JMenuItem("Quit");
    bodyMenu.add(muItem);
    bodyMenu.add(cubeItem);
    bodyMenu.addSeparator();
    bodyMenu.add(quitItem);
    menuBar.add(bodyMenu);

    JMenu colourMenu  = new JMenu("Colour");
    JMenuItem blueItem  = new JMenuItem("Blue");
    JMenuItem redItem   = new JMenuItem("Red");
    JMenuItem greenItem = new JMenuItem("Green");
    colourMenu.add(blueItem);
    colourMenu.add(redItem);
    colourMenu.add(greenItem);
    menuBar.add(colourMenu);

    muItem.addActionListener(e -> {
      body = Body.loadBody("/com/ullgren/modern/simple3d/mu.body", body.getColour());
      for (int i = 0; i < 60; i++) body.rotateZY();
      animationController.setBody(body);
      canvas.repaint();
    });
    cubeItem.addActionListener(e -> {
      body = Body.loadBody("/com/ullgren/modern/simple3d/cube.body", body.getColour());
      animationController.setBody(body);
      canvas.repaint();
    });
    quitItem.addActionListener(e -> System.exit(0));

    blueItem.addActionListener(e  -> { body.setColour(Color.blue);  canvas.repaint(); });
    redItem.addActionListener(e   -> { body.setColour(Color.red);   canvas.repaint(); });
    greenItem.addActionListener(e -> { body.setColour(Color.green); canvas.repaint(); });

    return menuBar;
  }

  private JPanel buildCanvas() {
    JPanel panel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        starField.draw(g, canvasWidth, canvasHeight);
        double scale = zoom * Math.min(canvasWidth, canvasHeight)
            / (double) Math.min(Simple3D.WIDTH, Simple3D.HEIGHT);
        renderer.render(body, g, canvasWidth / 2, canvasHeight / 2, scale, canvasWidth, canvasHeight);
      }
    };

    panel.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        int w = panel.getWidth();
        int h = panel.getHeight();
        if (w > 0 && h > 0) {
          canvasWidth  = w;
          canvasHeight = h;
        }
        panel.repaint();
      }
    });

    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        animationController.applyImpulse(
            e.getX(), e.getY(), canvasWidth / 2, canvasHeight / 2, SENSITIVITY);
      }
    });

    panel.addMouseWheelListener(e -> {
      zoom *= Math.pow(1.1, -e.getPreciseWheelRotation());
      zoom = Math.max(0.1, Math.min(zoom, 10.0));
      panel.repaint();
    });

    panel.setBackground(Color.black);
    panel.setForeground(Color.white);
    panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
    return panel;
  }
}
