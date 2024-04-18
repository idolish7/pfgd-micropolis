// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import static micropolisj.gui.MainWindow.EXTENSION;

import java.awt.BorderLayout;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import micropolisj.engine.GameLevel;
import micropolisj.engine.MapGenerator;
import micropolisj.engine.Micropolis;

public class NewCityDialog extends JDialog
{
	Micropolis engine;
	JButton previousMapBtn;
	Stack<Micropolis> previousMaps = new Stack<Micropolis>();
	Stack<Micropolis> nextMaps = new Stack<Micropolis>();
	OverlayMapView mapPane;
	HashMap<Integer,JRadioButton> levelBtns = new HashMap<Integer,JRadioButton>();
	HashMap<Integer,JToggleButton> featureBtns = new HashMap<Integer,JToggleButton>();


	static final ResourceBundle strings = MainWindow.strings;

	public NewCityDialog(MainWindow owner, boolean showCancelOption)
	{
		super(owner);
		setTitle(strings.getString("welcome.caption"));
		setModal(true);

		assert owner != null;

		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
		getContentPane().add(p1, BorderLayout.CENTER);

		engine = new Micropolis();

		setFeaturePresence(0, true);
		setFeaturePresence(1, true);
		setFeaturePresence(2, true);

		new MapGenerator(engine).generateNewCity(false, false, false);

		mapPane = new OverlayMapView(engine);
		mapPane.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(mapPane, BorderLayout.WEST);

		JPanel p2 = new JPanel(new BorderLayout());
		p1.add(p2, BorderLayout.CENTER);

		Box levelBox = new Box(BoxLayout.Y_AXIS);
		levelBox.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
		p2.add(levelBox, BorderLayout.CENTER);

		levelBox.add(Box.createVerticalGlue());
		JRadioButton radioBtn;
		for (int lev = GameLevel.MIN_LEVEL; lev <= GameLevel.MAX_LEVEL; lev++)
		{
			final int x = lev;
			radioBtn = new JRadioButton(strings.getString("menu.difficulty."+lev));
			radioBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					setGameLevel(x);
				}});
			levelBox.add(radioBtn);
			levelBtns.put(lev, radioBtn);
		}
		levelBox.add(Box.createVerticalGlue());
		setGameLevel(GameLevel.MIN_LEVEL);

		Box featureBox = new Box(BoxLayout.Y_AXIS);
		featureBox.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
		p2.add(featureBox, BorderLayout.CENTER);
		
		featureBox.add(Box.createVerticalGlue());
		for (int feature = 0; feature < 3; feature++) 
		{
			final int x = feature;
			String resourceName = "feature.number."+feature;
			final JToggleButton toggleButton = new JToggleButton(strings.getString(resourceName));
			toggleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setFeaturePresence(x, toggleButton.isSelected());
			}});
			featureBox.add(toggleButton);
			featureBtns.put(feature, toggleButton);
		}
		featureBox.add(Box.createVerticalGlue());
		setFeaturePresence(0, false);
		setFeaturePresence(1, false);
		setFeaturePresence(2, false);

		JPanel buttonPane = new JPanel();
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton btn;
		btn = new JButton(strings.getString("welcome.previous_map"));
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onPreviousMapClicked();
			}});
		btn.setEnabled(false);
		buttonPane.add(btn);
		previousMapBtn = btn;

		btn = new JButton(strings.getString("welcome.play_this_map"));
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onPlayClicked();
			}});
		buttonPane.add(btn);
		getRootPane().setDefaultButton(btn);

		btn = new JButton(strings.getString("welcome.next_map"));
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onNextMapClicked();
			}});
		buttonPane.add(btn);

		btn = new JButton(strings.getString("welcome.load_city"));
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onLoadCityClicked();
			}});
		buttonPane.add(btn);

		if (showCancelOption) {
			btn = new JButton(strings.getString("welcome.cancel"));
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					onCancelClicked();
				}});
			buttonPane.add(btn);
		}
		else {
			btn = new JButton(strings.getString("welcome.quit"));
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					onQuitClicked();
				}});
			buttonPane.add(btn);
		}

		pack();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(owner);
		getRootPane().registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				dispose();
			}},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void onPreviousMapClicked()
	{
		if (previousMaps.isEmpty())
			return;

		nextMaps.push(engine);
		engine = previousMaps.pop();
		mapPane.setEngine(engine);

		previousMapBtn.setEnabled(!previousMaps.isEmpty());
	}

	private void onNextMapClicked()
	{
		if (nextMaps.isEmpty())
		{
			Micropolis m = new Micropolis();
		
		ArrayList<Boolean> featuresEnabledList = new ArrayList<Boolean>(3);
		featuresEnabledList = getFeaturePresences();
			new MapGenerator(m).generateNewCity(featuresEnabledList.get(0), featuresEnabledList.get(1), featuresEnabledList.get(2));
			nextMaps.add(m);
		}

		previousMaps.push(engine);
		engine = nextMaps.pop();
		mapPane.setEngine(engine);

		previousMapBtn.setEnabled(true);
	}

	private void onLoadCityClicked()
	{
		try
		{
			JFileChooser fc = new JFileChooser();
			FileNameExtensionFilter filter1 = new FileNameExtensionFilter(strings.getString("cty_file"), EXTENSION);
			fc.setFileFilter(filter1);

			int rv = fc.showOpenDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				Micropolis newEngine = new Micropolis();
				newEngine.load(file);
				startPlaying(newEngine, file);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(this, e, strings.getString("main.error_caption"),
				JOptionPane.ERROR_MESSAGE);
		}
	}

	void startPlaying(Micropolis newEngine, File file)
	{
		MainWindow win = (MainWindow) getOwner();
		win.setEngine(newEngine);
		win.currentFile = file;
		win.makeClean();
		dispose();
	}

	private void onPlayClicked()
	{
		engine.setGameLevel(getSelectedGameLevel());
		engine.setFunds(GameLevel.getStartingFunds(engine.gameLevel));
		startPlaying(engine, null);
	}

	private void onCancelClicked()
	{
		dispose();
	}

	private void onQuitClicked()
	{
		System.exit(0);
	}

	private int getSelectedGameLevel()
	{
		for (int lev : levelBtns.keySet())
		{
			if (levelBtns.get(lev).isSelected()) {
				return lev;
			}
		}
		return GameLevel.MIN_LEVEL;
	}

	private void setGameLevel(int level)
	{
		for (int lev : levelBtns.keySet())
		{
			levelBtns.get(lev).setSelected(lev == level);
		}
	}

	private void setFeaturePresence(Integer feature, boolean value)
	{
		for (int key = 0; key <= 2; key++) {
			if (!featureBtns.containsKey(key)) {
				JToggleButton toggleButton = new JToggleButton("Button " + key);
				featureBtns.put(key, toggleButton);
			}
		}
		featureBtns.get(feature).setSelected(value);
	}

	public ArrayList getFeaturePresences()
	{
		ArrayList<Boolean> featuresEnabledList = new ArrayList<Boolean>(3);
		for (int feature : featureBtns.keySet())
		{
			featuresEnabledList.add(featureBtns.get(feature).isSelected());
		}
		return featuresEnabledList;
	}
}
