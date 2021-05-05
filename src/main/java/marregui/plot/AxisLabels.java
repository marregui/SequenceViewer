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


public class AxisLabels {
    public static final int X_AXIS_SIGNIFICANT_FIGURES = 1;
    public static final int Y_AXIS_SIGNIFICANT_FIGURES = 3;

    public static final String formatToSignificantFigures(float value, int significantFigures) {
        return String.format(getSignificantFiguresTpt(significantFigures), value);
    }

    public static final String formatForXAxis(float value) {
        return String.format(getSignificantFiguresTpt(X_AXIS_SIGNIFICANT_FIGURES), value);
    }

    public static final String formatForYAxis(float value) {
        return String.format(getSignificantFiguresTpt(Y_AXIS_SIGNIFICANT_FIGURES), value);
    }

    private static final String getSignificantFiguresTpt(int significantFigures) {
        return String.format("%%.%df", Integer.valueOf(significantFigures));
    }

    private String labelFloatFormatingTpt;
    private String[] labels;
    private int[] labelWidths;
    private int[] labelHeights;
    private int[] tickPositions;
    private int tickLength;


    public AxisLabels(String[] labels,
                      int[] labelWidths,
                      int[] labelHeights,
                      int[] tickPositions,
                      int tickLength,
                      int significantDecimals) {
        this.labels = labels;
        this.labelWidths = labelWidths;
        this.labelHeights = labelHeights;
        this.tickPositions = tickPositions;
        this.tickLength = tickLength;
        this.labelFloatFormatingTpt = getSignificantFiguresTpt(significantDecimals);
    }

    public String getLabelFloatFormatingTpt() {
        return this.labelFloatFormatingTpt;
    }

    public int getYPositionOfZeroLabel() {
        int position = -1;
        String zero = String.format(this.labelFloatFormatingTpt, Float.valueOf(0.0F));
        for (int i = 0; i < this.labels.length; i++) {
            if (zero.equals(this.labels[i])) {
                position = this.tickPositions[i];
                break;
            }
        }
        return position;
    }


    public int getSize() {
        return this.labels.length;
    }


    public String getLabel(int n) {
        return this.labels[n];
    }


    public int getLabelWidth(int n) {
        return this.labelWidths[n];
    }


    public int getLabelHeight(int n) {
        return this.labelHeights[n];
    }


    public int getTickPosition(int n) {
        return this.tickPositions[n];
    }


    public int getTickLength() {
        return this.tickLength;
    }


    public void copyTickPositions(int[] newTickPosition) {
        assert this.tickPositions.length == newTickPosition.length;

        if (this.tickPositions.length == 0) {
            throw new Error("Tick positions cannot be null");
        }

        System.arraycopy(newTickPosition, 0, this.tickPositions, 0, newTickPosition.length);
    }
}