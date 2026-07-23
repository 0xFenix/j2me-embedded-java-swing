package mzmod.ui;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import mzmod.merger.JarMerger;

/**
 * Main application window for MZMOD Advance Menu (Swing edition). Dark mode with rounded corners.
 */
public class MainFrame extends JFrame {

  private final JarMerger merger = new JarMerger();

  // ── UI Components ───────────────────────────────────────────────
  private final DefaultListModel<File> jarListModel = new DefaultListModel<>();
  private final JList<File> jarList = new JList<>(jarListModel);
  private final JTextField programNameField =
      createFilledField(UserSettings.get("programName", "Merged App"));
  private final JTextField displayNameField =
      createFilledField(UserSettings.get("displayName", "Merged App"));
  private final JTextField vendorField = createFilledField(UserSettings.get("vendor", "MZMOD"));
  private final JTextField iconField = UIHelpers.styledField("Path to icon.png (optional)");
  private final JTextField savePathField =
      createFilledField(UserSettings.get("savePath", "Embedded.jar"));
  private final JTextField copyCountField = createFilledField(UserSettings.get("copyCount", "1"));
  private final JProgressBar progressBar = UIHelpers.styledProgressBar();
  private final JLabel statusLabel = UIHelpers.label("Ready");
  private final JTextArea logArea = new JTextArea();
  private final JScrollPane logScroll = new JScrollPane(logArea);
  private final JLabel fileCountLabel = UIHelpers.helperText("0 file selected");

  private Action selectJarAction;
  private Action mergeAction;
  private Action openOutputAction;

  public MainFrame() {
    super("MZMOD Advance Menu v4.0");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(760, 640);
    setMinimumSize(new Dimension(640, 520));
    setLocationRelativeTo(null);
    applyFlatLafCustomizations();
    initComponents();
    setupKeyboardShortcuts();
  }

  private static JTextField createFilledField(String text) {
    JTextField field = UIHelpers.styledField("");
    field.setText(text);
    return field;
  }

  // ── FlatLaf Properties (Dark Mode + Radius) ─────────────────────
  private void applyFlatLafCustomizations() {
    // Global radius
    UIManager.put("Button.arc", Theme.RADIUS_MD);
    UIManager.put("Component.arc", Theme.RADIUS_MD);
    UIManager.put("TextComponent.arc", Theme.RADIUS_MD);
    UIManager.put("Component.focusWidth", 1);
    UIManager.put("Component.focusColor", Theme.BORDER_FOCUS);
    UIManager.put("Component.focusedBorderColor", Theme.BORDER_FOCUS);

    // Tabs
    UIManager.put("TabbedPane.tabHeight", Theme.TAB_HEIGHT);
    UIManager.put("TabbedPane.tabType", "underline");
    UIManager.put("TabbedPane.underlineColor", Theme.TAB_ACTIVE);
    UIManager.put("TabbedPane.hoverColor", Theme.TAB_HOVER);
    UIManager.put("TabbedPane.background", Theme.BACKGROUND);
    UIManager.put("TabbedPane.contentAreaColor", Theme.BACKGROUND);

    // Progress bar
    UIManager.put("ProgressBar.foreground", Theme.ACCENT);
    UIManager.put("ProgressBar.background", Theme.SURFACE_ALT);
    UIManager.put("ProgressBar.selectionForeground", Theme.ON_PRIMARY);
    UIManager.put("ProgressBar.style", "round");

    // List
    UIManager.put("List.background", Theme.LIST_BG);
    UIManager.put("List.selectionBackground", Theme.LIST_SEL_BG);
    UIManager.put("List.selectionForeground", Theme.LIST_SEL_FG);
    UIManager.put("List.border", Theme.RADIUS_MD);

    // Scroll
    UIManager.put("ScrollBar.track", Theme.SURFACE_ALT);
    UIManager.put("ScrollBar.thumb", Theme.BORDER);
    UIManager.put("ScrollBar.width", 10);

    // Text fields
    UIManager.put("TextComponent.placeholderForeground", Theme.TEXT_MUTED);

    // Buttons
    UIManager.put("Button.background", Theme.SURFACE_ALT);
    UIManager.put("Button.foreground", Theme.TEXT_PRIMARY);
    UIManager.put("Button.hoverBackground", Theme.TAB_HOVER);
    UIManager.put("Button.defaultBackground", Theme.ACCENT);
    UIManager.put("Button.defaultForeground", Theme.ON_PRIMARY);

    // Panels
    UIManager.put("Panel.background", Theme.BACKGROUND);
  }

  // ── Main Init ───────────────────────────────────────────────────
  private void initComponents() {
    JTabbedPane tabs = UIHelpers.styledTabs();
    tabs.addTab("  JAR Files  ", createJarPanel());
    tabs.addTab("  Settings  ", createSettingsPanel());
    tabs.addTab("  Merge  ", createMergePanel());
    setContentPane(tabs);
  }

  // ── Keyboard Shortcuts ──────────────────────────────────────────
  private void setupKeyboardShortcuts() {
    JRootPane root = getRootPane();
    InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = root.getActionMap();

    selectJarAction =
        new AbstractAction("selectJar") {
          @Override
          public void actionPerformed(ActionEvent e) {
            selectJar();
          }
        };
    mergeAction =
        new AbstractAction("merge") {
          @Override
          public void actionPerformed(ActionEvent e) {
            startMerge();
          }
        };
    openOutputAction =
        new AbstractAction("openOutput") {
          @Override
          public void actionPerformed(ActionEvent e) {
            openOutputFolder();
          }
        };

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "selectJar");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "merge");
    im.put(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
        "openOutput");

    am.put("selectJar", selectJarAction);
    am.put("merge", mergeAction);
    am.put("openOutput", openOutputAction);
  }

  // ═════════════════════════════════════════════════════════════════
  //  TAB 1: JAR File Selection
  // ═════════════════════════════════════════════════════════════════

  private JPanel createJarPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 0));
    panel.setBackground(Theme.BACKGROUND);
    panel.setBorder(
        new EmptyBorder(Theme.SPACING_MD, Theme.SPACING_MD, Theme.SPACING_MD, Theme.SPACING_MD));

    // ── Top: toolbar ──
    JPanel toolbar =
        new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACING_SM, Theme.SPACING_SM));
    toolbar.setBackground(Theme.SURFACE);
    toolbar.setBorder(
        new EmptyBorder(Theme.SPACING_XS, Theme.SPACING_SM, Theme.SPACING_XS, Theme.SPACING_SM));
    // Rounded corners via client property
    toolbar.putClientProperty("JPanel.style", "roundRect");

    JButton addBtn = UIHelpers.toolbarButton("Select JAR", null);
    addBtn.setToolTipText("Choose a JAR file to merge (Ctrl+O)");
    addBtn.addActionListener(e -> selectJar());

    JButton removeBtn = UIHelpers.destructiveButton("Remove");
    removeBtn.setToolTipText("Remove selected JAR from list");
    removeBtn.addActionListener(
        e -> {
          merger.clearSourceJar();
          jarListModel.clear();
          fileCountLabel.setText("0 file selected");
          statusLabel.setText("Cleared");
        });

    toolbar.add(addBtn);
    toolbar.add(Box.createHorizontalStrut(Theme.SPACING_SM));
    toolbar.add(removeBtn);
    toolbar.add(Box.createHorizontalGlue());
    toolbar.add(fileCountLabel);

    panel.add(toolbar, BorderLayout.NORTH);

    // ── Center: file list ──
    jarList.setFont(Theme.FONT_REGULAR);
    jarList.setCellRenderer(new FileListRenderer());
    jarList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jarList.setFixedCellHeight(38);
    jarList.setBackground(Theme.LIST_BG);

    JScrollPane listScroll = UIHelpers.styledScrollPane(jarList);
    listScroll.setPreferredSize(new Dimension(0, 200));

    // Empty state
    JPanel emptyPanel = new JPanel(new GridBagLayout());
    emptyPanel.setBackground(Theme.LIST_BG);
    JLabel emptyLabel =
        UIHelpers.helperText("No JAR file selected. Click \"Select JAR\" to begin.");
    emptyPanel.add(emptyLabel);
    jarList.putClientProperty("JList.emptyView", emptyPanel);

    panel.add(listScroll, BorderLayout.CENTER);

    // ── Bottom: copy count ──
    JPanel countPanel = new JPanel(new BorderLayout(Theme.SPACING_MD, 0));
    countPanel.setBackground(Theme.SURFACE);
    countPanel.setBorder(
        new EmptyBorder(Theme.SPACING_MD, Theme.SPACING_LG, Theme.SPACING_MD, Theme.SPACING_LG));
    countPanel.putClientProperty("JPanel.style", "roundRect");

    JLabel countLabel = UIHelpers.label("Number of copies:");
    countLabel.setToolTipText("How many copies of the JAR to merge (1 = single copy)");
    copyCountField.setPreferredSize(new Dimension(80, Theme.INPUT_HEIGHT));
    copyCountField.setHorizontalAlignment(JTextField.CENTER);
    copyCountField.setToolTipText("Enter a number between 1 and 99");

    countPanel.add(countLabel, BorderLayout.WEST);
    countPanel.add(copyCountField, BorderLayout.EAST);
    panel.add(countPanel, BorderLayout.SOUTH);

    return panel;
  }

  private void selectJar() {
    JFileChooser chooser = new JFileChooser();
    chooser.setMultiSelectionEnabled(false);
    chooser.setFileFilter(new FileNameExtensionFilter("JAR files (*.jar)", "jar"));
    chooser.setDialogTitle("Select JAR File");
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      merger.setSourceJar(f);
      jarListModel.clear();
      jarListModel.addElement(f);
      fileCountLabel.setText("1 file selected");
      statusLabel.setText("JAR loaded: " + f.getName());
    }
  }

  // ═════════════════════════════════════════════════════════════════
  //  TAB 2: Settings
  // ═════════════════════════════════════════════════════════════════

  private JPanel createSettingsPanel() {
    JPanel outer = new JPanel(new BorderLayout());
    outer.setBackground(Theme.BACKGROUND);
    outer.setBorder(
        new EmptyBorder(Theme.SPACING_XL, Theme.SPACING_XL, Theme.SPACING_XL, Theme.SPACING_XL));

    JPanel form = new JPanel();
    form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
    form.setBackground(Theme.BACKGROUND);

    // Section: General
    form.add(UIHelpers.sectionHeader("General"));
    form.add(createFormRow("Program Name:", programNameField, "Internal program identifier"));
    form.add(createFormRow("Display Name:", displayNameField, "Name shown to users"));
    form.add(createFormRow("Vendor:", vendorField, "Vendor/author name"));

    form.add(Box.createVerticalStrut(Theme.SPACING_LG));

    // Section: Output
    form.add(UIHelpers.sectionHeader("Output"));
    form.add(createIconRow());
    form.add(createSavePathRow());

    form.add(Box.createVerticalGlue());

    JScrollPane scroll = new JScrollPane(form);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    outer.add(scroll, BorderLayout.CENTER);

    return outer;
  }

  private JPanel createFormRow(String labelText, JTextField field, String tooltip) {
    JPanel row = new JPanel(new BorderLayout(Theme.SPACING_MD, 0));
    row.setBackground(Theme.BACKGROUND);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
    row.setBorder(new EmptyBorder(Theme.SPACING_XS, 0, Theme.SPACING_SM, 0));

    JLabel lbl = UIHelpers.label(labelText);
    lbl.setToolTipText(tooltip);
    lbl.setPreferredSize(new Dimension(120, Theme.INPUT_HEIGHT));
    lbl.setVerticalAlignment(SwingConstants.CENTER);
    field.setToolTipText(tooltip);

    row.add(lbl, BorderLayout.WEST);
    row.add(field, BorderLayout.CENTER);
    return row;
  }

  private JPanel createIconRow() {
    JPanel row = new JPanel(new BorderLayout(Theme.SPACING_MD, 0));
    row.setBackground(Theme.BACKGROUND);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
    row.setBorder(new EmptyBorder(Theme.SPACING_XS, 0, Theme.SPACING_SM, 0));

    JLabel lbl = UIHelpers.label("Icon:");
    lbl.setToolTipText("PNG icon for the merged JAR (optional)");
    lbl.setPreferredSize(new Dimension(120, Theme.INPUT_HEIGHT));
    lbl.setVerticalAlignment(SwingConstants.CENTER);

    JButton browseBtn = UIHelpers.toolbarButton("...", null);
    browseBtn.setPreferredSize(new Dimension(44, Theme.INPUT_HEIGHT));
    browseBtn.setToolTipText("Browse for icon file");
    browseBtn.addActionListener(
        e -> {
          JFileChooser chooser = new JFileChooser();
          chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
          chooser.setDialogTitle("Select Icon");
          if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            iconField.setText(chooser.getSelectedFile().getAbsolutePath());
          }
        });

    JPanel fieldPanel = new JPanel(new BorderLayout(4, 0));
    fieldPanel.setBackground(Theme.BACKGROUND);
    fieldPanel.add(iconField, BorderLayout.CENTER);
    fieldPanel.add(browseBtn, BorderLayout.EAST);

    row.add(lbl, BorderLayout.WEST);
    row.add(fieldPanel, BorderLayout.CENTER);
    return row;
  }

  private JPanel createSavePathRow() {
    JPanel row = new JPanel(new BorderLayout(Theme.SPACING_MD, 0));
    row.setBackground(Theme.BACKGROUND);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
    row.setBorder(new EmptyBorder(Theme.SPACING_XS, 0, Theme.SPACING_SM, 0));

    JLabel lbl = UIHelpers.label("Save Path:");
    lbl.setToolTipText("Output JAR file path");
    lbl.setPreferredSize(new Dimension(120, Theme.INPUT_HEIGHT));
    lbl.setVerticalAlignment(SwingConstants.CENTER);

    JButton browseBtn = UIHelpers.toolbarButton("Browse...", null);
    browseBtn.setPreferredSize(new Dimension(100, Theme.INPUT_HEIGHT));
    browseBtn.setToolTipText("Choose output file location");
    browseBtn.addActionListener(
        e -> {
          JFileChooser chooser = new JFileChooser();
          chooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
          chooser.setDialogTitle("Save As");
          if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            savePathField.setText(chooser.getSelectedFile().getAbsolutePath());
          }
        });

    JPanel fieldPanel = new JPanel(new BorderLayout(4, 0));
    fieldPanel.setBackground(Theme.BACKGROUND);
    fieldPanel.add(savePathField, BorderLayout.CENTER);
    fieldPanel.add(browseBtn, BorderLayout.EAST);

    row.add(lbl, BorderLayout.WEST);
    row.add(fieldPanel, BorderLayout.CENTER);
    return row;
  }

  // ═════════════════════════════════════════════════════════════════
  //  TAB 3: Merge & Progress
  // ═════════════════════════════════════════════════════════════════

  private JPanel createMergePanel() {
    JPanel panel = new JPanel(new BorderLayout(0, Theme.SPACING_MD));
    panel.setBackground(Theme.BACKGROUND);
    panel.setBorder(
        new EmptyBorder(Theme.SPACING_XL, Theme.SPACING_XL, Theme.SPACING_XL, Theme.SPACING_XL));

    // ── Top: action buttons ──
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SPACING_LG, 0));
    topPanel.setBackground(Theme.BACKGROUND);

    JButton mergeBtn = UIHelpers.primaryButton("Start Merge", null);
    mergeBtn.setToolTipText("Start merging (F5)");
    mergeBtn.setPreferredSize(new Dimension(220, 46));
    mergeBtn.setFont(Theme.FONT_LARGE);
    mergeBtn.addActionListener(e -> startMerge());

    JButton openOutputBtn = UIHelpers.secondaryButton("Open Output Folder", null);
    openOutputBtn.setToolTipText("Open the folder containing merged file (Ctrl+Shift+O)");
    openOutputBtn.addActionListener(e -> openOutputFolder());

    topPanel.add(mergeBtn);
    topPanel.add(openOutputBtn);
    panel.add(topPanel, BorderLayout.NORTH);

    // ── Center: progress + log ──
    JPanel centerPanel = new JPanel(new BorderLayout(0, Theme.SPACING_MD));
    centerPanel.setBackground(Theme.BACKGROUND);

    // Status + progress
    JPanel progressPanel = new JPanel(new BorderLayout(0, Theme.SPACING_XS));
    progressPanel.setBackground(Theme.BACKGROUND);

    progressBar.setString("Ready to merge");
    progressBar.setStringPainted(true);
    progressPanel.add(progressBar, BorderLayout.CENTER);

    JPanel statusRow = new JPanel(new BorderLayout());
    statusRow.setBackground(Theme.BACKGROUND);
    statusLabel.setFont(Theme.FONT_SMALL);
    statusLabel.setForeground(Theme.TEXT_SECONDARY);
    statusRow.add(statusLabel, BorderLayout.WEST);
    progressPanel.add(statusRow, BorderLayout.SOUTH);

    centerPanel.add(progressPanel, BorderLayout.NORTH);

    // ── Log area ──
    JPanel logPanel = new JPanel(new BorderLayout());
    logPanel.setBackground(Theme.BACKGROUND);

    logArea.setEditable(false);
    logArea.setFont(Theme.FONT_MONO);
    logArea.setBackground(Theme.LOG_BG);
    logArea.setForeground(Theme.LOG_FG);
    logArea.setCaretColor(Theme.LOG_FG);
    logArea.setMargin(new Insets(10, 10, 10, 10));

    JScrollPane logScrollLocal = new JScrollPane(logArea);
    logScrollLocal.setBorder(new EmptyBorder(0, 0, 0, 0));
    logScrollLocal.getViewport().setBackground(Theme.LOG_BG);
    logScrollLocal.setPreferredSize(new Dimension(0, 220));
    logPanel.add(logScrollLocal, BorderLayout.CENTER);

    // Clear log button
    JPanel logToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    logToolbar.setBackground(Theme.BACKGROUND);
    JButton clearLogBtn = UIHelpers.toolbarButton("Clear Log", null);
    clearLogBtn.setToolTipText("Clear the log output");
    clearLogBtn.addActionListener(e -> logArea.setText(""));
    logToolbar.add(clearLogBtn);
    logPanel.add(logToolbar, BorderLayout.SOUTH);

    centerPanel.add(logPanel, BorderLayout.CENTER);
    panel.add(centerPanel, BorderLayout.CENTER);

    return panel;
  }

  private void startMerge() {
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

    UserSettings.save(
        merger.programName,
        merger.displayName,
        merger.vendorName,
        merger.iconPath,
        merger.savePath,
        String.valueOf(copies));

    progressBar.setForeground(Theme.INFO);
    statusLabel.setText("Merging...");
    statusLabel.setForeground(Theme.INFO);

    merger.setProgressListener(
        new JarMerger.ProgressListener() {
          @Override
          public void onProgress(int current, int total, String message) {
            SwingUtilities.invokeLater(
                () -> {
                  progressBar.setMaximum(total);
                  progressBar.setValue(current);
                  progressBar.setString(message);
                  log(message);
                });
          }

          @Override
          public void onComplete(boolean success, String message) {
            SwingUtilities.invokeLater(
                () -> {
                  if (success) {
                    progressBar.setForeground(Theme.SUCCESS);
                    progressBar.setString("Done!");
                    progressBar.setValue(progressBar.getMaximum());
                    statusLabel.setText("Merge completed successfully");
                    statusLabel.setForeground(Theme.SUCCESS);
                  } else {
                    progressBar.setForeground(Theme.ERROR);
                    progressBar.setString("Failed!");
                    statusLabel.setText("Merge failed");
                    statusLabel.setForeground(Theme.ERROR);
                  }
                  log(message);
                  log("-----");
                });
          }
        });

    log("Starting merge: " + merger.copyCount + " copy(ies) of the selected JAR...");
    merger.mergeAsync();
  }

  private void openOutputFolder() {
    try {
      String path = savePathField.getText().trim();
      String dir = path.substring(0, Math.max(path.lastIndexOf(File.separatorChar), 0));
      if (dir.isEmpty()) dir = ".";
      Desktop.getDesktop().open(new File(dir));
    } catch (Exception ex) {
      log("Cannot open folder: " + ex.getMessage());
      statusLabel.setText("Error: " + ex.getMessage());
      statusLabel.setForeground(Theme.ERROR);
    }
  }

  // ── Logging ─────────────────────────────────────────────────────
  private void log(String msg) {
    logArea.append(msg + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  // ═════════════════════════════════════════════════════════════════
  //  Custom Cell Renderer for File List
  // ═════════════════════════════════════════════════════════════════

  private static class FileListRenderer extends DefaultListCellRenderer {
    private static final int PAD = 10;

    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value instanceof File) {
        File f = (File) value;
        setText("  " + f.getName());
        setToolTipText(f.getAbsolutePath());
        setIcon(UIManager.getIcon("FileView.fileIcon"));
      }

      setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

      if (isSelected) {
        setBackground(Theme.LIST_SEL_BG);
        setForeground(Theme.LIST_SEL_FG);
      } else {
        setBackground(Theme.LIST_BG);
        setForeground(Theme.TEXT_PRIMARY);
      }
      return this;
    }
  }

  // ═════════════════════════════════════════════════════════════════
  //  Main Entry
  // ═════════════════════════════════════════════════════════════════

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(new FlatMacDarkLaf());
    } catch (Exception e) {
      System.err.println("Failed to set FlatLaf, using default L&F");
    }

    SwingUtilities.invokeLater(
        () -> {
          MainFrame frame = new MainFrame();
          frame.setVisible(true);
        });
  }
}
