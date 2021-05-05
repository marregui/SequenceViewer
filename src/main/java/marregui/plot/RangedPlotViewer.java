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

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;

import marregui.plot.range.RangeSlider;
import marregui.plot.range.RangeSlider.SliderSide;
import marregui.plot.range.RangeSlider.RangeChangedObserver;

public class RangedPlotViewer extends JPanel {
    private static final int MIN = 0;
    private static final int MAX = 100;


    public final PlotViewer plotViewer;
    public final RangeSlider horizontalSlider, verticalSlider;

    public RangedPlotViewer(PlotViewer plotViewer) {
        this.plotViewer = plotViewer;
        this.horizontalSlider = RangeSlider.getHorizontalRangeSlider(MIN, MAX, new RangeChangedObserver() {
            @Override
            public void rangeChanged(int minValue, int maxValue, SliderSide sliderSide) {
                switch (sliderSide) {
                    case NONE:
                        break;

                    case TOP_OR_LEFT:
                        RangedPlotViewer.this.plotViewer.transformMinXRange(minValue, MIN, MAX, false);
                        break;

                    case BOTTOM_OR_RIGHT:
                        RangedPlotViewer.this.plotViewer.transformMaxXRange(maxValue, MIN, MAX, false);
                        break;

                    case THUMB:
                        RangedPlotViewer.this.plotViewer.transformXRange(minValue, maxValue, MIN, MAX, false);
                        break;
                }
            }
        });
        this.plotViewer.setHorizontalRageSlider(this.horizontalSlider);
        this.verticalSlider = RangeSlider.getVerticalRangeSlider(MIN, MAX, new RangeChangedObserver() {
            @Override
            public void rangeChanged(int minValue, int maxValue, SliderSide sliderSide) {
                final int max = MAX - minValue;
                final int min = MAX - maxValue;
                System.out.printf("%s [%d, %d]\n", sliderSide, min, max);
                switch (sliderSide) {
                    case NONE:
                        break;

                    case TOP_OR_LEFT:
                        RangedPlotViewer.this.plotViewer.transformMaxYRange(max, MIN, MAX);
                        break;

                    case BOTTOM_OR_RIGHT:
                        RangedPlotViewer.this.plotViewer.transformMinYRange(min, MIN, MAX);
                        break;

                    case THUMB:
                        RangedPlotViewer.this.plotViewer.transformYRange(min, max, MIN, MAX);
                        break;
                }
            }
        });
        this.plotViewer.setVerticalRageSlider(this.verticalSlider);

        JPanel horizontalSliderPanel = new JPanel(new BorderLayout());
        horizontalSliderPanel.add(this.horizontalSlider, BorderLayout.CENTER);
        horizontalSliderPanel.setPreferredSize(new Dimension(8, 8));
        JPanel verticalSliderPanel = new JPanel(new BorderLayout());
        verticalSliderPanel.add(this.verticalSlider, BorderLayout.CENTER);
        verticalSliderPanel.setPreferredSize(new Dimension(8, 8));
        setLayout(new BorderLayout());
        add(this.plotViewer, BorderLayout.CENTER);
        add(verticalSliderPanel, BorderLayout.WEST);
        add(horizontalSliderPanel, BorderLayout.SOUTH);
    }
}