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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class RangeDialog extends JDialog {
    public static class RangeValues {
        public final float minx, miny, maxx, maxy;

        public RangeValues(float minx, float maxx, float miny, float maxy) {
            this.minx = minx;
            this.maxx = maxx;
            this.miny = miny;
            this.maxy = maxy;
        }

        @Override
        public String toString() {
            return String.format("X: [%.3f, %.3f], Y: [%.3f, %.3f]",
                    Float.valueOf(this.minx), Float.valueOf(this.maxx),
                    Float.valueOf(this.miny), Float.valueOf(this.maxy));
        }
    }

    public static interface RangeValuesObserver {
        public void rangeValuesChanged(RangeValues rangeValues);
    }

    public static void askForNewRanges(String bandName, float minx, float maxx, float miny, float maxy, RangeValuesObserver callback) {
        new RangeDialog(bandName, minx, maxx, miny, maxy, callback).setVisible(true);
    }

    private AxisRangePanel xRange, yRange;
    private RangeValuesObserver callback;

    private RangeDialog(String bandName, float minx, float maxx, float miny, float maxy, RangeValuesObserver callback) {
        this.xRange = new AxisRangePanel(minx, maxx, Axis.X, false);
        this.yRange = new AxisRangePanel(miny, maxy, Axis.Y, false);
        this.callback = callback;
        JPanel rangesPanel = new JPanel(new GridLayout(2, 1));
        rangesPanel.add(this.xRange);
        rangesPanel.add(this.yRange);
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    setRangeValues();
                } catch (Throwable t) {
                    JOptionPane.showMessageDialog(
                            RangeDialog.this,
                            "Make sure the values are valid",
                            "Problem",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RangeDialog.this.setVisible(false);
                RangeDialog.this.dispose();
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        buttonPanel.add(applyButton);

        setIconImage(ImageUtils.loadImage("Application.png"));
        setTitle(bandName);
        setSize(325, 200);
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(rangesPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Center in the screen
        Dimension screenDimensions = getToolkit().getScreenSize();
        int x = (screenDimensions.width - getWidth()) / 2;
        int y = (screenDimensions.height - getHeight()) / 2;
        setLocation(x, y);
    }

    private void setRangeValues() {
        float minx = this.xRange.getMin();
        float maxx = this.xRange.getMax();
        float miny = this.yRange.getMin();
        float maxy = this.yRange.getMax();
        this.callback.rangeValuesChanged(new RangeValues(minx, maxx, miny, maxy));
    }

    public static void main(String[] args) throws Exception {
        RangeDialog.askForNewRanges("Arregui-1", 10, 20, 1, 200, new RangeValuesObserver() {
            @Override
            public void rangeValuesChanged(RangeValues rangeValues) {
                System.out.println(rangeValues);
            }
        });
    }
}
