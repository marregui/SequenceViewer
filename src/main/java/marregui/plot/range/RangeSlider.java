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


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public abstract class RangeSlider extends JComponent implements MouseListener, MouseMotionListener {
    public static RangeSlider getHorizontalRangeSlider(int minimum, int maximum, RangeChangedObserver callback) {
        return new HorizontalRangeSlider(minimum, maximum, callback);
    }

    public static RangeSlider getVerticalRangeSlider(int minimum, int maximum, RangeChangedObserver callback) {
        return new VerticalRangeSlider(minimum, maximum, callback);
    }

    protected static final Color CENTER_AREA_COLOR = new Color(240, 240, 250);
    protected static final Color ALT_CENTER_AREA_COLOR = new Color(225, 225, 255);
    protected static final int ARROW_SZ = 16;
    protected static final int ARROW_WIDTH = 8;
    protected static final int ARROW_HEIGHT = 4;

    public static enum SliderSide {NONE, TOP_OR_LEFT, BOTTOM_OR_RIGHT, THUMB;}

    public static interface RangeChangedObserver {
        public void rangeChanged(int minValue, int maxValue, SliderSide sliderSide);
    }

    protected SliderSide pick;
    protected int pickOffsetLow, pickOffsetHigh;
    protected BoundedRangeModel model;
    protected RangeChangedObserver callback;


    protected RangeSlider(int minimum, int maximum, RangeChangedObserver callback) {
        this.callback = callback;
        this.model = new DefaultBoundedRangeModel(minimum, maximum, minimum, maximum);
        this.model.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        });
        setFocusable(true);
        setForeground(Color.LIGHT_GRAY);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public boolean isFullyStretched() {
        return getLowValue() == getMin() && getHighValue() == getMax();
    }

    protected abstract void customRender(Graphics2D g2, int width, int height, int min, int max);

    protected abstract void paintArrow(Graphics2D g2, double x, double y, int w, int h, boolean topDown);

    protected abstract int toLocal(int xOrY);

    protected abstract int toScreen(int xOrY);

    protected abstract int getRelevantCoordinate(MouseEvent e);

    public void reset() {
        setLowValue(getMin());
        setHighValue(getMax());
    }

    public int getMin() {
        return this.model.getMinimum();
    }

    public int getMax() {
        return this.model.getMaximum();
    }

    public int getLowValue() {
        return this.model.getValue();
    }

    public int getHighValue() {
        return this.model.getValue() + this.model.getExtent();
    }

    public void setLowValue(int lowValue) {
        int e = (this.model.getValue() - lowValue) + this.model.getExtent();
        this.model.setRangeProperties(lowValue, e, this.model.getMinimum(), this.model.getMaximum(), false);
        this.model.setValue(lowValue);
    }

    public void setHighValue(int highValue) {
        this.model.setExtent(highValue - this.model.getValue());
    }

    @Override
    public void paintComponent(Graphics g) {
        Rectangle bounds = getBounds();
        int width = (int) bounds.getWidth() - 1;
        int height = (int) bounds.getHeight() - 1;
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(getBackground());
        g2.fillRect(0, 0, width, height);
        g2.setColor(getForeground());
        g2.drawRect(0, 0, width, height);
        customRender(g2, width, height, toScreen(getLowValue()), toScreen(getHighValue()));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int relevantCoordinate = getRelevantCoordinate(e);
        pickHandle(relevantCoordinate);
        this.pickOffsetLow = relevantCoordinate - toScreen(getLowValue());
        this.pickOffsetHigh = relevantCoordinate - toScreen(getHighValue());
        repaint();
    }

    protected void paint3DRectLighting(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(Color.WHITE);
        g2.drawLine(x + 1, y + 1, x + 1, y + height - 1);
        g2.drawLine(x + 1, y + 1, x + width - 1, y + 1);
        g2.setColor(Color.GRAY);
        g2.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        g2.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 1);
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(x, y + height, x + width, y + height);
        g2.drawLine(x + width, y, x + width, y + height);
    }

    protected void pickHandle(int xOrY) {
        int min = toScreen(getLowValue());
        int max = toScreen(getHighValue());
        this.pick = SliderSide.NONE;
        if ((xOrY > (min - ARROW_SZ)) && (xOrY < min)) {
            this.pick = SliderSide.TOP_OR_LEFT;
        } else if ((xOrY >= min) && (xOrY <= max)) {
            pick = SliderSide.THUMB;
        } else if ((xOrY > max) && (xOrY < (max + ARROW_SZ))) {
            this.pick = SliderSide.BOTTOM_OR_RIGHT;
        }
    }

    private void dealWithTopOrLeftSlideMove(int value) {
        int low = toLocal(value - this.pickOffsetLow);
        if (low < getMin()) {
            low = getMin();
        }
        if (low > getMax()) {
            low = getMax();
        }
        if (low > getHighValue()) {
            low = getHighValue();
        }
        setLowValue(low);
    }

    private void dealWithBottomOrRightSlideMove(int value) {
        int high = toLocal(value - this.pickOffsetHigh);
        if (high < getMin()) {
            high = getMin();
        }
        if (high > getMax()) {
            high = getMax();
        }
        if (high < getLowValue()) {
            high = getLowValue();
        }
        setHighValue(high);
    }

    private void dealWithThumbSlideMove(int value) {
        dealWithTopOrLeftSlideMove(value);
        dealWithBottomOrRightSlideMove(value);
    }

    public void mouseDragged(MouseEvent e) {
        requestFocus();
        int value = getRelevantCoordinate(e);
        switch (this.pick) {
            case NONE:
                break;

            case TOP_OR_LEFT:
                dealWithTopOrLeftSlideMove(value);
                break;

            case BOTTOM_OR_RIGHT:
                dealWithBottomOrRightSlideMove(value);
                break;

            case THUMB:
                dealWithThumbSlideMove(value);
                break;
        }
        performCallback();
    }

    private void performCallback() {
        switch (this.pick) {
            case NONE:
                break;
            case TOP_OR_LEFT:
                this.callback.rangeChanged(getLowValue(), getHighValue(), this.pick);
                break;
            case BOTTOM_OR_RIGHT:
                this.callback.rangeChanged(getLowValue(), getHighValue(), this.pick);
                break;
            case THUMB:
                this.callback.rangeChanged(getLowValue(), getHighValue(), this.pick);
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        performCallback();
        this.pick = SliderSide.NONE;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        pickHandle(getRelevantCoordinate(e));
        if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount() && SliderSide.THUMB == this.pick) {
            reset();
            performCallback();
            this.pick = SliderSide.NONE;
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) { /* no-op */ }


    @Override
    public void mouseEntered(MouseEvent e) { /* no-op */ }

    @Override
    public void mouseExited(MouseEvent e) { /* no-op */ }
}