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
 * Sovellus Simple3D piirtää ruudulle kolmiulotteisen kappaleen, joka pyörii. Pyörimisen
 * suuntaa voidaan muuttaa klikkaamalla kappaletta sen eri kohdista ja kappaleen väriä seka muotoa
 * voidaan muuttaa asianomaisista valikoista
 * <p>
 * Sovelluksen Simple3D ja siihen liittyvat luokat Piste ja Kappale on kirjoittanut tammikuussa
 * 1998. Minimaalisesti refaktoroitu ja viety Githubiin elokuussa 2020.
 * <p>
 * (c) Marko Ullgren 1997-1998
 */
public class Simple3D extends JFrame {

  static final int LEVEYS = 350;    // ikkunan leveys
  static final int KORKEUS = 375;   // ikkunan korkeus

  static final int kx = LEVEYS / 2;  // ikkunan keskikohta
  static final int ky = KORKEUS / 2;

  private Kappale kpl;              // näytettävä Kappale
  private int Lxz, Lyz;            // systeemin liikemaaramomentti y- ja x- akseleiden suhteen

  private Timer animTimer;          // animaation ajastin
  private JPanel canvas;            // piirtoalue

  public static void main(String[] args) {
    EventQueue.invokeLater(() -> new Simple3D().init());
  }

  public void init() {

    // luodaan uusi Kappale:
    kpl = new Kappale(muPisteet(), muViivat(), Color.blue);

    // käännetään kappale oikein pain:
    for (int i = 0; i < 60; i++) {
      kpl.kaannaZY();
    }

    // Avataan uusi ikkuna sovellusta varten
    this.setTitle("A Simple 3D application (c) Marko Ullgren 1998");
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setResizable(false);

    // Asetetaan ikkunan GUI-komponentit paikoilleen
    JMenuBar valikkorivi = new JMenuBar();
    this.setJMenuBar(valikkorivi);

    JMenu kappaleet = new JMenu("Body");
    valikkorivi.add(kappaleet);
    JMenuItem muItem = new JMenuItem("MU");
    JMenuItem cubeItem = new JMenuItem("Cube");
    JMenuItem quitItem = new JMenuItem("Quit");
    kappaleet.add(muItem);
    kappaleet.add(cubeItem);
    kappaleet.addSeparator();
    kappaleet.add(quitItem);

    JMenu variMenu = new JMenu("Colour");
    valikkorivi.add(variMenu);
    JMenuItem blueItem = new JMenuItem("Blue");
    JMenuItem redItem = new JMenuItem("Red");
    JMenuItem greenItem = new JMenuItem("Green");
    variMenu.add(blueItem);
    variMenu.add(redItem);
    variMenu.add(greenItem);

    // vaihdetaan kappaleen väriä tarvittaessa:
    blueItem.addActionListener(e -> { kpl.vaihdaVari(Color.blue); canvas.repaint(); });
    redItem.addActionListener(e -> { kpl.vaihdaVari(Color.red); canvas.repaint(); });
    greenItem.addActionListener(e -> { kpl.vaihdaVari(Color.green); canvas.repaint(); });

    // vaihdetaan kappaletta tarvittaessa:
    muItem.addActionListener(e -> {
      kpl = new Kappale(muPisteet(), muViivat(), kpl.vari());
      for (int i = 0; i < 60; i++) kpl.kaannaZY();
      canvas.repaint();
    });
    cubeItem.addActionListener(e -> {
      kpl = new Kappale(cubePisteet(), cubeViivat(), kpl.vari());
      canvas.repaint();
    });
    quitItem.addActionListener(e -> System.exit(0));

    // Piirtoalue mustalla taustalla
    canvas = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        kpl.piirra(g, kx, ky);
      }
    };
    canvas.setBackground(Color.black);
    canvas.setForeground(Color.white);
    canvas.setPreferredSize(new Dimension(LEVEYS, KORKEUS));

    // Tutkitaan, missä kohtaa ruutua on painettu hiirtä ja
    // muutetaan kappaleen liikemäärämomenttia sen mukaan.
    canvas.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        boolean kaynnistetaan = (Lxz == 0 && Lyz == 0);
        int herkkyys = 50; // vaikuttavan klikkauksen etäisyys keskeltä

        if (e.getX() < kx - herkkyys) Lxz--;
        if (e.getX() > kx + herkkyys) Lxz++;
        if (e.getY() < ky - herkkyys) Lyz++;
        if (e.getY() > ky + herkkyys) Lyz--;

        if (Lyz == 0 && Lxz == 0) {
          stopAnimation();
        } else if (kaynnistetaan) {
          startAnimation();
        }
      }
    });

    this.setContentPane(canvas);
    this.pack();
    this.setVisible(true);
  }

  private void startAnimation() {
    if (animTimer == null) {
      animTimer = new Timer(40, e -> {
        // Tutkitaan liikemäärämomenttia ja käännetään kappaletta sen mukaan
        for (int i = 0; i < Lxz; i++) kpl.kaannaXZ();
        for (int i = 0; i < Lyz; i++) kpl.kaannaZY();
        if (Lxz < 0) for (int i = 0; i > Lxz; i--) kpl.kaannaZX();
        if (Lyz < 0) for (int i = 0; i > Lyz; i--) kpl.kaannaYZ();
        canvas.repaint();
      });
    }
    animTimer.start();
  }

  private void stopAnimation() {
    Lxz = Lyz = 0;
    if (animTimer != null) {
      animTimer.stop();
    }
  }

  private static Piste[] muPisteet() {
    // palautetaan arvona MU-kappaleen pisteistä muodostuva taulukko

    Piste[] points = new Piste[36];

    points[0] = new Piste(-150, -90, 25);
    points[1] = new Piste(-90, -90, 25);
    points[2] = new Piste(-90, 30, 25);
    points[3] = new Piste(-60, 0, 25);
    points[4] = new Piste(-30, 30, 25);
    points[5] = new Piste(-30, -60, 25);
    points[6] = new Piste(0, -90, 25);
    points[7] = new Piste(90, -90, 25);
    points[8] = new Piste(120, -60, 25);
    points[9] = new Piste(120, 90, 25);
    points[10] = new Piste(60, 90, 25);
    points[11] = new Piste(60, -30, 25);
    points[12] = new Piste(30, -30, 25);
    points[13] = new Piste(30, 90, 25);
    points[14] = new Piste(-30, 90, 25);
    points[15] = new Piste(-60, 60, 25);
    points[16] = new Piste(-90, 90, 25);
    points[17] = new Piste(-150, 90, 25);
    for (int i = 18; i < 36; i++) {
      points[i] = new Piste(points[i - 18]);
      points[i].z = -25;
    }
    return points;
  }

  private static int[][] muViivat() {
    // palautetaan arvona MU-kappaleen viivoista muodostuva taulukko

    int[][] viivat = new int[55][2];

    for (int i = 0; i < 17; i++) {
      viivat[i][0] = i;
      viivat[i][1] = i + 1;
    }
    for (int i = 18; i < 35; i++) {
      viivat[i][0] = i;
      viivat[i][1] = i + 1;
    }
    for (int i = 35; i < 53; i++) {
      viivat[i][0] = i - 35;
      viivat[i][1] = i - 17;
    }
    viivat[53][0] = 17;
    viivat[53][1] = 0;
    viivat[54][0] = 35;
    viivat[54][1] = 18;

    return viivat;
  }

  private static Piste[] cubePisteet() {
    // palautetaan arvona kuution pisteistä muodostuva taulukko

    Piste[] points = new Piste[8];

    points[0] = new Piste(-90, 90, -90);
    points[1] = new Piste(90, 90, -90);
    points[2] = new Piste(90, -90, -90);
    points[3] = new Piste(-90, -90, -90);
    points[4] = new Piste(-90, 90, 90);
    points[5] = new Piste(90, 90, 90);
    points[6] = new Piste(90, -90, 90);
    points[7] = new Piste(-90, -90, 90);

    return points;
  }

  private static int[][] cubeViivat() {
    // palautetaan arvona kuution viivoista muodostuva taulukko

    int[][] viivat = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    return viivat;
  }

}
