
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
//import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//import java.awt.Graphics;
//import java.awt.Image;
//import java.awt.MouseInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
//import java.awt.image.BufferedImage;
//import java.awt.image.DataBufferInt;
//import java.util.HashMap;
import java.util.Map;
//import java.util.Map;

public class FallingSand {
	private JFrame window;
	private ImagePanel drawingPanel;

	private int height = 720, width = 1020;

	public FallingSand() {
		window = new JFrame("Falling Sand");
		drawingPanel = new ImagePanel(width, height);

		// drawingPanel.setSize(width, height);
		// drawingPanel.setPreferredSize(new Dimension(width, height));
//		window.setSize(width, height);
//		window.setPreferredSize(new Dimension(width, height));
		window.setLayout(new BorderLayout());
		window.add(drawingPanel, BorderLayout.CENTER);

		// Button for sim
		JButton pause = new JButton("Running");
		pause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawingPanel.cycleSimulationStatus();
				pause.setText(drawingPanel.getSimulationStatus() ? "Running" : "Stopped");
			}
		});
		pause.setVisible(true);
		// window.add(pause, BorderLayout.NORTH);

		JLabel radiusLabel = new JLabel("Brush Radius: 25");

		JSlider radiusSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 25);
		radiusSlider.setMajorTickSpacing(10);
		radiusSlider.setMinorTickSpacing(1);
		radiusSlider.setPaintTicks(true);
		radiusSlider.setPaintLabels(true);

		radiusSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int value = ((JSlider) e.getSource()).getValue();
				drawingPanel.setSandSpawnRadius(value);
				radiusLabel.setText("Brush Radius: " + value);
			}
		});
		radiusLabel.setVisible(true);
		radiusLabel.setHorizontalAlignment(JLabel.CENTER);

		JLabel densityLabel = new JLabel("Density:" + .96);
		densityLabel.setHorizontalAlignment(JLabel.CENTER);
		JSlider densitySlider = createDensitySlider(drawingPanel, densityLabel);

		JLabel startStopLabel = new JLabel("Start / Stop Simulation");
		startStopLabel.setHorizontalAlignment(JLabel.CENTER);

		JLabel controlsLabel = new JLabel("Controls");
		controlsLabel.setFont(new Font(UIManager.getDefaults().getFont("Label.font").getName(), Font.BOLD, 30));
		controlsLabel.setHorizontalAlignment(JLabel.CENTER);

		JPanel controls = new JPanel(new GridLayout(5, 1));
		JPanel startStopControlPanel = new JPanel(new GridLayout(3, 1));
		JPanel radiusControlPanel = new JPanel(new GridLayout(3, 1));
		JPanel densityControlPanel = new JPanel(new GridLayout(3, 1));

		startStopControlPanel.add(startStopLabel);
		startStopControlPanel.add(pause);

		radiusControlPanel.add(radiusLabel);
		radiusControlPanel.add(radiusSlider);

		densityControlPanel.add(densityLabel);
		densityControlPanel.add(densitySlider);

		controls.add(controlsLabel);
		controls.add(startStopControlPanel);
		controls.add(radiusControlPanel);
		controls.add(densityControlPanel);
		
		window.add(controls, BorderLayout.EAST);

		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);

	}

	public JSlider createDensitySlider(ImagePanel drawingPanel, JLabel densityLabel) {
		JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 14, 4);
		slider.setMinorTickSpacing(1);
		slider.setMajorTickSpacing(2);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setSnapToTicks(true);

		// Create a label table to display specific labels at certain ticks
		java.util.HashMap<Integer, Double> values = new java.util.HashMap<Integer, Double>();
		values.put(0, .0);
		values.put(1, .0078125);
		values.put(2, .015625);
		values.put(3, .0234375);
		values.put(4, .03125);
		values.put(5, .046875);
		values.put(6, .0625);
		values.put(7, .09375);
		values.put(8, .125);
		values.put(9, .1875);
		values.put(10, .25);
		values.put(11, .375);
		values.put(12, .5);
		values.put(13, .75);
		values.put(14, 1.);

		java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
		for (Map.Entry<Integer, Double> entry : values.entrySet()) {
			Integer key = entry.getKey();
			Double val = entry.getValue();
			if (key%4==0)
				labelTable.put(key, new JLabel(String.format("%.3f", val)));
		}
		labelTable.put(14,new JLabel("1") );
		slider.setLabelTable(labelTable);
		
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double value = values.get(slider.getValue());
				densityLabel.setText(String.format("Density: %.3f", value));
				drawingPanel.setSandSpawnDensity(1 - value);
			}
		});

		return slider;
	}

	public static void main(String[] args) {
		FallingSand fs = new FallingSand();
	}

}
