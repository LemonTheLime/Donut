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




public class Donut extends Canvas implements Runnable {
	private static final long serialVersionUID = 1L;

	//STATIC FIELDS
	public static final int SCALE = 8; //80 x 45 resolution
	public static final int WIDTH = 2 * 320 / SCALE; //screen's pixel resolution
	public static final int HEIGHT = 2 * 180 / SCALE; //screen's pixel resolution
	public static String title = "Donut";
	
	//FRAME & THREAD FIELDS
	private Thread thread;
	private JFrame frame;
	private boolean running = false;
	private BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData(); //final screen pixels
	private int[] prePixels = new int[WIDTH * HEIGHT]; //array to hold pixels before drawing
	
	//LOGIC FIELDS
	private ArrayList<Point3D>points = new ArrayList<Point3D>();
	private ArrayList<Point2D>plane;
	private double focalLength;
	private double cameraDistance;
	private double[] zBuffer = new double[WIDTH * HEIGHT];
	
	//CONSTRUCTOR
	public Donut() {
		//create the frame and set the preferred dimensions
		frame = new JFrame();
		Dimension size = new Dimension(WIDTH * SCALE, HEIGHT * SCALE);
		setPreferredSize(size);
		
		//set the camera settings (focalLength, cameraDistance)
		focalLength = 10;
		cameraDistance = 80;
		
		//create the donut
		createDonutPoints();
	}
		
	//create the all the points in the donut
	private void createDonutPoints() {
		double r1 = 24;
		double r2 = 48;
		int n1 = 50; //to be tested
		int n2 = 50; //to be tested
		
		for(int i = 0; i < n1; i++) {
			double m = r2 + r1 * Math.cos(i * 2 * Math.PI / n1);
			double x;
			double y = r1 * Math.sin(i * 2 * Math.PI / n1);
			double z;
			for(int j = 0; j < n2; j++) {
				x = m * Math.cos(j * 2 * Math.PI / n2);
				z = m * Math.sin(j * 2 * Math.PI / n2);
				points.add(new Point3D(x, y, z));
			}
		}
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
	public void update() {
		
	}
	
	//render and draw the frame buffer
	public void render() {
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
	public void projectToScreen() {
		plane = new ArrayList<Point2D>();
		for(int i = 0; i < points.size(); i++) {
			plane.add(new Point2D(points.get(i)));
		}
	}
	
	//render the plane list of 2d points to the screen and use z-buffers
	public void renderPrePixels() {
		int centerXOffset = (int)(WIDTH / 2);
		int centerYOffset = (int)(HEIGHT / 2);
		for(int i = 0; i < plane.size(); i++) {
			//code
			int x = plane.get(i).getX() + centerXOffset;
			int y = plane.get(i).getY() + centerYOffset;
			if(zBuffer[x + y * WIDTH] == 0) {
				//initial rendering
				prePixels[x + y * WIDTH] = 0xffffff;
			} else if(plane.get(i).getZDepth() > zBuffer[x + y * WIDTH]) {
				//greater zDepth
				prePixels[x + y * WIDTH] = 0xffffff;
				zBuffer[x + y * WIDTH] = plane.get(i).getZDepth();
			}
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
		
		//rotate x axis about origin (z, y)
		public void rotateX(double a) {
			double u = z;
			double v = y;
			z = u * Math.cos(a) - v * Math.sin(a);
			y = u * Math.sin(a) + v * Math.cos(a);
		}
		//rotate y axis about origin (x, z)
		public void rotateY(double a) {
			double u = x;
			double v = z;
			x = x * Math.cos(a) - z * Math.sin(a);
			z = x * Math.sin(a) + z * Math.cos(a);
		}
		//rotate z axis about origin (x, y)
		public void rotateZ(double a) {
			double u = x;
			double v = y;
			x = u * Math.cos(a) - v * Math.sin(a);
			y = u * Math.sin(a) + v * Math.cos(a);
		}
	}
	
	//PRIVATE 2D POINT CLASS
	private class Point2D {
		//FIELDS
		private int x, y;
		private double luminance;
		private double zDepth;
		
		//CONSTRUCTOR
		public Point2D(Point3D p) {
			x = (int)(focalLength * p.getX() / (cameraDistance + p.getZ()));
			y = (int)(focalLength * p.getY() / (cameraDistance + p.getZ()));
			zDepth = 1 / (p.getZ() + cameraDistance);
		}
		
		//get x
		public int getX() {
			return x;
		}
		//get y
		public int getY() {
			return y;
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
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

}
