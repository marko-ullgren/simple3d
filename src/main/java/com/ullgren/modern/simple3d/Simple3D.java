package com.ullgren.modern.simple3d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import com.ullgren.modern.simple3d.control.AnimationController;
import com.ullgren.modern.simple3d.model.Body;
import com.ullgren.modern.simple3d.model.BodyLoader;
import com.ullgren.modern.simple3d.render.effect.Effect;
import com.ullgren.modern.simple3d.render.effect.ElasticEffect;
import com.ullgren.modern.simple3d.render.effect.NoEffect;
import com.ullgren.modern.simple3d.render.Renderer;
import com.ullgren.modern.simple3d.render.effect.RippleEffect;
import com.ullgren.modern.simple3d.render.effect.ShockwaveEffect;
import com.ullgren.modern.simple3d.render.StarField;
import com.ullgren.modern.simple3d.render.effect.VortexEffect;

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
  private Effect effect;
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
    effect = new ElasticEffect(canvas::repaint);
    renderer.setEffect(effect);
    animationController = new AnimationController(body, canvas::repaint);
    animationController.kickstart(0.5, 0.5);

    frame.setTitle("A Simple 3D application (c) Marko Ullgren 1998-2026");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setMinimumSize(new Dimension(150, 150));
    frame.setJMenuBar(buildMenuBar());
    frame.setContentPane(canvas);
    frame.pack();
    frame.setVisible(true);
  }

  private void loadShape(String resource) {
    body = BodyLoader.load(resource, body.getColour());
    animationController.setBody(body);
    canvas.repaint();
  }

  /** Stops the current effect and replaces it with {@code next}. */
  private void switchEffect(Effect next) {
    effect.stop();
    effect = next;
    renderer.setEffect(effect);
  }

  private Body buildBody() {
    Body b = BodyLoader.load("/com/ullgren/modern/simple3d/mu.body", Color.blue);
    for (int i = 0; i < 60; i++) b.rotateZY();
    return b;
  }

  private JMenuBar buildMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(buildBodyMenu());
    menuBar.add(buildColourMenu());
    menuBar.add(buildEffectMenu());
    return menuBar;
  }

  private JMenu buildBodyMenu() {
    JMenu menu = new JMenu("Body");
    ButtonGroup group = new ButtonGroup();

    menu.add(radioItem("MU", true, group, () -> {
      body = BodyLoader.load("/com/ullgren/modern/simple3d/mu.body", body.getColour());
      for (int i = 0; i < 60; i++) body.rotateZY();
      animationController.setBody(body);
      canvas.repaint();
    }));
    menu.addSeparator();
    menu.add(radioItem("Cube",        false, group, () -> loadShape("/com/ullgren/modern/simple3d/cube.body")));
    menu.add(radioItem("Tetrahedron", false, group, () -> loadShape("/com/ullgren/modern/simple3d/tetrahedron.body")));
    menu.add(radioItem("Octahedron",  false, group, () -> loadShape("/com/ullgren/modern/simple3d/octahedron.body")));
    menu.add(radioItem("Icosahedron", false, group, () -> loadShape("/com/ullgren/modern/simple3d/icosahedron.body")));
    menu.add(radioItem("Torus",       false, group, () -> {
      body = BodyLoader.load("/com/ullgren/modern/simple3d/torus.body", body.getColour());
      for (int i = 0; i < 5; i++) body.rotateXZ();
      for (int i = 0; i < 5; i++) body.rotateYZ();
      animationController.setBody(body);
      canvas.repaint();
    }));
    menu.add(radioItem("Pyramid",     false, group, () -> loadShape("/com/ullgren/modern/simple3d/pyramid.body")));
    menu.addSeparator();

    JMenuItem quitItem = new JMenuItem("Quit");
    quitItem.addActionListener(e -> System.exit(0));
    menu.add(quitItem);

    return menu;
  }

  private JMenu buildColourMenu() {
    JMenu menu = new JMenu("Colour");
    ButtonGroup group = new ButtonGroup();
    menu.add(radioItem("Blue",  true,  group, () -> { body.setColour(Color.blue);  canvas.repaint(); }));
    menu.add(radioItem("Red",   false, group, () -> { body.setColour(Color.red);   canvas.repaint(); }));
    menu.add(radioItem("Green", false, group, () -> { body.setColour(Color.green); canvas.repaint(); }));
    return menu;
  }

  private JMenu buildEffectMenu() {
    JMenu menu = new JMenu("Effect");
    ButtonGroup group = new ButtonGroup();
    menu.add(radioItem("Elastic Dent", true,  group, () -> switchEffect(new ElasticEffect(canvas::repaint))));
    menu.add(radioItem("Ripple",       false, group, () -> switchEffect(new RippleEffect(canvas::repaint))));
    menu.add(radioItem("Vortex",       false, group, () -> switchEffect(new VortexEffect(canvas::repaint))));
    menu.add(radioItem("Shockwave",    false, group, () -> switchEffect(new ShockwaveEffect(canvas::repaint))));
    menu.addSeparator();
    menu.add(radioItem("No Effect",    false, group, () -> switchEffect(new NoEffect())));
    return menu;
  }

  /** Creates a {@link JRadioButtonMenuItem}, registers it in {@code group}, and wires {@code action}. */
  private JRadioButtonMenuItem radioItem(String label, boolean selected, ButtonGroup group, Runnable action) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
    group.add(item);
    item.addActionListener(e -> action.run());
    return item;
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
        effect.trigger(e.getX(), e.getY());
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
