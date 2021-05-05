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

package marregui.plot.range;


import marregui.plot.ImageUtils;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;


public class HorizontalRangeSlider extends RangeSlider {
    public HorizontalRangeSlider(int minimum, int maximum, RangeChangedObserver callback) {
        super(minimum, maximum, callback);
    }

    @Override
    protected int getRelevantCoordinate(MouseEvent e) {
        return e.getX();
    }

    @Override
    protected void customRender(Graphics2D g2, int width, int height, int min, int max) {
        // Draw arrow and thumb backgrounds
        g2.setStroke(new BasicStroke(1));
        g2.setColor(getForeground());
        g2.fillRect(min - ARROW_SZ, 0, ARROW_SZ - 1, height);
        paint3DRectLighting(g2, min - ARROW_SZ, 0, ARROW_SZ - 1, height);

        g2.setColor(isFullyStretched() ? CENTER_AREA_COLOR : ALT_CENTER_AREA_COLOR);
        g2.fillRect(min, 0, max - min - 1, height);
        paint3DRectLighting(g2, min, 0, max - min - 1, height);

        g2.setColor(getForeground());
        g2.fillRect(max, 0, ARROW_SZ - 1, height);
        paint3DRectLighting(g2, max, 0, ARROW_SZ - 1, height);

        // Draw arrows
        g2.setColor(Color.black);
        paintArrow(g2, min - ARROW_SZ + (ARROW_SZ - ARROW_HEIGHT) / 2.0, (height - ARROW_WIDTH) / 2.0, ARROW_HEIGHT, ARROW_WIDTH, true);
        paintArrow(g2, max + (ARROW_SZ - ARROW_HEIGHT) / 2.0, (height - ARROW_WIDTH) / 2.0, ARROW_HEIGHT, ARROW_WIDTH, false);
    }

    @Override
    protected void paintArrow(Graphics2D g2, double x, double y, int w, int h, boolean topDown) {
        int intX = (int) (x + 0.5);
        int intY = (int) (y + 0.5);

        if (h % 2 == 0) {
            h = h - 1;
        }

        if (topDown) {
            for (int i = 0; i < (h / 2 + 1); i++) {
                g2.drawLine(intX + i, intY + i, intX + i, intY + h - i - 1);
            }
        } else {
            for (int i = 0; i < (h / 2 + 1); i++) {
                g2.drawLine(intX + i, intY + h / 2 - i, intX + i, intY + h - h / 2 + i - 1);
            }
        }
    }

    @Override
    protected int toLocal(int xOrY) {
        Dimension sz = getSize();
        int min = this.model.getMinimum();
        double scale = (sz.width - (2 * ARROW_SZ)) / (double) (this.model.getMaximum() - min);
        return (int) (((xOrY - ARROW_SZ) / scale) + min + 0.5);
    }

    @Override
    protected int toScreen(int xOrY) {
        Dimension sz = getSize();
        int min = this.model.getMinimum();
        double scale = (sz.width - (2 * ARROW_SZ)) / (double) (this.model.getMaximum() - min);
        return (int) (ARROW_SZ + ((xOrY - min) * scale) + 0.5);
    }

    public static void main(String[] args) throws Exception {
        final HorizontalRangeSlider slider = new HorizontalRangeSlider(0, 100, new RangeChangedObserver() {
            @Override
            public void rangeChanged(int minValue, int maxValue, SliderSide sliderSide) {
                System.out.printf("vmin: %d, vmax: %d -> %s\n", Integer.valueOf(minValue), Integer.valueOf(maxValue), sliderSide);
            }
        });
        JFrame frame = new JFrame();
        frame.setIconImage(ImageUtils.loadImage("Application.png"));
        frame.setTitle("Slider");
        frame.setSize(500, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(slider, BorderLayout.CENTER);

        // Center in the screen
        Dimension screenDimensions = frame.getToolkit().getScreenSize();
        int x = (screenDimensions.width - frame.getWidth()) / 2;
        int y = (screenDimensions.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setVisible(true);
    }
}