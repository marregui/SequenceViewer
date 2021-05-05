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


import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;


public class ImageSaver {
    public static void save(final JPanel painter, int width, int height) {
        final BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB
        );
        final AtomicBoolean painterThreadIsDone = new AtomicBoolean(false);
        Thread painterThread = new Thread(() -> {
            // Create an image of the PlotControlPanel
            Graphics2D g2d = (Graphics2D) image.getGraphics();
            g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            painter.paint(g2d);
            painterThreadIsDone.set(true);
        });
        painterThread.setDaemon(true);
        painterThread.start();

        // Permitted Image File Extensions
        final Set<String> imgFileExtensions = new TreeSet<>();
        ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(image);
        for (String imgFileExtension : ImageIO.getWriterFormatNames()) {
            Iterator<ImageWriter> itWriters = ImageIO.getImageWriters(spec, imgFileExtension);
            if (itWriters.hasNext()) {
                imgFileExtensions.add(imgFileExtension.toLowerCase());
            }
        }

        // Choose a file name and extension
        JFileChooser imageFileChooser = new JFileChooser();
        imageFileChooser.setDialogTitle(
                "Give a name and extension to the snapshot"
        );
        imageFileChooser.setSelectedFile(new File("changeme.png"));
        imageFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        imageFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        imageFileChooser.setMultiSelectionEnabled(false);
        imageFileChooser.setFileFilter(new FileFilter() {
            private final StringBuffer description = new StringBuffer();

            @Override
            public String getDescription() {
                if (description.length() == 0) {
                    description.append("Image Formats: ");
                    for (String ext : imgFileExtensions) {
                        description.append(ext).append(", ");
                    }
                    description.setLength(description.length() - 2);
                }
                return description.toString();
            }

            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                if (fileName.length() > 0) {
                    int idx = fileName.lastIndexOf(".");
                    if (-1 != idx) {
                        String ext = fileName.substring(idx + 1).toLowerCase();
                        return imgFileExtensions.contains(ext);
                    }
                }
                return false;
            }
        });

        // Save the file
        int returnVal = imageFileChooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = imageFileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            try {
                if (fileName.length() > 0) {
                    int idx = fileName.lastIndexOf(".");
                    String ext = imgFileExtensions.toArray()[0].toString();
                    if (idx != -1) {
                        ext = fileName.substring(idx + 1).toLowerCase();
                    } else {
                        selectedFile = new File(selectedFile.getAbsolutePath() + "." + ext);
                        fileName = selectedFile.getName();
                    }
                    Iterator<ImageWriter> imgageWriterIterator = ImageIO.getImageWritersBySuffix(ext);
                    ImageWriter imgageWriter = null;
                    if (imgageWriterIterator.hasNext()) {
                        imgageWriter = imgageWriterIterator.next();
                    }
                    if (null != imgageWriter) {
                        ImageWriteParam imageWriterParams = imgageWriter.getDefaultWriteParam();
                        if (imageWriterParams.canWriteCompressed()) {
                            imageWriterParams.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
                        }
                        FileImageOutputStream fileOutputStream = new FileImageOutputStream(selectedFile);
                        imgageWriter.setOutput(fileOutputStream);
                        while (!painterThreadIsDone.get()) {
                            try {
                                Thread.sleep(200L);
                            } catch (InterruptedException ie) { /* no-op */ }
                        }
                        imgageWriter.write(image);
                        fileOutputStream.close();
                    } else {
                        throw new InvalidParameterException("Unsupported file extension " + ext);
                    }
                }
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                        null,
                        String.format("Could not save file '%s': %s", fileName, t.getMessage()),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
