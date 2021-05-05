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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import marregui.plot.AxisLabels;

public class FloatValuePanel extends JPanel {
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTON_WIDTH = 42;

    private final float increment;
    private final Axis axis;
    private JTextField text;
    private JButton upButton, downButton;

    public FloatValuePanel(float value, Axis axis) {
        this.increment = getIncrement(axis);
        this.axis = axis;
        Dimension textDimension = new Dimension((int) Math.round(1.5F * BUTTON_WIDTH), 2 * BUTTON_HEIGHT);
        this.text = new JTextField();
        this.text.setSize(textDimension);
        this.text.setPreferredSize(textDimension);
        this.text.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                String currentValue = FloatValuePanel.this.text.getText();
                int len = currentValue.length();
                StringBuilder newValue = new StringBuilder(currentValue.substring(0, offs));
                newValue.append(str);
                if (offs != len) {
                    newValue.append(currentValue.substring(offs + 1));
                }
                try {
                    String newValueStr = newValue.toString();
                    if (null == newValueStr || newValueStr.isEmpty() || newValueStr.equals("-")) {
                        super.insertString(offs, str, a);
                    } else {
                        Float.valueOf(newValueStr);
                        super.insertString(offs, str, a);
                    }
                } catch (NumberFormatException nfe) {
                    // not a number
                }
            }
        });
        setValue(value);

        // Buttons
        Font buttonFont = new Font("Arial", Font.BOLD, 9);
        Dimension buttonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        this.upButton = new JButton("+");
        this.upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                increment();
            }
        });
        this.upButton.setSize(buttonSize);
        this.upButton.setPreferredSize(buttonSize);
        this.upButton.setFont(buttonFont);
        this.downButton = new JButton("-");
        this.downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                decrement();
            }
        });
        this.downButton.setSize(buttonSize);
        this.downButton.setPreferredSize(buttonSize);
        this.downButton.setFont(buttonFont);
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        buttonsPanel.add(this.upButton);
        buttonsPanel.add(this.downButton);

        setLayout(new BorderLayout());
        add(this.text, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.EAST);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        this.text.setEnabled(isEnabled);
        this.upButton.setEnabled(isEnabled);
        this.downButton.setEnabled(isEnabled);
    }

    private static int significantFigures(Axis axis) {
        int significantFigures = -1;
        switch (axis) {
            case X:
                significantFigures = AxisLabels.X_AXIS_SIGNIFICANT_FIGURES;
                break;
            case Y:
                significantFigures = AxisLabels.Y_AXIS_SIGNIFICANT_FIGURES;
                break;
        }
        assert (significantFigures > 1);
        return significantFigures;
    }

    private static float getIncrement(Axis axis) {
        StringBuilder sb = new StringBuilder("0.");
        for (int i = 0; i < significantFigures(axis) - 1; i++) {
            sb.append("0");
        }
        sb.append("1");
        return Float.valueOf(sb.toString());
    }

    public float getValue() {
        String str = this.text.getText();
        if (null == str || str.isEmpty() || str.equals("-")) {
            setValue(0.0F);
            return 0.0F;
        }
        return Float.valueOf(str);
    }

    public void setValue(float value) {
        String str = null;
        switch (this.axis) {
            case X:
                str = AxisLabels.formatForXAxis(value);
                break;
            case Y:
                str = AxisLabels.formatForYAxis(value);
                break;
        }
        this.text.setText(str);
    }

    private void increment() {
        setValue(getValue() + this.increment);
    }

    private void decrement() {
        setValue(getValue() - this.increment);
    }
}