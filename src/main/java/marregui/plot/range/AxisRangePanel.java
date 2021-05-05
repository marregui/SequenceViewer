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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class AxisRangePanel extends JPanel {
    public static interface RangeHasBeenChanged {
        public void newRangeValues(float min, float max, Axis axis);
    }

    private Axis axis;
    private FloatValuePanel minValue, maxValue;
    private JButton applyButton;
    private List<RangeHasBeenChanged> rangeChangeObservers;

    public AxisRangePanel(float min, float max, Axis axis, boolean withApplyButton) {
        this.rangeChangeObservers = new CopyOnWriteArrayList<AxisRangePanel.RangeHasBeenChanged>();

        JLabel minLabel = new JLabel("min: ");
        this.minValue = new FloatValuePanel(min, axis);
        JLabel maxLabel = new JLabel("max: ");
        this.maxValue = new FloatValuePanel(max, axis);

        setBorder(BorderFactory.createTitledBorder(String.format("%s Axis", axis)));
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(minLabel);
        add(this.minValue);
        add(maxLabel);
        add(this.maxValue);
        if (withApplyButton) {
            this.applyButton = new JButton("Apply");
            this.applyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    notifyRangeChangeObservers();
                }
            });
            add(this.applyButton);
        }
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        this.minValue.setEnabled(isEnabled);
        this.maxValue.setEnabled(isEnabled);
        if (null != this.applyButton) {
            this.applyButton.setEnabled(isEnabled);
        }
    }

    public void setMin(float min) {
        this.minValue.setValue(min);
    }

    public float getMin() {
        return this.minValue.getValue();
    }

    public void setMax(float max) {
        this.maxValue.setValue(max);
    }

    public float getMax() {
        return this.maxValue.getValue();
    }

    public void addRangeChangeObserver(RangeHasBeenChanged observer) {
        this.rangeChangeObservers.add(observer);
    }

    private void notifyRangeChangeObservers() {
        float min = getMin();
        float max = getMax();
        if (min >= max) {
            JOptionPane.showMessageDialog(
                    this,
                    "Max value must be greater than Min value",
                    "Range problem",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            for (RangeHasBeenChanged observer : this.rangeChangeObservers) {
                observer.newRangeValues(min, max, this.axis);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        AxisRangePanel cd = new AxisRangePanel(10, 200, Axis.X, false);
        cd.setVisible(true);

        JFrame frame = new JFrame();
        frame.setIconImage(ImageUtils.loadImage("Application.png"));
        frame.setTitle("X-Y Ranges");
        frame.setSize(500, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(cd, BorderLayout.CENTER);

        // Center in the screen
        Dimension screenDimensions = frame.getToolkit().getScreenSize();
        int x = (screenDimensions.width - frame.getWidth()) / 2;
        int y = (screenDimensions.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setVisible(true);
    }

}
