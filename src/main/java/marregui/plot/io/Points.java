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

import java.util.Arrays;

public class Points {
    private static final int SCALE = 100;

    private float[] points;
    private boolean[] pointIsValid;
    private boolean pointsAreAlwaysValid;
    private int offset;
    private int size;
    private float min, max;

    public Points(boolean pointsAreAlwaysValid) {
        this.pointsAreAlwaysValid = pointsAreAlwaysValid;
        this.points = new float[SCALE];
        if (!this.pointsAreAlwaysValid) {
            this.pointIsValid = new boolean[SCALE];
            Arrays.fill(this.pointIsValid, false);
        }
        this.offset = 0;
        this.size = SCALE;
    }

    public int getSize() {
        return this.offset;
    }

    public void addNonValidPoint() {
        addPoint(Float.NaN, false);
    }

    public void addValidPoint(float value) {
        addPoint(value, true);
    }

    private void addPoint(float value, boolean isValid) {
        if (this.offset >= this.size) {
            float[] tmpPoints = new float[this.size + SCALE];
            System.arraycopy(this.points, 0, tmpPoints, 0, this.size);
            this.points = tmpPoints;
            if (!this.pointsAreAlwaysValid) {
                boolean[] tmpPointIsValid = new boolean[this.size + SCALE];
                System.arraycopy(this.pointIsValid, 0, tmpPointIsValid, 0, this.size);
                this.pointIsValid = tmpPointIsValid;
            }
            this.size += SCALE;
        }
        this.points[this.offset] = value;
        if (false == this.pointsAreAlwaysValid) {
            this.pointIsValid[this.offset] = isValid;
        }
        this.offset++;
    }

    protected void done() {
        this.min = Float.MAX_VALUE;
        this.max = Float.MIN_VALUE;
        for (int i = 0; i < this.offset; i++) {
            if (this.pointsAreAlwaysValid || this.pointIsValid[i]) {
                this.min = Math.min(this.min, this.points[i]);
                this.max = Math.max(this.max, this.points[i]);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Points) {
            Points that = (Points) o;
            if (that.getSize() == this.getSize()) {
                for (int i = 0; i < this.getSize(); i++) {
                    if (this.get(i) != that.get(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.points);
    }

    public float get(int i) {
        return this.points[i];
    }

    public boolean isValid(int i) {
        return this.pointsAreAlwaysValid || this.pointIsValid[i];
    }

    public void setValid(int i, boolean isValid) {
        if (false == this.pointsAreAlwaysValid) {
            this.pointIsValid[i] = isValid;
        }
    }

    public void validateAllPoints(boolean pointsAreValid) {
        if (false == this.pointsAreAlwaysValid) {
            Arrays.fill(this.pointIsValid, 0, getSize(), pointsAreValid);
        }
    }

    public float min() {
        return this.min;
    }

    public float max() {
        return this.max;
    }
}