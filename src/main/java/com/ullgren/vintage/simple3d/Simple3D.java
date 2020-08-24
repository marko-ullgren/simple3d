package com.ullgren.vintage.simple3d;

import java.awt.Color;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;

/**
 * Sovellus Simple3D Sovelma piirtää ruudulle kolmiulotteisen kappaleen, joka pyörii. Pyörimisen
 * suuntaa voidaan muuttaa klikkaamalla kappaletta sen eri kohdista ja kappaleen väriä seka muotoa
 * voidaan muuttaa asianomaisista valikoista
 * <p>
 * Sovelluksen Simple3D ja siihen liittyvat luokat Piste ja Kappale on kirjoittanut tammikuussa
 * 1998. Minimaalisesti refaktoroitu ja viety Githubiin elokuussa 2020.
 * <p>
 * (c) Marko Ullgren 1997-1998
 */
public class Simple3D extends Frame implements Runnable {

  static final int LEVEYS = 350;    // ikkunan leveys
  static final int KORKEUS = 375;   // ikkunan korkeus

  static final int kx = LEVEYS / 2;  // ikkunan keskikohta
  static final int ky = KORKEUS / 2;

  private Kappale kpl;              // näytettävä Kappale
  private int Lxz, Lyz;             // systeemin liikemaaramomentti y- ja x- akseleiden suhteen

  public Thread thread;             // pääsilmukan thread

  public static void main(String[] args) {
    new Simple3D().init();
  }

  public void init() {

    // luodaan uusi Kappale:
    kpl = new Kappale(muPisteet(), muViivat(), Color.blue);

    // kaannetaan kappale oikein pain:
    for (int i = 0; i < 60; i++) {
      kpl.kaannaZY();
    }

    setBackground(Color.black);

    // Avataan uusi ikkuna sovellusta varten

    this.setTitle("A Simple 3D application (c) Marko Ullgren 1998");
    this.setBackground(Color.black);
    this.setForeground(Color.white);

    // Asetetaan ikkunan GUI-komponentit paikoilleen

    MenuBar valikkorivi = new MenuBar();
    this.setMenuBar(valikkorivi);
    Menu kappaleet = new Menu("Body");
    valikkorivi.add(kappaleet);
    kappaleet.add("MU");
    kappaleet.add("Cube");
    kappaleet.addSeparator();
    kappaleet.add("Quit");
    Menu vari = new Menu("Colour");
    valikkorivi.add(vari);
    vari.add("Blue");
    vari.add("Red");
    vari.add("Green");
    this.resize(LEVEYS, KORKEUS);
    this.validate();
    this.setVisible(true);

  }

  public void start() {
    repaint();
    if (Lxz != 0 || Lyz != 0) {
      if (thread == null) {
        thread = new Thread(this);
      }
      thread.start();
    }
  }

  public void stop() {
    Lxz = Lyz = 0;
    thread.interrupt();
    thread = null;
  }

  public void paint(Graphics g) {
    kpl.piirra(g, kx, ky);
  }

  public void run() {
    // Pääsilmukka
    // asetetaan threadin prioritetti minimiin, jottei se hairitse muuta
    // toimintaa

    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    long startTime = System.currentTimeMillis();  // alkuaika, käytetään tahdistamiseen

    while (Thread.currentThread() == thread) {

      // Tutkitaan liikemäärämomenttia ja käännetään kappaletta sen mukaan

      for (int i = 0; i < Lxz; i++) {
        kpl.kaannaXZ();
      }
      for (int i = 0; i < Lyz; i++) {
        kpl.kaannaZY();
      }
      if (Lxz < 0) {
        for (int i = 0; i > Lxz; i--) {
          kpl.kaannaZX();
        }
      }
      if (Lyz < 0) {
        for (int i = 0; i > Lyz; i--) {
          kpl.kaannaYZ();
        }
      }

      repaint();      // päivitetään ruutua

      try {           // jos on aikaa, niin odotellaan
        startTime += 40;
        thread.sleep(Math.max(0, startTime - System.currentTimeMillis()));
      } catch (InterruptedException e) {
        break;
      }
    }

  }

  public void update(Graphics g) {
    // ruudun päivitys
    g.clearRect(0, 0, this.size().width - 1, this.size().height);
    kpl.piirra(g, kx, ky);
  }

  public boolean mouseDown(Event e, int x, int y) {

    // Tutkitaan, missä kohtaa ruutua on painettu hiirtä ja
    // muutetaan kappaleen liikemäärämomenttia sen mukaan.

    boolean kaynnistetaan = false;

    int herkkyys = 50; // vaikuttavan klikkauksen etäisyys keskeltä

    if (Lxz == 0 && Lyz == 0) {
      kaynnistetaan = true;
    }
    if (x < kx - herkkyys) {
      Lxz--;
    }
    if (x > kx + herkkyys) {
      Lxz++;
    }
    if (y < ky - herkkyys) {
      Lyz++;
    }
    if (y > ky + herkkyys) {
      Lyz--;
    }
    if (Lyz == 0 && Lxz == 0) {
      stop();
    } else if (kaynnistetaan) {
      start();
    }
    return true;
  }

  public void destroy() {
    this.dispose();
    System.exit(0);
  }

  public boolean action(Event e, Object o) {

    // Reagoidaan GUI-komponenttien käyttöön

    // vaihdetaan kappaleen varia tarvittaessa:

    if (e.target instanceof MenuItem) {
      if (((MenuItem) e.target).getLabel().equals("Blue")) {
        kpl.vaihdaVari(Color.blue);
      } else if (((MenuItem) e.target).getLabel().equals("Red")) {
        kpl.vaihdaVari(Color.red);
      } else if (((MenuItem) e.target).getLabel().equals("Green")) {
        kpl.vaihdaVari(Color.green);
      }

      // vaihdetaan kappaletta tarvittaessa:

      else if (((MenuItem) e.target).getLabel().equals("MU")) {
        kpl = new Kappale(muPisteet(), muViivat(), kpl.vari());
        for (int i = 0; i < 60; i++) {
          kpl.kaannaZY();
        }
      } else if (((MenuItem) e.target).getLabel().equals("Cube")) {
        kpl = new Kappale(cubePisteet(), cubeViivat(), kpl.vari());
      } else if (((MenuItem) e.target).getLabel().equals("Quit")) {
        this.destroy();
      }
    }

    repaint();
    return true;
  }

  private static Piste[] muPisteet() {
    // palautetaan arvona MU-kappaleen pisteista muodostuva taulukko

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
    // palautetaan arvona kuution pisteista muodostuva taulukko

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
