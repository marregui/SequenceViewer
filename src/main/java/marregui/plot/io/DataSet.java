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

package marregui.plot.io;

import java.awt.Color;

public class DataSet {
    private static final int DEFAULT_HASH_CODE = 0;
    public static final Color DEFAULT_COLOR = new Color(0x006699);

    public final String id;
    public final float minX, maxX, minY, maxY;
    public final Points xValues, yValues, yError;
    private int hashCode;
    private Color color;

    public DataSet(String id, Points xValues, Points[] yValues) {
        this.id = id;
        this.xValues = xValues;
        this.yValues = yValues[DataFileParser.Y_VALUE_IDX];
        this.yError = yValues[DataFileParser.Y_ERROR_IDX];
        this.minX = this.xValues.min();
        this.maxX = this.xValues.max();
        this.minY = this.yValues.min();
        this.maxY = this.yValues.max();
        this.hashCode = DEFAULT_HASH_CODE;
        this.color = DEFAULT_COLOR;
    }

    public boolean isValid(int i) {
        return this.yValues.isValid(i);
    }

    public void validatePoints(float startx, float endx, boolean pointsAreValid) {
        for (int i = 0; i < getSize(); i++) {
            float x = this.xValues.get(i);
            if (x >= startx && x <= endx) {
                this.yValues.setValid(i, pointsAreValid);
            }
        }
    }

    public float[] getLocalMinMaxInYAxis(float minx, float maxx) {
        boolean valuesFound = false;
        float miny = Float.MAX_VALUE;
        float maxy = Float.MIN_VALUE;
        if (maxx > minx) {
            for (int i = 0; i < getSize(); i++) {
                float x = this.xValues.get(i);
                if (x >= minx && x <= maxx) {
                    float y = this.yValues.get(i);
                    miny = Math.min(miny, y);
                    maxy = Math.max(maxy, y);
                    valuesFound = true;
                }
            }
        }
        if (false == valuesFound) {
            miny = this.yValues.min();
            maxy = this.yValues.max();
        }
        return new float[]{miny, maxy};
    }

    public void invalidatePointsOut(float startx, float endx) {
        for (int i = 0; i < getSize(); i++) {
            float x = this.xValues.get(i);
            this.yValues.setValid(i, (x >= startx && x <= endx));
        }
    }

    public void validateAllPoints(boolean pointsAreValid) {
        this.yValues.validateAllPoints(pointsAreValid);
    }

    public int getSize() {
        return this.yValues.getSize();
    }

    @Override
    public boolean equals(Object o) {
        if (null != o) {
            DataSet that = (DataSet) o;
            return null != this.id && null != that.id && this.id.equals(that.id) &&
                    this.yValues.equals(that.yValues) &&
                    this.minY == that.minY &&
                    this.maxY == that.maxY &&
                    Math.abs(this.minX - that.minX) < 0.0001 &&
                    Math.abs(this.maxX - that.maxX) < 0.0001;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.hashCode == DEFAULT_HASH_CODE) {
            this.hashCode = yValues.hashCode();
        }
        return this.hashCode;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("point count: ")
                .append(yValues.getSize())
                .append(", X <")
                .append(minX)
                .append(", ")
                .append(maxX)
                .append(">, Y <")
                .append(minY)
                .append(", ")
                .append(maxY)
                .append(", \"")
                .append(id)
                .append("\">")
                .toString();
    }
}