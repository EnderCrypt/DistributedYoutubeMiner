package com.github.endercrypt.ytddb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class GuiFrame
{
	private JPanel jPanel;
	private JFrame jFrame;

	public GuiFrame(final Listener listener)
	{
		// JPanel
		jPanel = new JPanel()
		{
			{
				setPreferredSize(new Dimension(1000, 500));
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				Dimension screenSize = getSize();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, screenSize.width, screenSize.height);
				g.setColor(Color.BLACK);
				listener.onDraw((Graphics2D) g, screenSize);
			}
		};
		// JFrame
		jFrame = new JFrame();
		jFrame.add(jPanel);
		jFrame.pack();
		jFrame.setLocationRelativeTo(null);
		jFrame.setVisible(true);
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void repaint()
	{
		jFrame.repaint();
	}

	public static interface Listener
	{
		public void onDraw(Graphics2D g2d, Dimension screenSize);
	}
}
