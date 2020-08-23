package com.ullgren.vintage.simple3d;

/**
 * Luokassa Piste maaritellaan kolmiulotteisen koordinaatiston piste
 *    													(x,y,z)
 */
public class Piste {

	final double aste=(0.017*3);     			// Koordinaatiston kääntökulma
	final double sini=Math.sin(aste);			// Lasketaan sini ja kosini valmiiksi,
	final double kosini=Math.cos(aste);		// nopeuttaa huomattavasti
	public double x,y,z;

	Piste() {
		this.x=0;
		this.y=0;
		this.z=0;
	}
	
	Piste(int x, int y, int z) {
		this.x=x;
		this.y=y;
		this.z=z;
	}

	Piste(Piste p) {
		this.x=p.x;
		this.y=p.y;
		this.z=p.z;
	}

	public String toString() {
		return ("x: "+this.x+" y: "+this.y+" z: "+this.z);
	}


	//	joukko kaanna-metodeja, joilla kaannetaan koordinaatistoa tasossa
	//							|cos a	sin a |
	//	matriisilla |							|, missa a=aste
	//							|-sin a	cos a	|
	//

	public void kaannaXZ() {
		double apu=this.x;
		this.x=(this.x*kosini)-(this.z*sini);
		this.z=(apu*sini)+(this.z*kosini);
	}

	public void kaannaYZ() {
		double apu=this.y;
		this.y=(this.y*kosini)-(this.z*sini);
		this.z=(apu*sini)+(this.z*kosini);
	}

	public void kaannaZX() {
		double apu=this.z;
		this.z=(this.z*kosini)-(this.x*sini);
		this.x=(apu*sini)+(this.x*kosini);
	}

	public void kaannaZY() {
		double apu=this.z;
		this.z=(this.z*kosini)-(this.y*sini);
		this.y=(apu*sini)+(this.y*kosini);
	}

}
