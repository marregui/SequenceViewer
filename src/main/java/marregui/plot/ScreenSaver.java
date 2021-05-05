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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ScreenSaver extends JPanel {
    private static final int N = 800;
    private static final int PATCH_COUNT = (int) Math.pow(Math.log(N), 2);
    private static final int PATCH_DENSITY = 11;
    private static final long INITIAL_TICK_INTERVAL = 20L;
    private static final long TICK_INTERVAL_DELTA = 10L;
    private static final Color CANVAS_COLOR = new Color(190, 190, 190);
    private static final Color INK_COLOR = new Color(120, 120, 120);

    private boolean[] grid, swapgrid;
    private long tickInterval;
    private boolean tickLock;
    private Timer tickTimer;
    private volatile boolean isRunning;

    public ScreenSaver() {
        this.grid = new boolean[N * N];
        this.swapgrid = new boolean[N * N];
        this.tickInterval = INITIAL_TICK_INTERVAL;
        setDoubleBuffered(true);
        this.isRunning = false;
    }

    protected void firstGeneration() {
        Random rand = new Random();
        for (int n = 0; n < PATCH_COUNT; n++) {
            int a = rand.nextInt(N);
            int b = rand.nextInt(N);
            int c = rand.nextInt(N);
            int d = rand.nextInt(N);
            for (int i = Math.min(a, b); i < Math.max(a, b); i++) {
                for (int j = Math.min(c, d); j < Math.max(c, d); j++) {
                    int offset = i * N + j;
                    this.grid[offset] = rand.nextInt(this.grid.length) % PATCH_DENSITY == 0;
                }
            }
        }
        repaint();
    }

    private void armTicker() {
        this.tickTimer = new Timer(true);
        this.tickTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                progressGameByOneTick();
            }
        }, this.tickInterval, this.tickInterval);
    }

    public void start() {
        if (false == this.isRunning) {
            synchronized (this) {
                if (false == this.isRunning) {
                    this.isRunning = true;
                    firstGeneration();
                    armTicker();
                }
            }
        }
    }

    public void cancel() {
        if (this.isRunning) {
            synchronized (this) {
                if (this.isRunning) {
                    this.isRunning = false;
                    this.tickTimer.cancel();
                }
            }
        }
    }

    private int neighbourCount(int offset) {
        int count = 0;
        int i = offset / N;
        int j = offset % N;
        for (int x = i - 1; x < i + 2; x++) {
            for (int y = j - 1; y < j + 2; y++) {
                if (x >= 0 && x < N && y >= 0 && y < N && this.grid[(x * N) + y]) {
                    count++;
                }
            }
        }
        return this.grid[offset] ? count - 1 : count;
    }

    private void progressGameByOneTick() {

        if (false == this.tickLock) {
            this.tickLock = true;
            for (int i = 0; i < this.grid.length; i++) {
                int neighbourCount = neighbourCount(i);
                boolean isAlive = this.grid[i];
                if (isAlive) {
                    if ((neighbourCount >= 0 && neighbourCount <= 1) || neighbourCount > 3) {
                        isAlive = false;
                    }
                } else {
                    if (3 == neighbourCount) {
                        isAlive = true;
                    }
                }
                this.swapgrid[i] = isAlive;
            }
            boolean[] tmp = this.grid;
            this.grid = this.swapgrid;
            this.swapgrid = tmp;
            repaint();
        } else {
            this.tickTimer.cancel();
            this.tickInterval += TICK_INTERVAL_DELTA;
            armTicker();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponents(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        int w = getWidth();
        int h = getHeight();
        float yDelta = (float) h / N;
        float xDelta = (float) w / N;
        g2.setColor(CANVAS_COLOR);
        g2.fillRect(0, 0, w, h);
        g2.setColor(INK_COLOR);
        for (int i = 0; i < this.grid.length; i++) {
            if (this.grid[i]) {
                g2.fill(new Rectangle2D.Float((i / N) * xDelta, (i % N) * yDelta, xDelta, yDelta));
            }
        }
        this.tickLock = false;
    }

    public static void main(String[] args) {
        ScreenSaver screenSaver = new ScreenSaver();
        JFrame frame = new JFrame("Game Of Life");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int side = Math.round(Math.min(screen.width, screen.height) * 0.9F);
        frame.setSize(new Dimension(side, side));
        frame.getContentPane().add(screenSaver);
        frame.setVisible(true);
        screenSaver.start();
    }
}