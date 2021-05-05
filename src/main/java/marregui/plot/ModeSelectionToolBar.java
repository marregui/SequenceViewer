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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class ModeSelectionToolBar extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Font MODE_SELECTION_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Border FOCUS_BORDER = new LineBorder(Color.BLACK, 2);
    private static final Border NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
    private static final String ZOOM_MODE_ICON_NAME = "ZoomMode.png";
    private static final String VALIDATEPOINTS_MODE_ICON_NAME = "ValidatePointsMode.png";
    private static final String INVALIDATEPOINTS_MODE_ICON_NAME = "InvalidatePointsMode.png";
    private static final String INVALIDATEPOINTS_OUTSIDE_RECTANGLE_MODE_ICON_NAME = "InvalidatePointsOutsideRectangleMode.png";


    public static enum Mode {
        Zoom("Zoom", ZOOM_MODE_ICON_NAME),
        ValidatePoints("Valid", VALIDATEPOINTS_MODE_ICON_NAME),
        InvalidatePoints("Invalid", INVALIDATEPOINTS_MODE_ICON_NAME),
        InvalidatePointsOutsideRectangle("Invalid Out", INVALIDATEPOINTS_OUTSIDE_RECTANGLE_MODE_ICON_NAME);

        private String caption;
        private String iconName;

        private Mode(String caption, String iconName) {
            this.caption = caption;
            this.iconName = iconName;
        }

        public String getCaption() {
            return caption;
        }

        public String getIconName() {
            return iconName;
        }
    }

    private Mode currentMode;
    private JCheckBox affectsAllPlotsCb;
    private JButton[] buttons;


    public ModeSelectionToolBar() {
        this.currentMode = Mode.Zoom;
        final TitledBorder panelBorder = BorderFactory.createTitledBorder("Mode: " + Mode.Zoom);
        panelBorder.setTitleFont(MODE_SELECTION_FONT);
        setBorder(panelBorder);
        GridLayout layout = new GridLayout(1, Mode.values().length + 1);
        layout.setHgap(0);
        layout.setVgap(0);
        setLayout(layout);

        this.affectsAllPlotsCb = new JCheckBox("Affects all plots", true);
        this.affectsAllPlotsCb.setFont(MODE_SELECTION_FONT);
        this.buttons = new JButton[Mode.values().length];
        for (final Mode mode : Mode.values()) {
            final JButton button = new JButton(mode.getCaption());
            button.setFont(MODE_SELECTION_FONT);
            button.setBorder(mode == Mode.Zoom ? FOCUS_BORDER : NO_FOCUS_BORDER);
            button.setIcon(new ImageIcon(ImageUtils.loadImage(mode.getIconName())));
            button.setMargin(new Insets(0, 0, 0, 0));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ModeSelectionToolBar.this.currentMode = mode;
                    panelBorder.setTitle("Mode: " + mode.getCaption());
                    button.setBorder(FOCUS_BORDER);
                    for (int i = 0; i < ModeSelectionToolBar.this.buttons.length; i++) {
                        if (i != mode.ordinal()) {
                            ModeSelectionToolBar.this.buttons[i].setBorder(NO_FOCUS_BORDER);
                        }
                    }
                    if (mode != Mode.Zoom) {
                        ModeSelectionToolBar.this.affectsAllPlotsCb.setEnabled(true);
                    } else {
                        ModeSelectionToolBar.this.affectsAllPlotsCb.setEnabled(false);
                        ModeSelectionToolBar.this.affectsAllPlotsCb.setSelected(true);
                    }

                    ModeSelectionToolBar.this.repaint();
                }
            });
            ModeSelectionToolBar.this.buttons[mode.ordinal()] = button;
            add(button);
        }

        add(this.affectsAllPlotsCb);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        for (JButton button : this.buttons) {
            button.setEnabled(isEnabled);
        }
        this.affectsAllPlotsCb.setEnabled(isEnabled && this.currentMode != Mode.Zoom);
    }

    public boolean affectsAllPlots() {
        return this.affectsAllPlotsCb.isSelected();
    }

    public Mode getCurrentMode() {
        return this.currentMode;
    }
}
