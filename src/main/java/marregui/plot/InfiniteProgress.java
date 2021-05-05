/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.plot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Timer;
import java.util.TimerTask;

public class InfiniteProgress extends JFrame {
    public static InfiniteProgress get() {
        return new InfiniteProgress();
    }

    private static int SIDE = 100;

    private InfiniteProgressPanel progressPanel;

    private InfiniteProgress() {
        this.progressPanel = new InfiniteProgressPanel();
        setAlwaysOnTop(true);
        setUndecorated(true);
        setSize(SIDE, SIDE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - SIDE) / 2, (screen.height - SIDE) / 2);
        add(this.progressPanel);
    }

    public void startAnimation() {
        setVisible(true);
        this.progressPanel.startAnimation();
    }

    public void stopAnimation() {
        setVisible(false);
        this.progressPanel.stopAnimation();
        dispose();
    }

    private static class InfiniteProgressPanel extends JPanel implements MouseListener {
        private static final int NUMBER_OF_TICKS = 24;
        private static final long ANIMATION_DELAY = 80;
        private static final Color CANVAS_COLOR = new Color(250, 250, 250);
        private static final Color TICK_ON_COLOR = new Color(180, 180, 180);
        private static final Color TICK_OFF_COLOR = new Color(200, 200, 200);
        private static final AlphaComposite ALPHA_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8F);
        private volatile Timer animationThread;

        private int currentTick;

        private InfiniteProgressPanel() {
            this.currentTick = 0;
            this.animationThread = null;
            setOpaque(true);
            addMouseListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            /* no-op */
        }

        @Override
        public void mousePressed(MouseEvent e) {
            /* no-op */
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            /* no-op */
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            /* no-op */
        }

        @Override
        public void mouseExited(MouseEvent e) {
            /* no-op */
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (null != this.animationThread) {
                final int w = getWidth();
                final int h = getHeight();
                final double centerx = w / 2.0;
                final double centery = h / 2.0;
                final int tickLength = h / 4;
                final int tickWidth = 2;
                final int tickTipRadius = tickWidth / 2;
                final double tickAngleIncr = 2.0 * Math.PI / ((double) NUMBER_OF_TICKS);

                // Canvas
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(ALPHA_COMPOSITE);
                g2.setColor(Color.BLACK);
                g2.drawRect(1, 1, w - 2, h - 2);
                g2.setColor(CANVAS_COLOR);
                g2.fillRect(0, 0, w, h);

                // Ticks
                AffineTransform toCenter = AffineTransform.getTranslateInstance(centerx, centery);
                AffineTransform toBorder = AffineTransform.getTranslateInstance(tickLength / 2, -tickTipRadius);
                for (int i = 0; i < NUMBER_OF_TICKS; i++) {
                    Area a = new Area(new Rectangle2D.Double(tickTipRadius, 0, tickLength, tickWidth));
                    a.add(new Area(new Ellipse2D.Double(0, 0, tickWidth, tickWidth)));
                    a.add(new Area(new Ellipse2D.Double(tickLength, 0, tickWidth, tickWidth)));
                    a.transform(toCenter);
                    a.transform(toBorder);
                    a.transform(AffineTransform.getRotateInstance(i * tickAngleIncr, w / 2.0, h / 2.0));
                    g2.setColor(i == this.currentTick ? TICK_ON_COLOR : TICK_OFF_COLOR);
                    g2.fill(a);
                }
                this.currentTick = (this.currentTick + 1) % NUMBER_OF_TICKS;
            }
        }

        private void startAnimation() {
            if (null == this.animationThread) {
                synchronized (this) {
                    if (null == this.animationThread) {
                        this.animationThread = new Timer(true);
                        this.animationThread.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                repaint();
                            }
                        }, ANIMATION_DELAY, ANIMATION_DELAY);
                    }
                }
            }
        }

        private void stopAnimation() {
            if (null != this.animationThread) {
                synchronized (this) {
                    if (null != this.animationThread) {
                        this.animationThread.cancel();
                        this.animationThread = null;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        get().startAnimation();
    }
}