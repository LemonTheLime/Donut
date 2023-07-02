package com.LemonTheLime.Donut;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;

import javax.swing.JFrame;



/* Donut
 * This program renders a pixelated donut on screen, inspired by the donut.c project.
 * The program is displayed through a 640 x 360 jframe.
 * The program uses a directional light source. It may potentially be extended to a customizable point or spotlight source.
 */
public class Donut extends Canvas implements Runnable {
	private static final long serialVersionUID = 1L;

	//STATIC FIELDS
	public static final int WIDTH = 16; //width of the virtual screen
	public static final int HEIGHT = 9; //height of the virtual screen
	public static final int SCALE = 2; //160 x 90 resolution
	public static final int RES_X = 640 / SCALE; //screen's pixel resolution
	public static final int RES_Y = 360 / SCALE; //screen's pixel resolution
	public static String title = "Donut";
	
	//FRAME & THREAD FIELDS
	private Thread thread;
	private JFrame frame;
	private boolean running = false;
	private BufferedImage image = new BufferedImage(RES_X, RES_Y, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData(); //final screen pixels
	private int[] prePixels = new int[RES_X * RES_Y]; //array to hold pixels before drawing
	
	//LOGIC FIELDS
	private ArrayList<Point3D>points = new ArrayList<Point3D>();
	private ArrayList<Point2D>plane;
	private double focalLength;
	private double cameraDistance;
	private double[] zBuffer = new double[RES_X * RES_Y];
	private Vector3D lightVector = new Vector3D(0, -1, 1);
	
	//CONSTRUCTOR
	public Donut() {
		//create the frame and set the preferred dimensions
		frame = new JFrame();
		Dimension size = new Dimension(RES_X * SCALE, RES_Y * SCALE);
		setPreferredSize(size);
		
		//set the camera settings (focalLength, cameraDistance)
		focalLength = 10;
		cameraDistance = 30;
		
		//create the donut
		createDonutPoints();
	}
		
	//create the all the points in the donut
	/* Benchmark
	 * n1 = 96, n2 = 504, 48384
	 */
	private void createDonutPoints() {
		double r1 = 2.5;
		double r2 = 5;
		int n1 = 250;
		int n2 = 500;
		System.out.println("Total Points: " + (n1 * n2));
		
		
		//donut shell
		for(int i = 0; i < n1 / 1; i++) {
			double m = r2 + r1 * Math.cos(i * 2 * Math.PI / n1);
			double x;
			double y = r1 * Math.sin(i * 2 * Math.PI / n1);
			double z;
			for(int j = 0; j < n2; j++) {
				x = m * Math.cos(j * 2 * Math.PI / n2);
				z = m * Math.sin(j * 2 * Math.PI / n2);
				Point3D nextPoint = new Point3D(x, y, z);
				nextPoint.setRingCenter(r2 * Math.cos(j * 2 * Math.PI / n2),
						0, r2 * Math.sin(j * 2 * Math.PI / n2));
				points.add(nextPoint);
			}
		}
		//test
		rotate(0.9, 0, 0);

		return;
	}
	
	//start the thread
	public synchronized void start() {
		running = true;
		thread = new Thread(this, "Display");
		thread.start();
	}
		
	//run method
	public void run() {
		long lastTime = System.nanoTime();
		long timer = System.currentTimeMillis();
		final double ns = 1000000000.0 / 60.0; //60 updates per second
		double delta = 0;
		int frames = 0;
		while(running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			while(delta >= 1) {
				update();
				delta--;
			}
			render();
			frames++;
			
			if(System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				frame.setTitle(title + " | " + frames + " fps");
				frames = 0;
			}
		}
	}
	
	/* Update donut data
	 * Rotate all the points
	 */
	private void update() {
		rotate(0.0, 0.03, 0.00);
	}
	
	//render and draw the frame buffer
	private void render() {
		//create buffer strategy
		BufferStrategy b = getBufferStrategy();
		if(b == null) {
			createBufferStrategy(3);
			return;
		}
		
		clear();
		renderPixels();
		
		//Graphics context
		Graphics g = b.getDrawGraphics();
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		g.dispose();
		b.show();
	}
	
	//render the prepixels and copy them to the final pixels
	private void renderPixels() {
		//project all the 3D points to 2D points
		projectToScreen();
		
		//render the prepixels
		renderPrePixels();
		
		
		//copy prepixels to final pixels
		for(int i = 0; i < prePixels.length; i++) {
			pixels[i] = prePixels[i];
		}
	}

	//Clear the screen pixels and reset zbuffer
	private void clear() {
		for(int i = 0; i < prePixels.length; i++) {
			prePixels[i] = 0;
			zBuffer[i] = 0;
		}
	}

	//Project all the donut points on a xy plane. Fill the plane list with the points and use z-buffers to check
	private void projectToScreen() {
		plane = new ArrayList<Point2D>();
		for(int i = 0; i < points.size(); i++) {
			plane.add(new Point2D(points.get(i)));
		}
	}
	
	//render the plane list of 2d points to the screen and use z-buffers
	private void renderPrePixels() {

		int luminance;
		for(int i = 0; i < plane.size(); i++) {
			//code
			int x = plane.get(i).getScreenX();
			int y = plane.get(i).getScreenY();
			//GET ILLUMINATION HERE
			//points.get(i).updateSurfaceNormal();
			luminance = 0xffffff; //points.get(i).getLuminance();
			luminance = points.get(i).getLuminance();
			
			//System.out.println("x: " + x + " | y: " + y + " | test: " + (x + y * RES_X));
			if(zBuffer[x + y * RES_X] == 0) {
				//initial rendering
				prePixels[x + y * RES_X] = luminance;
				zBuffer[x + y * RES_X] = plane.get(i).getZDepth();
			} else if(plane.get(i).getZDepth() > zBuffer[x + y * RES_X]) {
				//greater zDepth
				prePixels[x + y * RES_X] = luminance;
				zBuffer[x + y * RES_X] = plane.get(i).getZDepth();
			}
		}
	}
	
	//rotate the entire donut
	private void rotate(double x, double y, double z) {
		for(int i = 0; i < points.size(); i++) {
			points.get(i).rotateX(x);
			points.get(i).rotateY(y);
			points.get(i).rotateZ(z);
			points.get(i).updateSurfaceNormal();

		}
	}
	
	//MAIN
	public static void main(String[] args) {
		Donut donut = new Donut();
		donut.frame.setResizable(false);
		donut.frame.setTitle(Donut.title);
		donut.frame.setBackground(Color.WHITE);
		donut.frame.add(donut);
		donut.frame.pack();
		donut.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		donut.frame.setLocationRelativeTo(null);
		donut.frame.setVisible(true);
		
		donut.start();
	}
	
	//PRIVATE 3D POINT CLASS
	private class Point3D {
		//FIELDS
		private double x, y, z;
		private double xr, yr, zr; //ring center point
		private Vector3D surfaceNormal;
		
		//CONSTRUCTOR
		public Point3D(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		

		//get x
		public double getX() {
			return x;
		}
		
		//get y
		public double getY() {
			return y;
		}
		//get z
		public double getZ() {
			return z;
		}
		
		//set ring center
		private void setRingCenter(double xr, double yr, double zr) {
			this.xr = xr;
			this.yr = yr;
			this.zr = zr;
			updateSurfaceNormal();
		}
		
		//set the surfaceNormal vector
		private void updateSurfaceNormal() {
			surfaceNormal = new Vector3D(x - xr, y - yr, z - zr);
		}
		
		//get the hexadecimal grey luminance
		public int getLuminance() {
			//dot product between -1 to 1
			double product = surfaceNormal.getX() * lightVector.getX() +
					surfaceNormal.getY() * lightVector.getY() +
					surfaceNormal.getZ() * lightVector.getZ();
			if(product < 0) {
				return getHexColor(product);
			} else {
				return 0;
			}
		}
		
		//get the hex color from a double -1 to 1
		private int getHexColor(double product) {
			String v = Integer.toHexString((int)((-product) * 256));
			if(v.length() == 1) {
				v = "0" + v;
			}
			v = v + v + v;
			try {
				int finalValue = Integer.parseInt(v, 16);
				return finalValue;
			} catch(Exception e) {
				System.out.println("v: " + v);
			}
			return 0;
		}


		//rotate x axis about origin (z, y)
		public void rotateX(double a) {
			double u = z;
			double v = y;
			z = u * Math.cos(a) - v * Math.sin(a);
			y = u * Math.sin(a) + v * Math.cos(a);
			u = zr;
			v = yr;
			zr = u * Math.cos(a) - v * Math.sin(a);
			yr = u * Math.sin(a) + v * Math.cos(a);
		}
		//rotate y axis about origin (x, z)
		public void rotateY(double a) {
			double u = x;
			double v = z;
			x = u * Math.cos(a) - v * Math.sin(a);
			z = u * Math.sin(a) + v * Math.cos(a);
			u = xr;
			v = zr;
			xr = u * Math.cos(a) - v * Math.sin(a);
			zr = u * Math.sin(a) + v * Math.cos(a);
		}
		//rotate z axis about origin (x, y)
		public void rotateZ(double a) {
			double u = x;
			double v = y;
			x = u * Math.cos(a) - v * Math.sin(a);
			y = u * Math.sin(a) + v * Math.cos(a);
			u = xr;
			v = yr;
			xr = u * Math.cos(a) - v * Math.sin(a);
			yr = u * Math.sin(a) + v * Math.cos(a);
		}
	}
	
	//PRIVATE 2D POINT CLASS
	private class Point2D {
		//FIELDS
		private double x, y; //(x, y) on the virtual screen
		private double zDepth;
		
		//CONSTRUCTOR
		public Point2D(Point3D p) {
			x = focalLength * p.getX() / (cameraDistance + p.getZ());
			y = focalLength * p.getY() / (cameraDistance + p.getZ());
			zDepth = 1 / (p.getZ() + cameraDistance);
		}
		
		//get x
		public int getScreenX() {
			int centerXOffset = (int)(RES_X / 2);
			return (int)(x * RES_X / WIDTH) + centerXOffset;
		}
		//get y
		public int getScreenY() {
			int centerYOffset = (int)(RES_Y / 2);
			return centerYOffset - (int)(y * RES_Y / HEIGHT);
		}
		//get zDepth
		public double getZDepth() {
			return zDepth;
		}
	}
	
	
	//PRIVATE 3D VECTOR CLASS
	private class Vector3D {
		//FIELDS
		private double x, y, z;
		
		//CONSTRUCTOR
		public Vector3D(double x, double y, double z) {
			double s = Math.sqrt(x * x + y * y + z * z);
			this.x = x / s;
			this.y = y / s;
			this.z = z / s;
		}
		
		//get x
		public double getX() {
			return x;
		}
		//get y
		public double getY() {
			return y;
		}
		//get z
		public double getZ() {
			return z;
		}
	}

}
