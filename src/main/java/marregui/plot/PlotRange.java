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

import java.awt.geom.Point2D;

public class PlotRange implements Cloneable {
    Point2D.Float min;
    Point2D.Float max;

    public PlotRange() {
        this.min = new Point2D.Float(-1, -1);
        this.max = new Point2D.Float(-1, -1);
    }

    public PlotRange(Point2D.Float min, Point2D.Float max) {
        this.min = min;
        this.max = max;
    }

    public void setMin(Point2D.Float min) {
        this.min = min;
    }

    public void setMin(float minx, float miny) {
        this.min = new Point2D.Float(minx, miny);
    }

    public void setMax(Point2D.Float max) {
        this.max = max;
    }

    public void setMax(float maxx, float maxy) {
        this.max = new Point2D.Float(maxx, maxy);
    }

    public Point2D.Float getInside(Point2D.Float point) {
        Point2D.Float pointInside = (Point2D.Float) point.clone();
        if (pointInside.x < this.min.x) {
            pointInside.x = this.min.x;
        } else if (pointInside.x > this.max.x) {
            pointInside.x = this.max.x;
        }

        if (pointInside.y < this.min.y) {
            pointInside.y = this.min.y;
        } else if (pointInside.y > this.max.y) {
            pointInside.x = this.max.y;
        }
        return pointInside;
    }

    @Override
    public Object clone() {
        return new PlotRange((Point2D.Float) this.min.clone(), (Point2D.Float) this.max.clone());
    }

    @Override
    public String toString() {
        return "min: " + this.min + ", max: " + this.max;
    }

    public boolean isUndefined() {
        return this.min.x == -1 && this.min.y == -1 && this.max.x == -1 && this.max.y == -1;
    }
}