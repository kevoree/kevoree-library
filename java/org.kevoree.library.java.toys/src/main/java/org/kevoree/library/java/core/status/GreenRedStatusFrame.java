package org.kevoree.library.java.core.status;

import org.kevoree.log.Log;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 11:58
 */
public class GreenRedStatusFrame extends JFrame {

    private int frameWidth = 150;
    private int frameHeight = 150;
    private Color c;

    public GreenRedStatusFrame(Color c) {
        super("Couleur " + c.toString());
        this.c = c;
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        pack();
    }

    public void paint(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = Graphics2D.class.cast(g);
            g2d.setColor(c);
            g2d.fillOval(0, 0, frameWidth, frameHeight);
        } else {
            Log.debug("Graphics are not 2D. Instance of:" + g.getClass());
        }
    }

    public final void setColor(Color c) {
        this.c = c;
        repaint();
        revalidate();
        Log.debug("SetColor " + c.toString());
    }
}
