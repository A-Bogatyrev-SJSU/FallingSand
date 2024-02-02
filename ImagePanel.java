import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.Timer;

public class ImagePanel extends JPanel implements MouseListener {
	private static final long serialVersionUID = 5897328252088027800L;

	private int[] color;
	// could use volatile image but then would have to rewrite buffered image
	// specific code
	private BufferedImage im;

	private final int width, height;

	// private Timer t;
	private Thread drawingThread;
	private volatile boolean simRunning = true;
	private boolean sandSpawning = false;
	private int sandSpawnRadius = 25;

	private double sandSpawnDensity = .96;

	private PixelMover pm;

	@SuppressWarnings("unused")
	private enum Material {
		AIR("Air", 0xFFFFFF), SAND("Sand", 0xFF0000), WATER("Water", 0x0000FF);

		public final String label;
		public final int color;

		private static final Map<Integer, Material> BY_COLOR = new HashMap<>();

		private Material(String label, int color) {
			this.label = label;
			this.color = color;
		}

		public static Material valueOfColor(int color) {
			return BY_COLOR.get(color);
		}
	}

	public ImagePanel(int width, int height) {
		this.width = width;
		this.height = height;
		im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		color = new int[width * height];
		for (int i = 0; i < color.length; i++) {
			color[i] = Math.random() > .8 * (Math.abs(i % width - width / 2)) / ((double) height) + .5 ? Material.SAND.color : Material.AIR.color;
		}
		im.setRGB(0, 0, width, height, color, 0, width);

		pm = new PixelMover();

		// timer was only at once ever 15 ms because of java limitations
//			t = new Timer(0, pm);
//			t.start();

		this.setPreferredSize(new Dimension(width, height));
		this.setMinimumSize(new Dimension(width, height));
		this.setMaximumSize(new Dimension(width, height));

		this.addMouseListener(this);
		this.repaint();

		// this runs much faster (as fast as a single thread can run in java)
		// TODO do proper thread lock / unlock
		Runnable r = new Runnable() {
			public void run() {
				while (getSimulationStatus()) {
					pm.processSand();
				}
			}
		};
		drawingThread = new Thread(r);
		drawingThread.start();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		// g.drawString("Hello World!", 100, 100);

		// bad practice, just want to get this done
		g.drawImage(im, 0, 0, null);
	}

	private class PixelMover implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			processSand();
		}

		public void processSand() {
			updateSand();

			// If user clicks and holds, sand will spawn
			if (sandSpawning) {
				spawnSand();
			}

			// update image and repaint panel
			im.setRGB(0, 0, width, height, color, 0, width);

			// if (System.currentTimeMillis() % 5 == 0)
			ImagePanel.this.repaint();
		}

		private void updateSand() {
			int[] oldPixels = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();
			for (int i = height - 1; i >= 0; i--) {
				for (int k = width - 1; k >= 0; k--) {
					int curPos = width * (i) + k;
					int curCol = oldPixels[curPos];

					// System.out.println("current: " + curPos + "\nmax: "+ (width * height - 1) +
					// "\ni: " + i+"\nl: " + k);

					// if not on the last row
					if (i < height - 1 && curCol != Material.AIR.color) {
						// System.out.println("");
						boolean direction = false;

						Integer l, ld, r, rd;
						// all the pixels that need to be scanned
						// □ ▣ □ = l cur r
						// □ □ □ = ld d rd
						// prioritizes down
						// if down is not available, then sides are prioritized,
						// however, will not move diagonally if there is a piece
						// already falling

						l = (k > 0) ? oldPixels[curPos - 1] : null;
						ld = (k > 0) && i < height - 1 ? oldPixels[curPos - 1 + (1) * width] : null;
						r = (k < width - 1) ? oldPixels[curPos + 1] : null;
						rd = (k < width - 1) && i < height - 1 ? oldPixels[curPos + 1 + (1) * width] : null;

						boolean leftEmpty = l != null && ld != null && l != Material.SAND.color && ld != Material.SAND.color;
						boolean rightEmpty = r != null && rd != null && r != Material.SAND.color && rd != Material.SAND.color;

						if (oldPixels[curPos + (1) * width] == Material.AIR.color) {
							color[curPos] = Material.AIR.color;
							color[curPos + (1) * width] = curCol;
						} else if (leftEmpty && rightEmpty) {
							// decide if particle should go left or right
							color[curPos] = Material.AIR.color;
							if (direction) {
								color[curPos - 1 + (1) * width] = Material.SAND.color;
							} else {
								color[curPos + 1 + (1) * width] = Material.SAND.color;
							}
							direction = !direction;
						} else if (leftEmpty) {
							color[curPos] = Material.AIR.color;
							color[curPos - 1 + (1) * width] = Material.SAND.color;
						} else if (rightEmpty) {
							color[curPos] = Material.AIR.color;
							color[curPos + 1 + (1) * width] = Material.SAND.color;
						}
					}
				}
			}
		}

		private void spawnSand() {
			var mloc = MouseInfo.getPointerInfo().getLocation();

			// color[(mloc.y -
			// ImagePanel.this.getLocationOnScreen().y)*width+(mloc.x-ImagePanel.this.getLocationOnScreen().x)]
			// = Material.SAND.color;
			int centerX = mloc.x - ImagePanel.this.getLocationOnScreen().x;
			int centerY = mloc.y - ImagePanel.this.getLocationOnScreen().y;
			// color[(centerY) * width + centerX] = Material.SAND.color;

			for (int y = Math.max(0, centerY - sandSpawnRadius); y < Math.min(centerY + sandSpawnRadius, height); y++) {
				for (int x = Math.max(0, centerX - sandSpawnRadius); x < Math.min(centerX + sandSpawnRadius, width); x++) {
					if (isInsideCircle(x, y, centerX, centerY, sandSpawnRadius) & Math.random() > sandSpawnDensity)
						color[y * width + x] = Material.SAND.color;
				}
			}
		}

		private static boolean isInsideCircle(int x, int y, int centerX, int centerY, int radius) {
			int dx = x - centerX;
			int dy = y - centerY;
			return dx * dx + dy * dy <= radius * radius;
		}

	}

	/**
	 * Pauses drawing of simulation of the sand
	 */
	public synchronized void pauseSimulation() {
		simRunning = false;
	}

	/**
	 * Pauses drawing of simulation of the sand
	 */
	public synchronized void resumeSimulation() {
		simRunning = true;
		// Bad practice
		drawingThread = new Thread(new Runnable() {
			public void run() {
				while (simRunning) {
					pm.processSand();
				}
			}
		});
		drawingThread.start();

	}

	public synchronized void cycleSimulationStatus() {
		simRunning = !simRunning;
		if (simRunning) {
			resumeSimulation();
		}
	}

	public synchronized boolean getSimulationStatus() {
		return simRunning;
	}

	/**
	 * @return the sandSpawnRadius
	 */
	public int getSandSpawnRadius() {
		return sandSpawnRadius;
	}

	/**
	 * @param sandSpawnRadius the sandSpawnRadius to set
	 */
	public void setSandSpawnRadius(int sandSpawnRadius) {
		this.sandSpawnRadius = sandSpawnRadius;
	}

	/**
	 * @return the sandSpawnDensity
	 */
	public double getSandSpawnDensity() {
		return sandSpawnDensity;
	}

	/**
	 * @param sandSpawnDensity the sandSpawnDensity to set
	 */
	public void setSandSpawnDensity(double sandSpawnDensity) {
		this.sandSpawnDensity = sandSpawnDensity;
	}

	public void clearSimulation() {
		pauseSimulation();
		for (int i = 0; i < color.length; i++) {
			color[i] = Material.AIR.color;
		}
		this.repaint();
		im.setRGB(0, 0, width, height, color, 0, width);
		pm.updateSand();
		for (int i = 0; i < color.length; i++) {
			color[i] = Material.AIR.color;
		}
		im.setRGB(0, 0, width, height, color, 0, width);
		resumeSimulation();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// System.out.println("Enabled sand spawning");
		if (e.getButton() == MouseEvent.BUTTON1)
			sandSpawning = true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// System.out.println("Disabled sand spawning");
		if (e.getButton() == MouseEvent.BUTTON1)
			sandSpawning = false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// System.out.println("Disabled sand spawning");
		sandSpawning = false;
	}
}