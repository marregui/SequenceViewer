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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import marregui.plot.ModeSelectionToolBar.Mode;
import marregui.plot.io.Data;
import marregui.plot.io.DataFileParser;
import marregui.plot.io.DataSet;
import marregui.plot.io.Points;
import marregui.plot.range.RangeDialog;
import marregui.plot.range.RangeDialog.RangeValues;
import marregui.plot.range.RangeDialog.RangeValuesObserver;
import marregui.plot.range.RangeSlider;


public class PlotViewer extends JPanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color BORDER_COLOR = new Color(153, 153, 153);
    private static final Color UNITS_COLOR = new Color(105, 105, 105);
    private static final Color ERROR_BARS_COLOR = new Color(0x770000);
    private static final Color INVALID_POINT_COLOR = new Color(105, 105, 105);
    private static final float[] DASHED_LINE = new float[]{1, 8};
    private static final int X_RANGE_NUMBER_OF_TICKS = 15;
    private static final int Y_RANGE_NUMBER_OF_TICKS = 10;
    private static final int INSET_TOP = 10;
    private static final int INSET_BOTTOM = 50;
    private static final int INSET_LEFT = 80;
    private static final int INSET_RIGHT = 10;
    private static final Insets PLOT_INSETS = new Insets(
            INSET_TOP, INSET_LEFT, INSET_BOTTOM, INSET_RIGHT
    );
    private static final float X_AXIS_EXTRA_VISIBILITY_DELTA = 0.01F;
    private static final float Y_AXIS_EXTRA_VISIBILITY_DELTA = 0.04F;

    private DataSet dataSet;
    private String xAxisLabel;
    private AxisLabels xTickLabels, yTickLabels;
    private PlotRange plotRange;
    private Point2D.Float selectionAreaStartPoint, selectionAreaEndPoint;
    private boolean selectionAreaFirstPointIsInsidePlotArea, selectionOriginatesInOtherPlot;
    private Stack<PlotRange> zoomStack;
    private int clickedMouseButton, plotHeight, plotWidth;
    private float xRange, yRange, xScale, yScale, pointSizeFactor;
    private AffineTransform pointTransformForZoom;
    private boolean hasTickLines, isVisibible, hasErrorBars, hasBaseLine, showNonValidPoints;
    private JCheckBoxMenuItem isVisibibleMenuItem, hasErrorBarsMenuItem, hasBaseLineMenuItem, hasTickLinesMenuItem, showNonValidPointsMenuItem;
    private JMenu plotMenu;
    private JPopupMenu plotPopupMenu;
    private JMenuItem yDataBandNameMenuItem, yDataRangeMenuItem;
    private SequenceViewer multiPlotViewer;
    private RangeSlider horizontalRangeSlider, verticalRangeSlider;


    public PlotViewer(String xAxisLabel, SequenceViewer multiPlotViewer) {
        this.xAxisLabel = xAxisLabel;
        this.multiPlotViewer = multiPlotViewer;
        this.pointSizeFactor = 1.8F;
        this.plotRange = new PlotRange();
        this.selectionAreaStartPoint = new Point2D.Float(0, 0);
        this.selectionAreaEndPoint = new Point2D.Float(0, 0);
        this.zoomStack = new Stack<PlotRange>();
        this.clickedMouseButton = MouseEvent.BUTTON1;
        this.selectionOriginatesInOtherPlot = false;
        createPlotMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setHorizontalRageSlider(RangeSlider horizontalRangeSlider) {
        this.horizontalRangeSlider = horizontalRangeSlider;
    }

    public void setVerticalRageSlider(RangeSlider verticalRangeSlider) {
        this.verticalRangeSlider = verticalRangeSlider;
    }

    public JPopupMenu getPlotPopupMenu() {
        return this.plotPopupMenu;
    }

    private void createPlotMenu() {
        this.plotMenu = new JMenu();

        // Create plot menu
        // Band name
        this.yDataBandNameMenuItem = new JMenuItem();
        this.plotMenu.add(this.yDataBandNameMenuItem);
        this.plotMenu.addSeparator();

        // Max, Min Y axis values
        this.yDataRangeMenuItem = new JMenuItem();
        this.plotMenu.add(this.yDataRangeMenuItem);

        // Restore original range
        JMenuItem restoreOriginalRangesMenuItem = new JMenuItem("Restore original X-Y Range", new ImageIcon(ImageUtils.loadImage("RestoreRanges.png")));
        restoreOriginalRangesMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restoreOriginalRanges();
            }
        });
        this.plotMenu.add(restoreOriginalRangesMenuItem);
        // Change ranges
        JMenuItem changeRangesMenuItem = new JMenuItem("Change X-Y Range", new ImageIcon(ImageUtils.loadImage("ChangeRanges.png")));
        changeRangesMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeRangesDialog();
            }
        });
        this.plotMenu.add(changeRangesMenuItem);
        // Hide other plots
        JMenuItem hideOtherPlotsMenuItem = new JMenuItem("Hide other plots", new ImageIcon(ImageUtils.loadImage("Dude.png")));
        hideOtherPlotsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PlotViewer.this.multiPlotViewer.renderOnlyThisPlot(PlotViewer.this.dataSet.id);
            }
        });
        this.plotMenu.add(hideOtherPlotsMenuItem);
        // Validate all points
        JMenuItem validateAllPointsMenuItem = new JMenuItem("Validate all points", new ImageIcon(ImageUtils.loadImage("Validate.png")));
        validateAllPointsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PlotViewer.this.dataSet.validateAllPoints(true);
                repaint();
            }
        });
        this.plotMenu.add(validateAllPointsMenuItem);

        this.hasBaseLine = true;
        this.hasTickLines = true;
        this.hasErrorBars = false;
        this.isVisibible = true;
        this.showNonValidPoints = true;
        this.hasBaseLineMenuItem = new JCheckBoxMenuItem("Show base line", this.hasBaseLine);
        this.hasTickLinesMenuItem = new JCheckBoxMenuItem("Show tick lines", this.hasTickLines);
        this.hasErrorBarsMenuItem = new JCheckBoxMenuItem("Show error bars", this.hasErrorBars);
        this.showNonValidPointsMenuItem = new JCheckBoxMenuItem("Show non valid points", this.showNonValidPoints);
        this.isVisibibleMenuItem = new JCheckBoxMenuItem("Show plot", this.isVisibible);

        // Plot visibility
        this.isVisibibleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleVisibilityMenuItem();
                PlotViewer.this.multiPlotViewer.renderAllPlots();
            }
        });
        this.plotMenu.add(this.isVisibibleMenuItem);

        // Base line visibility
        this.hasBaseLineMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleBaseLineMenuItem();
            }
        });
        this.plotMenu.add(this.hasBaseLineMenuItem);

        // Tick lines visibility
        this.hasTickLinesMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleTickLinesMenuItem();
            }
        });
        this.plotMenu.add(this.hasTickLinesMenuItem);

        // Error bars visibility
        this.hasErrorBarsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleErrorBarsMenuItem();
            }
        });
        this.plotMenu.add(this.hasErrorBarsMenuItem);

        // Non valid points visibility
        this.showNonValidPointsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleShowNonValidPointsMenuItem();
            }
        });
        this.plotMenu.add(this.showNonValidPointsMenuItem);

        this.plotPopupMenu = this.plotMenu.getPopupMenu();
    }

    private void changeRangesDialog() {
        String bandName = this.dataSet.id;
        float minx = this.plotRange.min.x;
        float maxx = this.plotRange.max.x;
        float miny = this.plotRange.min.y;
        float maxy = this.plotRange.max.y;
        RangeDialog.askForNewRanges(bandName, minx, maxx, miny, maxy, new RangeValuesObserver() {

            @Override
            public void rangeValuesChanged(RangeValues rangeValues) {
                if (null != rangeValues) {
                    float minx = rangeValues.minx;
                    float maxx = rangeValues.maxx;
                    float miny = rangeValues.miny;
                    float maxy = rangeValues.maxy;
                    changeXYRanges(minx, maxx, miny, maxy);
                    adjustHorizontalRangeSlider(minx, maxx, false);
                }
            }
        });
    }

    public void restoreOriginalRanges() {
        resetPlotRanges();
        repaint();
        if (null != this.horizontalRangeSlider) {
            this.horizontalRangeSlider.reset();
        }
    }

    public void toggleVisibilityMenuItem(boolean value) {
        this.isVisibible = value;
        this.isVisibibleMenuItem.setSelected(value);
    }

    private void toggleVisibilityMenuItem() {
        toggleVisibilityMenuItem(!this.isVisibible);
    }

    public void toggleErrorBarsMenuItem(boolean value) {
        this.hasErrorBars = value;
        this.hasErrorBarsMenuItem.setSelected(value);
        repaint();
    }

    private void toggleErrorBarsMenuItem() {
        toggleErrorBarsMenuItem(!this.hasErrorBars);
    }

    public void toggleShowNonValidPointsMenuItem(boolean value) {
        this.showNonValidPoints = value;
        this.showNonValidPointsMenuItem.setSelected(value);
        repaint();
    }

    private void toggleShowNonValidPointsMenuItem() {
        toggleShowNonValidPointsMenuItem(!this.showNonValidPoints);
    }

    public void toggleTickLinesMenuItem(boolean value) {
        this.hasTickLines = value;
        this.hasTickLinesMenuItem.setSelected(value);
        repaint();
    }

    private void toggleTickLinesMenuItem() {
        toggleTickLinesMenuItem(!this.hasTickLines);
    }

    public void toggleBaseLineMenuItem(boolean value) {
        this.hasBaseLine = value;
        this.hasBaseLineMenuItem.setSelected(value);
        repaint();
    }

    private void toggleBaseLineMenuItem() {
        toggleBaseLineMenuItem(!this.hasBaseLine);
    }

    @Override
    public boolean isVisible() {
        return this.isVisibible;
    }

    public void setXAxisUnits(String xAxisUnits) {
        this.xAxisLabel = xAxisUnits;
        repaint();
    }

    public DataSet getDataSet() {
        return this.dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
        this.plotMenu.setText(this.dataSet.id);
        this.yDataBandNameMenuItem.setText(String.format("Band name: %s", this.dataSet.id));
        this.yDataRangeMenuItem.setText(String.format(
                "Data range Y: [%s, %s]",
                AxisLabels.formatForYAxis(this.dataSet.minY), AxisLabels.formatForYAxis(this.dataSet.maxY)));

        // Plot ranges
        resetPlotRanges();
    }

    private static float getAxisExtraVisibilityDelta(float min, float max, float factor) {
        return Math.abs(max - min) * factor;
    }

    private float getXAxisExtraVisibilityDelta() {
        return (null != this.dataSet) ? getAxisExtraVisibilityDelta(this.dataSet.minX, this.dataSet.maxX, X_AXIS_EXTRA_VISIBILITY_DELTA) : 0.0F;
    }

    private float getYAxisExtraVisibilityDelta() {
        return (null != this.dataSet) ? getAxisExtraVisibilityDelta(this.dataSet.minY, this.dataSet.maxY, Y_AXIS_EXTRA_VISIBILITY_DELTA) : 0.0F;
    }

    private void resetPlotRanges() {
        if (null != this.dataSet) {
            float xdelta = getXAxisExtraVisibilityDelta();
            float ydelta = getYAxisExtraVisibilityDelta();
            this.plotRange.min.x = this.dataSet.minX - xdelta;
            this.plotRange.max.x = this.dataSet.maxX + xdelta;
            this.plotRange.min.y = this.dataSet.minY - ydelta;
            this.plotRange.max.y = this.dataSet.maxY + ydelta;
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED
        );
        super.paintComponent(g2);
        drawCanvasXYAxisAndTicks(g2);
        drawCurve(g2);
        drawZoomRectangle(g2);
    }

    private void drawZoomRectangle(Graphics2D g2) {

        if (false == this.selectionAreaStartPoint.equals(this.selectionAreaEndPoint)) {
            g2.setColor(Color.GREEN);
            float startx = this.selectionAreaStartPoint.x;
            float endx = this.selectionAreaEndPoint.x;
            float y = this.selectionOriginatesInOtherPlot ? this.plotRange.min.y + (this.yRange / 2.0F) : this.selectionAreaEndPoint.y;
            float len = 2.0F / this.yScale;
            g2.draw(new Line2D.Float(startx, y - len, startx, y + len));
            g2.draw(new Line2D.Float(startx, y, endx, y));
            g2.draw(new Line2D.Float(endx, y - len, endx, y + len));
        }
    }

    private void drawCurve(Graphics2D g2) {
        if (null != this.dataSet) {
            // Error bars
            Points x = this.dataSet.xValues;
            Points y = this.dataSet.yValues;
            Points error = this.dataSet.yError;
            float xTick = this.pointSizeFactor / this.xScale;
            float yTick = this.pointSizeFactor / this.yScale;
            float xPointWidth = xTick * 2.0F;
            float yPointWidth = yTick * 2.0F;

            GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, dataSet.getSize());
            boolean fistPointFound = false;
            for (int i = 0; i < this.dataSet.getSize(); i++) {
                // Error tick
                if (this.hasErrorBars & (this.dataSet.isValid(i) || this.showNonValidPoints)) {
                    g2.setColor(this.dataSet.isValid(i) ? ERROR_BARS_COLOR : INVALID_POINT_COLOR);
                    float y1 = y.get(i) - error.get(i); // Bottom
                    float y2 = y.get(i) + error.get(i); // Up
                    g2.draw(new Line2D.Float(x.get(i), y1, x.get(i), y2));
                }

                g2.setColor(this.dataSet.isValid(i) ? this.dataSet.getColor() : INVALID_POINT_COLOR);

                if (this.dataSet.isValid(i)) {
                    if (this.hasBaseLine) {
                        // Point
                        if (false == fistPointFound) {
                            path.moveTo(x.get(i), y.get(i));
                            fistPointFound = true;
                        } else {
                            path.lineTo(x.get(i), y.get(i));
                        }
                    }
                }

                // The point
                if (this.dataSet.isValid(i) || this.showNonValidPoints) {
                    g2.fill(new Ellipse2D.Float(x.get(i) - xTick, y.get(i) - yTick, xPointWidth, yPointWidth));
                }
            }

            // Plot the graph
            if (this.hasBaseLine) {
                g2.setColor(this.dataSet.getColor());
                g2.draw(path);
            }
        }
    }

    private void drawCanvasXYAxisAndTicks(Graphics2D g2) {
        Dimension windowDimension = getSize();
        this.plotWidth = windowDimension.width - (PLOT_INSETS.left + PLOT_INSETS.right);
        this.plotHeight = windowDimension.height - (PLOT_INSETS.top + PLOT_INSETS.bottom);
        if (this.plotRange.isUndefined()) {
            // When there is no data we don't know the plot range
            this.plotRange.setMin(0.0F, 0.0F);
            this.plotRange.setMax(this.plotWidth, this.plotHeight);
        }
        this.xRange = this.plotRange.max.x - this.plotRange.min.x;
        this.yRange = this.plotRange.max.y - this.plotRange.min.y;
        this.xScale = this.plotWidth / this.xRange;
        this.yScale = this.plotHeight / this.yRange;

        // Fill background and draw border around plot area.
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, windowDimension.width, windowDimension.height);
        g2.setColor(BORDER_COLOR);
        g2.drawRect(PLOT_INSETS.left, PLOT_INSETS.top, this.plotWidth, plotHeight);

        // Shift coordinate centre to bottom-left corner of the internal rectangle.
        g2.translate(PLOT_INSETS.left, windowDimension.height - PLOT_INSETS.bottom);

        // Draw ticks and tick labels
        drawTicksX(g2);
        drawTicksY(g2);
        drawAxisLabelsAndUnits(g2);

        // Scale the coordinate system to match plot coordinates
        this.pointTransformForZoom = g2.getTransform();
        this.pointTransformForZoom.scale(this.xScale, -1.0F * this.yScale);
        this.pointTransformForZoom.translate(-1.0F * this.plotRange.min.x, -1.0F * this.plotRange.min.y);
        try {
            this.pointTransformForZoom = this.pointTransformForZoom.createInverse();
        } catch (NoninvertibleTransformException ex) {
            System.err.println(ex.getMessage());
        }
        g2.scale(xScale, -yScale);
        g2.translate(-1.0F * this.plotRange.min.x, -1.0F * this.plotRange.min.y);

        // Draw only within plotting area
        g2.setClip(new Rectangle2D.Float(this.plotRange.min.x, this.plotRange.min.y, this.xRange, this.yRange));

        // Set stroke for curve and zoom
        g2.setStroke(new BasicStroke(Math.abs(1.0F / (100.0F * Math.max(this.xScale, this.yScale)))));
    }

    private void drawAxisLabelsAndUnits(Graphics2D g2) {
        FontMetrics fontMetrics = g2.getFontMetrics();
        char[] xAxisUnitsChars = this.xAxisLabel.toCharArray();
        int xAxisUnitsWidth = fontMetrics.charsWidth(xAxisUnitsChars, 0, xAxisUnitsChars.length);
        g2.setColor(UNITS_COLOR);
        g2.drawString(this.xAxisLabel, this.plotWidth - xAxisUnitsWidth, INSET_BOTTOM * 3 / 4);
        if (null != this.dataSet) {
            g2.drawString(
                    String.format(
                            "Zoom Range x:[%s, %s], y:[%s, %s]",
                            AxisLabels.formatForXAxis(this.plotRange.min.x),
                            AxisLabels.formatForXAxis(this.plotRange.max.x),
                            AxisLabels.formatForYAxis(this.plotRange.min.y),
                            AxisLabels.formatForYAxis(this.plotRange.max.y)),
                    0, Math.round(INSET_BOTTOM * 3 / 4));
        }
        // Draw Zero line
        int yPositionOfZero = this.yTickLabels.getYPositionOfZeroLabel();
        if (-1 != yPositionOfZero) {
            g2.setColor(Color.BLACK);
            g2.drawLine(0, yPositionOfZero, this.plotWidth, yPositionOfZero);
        }
    }

    private void drawTicksX(Graphics2D g2) {
        float xRangeTickInterval = this.xRange / X_RANGE_NUMBER_OF_TICKS;
        int[] xTickPositions = getTickPositions(this.plotRange.min.x, this.xRange, this.xScale, xRangeTickInterval, false);
        if (null != xTickPositions) {
            this.xTickLabels = initLabels(g2, this.plotRange.min.x, this.xRange, this.xScale, xRangeTickInterval, xTickPositions, AxisLabels.X_AXIS_SIGNIFICANT_FIGURES);
            int tickLength = this.xTickLabels.getTickLength();
            int labelVerticalPosition = tickLength + this.xTickLabels.getLabelHeight(0);
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            BasicStroke dashedStroke = createDashedStroke(stroke);
            for (int i = 0; i < this.xTickLabels.getSize(); i++) {
                int pos = this.xTickLabels.getTickPosition(i);
                g2.drawLine(pos, 0, pos, tickLength);
                g2.drawString(this.xTickLabels.getLabel(i), pos - this.xTickLabels.getLabelWidth(i) / 2, labelVerticalPosition);
                g2.setStroke(dashedStroke);
                if (this.hasTickLines) {
                    g2.drawLine(pos, 0, pos, -this.plotHeight);
                }
                g2.setStroke(stroke);
            }
        }
    }

    private void drawTicksY(Graphics2D g2) {
        float yRangeTickInterval = this.yRange / Y_RANGE_NUMBER_OF_TICKS;
        int[] yTickPositions = getTickPositions(this.plotRange.min.y, this.yRange, this.yScale, yRangeTickInterval, true);
        if (null != yTickPositions) {
            this.yTickLabels = initLabels(g2, this.plotRange.min.y, this.yRange, this.yScale, yRangeTickInterval, yTickPositions, AxisLabels.Y_AXIS_SIGNIFICANT_FIGURES);
            int tickLength = this.yTickLabels.getTickLength();
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            BasicStroke dashedStroke = createDashedStroke(stroke);
            for (int i = 0; i < this.yTickLabels.getSize(); i++) {
                int pos = this.yTickLabels.getTickPosition(i);
                g2.drawLine(0, pos, -tickLength, pos);
                g2.drawString(this.yTickLabels.getLabel(i), -(this.yTickLabels.getLabelWidth(i) + tickLength + 2), pos + this.yTickLabels.getLabelHeight(i) / 2 - 2);
                g2.setStroke(dashedStroke);
                if (this.hasTickLines) {
                    g2.drawLine(0, pos, this.plotWidth, pos);
                }
                g2.setStroke(stroke);
            }
        }
    }

    private AxisLabels initLabels(Graphics2D g2,
                                  float minValue,
                                  float range,
                                  float scale,
                                  float tickInterval,
                                  int[] tickPositions,
                                  int significantFigures) {
        float startValue = calculateStartValue(minValue, tickInterval);
        int tickNo = calculateTickNo(range, startValue, tickInterval);
        String[] labels = new String[tickNo];
        int[] labelWidths = new int[tickNo];
        int[] labelHeights = new int[tickNo];
        int tickLength = 10;
        FontMetrics fontMetrics = g2.getFontMetrics();
        for (int i = 0; i < tickNo; i++) {
            String label = AxisLabels.formatToSignificantFigures(startValue + i * tickInterval + minValue, significantFigures);
            Rectangle2D bounds = fontMetrics.getStringBounds(label, g2);
            labels[i] = label;
            labelWidths[i] = (int) bounds.getWidth();
            labelHeights[i] = (int) bounds.getHeight();
        }
        return new AxisLabels(labels, labelWidths, labelHeights, tickPositions, tickLength, significantFigures);
    }

    private int[] getTickPositions(float minPoint, float range, float scale, float tickInterval, boolean invert) {
        float start = calculateStartValue(minPoint, tickInterval);
        int tickNo = calculateTickNo(range, start, tickInterval);
        int[] tickPositions = null;
        if (tickNo > 0) {
            int inversionFactor = invert ? -1 : 1;
            tickPositions = new int[tickNo];
            for (int i = 0; i < tickNo; i++) {
                tickPositions[i] = inversionFactor * (int) ((start + i * tickInterval) * scale);
            }
        }
        return tickPositions;
    }

    private BasicStroke createDashedStroke(BasicStroke srcStroke) {
        return new BasicStroke(
                srcStroke.getLineWidth(),
                srcStroke.getEndCap(),
                srcStroke.getLineJoin(),
                srcStroke.getMiterLimit(),
                DASHED_LINE,
                0
        );
    }

    private float calculateStartValue(float minValue, float interval) {
        return (float) (Math.ceil(minValue / interval) * interval - minValue);
    }

    private int calculateTickNo(float range, float start, float interval) {
        return (int) (Math.abs(range - start) / interval + 1);
    }

    private void startMarkingSelectionArea(Point2D cursorPosition) {
        Point2D startPoint = this.pointTransformForZoom.transform(cursorPosition, null);
        this.selectionAreaStartPoint.setLocation(startPoint);
        this.selectionAreaEndPoint.setLocation(startPoint);
    }

    private void keepMarkingSelectionArea(Point2D cursorPosition) {
        Point2D.Float endPoint = (Point2D.Float) this.pointTransformForZoom.transform(cursorPosition, null);
        this.selectionAreaEndPoint = this.plotRange.getInside((Point2D.Float) endPoint);
        repaint();
    }

    private boolean isInside(int x, int y) {
        return x >= INSET_LEFT && x <= INSET_LEFT + this.plotWidth && y >= INSET_TOP && y <= INSET_TOP + this.plotHeight;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.selectionAreaFirstPointIsInsidePlotArea = isInside(e.getX(), e.getY());
        if (this.selectionAreaFirstPointIsInsidePlotArea) {
            mousePressedAction(e, false);
            if (null != this.multiPlotViewer && this.multiPlotViewer.getModeSelectionToolbar().affectsAllPlots()) {
                this.multiPlotViewer.mousePressedOnPlot(this.dataSet.id, e);
            }
        }
    }

    protected void mousePressedAction(MouseEvent e, boolean eventOriginatesInOtherPlot) {
        this.clickedMouseButton = e.getButton();
        this.selectionOriginatesInOtherPlot = eventOriginatesInOtherPlot;
        if (this.clickedMouseButton == MouseEvent.BUTTON1) {
            startMarkingSelectionArea(e.getPoint());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (this.selectionAreaFirstPointIsInsidePlotArea) {
            mouseDraggedAction(e, false);
            if (null != this.multiPlotViewer && this.multiPlotViewer.getModeSelectionToolbar().affectsAllPlots()) {
                this.multiPlotViewer.mouseDraggedOnPlot(this.dataSet.id, e);
            }
        }
    }

    protected void mouseDraggedAction(MouseEvent e, boolean eventOriginatesInOtherPlot) {
        this.selectionOriginatesInOtherPlot = eventOriginatesInOtherPlot;
        if (this.clickedMouseButton == MouseEvent.BUTTON1) {
            keepMarkingSelectionArea(e.getPoint());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (this.selectionAreaFirstPointIsInsidePlotArea) {
            mouseReleasedAction(e, false);
            if (null != this.multiPlotViewer && this.multiPlotViewer.getModeSelectionToolbar().affectsAllPlots()) {
                this.multiPlotViewer.mouseReleasedOnPlot(this.dataSet.id, e);
            }
        }
    }

    protected void mouseReleasedAction(MouseEvent e, boolean eventOriginatesInOtherPlot) {
        this.selectionOriginatesInOtherPlot = eventOriginatesInOtherPlot;
        if (this.clickedMouseButton == MouseEvent.BUTTON1) {
            if (null != this.multiPlotViewer) {
                boolean thereIsSelection = false == this.selectionAreaStartPoint.equals(this.selectionAreaEndPoint);
                if (Mode.Zoom == this.multiPlotViewer.getModeSelectionToolbar().getCurrentMode()) {
                    if (thereIsSelection) {
                        zoomIn();
                    } else {
                        zoomOut();
                    }
                } else if (Mode.InvalidatePoints == this.multiPlotViewer.getModeSelectionToolbar().getCurrentMode()) {
                    if (thereIsSelection) {
                        validatePoints(false);
                    }
                } else if (Mode.ValidatePoints == this.multiPlotViewer.getModeSelectionToolbar().getCurrentMode()) {
                    if (thereIsSelection) {
                        validatePoints(true);
                    }
                } else if (Mode.InvalidatePointsOutsideRectangle == this.multiPlotViewer.getModeSelectionToolbar().getCurrentMode()) {
                    if (thereIsSelection) {
                        invalidatePointsOut();
                    }
                }
            }
            this.selectionAreaEndPoint = this.selectionAreaStartPoint;
            repaint();
        }
    }

    private void invalidatePointsOut() {
        if (null != this.dataSet) {
            float startx = Math.min(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
            float endx = Math.max(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
            this.dataSet.invalidatePointsOut(startx, endx);
        }
    }

    private void validatePoints(boolean pointsAreValid) {
        if (null != this.dataSet) {
            float startx = Math.min(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
            float endx = Math.max(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
            this.dataSet.validatePoints(startx, endx, pointsAreValid);
        }
    }


    private void zoomIn() {
        this.zoomStack.push((PlotRange) this.plotRange.clone());
        this.plotRange.min.x = Math.min(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
        this.plotRange.max.x = Math.max(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
        adjustYRangeToLocalMinMax();
        adjustHorizontalRangeSlider();
    }

    public void changeXYRanges(float minx, float maxx, float miny, float maxy) {
        this.zoomStack.push((PlotRange) this.plotRange.clone());
        this.plotRange.min.x = minx;
        this.plotRange.max.x = maxx;
        this.plotRange.min.y = miny;
        this.plotRange.max.y = maxy;
        repaint();
    }

    public void transformXRange(int minValue, int maxValue, int sliderMin, int sliderMax, boolean comesFromAnotherPlot) {
        float dataScale = (this.dataSet.maxX - this.dataSet.minX) / (sliderMax - sliderMin);
        float min = this.dataSet.minX + (minValue * dataScale);
        float max = this.dataSet.minX + (maxValue * dataScale);
        changeXRange(min, max);
        if (comesFromAnotherPlot) {
            if (null != this.horizontalRangeSlider) {
                this.horizontalRangeSlider.setLowValue(minValue);
                this.horizontalRangeSlider.setHighValue(maxValue);
            }
        } else if (null != this.multiPlotViewer && this.multiPlotViewer.getModeSelectionToolbar().affectsAllPlots()) {
            this.multiPlotViewer.transformXRange(this.dataSet.id, minValue, maxValue, sliderMin, sliderMax);
        }
    }

    public void transformMinXRange(int value, int sliderMin, int sliderMax, boolean comesFromAnotherPlot) {
        float dataScale = (this.dataSet.maxX - this.dataSet.minX) / (sliderMax - sliderMin);
        float min = this.dataSet.minX + (value * dataScale);
        changeXRange(min, this.plotRange.max.x);
        if (comesFromAnotherPlot) {
            if (null != this.horizontalRangeSlider) {
                this.horizontalRangeSlider.setLowValue(value);
            }
        } else if (null != this.multiPlotViewer && this.multiPlotViewer.getModeSelectionToolbar().affectsAllPlots()) {
            this.multiPlotViewer.transformMinXRange(this.dataSet.id, value, sliderMin, sliderMax);
        }
    }

    public void transformMaxXRange(int value, int sliderMin, int sliderMax, boolean comesFromAnotherPlot) {
        float dataScale = (this.dataSet.maxX - this.dataSet.minX) / (sliderMax - sliderMin);
        float max = this.dataSet.minX + (value * dataScale);
        changeXRange(this.plotRange.min.x, max);
        if (comesFromAnotherPlot) {
            if (null != this.horizontalRangeSlider) {
                this.horizontalRangeSlider.setHighValue(value);
            }
        } else if (null != this.multiPlotViewer && this.multiPlotViewer.getModeSelectionToolbar().affectsAllPlots()) {
            this.multiPlotViewer.transformMaxXRange(this.dataSet.id, value, sliderMin, sliderMax);
        }
    }

    public void transformYRange(int minValue, int maxValue, int sliderMin, int sliderMax) {
        float dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
        float min = this.dataSet.minY + (minValue * dataScale);
        float max = this.dataSet.minY + (maxValue * dataScale);
        System.out.println("--> min, max: " + min + ", " + max);
        changeYRange(min, max);
    }

    public void transformMinYRange(int value, int sliderMin, int sliderMax) {
        float dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
        float min = this.dataSet.minY + (value * dataScale);
        System.out.println("--> min: " + min);
        changeYRange(min, this.plotRange.max.y);
    }

    public void transformMaxYRange(int value, int sliderMin, int sliderMax) {
        float dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
        float max = this.dataSet.minY + (value * dataScale);
        System.out.println("--> max: " + max);
        changeYRange(this.plotRange.min.y, max);
    }

    public void adjustHorizontalRangeSlider(float minx, float maxx) {
        adjustHorizontalRangeSlider(minx, maxx, true);
    }

    private void adjustHorizontalRangeSlider(float minx, float maxx, boolean affectPlot) {
        if (null != this.horizontalRangeSlider) {
            int sliderMin = this.horizontalRangeSlider.getMin();
            int sliderMax = this.horizontalRangeSlider.getMax();
            float sliderScale = (sliderMax - sliderMin) / (this.dataSet.maxX - this.dataSet.minX);
            float fmin = sliderMin + ((minx - this.dataSet.minX) * sliderScale);
            float fmax = sliderMin + ((maxx - this.dataSet.minX) * sliderScale);
            this.horizontalRangeSlider.setLowValue(Math.round(fmin));
            this.horizontalRangeSlider.setHighValue(Math.round(fmax));
            if (affectPlot) {
                changeXRange(minx, maxx);
            }
        }
    }

    private void adjustHorizontalRangeSlider() {
        adjustHorizontalRangeSlider(this.plotRange.min.x, this.plotRange.max.x, false);
    }

    private void adjustYRangeToLocalMinMax() {
        if (null != this.dataSet) {
            float min = -1.0F, max = -1.0F, deltaY = -1.0F;
            System.out.println("this.verticalRangeSlider.isfullyStretched? " + this.verticalRangeSlider.isFullyStretched());
            System.out.println("min: " + verticalRangeSlider.getMin() + ", max: " + verticalRangeSlider.getMax() + ", minVal:" + verticalRangeSlider.getLowValue() + ", max val: " + verticalRangeSlider.getHighValue());

            boolean needsToAdjustToLocalMinMax = (null == this.verticalRangeSlider || (null != this.verticalRangeSlider && this.verticalRangeSlider.isFullyStretched()));
            if (needsToAdjustToLocalMinMax) {
                System.out.println("needsToAdjustToLocalMinMax");
                final float[] minMaxY = this.dataSet.getLocalMinMaxInYAxis(this.plotRange.min.x, this.plotRange.max.x);
                min = minMaxY[0];
                max = minMaxY[1];
            } else if (null == this.verticalRangeSlider) {
                System.out.println(">> adjusting to what the slider in the Y axis has to say");
                final int sliderMax = this.verticalRangeSlider.getMax();
                final int sliderMin = this.verticalRangeSlider.getMin();
                final int sliderMaxValue = sliderMax - this.verticalRangeSlider.getMin();
                final int sliderMinValue = sliderMax - this.verticalRangeSlider.getMax();
                final float dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
                System.out.println("slider min, max: " + sliderMin + ", " + sliderMax);
                System.out.println("slider minValue, maxValue: " + sliderMinValue + ", " + sliderMaxValue);
                min = this.dataSet.minY + (sliderMinValue * dataScale);
                max = this.dataSet.minY + (sliderMaxValue * dataScale);
            }
            deltaY = getAxisExtraVisibilityDelta(min, max, Y_AXIS_EXTRA_VISIBILITY_DELTA);
            this.plotRange.min.y = min - deltaY;
            this.plotRange.max.y = max + deltaY;
        }
    }

    private void changeXRange(float minx, float maxx) {
        this.plotRange.min.x = minx;
        this.plotRange.max.x = maxx;
        adjustYRangeToLocalMinMax();
        repaint();
    }

    public void changeYRange(float miny, float maxy) {
        this.plotRange.min.y = miny;
        this.plotRange.max.y = maxy;
        repaint();
    }

    private void zoomOut() {
        if (false == this.zoomStack.isEmpty()) {
            this.plotRange = zoomStack.pop();
            adjustHorizontalRangeSlider();
        } else if (null != this.horizontalRangeSlider) {
            restoreOriginalRanges();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Nothing needed to be done
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            this.plotPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing needed to be done
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nothing needed to be done
    }

    public static void main(String[] args) throws Exception {
        JFrame mainFrame = new JFrame();
        mainFrame.setSize(1200, 800);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        Data data = DataFileParser.parse(new File("data/mathJustOne.txt"));
        mainFrame.setTitle(data.sourceName);
        String bandName = new ArrayList<String>(data.pointsPerBand.keySet()).get(0);
        PlotViewer plotViewer = new PlotViewer("Testing", new SequenceViewer());
        plotViewer.setXAxisUnits(bandName);
        plotViewer.setBackground(Color.WHITE);
        plotViewer.setOpaque(true);
        plotViewer.setDataSet(data.pointsPerBand.get(bandName));

        mainFrame.add(new RangedPlotViewer(plotViewer), BorderLayout.CENTER);

        // Center in the screen
        Dimension screenDimensions = mainFrame.getToolkit().getScreenSize();
        int x = (screenDimensions.width - mainFrame.getWidth()) / 2;
        int y = (screenDimensions.height - mainFrame.getHeight()) / 2;
        mainFrame.setLocation(x, y);
        mainFrame.setVisible(true);
    }
}