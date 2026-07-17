package mzmod.ui;

import mzmod.merger.JarMerger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

import com.formdev.flatlaf.FlatIntelliJLaf;

/**
 * Main application window for MZMOD Advance Menu (Swing edition).
 */
public class MainFrame extends JFrame {

    private final JarMerger merger = new JarMerger();

    // UI components
    private final DefaultListModel<File> jarListModel = new DefaultListModel<>();
    private final JList<File> jarList = new JList<>(jarListModel);
    private final JTextField programNameField = new JTextField("Merged App");
    private final JTextField displayNameField = new JTextField("Merged App");
    private final JTextField vendorField = new JTextField("MZMOD");
    private final JTextField iconField = new JTextField();
    private final JTextField savePathField = new JTextField("merged.jar");
    private final JTextField copyCountField = new JTextField("1");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JTextArea logArea = new JTextArea();
    private final JScrollPane logScroll = new JScrollPane(logArea);

    public MainFrame() {
        super("MZMOD Advance Menu v4.0");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 600);
        setMinimumSize(new Dimension(600, 500));
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("JAR Files", createJarPanel());
        tabs.addTab("Settings", createSettingsPanel());
        tabs.addTab("Merge", createMergePanel());
        setContentPane(tabs);
    }

    // ------------------------------------------------------------------
    //  Tab 1: JAR file selection
    // ------------------------------------------------------------------

    private JPanel createJarPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Toolbar
        var toolbar = new JToolBar();
        toolbar.setFloatable(false);
        var addBtn = new JButton("Select JAR...");
        var clearBtn = new JButton("Clear");
        toolbar.add(addBtn);
        toolbar.addSeparator();
        toolbar.add(clearBtn);
        panel.add(toolbar, BorderLayout.NORTH);

        // List (single file)
        jarList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof File) setText(((File) value).getAbsolutePath());
                return this;
            }
        });
        panel.add(new JScrollPane(jarList), BorderLayout.CENTER);

        // Number of copies (merge the same game N times)
        var countPanel = new JPanel(new BorderLayout(10, 0));
        countPanel.add(new JLabel("Số lượng bản ghép:"), BorderLayout.WEST);
        countPanel.add(copyCountField, BorderLayout.CENTER);
        panel.add(countPanel, BorderLayout.SOUTH);

        // Actions
        addBtn.addActionListener(e -> {
            var chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileNameExtensionFilter("JAR files (*.jar)", "jar"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                merger.setSourceJar(f);
                jarListModel.clear();
                jarListModel.addElement(f);
            }
        });

        clearBtn.addActionListener(e -> {
            merger.clearSourceJar();
            jarListModel.clear();
        });

        return panel;
    }

    // ------------------------------------------------------------------
    //  Tab 2: Settings
    // ------------------------------------------------------------------

    private JPanel createSettingsPanel() {
        var panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        var gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0;
        gc.insets = new Insets(5, 5, 5, 10);

        int row = 0;
        addField(panel, gc, row++, "Program Name:", programNameField);
        addField(panel, gc, row++, "Display Name:", displayNameField);
        addField(panel, gc, row++, "Vendor:", vendorField);

        // Icon with browse button
        gc.gridy = row; gc.gridx = 0;
        panel.add(new JLabel("Icon:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        var iconPanel = new JPanel(new BorderLayout(5, 0));
        iconPanel.add(iconField, BorderLayout.CENTER);
        var iconBrowse = new JButton("...");
        iconBrowse.setPreferredSize(new Dimension(30, iconField.getPreferredSize().height));
        iconBrowse.addActionListener(e -> {
            var chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                iconField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        iconPanel.add(iconBrowse, BorderLayout.EAST);
        panel.add(iconPanel, gc);
        row++;

        addField(panel, gc, row++, "Save Path:", savePathField);
        // Browse button for save path
        gc.gridy = row; gc.gridx = 0;
        panel.add(new JLabel("Browse:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        var saveBrowse = new JButton("Choose output file...");
        saveBrowse.addActionListener(e -> {
            var chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                savePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(saveBrowse, gc);
        row++;

        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gc, int row, String label, JTextField field) {
        gc.gridy = row; gc.gridx = 0;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1;
        panel.add(field, gc);
    }

    // ------------------------------------------------------------------
    //  Tab 3: Merge & progress
    // ------------------------------------------------------------------

    private JPanel createMergePanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        var topPanel = new JPanel();
        var mergeBtn = new JButton("Start Merge");
        mergeBtn.setFont(mergeBtn.getFont().deriveFont(Font.BOLD, 16f));
        topPanel.add(mergeBtn);
        var openOutputBtn = new JButton("Open Output Folder");
        openOutputBtn.addActionListener(e -> {
            try {
                String path = savePathField.getText().trim();
                String dir = path.substring(0, Math.max(path.lastIndexOf(File.separatorChar), 0));
                if (dir.isEmpty()) dir = ".";
                Desktop.getDesktop().open(new File(dir));
            } catch (Exception ex) {
                log("Cannot open folder: " + ex.getMessage());
            }
        });
        topPanel.add(openOutputBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        progressBar.setStringPainted(true);
        progressBar.setString("Ready to merge");
        panel.add(progressBar, BorderLayout.CENTER);

        // Log area
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logScroll.setPreferredSize(new Dimension(600, 200));
        panel.add(logScroll, BorderLayout.SOUTH);

        mergeBtn.addActionListener(e -> {
            // Apply settings
            merger.programName = programNameField.getText().trim();
            merger.displayName = displayNameField.getText().trim();
            merger.vendorName = vendorField.getText().trim();
            merger.iconPath = iconField.getText().trim();
            merger.savePath = savePathField.getText().trim();
            int copies = 1;
            try {
                copies = Integer.parseInt(copyCountField.getText().trim());
            } catch (NumberFormatException ex) {
                copies = 1;
            }
            if (copies < 1) copies = 1;
            merger.copyCount = copies;
            merger.setProgressListener(new JarMerger.ProgressListener() {
                @Override
                public void onProgress(int current, int total, String message) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setMaximum(total);
                        progressBar.setValue(current);
                        progressBar.setString(message);
                        log(message);
                    });
                }

                @Override
                public void onComplete(boolean success, String message) {
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            progressBar.setString("Done!");
                            progressBar.setValue(progressBar.getMaximum());
                        } else {
                            progressBar.setString("Failed!");
                        }
                        log(message);
                        log("-----");
                    });
                }
            });
            log("Starting merge: " + merger.copyCount + " copy(ies) of the selected JAR...");
            merger.mergeAsync();
        });

        return panel;
    }

    // ------------------------------------------------------------------
    //  Logging
    // ------------------------------------------------------------------

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ------------------------------------------------------------------
    //  Main entry
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf, using default L&F");
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}