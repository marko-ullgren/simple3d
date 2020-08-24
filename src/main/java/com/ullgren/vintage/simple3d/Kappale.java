package com.ullgren.vintage.simple3d;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Luokassa Kappale määritellään kolmeulotteisen avaruuden kappale.
 * <p>
 * Koordinaatiston origo on piirtoalueen keskellä, x-akselin positiivinen suunta on oikealle,
 * y-akselin ylöspäin ja z-akselin katsojasta poispäin.
 */
public class Kappale {

  public static final double PROJEKTIOKERROIN = 0.0010;
  private Piste[] pisteet;  // taulukkoon pisteet talletetaan kappaleen pisteet

  private int[][] yhd;    // Taulukkoon yhd, jonka tulee olla muotoa int[][2],
  // talletetaan ne pisteparit, jotka yhdistetaan
  // toisiinsa viivalla. Taulukko on siis muotoa {{p1,p2},...
  // ,{pn-1,pn}}, jossa px on pisteen indeksi pisteet-taulukossa

  private Color vari;       // kappaleen väri
  private Color tummavari;  // kappaleen taka-alalla olevien osien väri

  public Kappale(Piste[] pisteet, int[][] yhd, Color vari) {

    // konstruktorin parametrina annetaan Piste[]-taulukko kappaleen pisteille,
    // int[][2] taulukko kappaleen viivoille seka kappaleen vari

    this.pisteet = pisteet;
    this.yhd = yhd;
    this.vari = vari;
    this.tummavari = this.vari.darker();
  }

  public Kappale(double[][] piste, int[][] yhd, Color vari) {

    // konstruktorin parametrina annetaan double[][3]-taulukko kappaleen pisteille,
    // int[][2]-taulukko kappaleen viivoille seka vari

    this.pisteet = new Piste[piste.length];

    for (int i = 0; i < piste.length; i++) {
      this.pisteet[i].x = piste[i][0];
      this.pisteet[i].y = piste[i][1];
      this.pisteet[i].z = piste[i][2];
    }
    this.yhd = yhd;
    this.vari = vari;
    this.tummavari = this.vari.darker();
  }

  public Kappale(Kappale kpl) {

    // konstruktorin parametrina toinen kappale, jonka kopio luodaan

    this.pisteet = kpl.pisteet;
    this.yhd = kpl.yhd;
    this.vari = kpl.vari;
    this.tummavari = kpl.tummavari;
  }

  public void vaihdaVari(Color uusivari) {
    // vaihtaa kappaleen värin uudeksi
    this.vari = uusivari;
    this.tummavari = this.vari.darker();
  }

  /**
   * Piirtää kappaleen ruudulle
   *
   * @param g  Graphics-olio, jolle piirretään
   * @param kx x-akselin keskikohta
   * @param ky y-akselin keskikohta
   */
  public void piirra(Graphics g, int kx, int ky) {

    // käydään läpi yhd[][2]-taulukkoa kappaleen piirtämiseksi
    for (int i = 0; i < yhd.length; i++) {

      g.setColor(vari);
      double x0 = pisteet[yhd[i][0]].x;
      double x1 = pisteet[yhd[i][1]].x;
      double y0 = pisteet[yhd[i][0]].y;
      double y1 = pisteet[yhd[i][1]].y;

      // projisoidaan: kauempana olevat pisteet lähenevät origoa
      x0 = x0 - (x0 * PROJEKTIOKERROIN * pisteet[yhd[i][0]].z) + kx;
      x1 = x1 - (x1 * PROJEKTIOKERROIN * pisteet[yhd[i][1]].z) + kx;
      y0 = y0 - (y0 * PROJEKTIOKERROIN * pisteet[yhd[i][0]].z) + ky;
      y1 = y1 - (y1 * PROJEKTIOKERROIN * pisteet[yhd[i][1]].z) + ky;

      // muutetaan kauempana olevien viivojen väriä tummemmaksi
			if (pisteet[yhd[i][0]].z > 60 && pisteet[yhd[i][1]].z > 60) {
				g.setColor(tummavari);
			}

      // piirretään ruudulle
      g.drawLine((int) x0, (int) y0, (int) x1, (int) y1);

    }
  }

  // seuraavassa on joukko käännä-metodeja, joilla kaannetaan kappaletta eri
  // akseleiden suhteen

  public void kaannaXZ() {
		for (int i = 0; i < pisteet.length; i++) {
			pisteet[i].kaannaXZ();
		}
  }

  public void kaannaYZ() {
		for (int i = 0; i < pisteet.length; i++) {
			pisteet[i].kaannaYZ();
		}
  }

  public void kaannaZX() {
		for (int i = 0; i < pisteet.length; i++) {
			pisteet[i].kaannaZX();
		}
  }

  public void kaannaZY() {
		for (int i = 0; i < pisteet.length; i++) {
			pisteet[i].kaannaZY();
		}
  }

  public Color vari() {
    return this.vari;
  }

}
