package com.mysterymansion.ui;

import static com.mysterymansion.ui.Theme.FONT;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Label + value + rounded progress bar (used for Health and Moves). */
public class StatBar extends JPanel {
    private final String label;
    private Color color;
    private int value = 1, max = 1;

    public StatBar(String label, Color color) {
        this.label = label;
        this.color = color;
        setOpaque(false);
        setPreferredSize(new Dimension(180, 38));
    }

    public void setColor(Color c) { this.color = c; }

    public void update(int value, int max) {
        this.value = value;
        this.max = max;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font(FONT, Font.BOLD, 12));
        g2.setColor(new Color(200, 200, 205));
        g2.drawString(label, 0, 14);

        String v = Math.max(0, value) + " / " + max;
        g2.drawString(v, getWidth() - g2.getFontMetrics().stringWidth(v), 14);

        int barY = 22, barH = 10, w = getWidth();
        g2.setColor(new Color(58, 58, 62));
        g2.fillRoundRect(0, barY, w, barH, barH, barH);
        int fill = max <= 0 ? 0 : (int) (w * (Math.max(0, value) / (double) max));
        if (fill > 0) {
            g2.setColor(color);
            g2.fillRoundRect(0, barY, Math.max(fill, barH), barH, barH, barH);
        }
        g2.dispose();
    }
}
