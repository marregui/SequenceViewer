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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import marregui.plot.io.ImageSaver;
import marregui.plot.io.Data;
import marregui.plot.io.DataFileParser;
import marregui.plot.io.DataFilePersister;
import marregui.plot.io.DataSet;
import marregui.plot.range.Axis;
import marregui.plot.range.AxisRangePanel;
import marregui.plot.rfs.FileChooser;


public class SequenceViewer extends JPanel {

    public static final String DEFAULT_DATA_FOLDER = "data";

    private static final long serialVersionUID = 1L;
    private static final Color TITLE_COLOR = new Color(0x003399);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 12);
    private static final String WINDOW_TITLE = "SequenceViewer v1.0";
    private static final float WINDOW_WIDTH = 0.9F;
    private static final float WINDOW_HEIGHT = 0.85F;

    private final ModeSelectionToolBar modeSelectionToolbar;
    private JMenuBar menuBar;
    private JMenuItem saveTraceMenuItem, saveSnapshotMenuItem;
    private JMenu plotsTopLevelMenu;
    private final AxisRangePanel xRange;
    private final JLabel titleLabel;
    private final JPanel plotsPanel;
    private final ScreenSaver screenSaver;
    private final GridLayout plotsPanelLayout;
    private Data data;
    private final Map<String, RangedPlotViewer> plotViewers;
    private File currentDataFolder;


    public SequenceViewer() {
        this("X Axis", "Y Axis", "X Units", "Y Units");
    }


    public SequenceViewer(String xAxisLabel,
                          String yAxisLabel,
                          String xAxisUnits,
                          String yAxisUnits) {
        // Required data structures
        this.currentDataFolder = new File(DEFAULT_DATA_FOLDER).getAbsoluteFile();
        this.plotViewers = new LinkedHashMap<>();
        this.modeSelectionToolbar = new ModeSelectionToolBar();
        this.modeSelectionToolbar.setEnabled(false);
        this.xRange = new AxisRangePanel(-1.0F, -1.0F, Axis.X, true);
        this.xRange.addRangeChangeObserver((min, max, axis) -> changeXRangeOnPlots(min, max));
        this.xRange.setEnabled(false);

        // Arrangement of the gui components
        this.titleLabel = new JLabel();
        this.titleLabel.setForeground(TITLE_COLOR);
        this.setFont(TITLE_FONT);
        this.plotsPanelLayout = new GridLayout();
        this.plotsPanelLayout.setHgap(0);
        this.plotsPanelLayout.setVgap(0);
        this.plotsPanelLayout.setRows(1);
        this.plotsPanelLayout.setColumns(1);
        this.plotsPanel = new JPanel(this.plotsPanelLayout);
        this.plotsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.screenSaver = new ScreenSaver();
        this.plotsPanel.add(this.screenSaver);
        setLayout(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(this.titleLabel);
        JPanel xRangePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        xRangePanel.add(this.xRange);
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(this.modeSelectionToolbar, BorderLayout.CENTER);
        southPanel.add(xRangePanel, BorderLayout.EAST);
        add(titlePanel, BorderLayout.NORTH);
        add(this.plotsPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        createMenuBar();
        this.screenSaver.start();
    }

    public ModeSelectionToolBar getModeSelectionToolbar() {
        return this.modeSelectionToolbar;
    }

    public JMenuBar getMenuBar() {
        return this.menuBar;
    }

    private void createMenuBar() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        // Open 
        JMenu openMenu = new JMenu("Open Light Curve");
        openMenu.setIcon(new ImageIcon(ImageUtils.loadImage("OpenLightCurve.png")));
        JMenuItem openLocalFileMenuItem = new JMenuItem("Local", new ImageIcon(ImageUtils.loadImage("Earth.png")));
        openLocalFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
        openLocalFileMenuItem.addActionListener(e -> selectLocalLightCurveFile());
        JMenuItem openRemoteFileMenuItem = new JMenuItem("Remote", new ImageIcon(ImageUtils.loadImage("Ufo.png")));
        openRemoteFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
        openRemoteFileMenuItem.addActionListener(e -> selectRemoteLightCurveFile());
        openMenu.add(openLocalFileMenuItem);
        openMenu.add(openRemoteFileMenuItem);
        fileMenu.add(openMenu);
        // Save trace
        JMenuItem downloadRemoteData = new JMenuItem("Download data", new ImageIcon(ImageUtils.loadImage("Download.png")));
        downloadRemoteData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        downloadRemoteData.addActionListener(e -> downloadData());
        fileMenu.add(downloadRemoteData);
        // Save trace
        this.saveTraceMenuItem = new JMenuItem("Save trace", new ImageIcon(ImageUtils.loadImage("Save.png")));
        this.saveTraceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        this.saveTraceMenuItem.addActionListener(e -> saveTraceFile());
        fileMenu.add(this.saveTraceMenuItem);
        this.saveTraceMenuItem.setEnabled(false);
        // Snapshot
        this.saveSnapshotMenuItem = new JMenuItem("Snapshot", new ImageIcon(ImageUtils.loadImage("Snapshot.png")));
        this.saveSnapshotMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        this.saveSnapshotMenuItem.addActionListener(e -> saveSnapshotFile());
        fileMenu.add(this.saveSnapshotMenuItem);
        this.saveSnapshotMenuItem.setEnabled(false);

        // Open trace
        this.plotsTopLevelMenu = new JMenu("Plots");
        this.plotsTopLevelMenu.setMnemonic(KeyEvent.VK_P);
        this.plotsTopLevelMenu.setVisible(false);

        // Menu Bar
        this.menuBar = new JMenuBar();
        this.menuBar.setBorder(BorderFactory.createEmptyBorder());
        this.menuBar.add(fileMenu);
        this.menuBar.add(this.plotsTopLevelMenu);
    }

    private void createPlotsMenuItem(Map<String, DataSet> pointsPerBand) {
        this.plotsTopLevelMenu.removeAll();

        if (0 == pointsPerBand.size()) {
            return;
        }

        // Common menus for all plots
        // Max, Min X axis values
        DataSet firstDataSet = pointsPerBand.values().iterator().next();
        JMenuItem xDataRangeMenuItem = new JMenuItem(String.format(
                "Data range X: [%s, %s]",
                AxisLabels.formatForXAxis(firstDataSet.minX), AxisLabels.formatForXAxis(firstDataSet.maxX)));
        this.plotsTopLevelMenu.add(xDataRangeMenuItem);
        // Restore all plots to their original ranges
        JMenuItem restoreOriginalRangesMenuItem = new JMenuItem("Restore All plots to original X-Y Range", new ImageIcon(ImageUtils.loadImage("RestoreRanges.png")));
        restoreOriginalRangesMenuItem.addActionListener(e -> {
            for (RangedPlotViewer pv : SequenceViewer.this.plotViewers.values()) {
                pv.plotViewer.restoreOriginalRanges();
            }
        });
        this.plotsTopLevelMenu.add(restoreOriginalRangesMenuItem);
        // Show all plots 
        JMenuItem showAllPlotsMenuItem = new JMenuItem("Show All plots", new ImageIcon(ImageUtils.loadImage("Dude.png")));
        showAllPlotsMenuItem.addActionListener(e -> {
            for (RangedPlotViewer pv : SequenceViewer.this.plotViewers.values()) {
                pv.plotViewer.toggleVisibilityMenuItem(true);
            }
            renderAllPlots();
        });
        this.plotsTopLevelMenu.add(showAllPlotsMenuItem);

        // Show all base lines
        final JCheckBoxMenuItem showAllBaseLinesMenuItem = new JCheckBoxMenuItem("Show all base lines", true);
        showAllBaseLinesMenuItem.addActionListener(e -> {
            for (RangedPlotViewer pv : SequenceViewer.this.plotViewers.values()) {
                pv.plotViewer.toggleBaseLineMenuItem(showAllBaseLinesMenuItem.getState());
            }
        });
        this.plotsTopLevelMenu.add(showAllBaseLinesMenuItem);
        // Show all tick lines
        final JCheckBoxMenuItem showAllTickLinesMenuItem = new JCheckBoxMenuItem("Show all tick lines", true);
        showAllTickLinesMenuItem.addActionListener(e -> {
            for (RangedPlotViewer pv : SequenceViewer.this.plotViewers.values()) {
                pv.plotViewer.toggleTickLinesMenuItem(showAllTickLinesMenuItem.getState());
            }
        });
        this.plotsTopLevelMenu.add(showAllTickLinesMenuItem);
        // Show all error bars
        final JCheckBoxMenuItem showAllErrorBarsMenuItem = new JCheckBoxMenuItem("Show all error bars", false);
        showAllErrorBarsMenuItem.addActionListener(e -> {
            for (RangedPlotViewer pv : SequenceViewer.this.plotViewers.values()) {
                pv.plotViewer.toggleErrorBarsMenuItem(showAllErrorBarsMenuItem.getState());
            }
        });
        this.plotsTopLevelMenu.add(showAllErrorBarsMenuItem);
        // Show non valid points
        final JCheckBoxMenuItem showAllNonValidPointsMenuItem = new JCheckBoxMenuItem("Show all non valid points", true);
        showAllNonValidPointsMenuItem.addActionListener(e -> {
            for (RangedPlotViewer pv : SequenceViewer.this.plotViewers.values()) {
                pv.plotViewer.toggleShowNonValidPointsMenuItem(showAllNonValidPointsMenuItem.getState());
            }
        });
        this.plotsTopLevelMenu.add(showAllNonValidPointsMenuItem);

        // Menus specific to each plot
        for (final String bandName : pointsPerBand.keySet()) {
            final JPopupMenu bandNamePopupMenu = this.plotViewers.get(bandName).plotViewer.getPlotPopupMenu();
            final JMenuItem bandNameMenuItem = new JMenuItem(bandName);
            bandNameMenuItem.addActionListener(e -> {
                Point location = bandNameMenuItem.getLocation();
                bandNamePopupMenu.show(SequenceViewer.this, location.x + bandNameMenuItem.getWidth() / 2, location.y);
            });
            this.plotsTopLevelMenu.add(bandNameMenuItem);
        }
        this.plotsTopLevelMenu.setVisible(true);
    }

    private void selectLightCurveFile(File selectedFile) {
        if (null != selectedFile) {
            this.data = null;
            try {
                this.data = DataFileParser.parse(selectedFile);
                createPlots();
                renderAllPlots();
                this.saveTraceMenuItem.setEnabled(true);
                this.saveSnapshotMenuItem.setEnabled(true);
                this.xRange.setEnabled(true);
                this.modeSelectionToolbar.setEnabled(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        SequenceViewer.this,
                        e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        }
    }

    private void selectLocalLightCurveFile() {
        JFileChooser fileChooser = new JFileChooser(this.currentDataFolder);
        fileChooser.setDialogTitle("Select");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.currentDataFolder = fileChooser.getCurrentDirectory().getAbsoluteFile();
            selectLightCurveFile(fileChooser.getSelectedFile());
        }
    }

    private void downloadData() {
        FileChooser.selectFolder();
    }


    private void selectRemoteLightCurveFile() {
        selectLightCurveFile(FileChooser.selectFile());
    }

    private void saveSnapshotFile() {
        ImageSaver.save(this.plotsPanel, this.plotsPanel.getWidth(), this.plotsPanel.getHeight());
    }

    private void saveTraceFile() {
        if (null == this.data) {
            JOptionPane.showMessageDialog(
                    this,
                    "No data to be saved",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Choose a file name and extension
        JFileChooser imageFileChooser = new JFileChooser();
        imageFileChooser.setDialogTitle("Saving trace data");
        imageFileChooser.setSelectedFile(new File(this.data.getFilePath() + "_changeme.txt"));
        imageFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        imageFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        imageFileChooser.setMultiSelectionEnabled(false);

        // Save the file
        int returnVal = imageFileChooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = imageFileChooser.getSelectedFile();
            try {
                if (selectedFile.exists()) {
                    returnVal = JOptionPane.showConfirmDialog(this, "Override file?", "Dilema", JOptionPane.YES_NO_OPTION);
                    if (JOptionPane.YES_OPTION == returnVal) {
                        DataFilePersister.persist(this.data, selectedFile);
                    }
                } else {
                    DataFilePersister.persist(this.data, selectedFile);
                }
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Could not save file '%s': %s", selectedFile.getAbsolutePath(), t.getMessage()),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void changeXRangeOnPlots(float min, float max) {
        if (null != this.data) {
            for (RangedPlotViewer pv : this.plotViewers.values()) {
                pv.plotViewer.adjustHorizontalRangeSlider(min, max);
            }
        }
    }

    private void createPlots() {
        if (null != this.data) {
            this.titleLabel.setText(String.format("%s  -  %s", data.sourceName, data.getFilePath()));
            this.plotViewers.clear();
            for (String bandName : data.pointsPerBand.keySet()) {
                PlotViewer plotViewer = new PlotViewer(bandName, this);
                plotViewer.setBackground(Color.WHITE);
                plotViewer.setOpaque(true);
                plotViewer.setDataSet(this.data.pointsPerBand.get(bandName));
                this.plotViewers.put(bandName, new RangedPlotViewer(plotViewer));
            }
            createPlotsMenuItem(this.data.pointsPerBand);
        }
    }

    protected void transformXRange(String bandName, int minValue, int maxValue, int sliderMin, int sliderMax) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                if (!bandName.equals(targetBandName)) {
                    PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                    pv.transformXRange(minValue, maxValue, sliderMin, sliderMax, true);
                }
            }
        }
    }

    protected void transformMinXRange(String bandName, int value, int sliderMin, int sliderMax) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                if (!bandName.equals(targetBandName)) {
                    PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                    pv.transformMinXRange(value, sliderMin, sliderMax, true);
                }
            }
        }
    }

    protected void transformMaxXRange(String bandName, int value, int sliderMin, int sliderMax) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                if (!bandName.equals(targetBandName)) {
                    PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                    pv.transformMaxXRange(value, sliderMin, sliderMax, true);
                }
            }
        }
    }

    protected void mousePressedOnPlot(String bandName, MouseEvent e) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                if (!bandName.equals(targetBandName)) {
                    PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                    pv.mousePressedAction(e, true);
                }
            }
        }
    }

    protected void mouseDraggedOnPlot(String bandName, MouseEvent e) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                if (!bandName.equals(targetBandName)) {
                    PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                    pv.mouseDraggedAction(e, true);
                }
            }
        }
    }

    protected void mouseReleasedOnPlot(String bandName, MouseEvent e) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                if (!bandName.equals(targetBandName)) {
                    PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                    pv.mouseReleasedAction(e, true);
                }
            }
        }
    }

    protected void renderOnlyThisPlot(String bandName) {
        if (null != this.data && null != bandName) {
            for (String targetBandName : this.plotViewers.keySet()) {
                boolean value = bandName.equals(targetBandName);
                PlotViewer pv = this.plotViewers.get(targetBandName).plotViewer;
                pv.toggleVisibilityMenuItem(value);
            }
            renderAllPlots();
        }
    }

    protected void renderAllPlots() {
        if (null != this.data) {
            float minx = Float.MAX_VALUE;
            float maxx = Float.MIN_VALUE;
            Set<String> visibleBandNames = new HashSet<>();
            for (String bandName : this.plotViewers.keySet()) {
                PlotViewer pv = this.plotViewers.get(bandName).plotViewer;
                if (pv.isVisible()) {
                    DataSet dataSet = pv.getDataSet();
                    minx = Math.min(minx, dataSet.minX);
                    maxx = Math.max(maxx, dataSet.maxX);
                    visibleBandNames.add(bandName);
                }
            }
            this.plotsPanel.removeAll();
            if (0 == visibleBandNames.size()) {
                this.plotsPanelLayout.setRows(1);
                this.plotsPanel.add(this.screenSaver);
                this.screenSaver.start();
            } else {
                this.screenSaver.cancel();
                this.plotsPanelLayout.setRows(visibleBandNames.size());
                for (String bandName : this.data.pointsPerBand.keySet()) {
                    if (visibleBandNames.contains(bandName)) {
                        this.plotsPanel.add(this.plotViewers.get(bandName));
                    }
                }
                this.xRange.setMin(minx);
                this.xRange.setMax(maxx);
            }
            this.plotsPanel.revalidate();
            this.plotsPanel.repaint();
        }
    }


    public static void main(String[] args) throws Exception {
        Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();

        JFrame mainFrame = new JFrame();
        mainFrame.setIconImage(ImageUtils.loadImage("Application.png"));
        mainFrame.setTitle(WINDOW_TITLE);
        int w = Math.round(screenDimensions.width * WINDOW_WIDTH);
        int h = Math.round(screenDimensions.height * WINDOW_HEIGHT);
        mainFrame.setSize(w, h);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());
        SequenceViewer multiPlotViewer = new SequenceViewer();
        mainFrame.add(multiPlotViewer, BorderLayout.CENTER);
        mainFrame.setJMenuBar(multiPlotViewer.getMenuBar());

        // Center in the screen
        int x = (screenDimensions.width - mainFrame.getWidth()) / 2;
        int y = (screenDimensions.height - mainFrame.getHeight()) / 2;
        mainFrame.setLocation(x, y);
        mainFrame.setVisible(true);
    }
}

