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

package marregui.plot.rfs;

import marregui.plot.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;


/**
 * Dialog that allows the user to select a file or a folder from a remote
 * datachest server and download it. Files and folders are downloaded and
 * stored into a local folder which path is configured in the configuration
 * properties file by key 'datachest.downloads.folder.name'. Files and folders
 * remain in this local folder remain.
 */
public class FileChooser extends JDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileChooser.class);
    private static final String REMOTE_URL = "http://integral.esac.esa.int/datachest";
    private static final String DOWNLOAD_FOLDER = "datachest-downloads";
    private static final long serialVersionUID = 1L;
    private static final long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
    private static final int ALLOWED_DAYS_IN_CACHE = 1;
    private static final int WIDTH = 550;
    private static final int HEIGHT = 350;
    private static final ImageIcon ROOT_ICON = new ImageIcon(ImageUtils.loadImage("root.png"));
    private static final ImageIcon NODE_ICON = new ImageIcon(ImageUtils.loadImage("folder.png"));
    private static final ImageIcon LEAF_ICON = new ImageIcon(ImageUtils.loadImage("file.png"));
    private static final Color SELECTED_COLOR = new Color(40, 200, 40);

    /**
     * @return Shows a selection dialog for a remote file of the
     * Datachest server. The file is downloaded to the local
     * file system (overriding any previous version) and a
     * File handle is returned
     */
    public static File selectFile() {
        LOGGER.debug("Select FILE is invoked");
        return select(SelectionMode.File);
    }

    /**
     * @return Shows a selection dialog for a remote folder of the
     * Datachest server. The folder is downloaded to the local
     * file system (overriding any previous version) in tar/gz
     * format with '.tgz' extension and a File handle is returned
     */
    public static File selectFolder() {
        LOGGER.debug("Select FOLDER is invoked");
        File targz = select(SelectionMode.Folder);
        JOptionPane.showMessageDialog(null, String.format("File available at: %s", targz.getAbsolutePath()));
        return targz;
    }

    private static File select(SelectionMode selectionMode) {
        // Choose a file
        FileChooser fileChooser = new FileChooser(REMOTE_URL, selectionMode);
        fileChooser.setVisible(true);
        FSEntry selectedEntry = fileChooser.getSelectedFSEntry();

        // Download the file to local
        File file = null;
        try {
            file = DatachestProtocol.get(fileChooser.getServerURL(), selectedEntry, manageDownloadsFolder());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    String.format("Problem: %s", IOUtils.exceptionAsString(e)),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return file;
    }

    private static File manageDownloadsFolder() {
        File downloadsFolder = new File(DOWNLOAD_FOLDER);
        if (false == downloadsFolder.exists()) {
            LOGGER.debug(String.format("Creating downloads folder: %s", downloadsFolder.getAbsolutePath()));
            downloadsFolder.mkdirs();
        }

        // Delete files from the downloads folder older than one day
        long currentTimeMillis = System.currentTimeMillis();
        for (File file : downloadsFolder.listFiles()) {
            long days = Math.abs(currentTimeMillis - file.lastModified()) / MILLIS_IN_A_DAY;
            if (days >= ALLOWED_DAYS_IN_CACHE) {
                LOGGER.debug(String.format("Removing expired file: %s", file.getName()));
                file.delete();
            }
        }
        return downloadsFolder;
    }

    private static enum SelectionMode {File, Folder;}

    private JTree tree;
    private FSEntry selectedFSEntry;
    private JButton openButton;
    private String serverURL;

    private FileChooser(String defaultServerURL, final SelectionMode selectionMode) {
        this.serverURL = defaultServerURL;
        setTitle(String.format("Select %s", selectionMode));
        setSize(WIDTH, HEIGHT);
        setModalityType(ModalityType.DOCUMENT_MODAL);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - WIDTH) / 2, (screen.height - HEIGHT) / 2);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) { /* no-op */ }

            @Override
            public void windowIconified(WindowEvent e) { /* no-op */ }

            @Override
            public void windowDeiconified(WindowEvent e) { /* no-op */ }

            @Override
            public void windowDeactivated(WindowEvent e) { /* no-op */ }

            @Override
            public void windowClosing(WindowEvent e) {
                closeAction();
            }

            @Override
            public void windowActivated(WindowEvent e) { /* no-op */ }

            @Override
            public void windowClosed(WindowEvent e) { /* no-op */ }
        });

        // Tree
        this.tree = new JTree(createNodes(defaultServerURL));
        this.tree.setBorder(BorderFactory.createTitledBorder("Data"));
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                FSEntry entry = (FSEntry) node.getUserObject();
                setSelectedFSEntry(entry.isRoot() ? null :
                        (selectionMode == SelectionMode.Folder && entry.isFolder && e.isAddedPath()) ||
                                (selectionMode == SelectionMode.File && false == entry.isFolder && e.isAddedPath()) ?
                                entry : null);
            }
        });
        this.tree.setCellRenderer(new TreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean isSelected,
                                                          boolean isExpanded,
                                                          boolean isLeaf,
                                                          int row,
                                                          boolean hasFocus) {
                FSEntry entry = (FSEntry) ((DefaultMutableTreeNode) value).getUserObject();
                JLabel label = new JLabel(entry.toString());
                label.setIcon(entry.isFolder ? entry.isRoot() ? ROOT_ICON : NODE_ICON : LEAF_ICON);
                label.setForeground(Color.BLACK);
                if (isSelected) {
                    label.setText(String.format("<html><b><u>%s</u></b></html>", entry));
                    label.setForeground(SELECTED_COLOR);
                }
                return label;
            }
        });

        // Buttons
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedFSEntry(null);
                FileChooser.this.dispose();
            }
        });
        this.openButton = new JButton("Open");
        this.openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooser.this.dispose();
            }
        });
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(this.openButton);
        buttonsPanel.add(cancelButton);

        // Server selector
        ServerSelector serverSelector = new ServerSelector(defaultServerURL, new ServerSelector.Callback() {
            @Override
            public void serverSelected(String serverURL) {
                changeDataChestServerURL(serverURL);
            }
        });

        setLayout(new BorderLayout());
        add(serverSelector, BorderLayout.NORTH);
        add(new JScrollPane(this.tree), BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
        setSelectedFSEntry(null);
    }

    private String getServerURL() {
        return this.serverURL;
    }

    private void changeDataChestServerURL(String serverURL) {
        try {
            this.tree.setModel(new DefaultTreeModel(createNodes(serverURL)));
            this.serverURL = serverURL;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    String.format("Problem with the server URL: %s", e.getMessage()),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private FSEntry getSelectedFSEntry() {
        return this.selectedFSEntry;
    }

    private void setSelectedFSEntry(FSEntry entry) {
        this.selectedFSEntry = entry;
        this.openButton.setEnabled(null != entry);
        if (null == entry) {
            this.tree.getSelectionModel().setSelectionPath(null);
        }
    }

    private static DefaultMutableTreeNode createNodes(String serverURL) {
        FSEntry rootEntry = null;
        try {
            rootEntry = DatachestProtocol.list(serverURL);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    String.format("Cannot reach server: %s", serverURL),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return new DefaultMutableTreeNode(FSEntry.folder("Server not accessible", 0), false);
        }

        return createNodes(new DefaultMutableTreeNode(rootEntry, rootEntry.isFolder), rootEntry);
    }

    private static DefaultMutableTreeNode createNodes(DefaultMutableTreeNode parent, FSEntry entry) {
        for (FSEntry child : entry.contents()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(child, child.isFolder);
            parent.add(node);
            if (child.isFolder) {
                createNodes(node, child);
            }
        }
        return parent;
    }

    private void closeAction() {
        setSelectedFSEntry(null);
        dispose();
    }

    public static void main(String[] args) throws Exception {
//        File file = DatachestFileChooser.selectFile();
//        System.out.println("File: " + file);

        File file = FileChooser.selectFolder();
        System.out.println("File: " + file);
    }
}
