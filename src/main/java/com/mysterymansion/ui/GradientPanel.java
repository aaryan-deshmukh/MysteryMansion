package com.mysterymansion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/** Panel with a diagonal gradient built from a room's theme colour. */
public class GradientPanel extends JPanel {
    private Color color;

    public GradientPanel(Color color) {
        super(new BorderLayout());
        this.color = color;
        setOpaque(false);
    }

    public void setColor(Color c) {
        this.color = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Color start = new Color(Math.min(255, color.getRed() * 2),
                Math.min(255, color.getGreen() * 2),
                Math.min(255, color.getBlue() * 2));
        g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), new Color(15, 15, 18)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
