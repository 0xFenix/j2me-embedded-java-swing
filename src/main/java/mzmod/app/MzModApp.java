package mzmod.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;
import mzmod.bytecode.ClassModifier;
import mzmod.bytecode.StaticClassGenerator;
import mzmod.compress.Inflater;
import mzmod.ui.CanvasList;
import mzmod.zip.BufferedOutput;
import mzmod.zip.ByteArrayReader;
import mzmod.zip.ZipEntryInfo;
import mzmod.zip.ZipWriter;

public class MzModApp extends MIDlet implements Runnable, CommandListener {
  private static Alert statusAlert;
  private Choice fileListChoice;
  private Displayable fileListScreen;
  private Image folderIcon;
  private Image fileIcon;
  private static MIDlet instance;
  private ByteArrayReader zipReader;
  private final Inflater inflater = new Inflater();
  private static Display display;
  private final Form progressForm = new Form("");
  private final Form settingsForm = new Form("Add and Set");
  private final Form addToSetForm = new Form("Add to Set");
  private Choice listChoice = new CanvasList("List", 2);
  private Displayable listScreen;
  private Choice settingsChoice = new CanvasList("Set", 2);
  private Displayable settingsScreen;
  private TextField iconField;
  private TextField programNameField;
  private TextField displayNameField;
  private TextField vendorNameField;
  private TextField savePathField;
  private static final ClassModifier classModifier;
  private static final ByteArrayOutputStream classOutputBuffer;
  private static final DataOutputStream classOutputWriter;
  private static final Hashtable jarCache;
  private static final Hashtable failedJars;
  private static final Vector mergedEntries;
  private static String currentDirectory;
  private static String selectedPath;
  private int mergeIndex;
  private int operationType;
  private String[] rootPaths;
  private static final char[] BASE64_CHARS;
  private static final byte[] BASE64_DECODE_TABLE;
  private static Hashtable readConnections;
  private static Hashtable writeConnections;
  private boolean enableScreenshots;
  private boolean enableList;
  private boolean enableListMainPage;
  private boolean addBackground;
  private boolean useList;
  private JarInfo currentJarInfo;

  private static Throwable ensureDirectoryExists(String var0) {
    try {
      FileConnection var1;
      if (!(var1 = (FileConnection) Connector.open(var0, 1)).isDirectory()) {
        var1.close();
        var1 = (FileConnection) Connector.open(var0, 2);

        try {
          var1.mkdir();
          var1.close();
          return null;
        } catch (Throwable var3) {
          if (var0.endsWith("/")) {
            var0 = var0.substring(0, var0.length() - 1);
          }

          ensureDirectoryExists(var0.substring(0, var0.lastIndexOf(47) + 1));
          var1.mkdir();
        }
      }

      try {
        var1.close();
      } catch (Throwable var2) {
      }

      return null;
    } catch (Throwable var4) {
      return var4;
    }
  }

  private void handleFileSelect() {
    String var1;
    if ((var1 = this.fileListChoice.getString(this.fileListChoice.getSelectedIndex()))
        .endsWith("/")) {
      selectedPath = currentDirectory.concat(var1);
      this.operationType = 0;
      (new Thread(this)).start();
    } else {
      String var2;
      if ((var2 = var1.toLowerCase()).endsWith(".zip")
          || var2.endsWith(".jar")
          || var2.endsWith("_jar")
          || var2.endsWith("_zip")) {
        var1 = currentDirectory.concat(var1);
        if (failedJars.containsKey(var1)) {
          return;
        }

        if (jarCache.containsKey(var1)) {
          this.showJarDetails((JarInfo) jarCache.get(var1));
          return;
        }

        selectedPath = var1;
        this.operationType = 1;
        (new Thread(this)).start();
      }
    }
  }

  public final void commandAction(Command var1, Displayable var2) {
    int var3 = var1.getCommandType();
    String var6;
    if (var2 == this.fileListScreen) {
      if (var1 == List.SELECT_COMMAND) {
        this.handleFileSelect();
      } else {
        String var11;
        switch (var3) {
          case 1:
            if (!currentDirectory.equals("file:///")) {
              for (var3 = this.fileListChoice.size() - 1;
                  var3 > 0 && this.fileListChoice.getImage(var3) == this.fileIcon;
                  --var3) {
                var11 = currentDirectory + this.fileListChoice.getString(var3);
                if (!jarCache.containsKey(var11)) {
                  this.listChoice.append(var11, (Image) null);
                }
              }

              return;
            }
            break;
          case 2:
            this.browseDirectory(currentDirectory.concat("../"));
            return;
          case 3:
            display.setCurrent(this.settingsScreen);
            break;
          case 4:
            this.handleFileSelect();
            return;
          case 5:
            display.setCurrent(this.progressForm);
            return;
          case 6:
            display.setCurrent(this.listScreen);
            return;
          case 7:
            this.fileListChoice.deleteAll();
            this.notifyDestroyed();
            return;
          case 8:
            if ((var6 =
                        (var11 =
                                this.fileListChoice.getString(
                                    this.fileListChoice.getSelectedIndex()))
                            .toLowerCase())
                    .endsWith(".zip")
                || var6.endsWith(".jar")
                || var6.endsWith("_jar")
                || var6.endsWith("_zip")) {
              this.listChoice.setSelectedIndex(
                  this.listChoice.append(currentDirectory.concat(var11), (Image) null), true);
              return;
            }
        }
      }
    } else if (var2 == this.progressForm) {
      display.setCurrent(this.fileListScreen);
    } else {
      boolean var5;
      if (var2 == this.listChoice) {
        int var10;
        switch (var3) {
          case 1:
            var5 = false;

            for (var10 = this.listChoice.size() - 1; var10 >= 0; --var10) {
              if (this.listChoice.isSelected(var10)) {
                var5 = true;
                break;
              }
            }

            if (var5) {
              display.setCurrent(this.settingsForm);
              return;
            }
            break;
          case 2:
            display.setCurrent(this.fileListScreen);
            return;
          case 3:
            for (var10 = this.listChoice.size() - 1; var10 >= 0; --var10) {
              if (this.listChoice.isSelected(var10)) {
                this.listChoice.delete(var10);
              }
            }

            return;
          case 4:
          case 5:
          case 6:
          case 7:
          default:
            break;
          case 8:
            for (var10 = this.listChoice.size() - 1; var10 >= 0; --var10) {
              this.listChoice.setSelectedIndex(var10, true);
            }
        }

      } else if (var2 == this.settingsForm) {
        if (2 == var3) {
          display.setCurrent(this.listScreen);
        } else {
          this.saveSettings();
          this.operationType = 2;
          (new Thread(this)).start();
        }
      } else if (var2 != this.addToSetForm) {
        if (var2 == this.settingsChoice) {
          if (4 == var3) {
            this.enableScreenshots = this.settingsChoice.isSelected(0);
            this.enableList = this.settingsChoice.isSelected(1);
            this.enableListMainPage = this.settingsChoice.isSelected(2);
            this.addBackground = this.settingsChoice.isSelected(3);
            this.useList = this.settingsChoice.isSelected(4);
            if (this.settingsChoice.isSelected(5)) {
              try {
                RecordStore var7 = RecordStore.openRecordStore("color_setting", true);
                ByteArrayOutputStream var8;
                DataOutputStream var12 = new DataOutputStream(var8 = new ByteArrayOutputStream());
                if (var7.getNumRecords() > 0) {
                  var12.writeInt(CanvasList.bgColor);
                  var12.writeInt(CanvasList.textColor);
                  var12.writeInt(CanvasList.titleGradientEnd);
                  var12.writeInt(CanvasList.titleGradientStart);
                  var12.writeInt(CanvasList.menuTextColor);
                  var12.writeInt(CanvasList.selectedGradientStart);
                }

                var12.close();
                byte[] var9 = var8.toByteArray();
                if (var7.getNumRecords() > 0) {
                  var7.setRecord(1, var9, 0, var9.length);
                } else {
                  var7.addRecord(var9, 0, var9.length);
                }

                var7.closeRecordStore();
              } catch (Exception var4) {
              }
            }

            this.saveSettings();
          }

          display.setCurrent(this.fileListScreen);
        }

      } else {
        if (4 == var3) {
          var5 = true;
          var6 = this.currentJarInfo.jarPath;

          for (var3 = 0; var3 < this.listChoice.size(); ++var3) {
            if (var6.equals(this.listChoice.getString(var3))) {
              var5 = false;
              break;
            }
          }

          if (var5) {
            this.listChoice.setSelectedIndex(this.listChoice.append(var6, (Image) null), true);
          }
        }

        this.updateJarDisplayNames(this.currentJarInfo);
        display.setCurrent(this.fileListScreen);
      }
    }
  }

  private void mergeJars(ZipWriter var1, OutputStream var2, String[] var3) {
    this.mergeIndex = 0;

    for (int var4 = 0; var4 < var3.length; ++var4) {
      String var5 = var3[var4];
      JarInfo var6;
      if ((var6 = (JarInfo) jarCache.get(var5)) == null) {
        this.zipReader = null;

        try {
          this.zipReader = new ByteArrayReader(readFile(var5));
          Vector var7 = this.readZipEntries(this.zipReader);
          (var6 = new JarInfo(var5, var7))
              .parseManifest(this.readZipEntryData((ZipEntryInfo) var7.elementAt(0)));
        } catch (Exception var9) {
          var9.printStackTrace();
          this.progressForm.append(var5 + "\nRead Failure");
          continue;
        }
      } else {
        this.zipReader = new ByteArrayReader(readFile(var5));
      }

      long var10 = System.currentTimeMillis();
      this.writeJarEntry(var6, var1, var2);
      this.progressForm.append(
          var5 + "\n" + (System.currentTimeMillis() - var10) + loadString('k') + "\n");
    }
  }

  private void writeJarEntry(JarInfo var1, ZipWriter var2, OutputStream var3) {
    int var4 = var1.getMergeCount();
    String var5 = var1.buildManifestEntries(mergedEntries, this.mergeIndex);

    try {
      var2.putNextEntry(var1.tempIniName);
      var2.write(var5.getBytes("utf-8"));
      var2.closeEntry();
    } catch (Exception var19) {
    }

    Vector var20;
    int var6 = (var20 = var1.entries).size() - 1;
    Vector var7 = new Vector(var6);

    int var8;
    ZipEntryInfo var9;
    for (var8 = 1; var8 <= var6; ++var8) {
      if ((var9 = (ZipEntryInfo) var20.elementAt(var8)).toString().endsWith(".class")) {
        var7.addElement(var9);
      }
    }

    var20 = var7;
    var6 = var7.size();
    StringItem var21 = new StringItem("", "");

    Gauge var10;
    int var11;
    int var24;
    for (var8 = 0; var8 < var4; ++var8) {
      String var22 = this.mergeIndex == 0 ? "" : (char) (97 + this.mergeIndex) + "/";
      ++this.mergeIndex;
      this.progressForm.append(
          var10 = new Gauge(this.mergeIndex + ": " + var1.jarPath, false, var6, 0));
      var11 = this.progressForm.append(var21);
      display.setCurrent(this.progressForm);
      Hashtable var12 = ClassModifier.initLibraryMappings();

      for (int var13 = 0; var13 < var6; ++var13) {
        String var14 =
            (var14 = ((ZipEntryInfo) var20.elementAt(var13)).toString())
                .substring(0, var14.length() - 6);
        var12.put(var14, var22 + var14);
      }

      var24 = 0;

      String var15;
      while (var24 < var6) {
        ZipEntryInfo var25;
        var15 = (var25 = (ZipEntryInfo) var20.elementAt(var24++)).toString();
        var21.setText(var15);
        var10.setValue(var24);

        try {
          byte[] var27;
          if ((var27 = this.readZipEntryData(var25)) != null) {
            var2.putNextEntry(var22 + var15);
            var2.write(processClassFile(var27, var22.length() > 0 ? var22.charAt(0) : 'a'));
            var2.closeEntry();
          } else {
            this.progressForm.append(var15 + "Extract failed\n");
          }
        } catch (Exception var18) {
        }
      }

      var15 = var22.length() > 0 ? var22.charAt(0) + "/Static" : "Static";

      try {
        byte[] var23;
        if ((var23 = StaticClassGenerator.generate(ClassModifier.MODIFIED_CLASSES, var15))
            != null) {
          var2.putNextEntry(var15 + ".class");
          var2.write(var23);
          var2.closeEntry();
        }
      } catch (Exception var17) {
      }

      this.progressForm.delete(var11);
    }

    if ((var6 = (var20 = var1.getResourceEntries()).size()) > 0) {
      var8 = 0;
      this.progressForm.append(var10 = new Gauge(var1.jarPath + "Resources", false, var6, 0));
      var11 = this.progressForm.append(var21);
      display.setCurrent(this.progressForm);

      while (var8 < var6) {
        var24 = (var9 = (ZipEntryInfo) var20.elementAt(var8++)).localHeaderOffset;
        String var26 = var9.toString();
        var10.setValue(var8);
        var21.setText(var26);

        try {
          var2.putNextEntry(var9);
          var2.closeCurrentEntry();
          this.zipReader.seek(var24 + 26);
          this.zipReader.skip((long) (this.zipReader.readShort() + this.zipReader.readShort()));
          var3.write(this.zipReader.buffer, this.zipReader.getPosition(), var9.compressedSize);
          var3.flush();
          if (var26.equals(var1.iconPath)) {
            ZipEntryInfo var28;
            (var28 = new ZipEntryInfo(var1.tempIconName)).compressionMethod =
                var9.compressionMethod;
            var28.versionNeeded = var9.versionNeeded;
            var28.compressedSize = var9.compressedSize;
            var28.uncompressedSize = var9.uncompressedSize;
            var28.crc32 = var9.crc32;
            var28.lastModifiedTime = var9.lastModifiedTime;
            var2.putNextEntry(var28);
            var2.closeCurrentEntry();
            this.zipReader.seek(var24 + 26);
            this.zipReader.skip((long) (this.zipReader.readShort() + this.zipReader.readShort()));
            var3.write(this.zipReader.buffer, this.zipReader.getPosition(), var9.compressedSize);
            var3.flush();
          }
        } catch (Exception var16) {
        }
      }

      this.progressForm.delete(var11);
    }

    this.zipReader.close();
    this.zipReader = null;
  }

  private void createStandardUI() {
    this.fileListScreen = new List(loadString('c'), 3);
    this.fileListChoice = (Choice) this.fileListScreen;
    this.listScreen = new List("List", 2);
    this.listChoice = (Choice) this.listScreen;
    this.settingsScreen = new List("Set", 2);
    this.settingsChoice = (Choice) this.settingsScreen;
    this.initializeDisplay();
  }

  private void createCustomUI() {
    this.fileListScreen = new CanvasList(loadString('c'), 3);
    this.fileListChoice = (Choice) this.fileListScreen;
    this.listScreen = new CanvasList("List", 2);
    this.listChoice = (Choice) this.listScreen;
    this.settingsScreen = new CanvasList("Set", 2);
    this.settingsChoice = (Choice) this.settingsScreen;
    this.initializeDisplay();
  }

  public MzModApp() {
    instance = this;
    this.folderIcon = loadIcon('a');
    this.fileIcon = loadIcon('b');
    this.iconField = new TextField("Icon:", (String) null, 200, 0);
    this.programNameField = new TextField("Program Name:", (String) null, 200, 0);
    this.displayNameField = new TextField("Display Name:", (String) null, 200, 0);
    this.vendorNameField = new TextField("Vendor Name:", (String) null, 200, 0);
    this.savePathField = new TextField("Save Path:", (String) null, 200, 0);
    this.settingsForm.append(this.iconField);
    this.settingsForm.append(this.programNameField);
    this.settingsForm.append(this.displayNameField);
    this.settingsForm.append(this.vendorNameField);
    this.settingsForm.append(this.savePathField);
    this.settingsForm.addCommand(new Command(this.getAppProperty("start"), 1, 5));
    this.settingsForm.setCommandListener(this);
    Command var1 = new Command(loadString('e'), 2, 2);
    this.settingsForm.addCommand(var1);
    this.addToSetForm.addCommand(var1);
    this.progressForm.addCommand(var1);
    this.addToSetForm.append(new TextField("Quantity:", (String) null, 2, 2));

    for (int var3 = 1; var3 < 26; ++var3) {
      this.addToSetForm.append(new TextField("Display Name" + var3 + ":", (String) null, 20, 0));
    }

    this.addToSetForm.setCommandListener(this);
    this.addToSetForm.addCommand(new Command("Add", 4, 1));
    this.progressForm.setCommandListener(this);

    try {
      RecordStore.openRecordStore("List", false).closeRecordStore();
      this.useList = true;
      this.createCustomUI();
    } catch (Throwable var2) {
      this.useList = false;
      this.createStandardUI();
    }
  }

  private void initializeDisplay() {
    (display = Display.getDisplay(this)).setCurrent(this.fileListScreen);
    this.fileListScreen.setCommandListener(this);
    Command var1 = new Command(loadString('d'), 4, 1);
    this.fileListScreen.addCommand(var1);
    var1 = new Command(loadString('e'), 2, 2);
    this.fileListScreen.addCommand(var1);
    this.listScreen.addCommand(var1);
    this.settingsScreen.addCommand(var1);
    this.settingsScreen.addCommand(new Command("Save", 4, 1));
    this.settingsScreen.setCommandListener(this);
    this.settingsChoice.append("Enable Screenshots", (Image) null);
    this.settingsChoice.append("Enable List", (Image) null);
    this.settingsChoice.append("Enable List main page", (Image) null);
    this.settingsChoice.append("Add a background", (Image) null);
    this.settingsChoice.append("Use List", (Image) null);
    this.settingsChoice.append("Enable Color List", (Image) null);
    this.settingsChoice.setSelectedIndex(4, this.useList);
    this.listChoice.setFitPolicy(1);
    this.fileListScreen.addCommand(new Command(loadString('f'), 7, 3));
    this.fileListScreen.addCommand(new Command(loadString('o'), 5, 4));
    this.fileListScreen.addCommand(new Command(this.getAppProperty("add"), 8, 5));
    this.fileListScreen.addCommand(new Command("Add All", 1, 6));
    this.fileListScreen.addCommand(new Command("Set", 3, 7));
    var1 = new Command(this.getAppProperty("start"), 1, 5);
    this.listScreen.addCommand(var1);
    this.listScreen.addCommand(new Command(this.getAppProperty("delete"), 3, 5));
    this.listScreen.addCommand(new Command(this.getAppProperty("selectall"), 8, 5));
    this.listScreen.setCommandListener(this);
    String var2 = loadLastDirectory();
    this.browseDirectory(var2);
    this.loadSettings();
    this.fileListScreen.addCommand(new Command(this.getAppProperty("view"), 6, 5));
  }

  private void loadSettings() {
    RecordStore var1;
    DataInputStream var2;
    try {
      if ((var1 = RecordStore.openRecordStore("setting", true)).getNumRecords() <= 0) {
        this.iconField.setString(this.getAppProperty("icon"));
        this.programNameField.setString(this.getAppProperty("MIDlet-Name"));
        this.displayNameField.setString(this.getAppProperty("MIDlet-Name"));
        this.vendorNameField.setString(this.getAppProperty("MIDlet-Vendor"));
        this.savePathField.setString(this.getAppProperty("savedir"));
      } else {
        var2 = new DataInputStream(new ByteArrayInputStream(var1.getRecord(1)));
        this.iconField.setString(var2.readUTF());
        this.programNameField.setString(var2.readUTF());
        this.displayNameField.setString(var2.readUTF());
        this.vendorNameField.setString(var2.readUTF());
        this.savePathField.setString(var2.readUTF());
        this.settingsChoice.setSelectedIndex(0, this.enableScreenshots = var2.readBoolean());
        this.settingsChoice.setSelectedIndex(1, this.enableList = var2.readBoolean());
        this.settingsChoice.setSelectedIndex(2, this.enableListMainPage = var2.readBoolean());
        this.settingsChoice.setSelectedIndex(3, this.addBackground = var2.readBoolean());
        var2.close();
        int var3 =
            (var2 = new DataInputStream(new ByteArrayInputStream(var1.getRecord(2)))).readInt();
        int var4 = 0;

        while (true) {
          if (var4 >= var3) {
            var2.close();
            break;
          }

          this.listChoice.append(var2.readUTF(), (Image) null);
          ++var4;
        }
      }

      var1.closeRecordStore();
    } catch (Throwable var6) {
    }

    try {
      if ((var1 = RecordStore.openRecordStore("color_setting", false)).getNumRecords() > 0) {
        CanvasList.bgColor =
            (var2 = new DataInputStream(new ByteArrayInputStream(var1.getRecord(1)))).readInt();
        CanvasList.textColor = var2.readInt();
        CanvasList.titleGradientEnd = var2.readInt();
        CanvasList.titleGradientStart = var2.readInt();
        CanvasList.menuTextColor = var2.readInt();
        CanvasList.selectedGradientStart = var2.readInt();
        var2.close();
      }

      var1.closeRecordStore();
    } catch (Exception var5) {
      CanvasList.resetColors();
    }
  }

  private Vector readZipEntries(ByteArrayReader var1) {
    Vector var2;
    (var2 = new Vector()).addElement(this);

    int var4;
    int var5;
    for (; var1.readInt() == 1347092738; var1.skip((long) (var4 + var5))) {
      ZipEntryInfo var3 = new ZipEntryInfo((String) null);
      var1.skip(4L);
      var3.versionNeeded = var1.readShort();
      var3.compressionMethod = (short) var1.readShort();
      var3.lastModifiedTime = var1.readLittleEndianInt();
      var3.crc32 = var1.readLittleEndianInt();
      var3.compressedSize = var1.readLittleEndianInt();
      var3.uncompressedSize = var1.readLittleEndianInt();
      var3.nameLength = var1.readShort();
      var4 = var1.readShort();
      var5 = var1.readShort();
      var1.skip(8L);
      var3.localHeaderOffset = var1.readLittleEndianInt();
      byte[] var6 = new byte[var3.nameLength];
      var1.readBytes(var6, var3.nameLength);
      var3.setNameFromBytes(var6);
      if (!var3.name.endsWith("/")) {
        if (var3.toString().equals("META-INF/MANIFEST.MF")) {
          var2.setElementAt(var3, 0);
        } else {
          var2.addElement(var3);
        }
      }
    }

    return var2;
  }

  private boolean mergeSelectedJars(String[] var1) {
    String var2 = this.savePathField.getString();
    BufferedOutput var3 = null;

    try {
      int var4 = var2.lastIndexOf(47);
      String var5;
      FileConnection var6;
      if ((var6 = openReadConnection(var5 = var2.substring(0, var4 + 1))) == null) {
        this.progressForm.append("Invalid path" + var2);
        return false;
      } else {
        Throwable var7;
        if (!var6.isDirectory() && (var7 = ensureDirectoryExists(var5)) != null) {
          this.progressForm.append("Can not create folder\n" + var5 + "\n" + var7.toString());
          return false;
        } else {
          if (var2.toLowerCase().endsWith(".jar")) {
            var6 = openWriteConnection(var2 + "_jar");
          } else {
            var6 = openWriteConnection(var2);
          }

          try {
            var6.setWritable(true);
            var6.delete();
          } catch (Exception var11) {
          }

          try {
            var6.create();
          } catch (Exception var10) {
            this.progressForm.append("Can not create file\n" + var2);
            return false;
          }

          var3 = new BufferedOutput(var6.openOutputStream());
          ZipWriter var15 = new ZipWriter(var3);
          this.writeBaseEntries(var15, var3);
          mergedEntries.removeAllElements();
          this.mergeJars((ZipWriter) var15, (OutputStream) var3, (String[]) var1);
          var15.putNextEntry("META-INF/MANIFEST.MF");
          StringBuffer var13;
          (var13 =
                  new StringBuffer(
                      "Manifest-Version: 1.0\r\nMicroEdition-Configuration: CLDC-1.1\r\nMicroEdition-Profile: MIDP-2.0\r\nMIDlet-Version: 2.5\r\nMIDlet-Vendor: "))
              .append(this.vendorNameField.getString())
              .append("\r\nMIDlet-Name: ")
              .append(this.programNameField.getString())
              .append("\r\nMIDlet-1: ")
              .append(this.displayNameField.getString())
              .append(",/MZMOD/mid.png,MZMOD.MZMOD\n");
          if (this.addBackground) {
            var13.append(
                "MIDlet-Delete-Confirm: Do you really want to kill me? :) okay, visit http://plunder.com/devoicy for more java apps.\nMIDlet-Description: NO COMMENT\nSEMC-StandbyApplication: Y\nNokia-MIDlet-no-exit: true\n\n");
          }

          for (int var14 = 0; var14 < mergedEntries.size(); ++var14) {
            var13.append(var14).append(": ").append(mergedEntries.elementAt(var14)).append("\r\n");
          }

          var15.write(var13.append("\r\n").toString().getBytes("utf-8"));
          var15.closeEntry();
          var15.finish();
          var15.close();
          if (var2.toLowerCase().endsWith(".jar")) {
            try {
              var6.rename(var2.substring(var4 + 1));
              writeConnections.remove(var2 + "_jar");
            } catch (Exception var9) {
            }
          }

          System.gc();
          return true;
        }
      }
    } catch (Throwable var12) {
      var12.printStackTrace();
      if (var3 != null) {
        var3.close();
      }

      this.progressForm.append(var12.toString());
      return false;
    }
  }

  private static void showStatus(String var0) {
    statusAlert.setString(var0);
    display.setCurrent(statusAlert);
  }

  public final void run() {
    switch (this.operationType) {
      case 0:
        this.browseDirectory(selectedPath);
        return;
      case 1:
        String jarPath = selectedPath;
        if (this.findZipCentralDirectory(readFile(jarPath))) {
          try {
            Vector zipEntries;
            if ((zipEntries = this.readZipEntries(this.zipReader)).elementAt(0) == this) {
              zipEntries.removeAllElements();
              this.zipReader.close();
              showStatus(jarPath + "Read failed: not a valid zip file");
              return;
            }
            System.out.println("Read " + zipEntries.size() + " entries from " + jarPath);
            JarInfo jarInfo = new JarInfo(jarPath, zipEntries);
            jarCache.put(jarPath, jarInfo);
            this.showJarDetails(jarInfo);
            return;
          } catch (Exception var6) {
            showStatus(jarPath + "\nRead Failed:\n" + var6.toString());
            var6.printStackTrace();
            System.out.println(var6.toString());
            failedJars.put(jarPath, jarPath);
            return;
          }
        }
        break;
      case 2:
        long millis = System.currentTimeMillis();
        boolean isMerge = false;

        try {
          Vector var4 = new Vector();

          for (int var5 = 0; var5 < this.listChoice.size(); ++var5) {
            if (this.listChoice.isSelected(var5)) {
              var4.addElement(this.listChoice.getString(var5));
            }
          }

          if (var4.size() > 0) {
            this.progressForm.deleteAll();
            display.setCurrent(this.progressForm);
            String[] var11 = new String[var4.size()];
            var4.copyInto(var11);
            isMerge = this.mergeSelectedJars(var11);
          }
        } catch (Throwable var8) {
          this.progressForm.append(var8.toString());
        }

        if (this.zipReader != null) {
          this.zipReader.close();
          this.zipReader = null;
        }

        this.progressForm.append(
            loadString((char) (isMerge ? 'h' : 'i'))
                + loadString('j')
                + "Total Time\n"
                + (System.currentTimeMillis() - millis)
                + loadString('k'));

        try {
          Thread.sleep(1000L);
        } catch (Throwable var7) {
        }

        display.setCurrent(this.fileListScreen);
    }
  }

  private byte[] readZipEntryData(ZipEntryInfo zipEntryInfo) {
    // print log when zipEntryInfo null

    if (zipEntryInfo == null) {
      System.out.println("zipEntryInfo is null");
      return null;
    }

    this.zipReader.seek(zipEntryInfo.localHeaderOffset + 26);

    try {
      int var3;
      byte[] var2 = new byte[var3 = zipEntryInfo.uncompressedSize];
      this.zipReader.skip((long) (this.zipReader.readShort() + this.zipReader.readShort()));
      if (zipEntryInfo.compressionMethod == 8) {
        this.inflater.reset();
        this.inflater.setInput(
            this.zipReader.buffer, this.zipReader.getPosition(), zipEntryInfo.compressedSize);
        int var6 = 0;

        while (var3 - var6 > 0) {
          int var4 = this.inflater.inflate(var2, var6, var3 - var6);
          var6 += var4;
          if (var4 <= 0) {
            return null;
          }
        }
      } else {
        this.zipReader.readBytes(var2, var3);
      }

      return var2;
    } catch (Throwable var5) {
      var5.printStackTrace();
      return null;
    }
  }

  protected final void destroyApp(boolean var1) {}

  protected final void pauseApp() {}

  protected final void startApp() {}

  private static String loadLastDirectory() {
    try {
      RecordStore var0;
      if ((var0 = RecordStore.openRecordStore("dir", true)).getNumRecords() > 0) {
        String var1 = new String(var0.getRecord(1), "utf-8");
        var0.closeRecordStore();
        return var1;
      }
    } catch (Throwable var2) {
    }

    return null;
  }

  private static void saveLastDirectory(String var0) {
    try {
      RecordStore var1 = RecordStore.openRecordStore("dir", true);
      byte[] var3 = var0.getBytes("utf-8");
      if (var1.getNumRecords() > 0) {
        var1.setRecord(1, var3, 0, var3.length);
      } else {
        var1.addRecord(var3, 0, var3.length);
      }

      var1.closeRecordStore();
    } catch (Throwable var2) {
    }
  }

  private static FileConnection findValidDirectory(String var0) {
    currentDirectory = var0;
    FileConnection var1 = openReadConnection(var0);

    do {
      if (var1 != null && var1.isDirectory()) {
        return var1;
      }

      int var2;
      String var10000 =
          (var2 = var0.lastIndexOf(47, var0.length() - 2)) > 0 ? var0.substring(0, var2 + 1) : null;
      var0 = var10000;
      currentDirectory = var10000;
    } while (var0 != null && var0.length() > 8);

    return null;
  }

  private void browseDirectory(String directoryPath) {
    while (true) {
      int var2;
      Enumeration var4;
      if (directoryPath != null && !directoryPath.equals("file:///")) {
        FileConnection var5;
        if (directoryPath.endsWith("../")) {
          String var10000 =
              (var2 = directoryPath.lastIndexOf(47, directoryPath.length() - 5)) > 0
                  ? directoryPath.substring(0, var2 + 1)
                  : null;
          directoryPath = var10000;
          if (var10000 == null || directoryPath.length() <= 8) {
            directoryPath = null;
            continue;
          }

          if ((var5 = findValidDirectory(directoryPath)) == null) {
            directoryPath = null;
            continue;
          }
        } else if ((var5 = findValidDirectory(directoryPath)) == null) {
          directoryPath = null;
          continue;
        }

        try {
          var4 = var5.list("*", true);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        saveLastDirectory(currentDirectory);
        this.fileListChoice.deleteAll();
        this.fileListChoice.append("../", this.folderIcon);

        String var3;
        while (var4.hasMoreElements()) {
          if ((var3 = (String) var4.nextElement()).endsWith("/")) {
            this.fileListChoice.append(var3, this.folderIcon);
          }
        }

        try {
          var4 = var5.list("*", true);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }

        while (true) {
          String var6;
          do {
            if (!var4.hasMoreElements()) {
              return;
            }
          } while (!(var6 = (var3 = (String) var4.nextElement()).toLowerCase()).endsWith(".zip")
              && !var6.endsWith(".jar")
              && !var6.endsWith("_jar")
              && !var6.endsWith("_zip"));

          this.fileListChoice.append(var3, this.fileIcon);
        }
      }

      currentDirectory = "file:///";
      saveLastDirectory("file:///");
      this.fileListChoice.deleteAll();
      if (this.rootPaths == null) {
        var4 = FileSystemRegistry.listRoots();

        while (var4.hasMoreElements()) {
          this.fileListChoice.append((String) var4.nextElement(), this.folderIcon);
        }

        this.rootPaths = new String[this.fileListChoice.size()];

        for (var2 = this.rootPaths.length - 1; var2 >= 0; --var2) {
          this.rootPaths[var2] = this.fileListChoice.getString(var2);
        }

        return;
      }

      for (var2 = 0; var2 < this.rootPaths.length; ++var2) {
        this.fileListChoice.append(this.rootPaths[var2], this.folderIcon);
      }

      return;
    }
  }

  private boolean findZipCentralDirectory(byte[] bytes) {
    try {
      if (bytes != null) {
        int var2;
        for (var2 = bytes.length - 22;
            var2 > 0
                && (bytes[var2] != 80
                    || bytes[var2 + 1] != 75
                    || bytes[var2 + 2] != 5
                    || bytes[var2 + 3] != 6);
            --var2) {}

        if (var2 < 20) {
          return false;
        }

        var2 += 16;
        var2 =
            bytes[var2++] & 255
                | (bytes[var2++] & 255) << 8
                | (bytes[var2++] & 255) << 16
                | (bytes[var2] & 255) << 24;
        this.zipReader = new ByteArrayReader(bytes);
        this.zipReader.seek(var2);
        return true;
      }
    } catch (Throwable var3) {
    }

    return false;
  }

  private static byte[] decodeBase64(String s) {
    int var1;
    byte[] var7;
    if ((var1 = (var7 = instance.getAppProperty(s).getBytes()).length / 4 * 3) == 0) {
      return var7;
    } else {
      if (var7[var7.length - 1] == 61) {
        --var1;
        if (var7[var7.length - 2] == 61) {
          --var1;
        }
      }

      byte[] var2 = new byte[var1];
      int var3 = 0;
      int var4 = 0;

      for (var1 = var7.length; var1 > 0; var1 -= 4) {
        int var6 = 3;
        int var5 =
            (BASE64_DECODE_TABLE[var7[var3++] & 255] << 6 | BASE64_DECODE_TABLE[var7[var3++] & 255])
                << 6;
        if (var7[var3] != 61) {
          var5 |= BASE64_DECODE_TABLE[var7[var3++] & 255];
        } else {
          --var6;
        }

        var5 <<= 6;
        if (var7[var3] != 61) {
          var5 |= BASE64_DECODE_TABLE[var7[var3++] & 255];
        } else {
          --var6;
        }

        if (var6 > 2) {
          var2[var4 + 2] = (byte) var5;
        }

        var5 >>= 8;
        if (var6 > 1) {
          var2[var4 + 1] = (byte) var5;
        }

        var5 >>= 8;
        var2[var4] = (byte) var5;
        var4 += var6;
      }

      return var2;
    }
  }

  private static Image loadIcon(char c) {
    byte[] var1;
    return Image.createImage(var1 = decodeBase64(String.valueOf(c)), 0, var1.length);
  }

  private static String loadString(char var0) {
    try {
      return new String(decodeBase64(String.valueOf(var0)), "utf-8");
    } catch (Throwable var1) {
      return "a";
    }
  }

  private static FileConnection openReadConnection(String s) {
    try {
      FileConnection var1;
      if (readConnections.containsKey(s)) {
        var1 = (FileConnection) readConnections.get(s);
      } else {
        var1 = (FileConnection) Connector.open(s, 1);
        readConnections.put(s, var1);
      }

      return var1;
    } catch (Throwable var2) {
      showStatus(var2.toString());
      return null;
    }
  }

  private static FileConnection openWriteConnection(String s) {
    try {
      FileConnection var1;
      if (writeConnections.containsKey(s)) {
        var1 = (FileConnection) writeConnections.get(s);
      } else {
        var1 = (FileConnection) Connector.open(s, 2);
        writeConnections.put(s, var1);
      }

      return var1;
    } catch (Throwable var2) {
      return null;
    }
  }

  private byte[] readResource(String s) {
    try {
      DataInputStream var4;
      byte[] var2 =
          new byte
              [(var4 = new DataInputStream(this.getClass().getResourceAsStream(s))).available()];
      var4.readFully(var2);
      var4.close();
      return var2;
    } catch (Exception var3) {
      return null;
    }
  }

  private static byte[] readFile(String s) {
    FileConnection var3 = openReadConnection(s);

    try {
      int var1;
      if ((var1 = (int) var3.fileSize()) >= 10 && var1 <= 41943040) {
        DataInputStream var4 = var3.openDataInputStream();
        byte[] var5 = new byte[var1];
        var4.readFully(var5);
        var4.close();
        return var5;
      } else {
        return null;
      }
    } catch (Exception var2) {
      return null;
    }
  }

  private void writeBaseEntries(ZipWriter zipWriter, BufferedOutput bufferedOutput) {
    try {
      zipWriter.putNextEntry("MZMOD/mid.png");
      byte[] var3;
      if ((var3 = readFile(this.iconField.getString())) == null) {
        System.out.println("Read icon failed: " + this.iconField.getString());
        this.progressForm.append(this.iconField.getString() + "Read Failure");
        zipWriter.write(this.readResource("/icon.png"));
      } else {
        zipWriter.write(var3);
      }

      zipWriter.closeEntry();
      this.zipReader = new ByteArrayReader(this.readResource("/MZMODlibs.jar"));
      Vector var10;
      int var4 = (var10 = this.readZipEntries(this.zipReader)).size();
      int var5 = 1;

      while (true) {
        ZipEntryInfo var6;
        String var7;
        int var8;
        do {
          do {
            if (var5 >= var4) {
              this.zipReader =
                  new ByteArrayReader(
                      this.readResource(
                          this.enableScreenshots ? "/MZMODSSlib.jar" : "/MZMODlib.jar"));
              var4 = (var10 = this.readZipEntries(this.zipReader)).size();
              var5 = 1;

              while (true) {
                while (true) {
                  do {
                    do {
                      if (var5 >= var4) {
                        if (this.enableScreenshots
                            && (this.enableList || this.enableListMainPage)) {
                          this.zipReader = new ByteArrayReader(this.readResource("/MZMODlib.jar"));
                          var4 = (var10 = this.readZipEntries(this.zipReader)).size();
                          var5 = 1;

                          while (var5 < var4) {
                            if ((var6 = (ZipEntryInfo) var10.elementAt(var5++))
                                .toString()
                                .endsWith("List.class")) {
                              var8 = var6.localHeaderOffset;
                              this.zipReader.seek(var8 + 26);
                              zipWriter.putNextEntry(var6);
                              zipWriter.closeCurrentEntry();
                              this.zipReader.seek(var8 + 26);
                              this.zipReader.skip(
                                  (long) (this.zipReader.readShort() + this.zipReader.readShort()));
                              bufferedOutput.write(
                                  this.zipReader.buffer,
                                  this.zipReader.getPosition(),
                                  var6.compressedSize);
                              bufferedOutput.flush();
                            }
                          }
                        }

                        return;
                      }
                    } while (!(var7 = (var6 = (ZipEntryInfo) var10.elementAt(var5++)).toString())
                        .endsWith(".class"));
                  } while (!this.enableList
                      && !this.enableListMainPage
                      && var7.endsWith("List.class"));

                  if (this.enableListMainPage && var7.endsWith("MZMOD.class")) {
                    byte[] var12;
                    int var11 = patchClassFile(var12 = this.readZipEntryData(var6));
                    zipWriter.putNextEntry(var7);
                    zipWriter.write(var12, 0, var11);
                    zipWriter.closeEntry();
                  } else {
                    var8 = var6.localHeaderOffset;
                    this.zipReader.seek(var8 + 26);
                    zipWriter.putNextEntry(var6);
                    zipWriter.closeCurrentEntry();
                    this.zipReader.seek(var8 + 26);
                    this.zipReader.skip(
                        (long) (this.zipReader.readShort() + this.zipReader.readShort()));
                    bufferedOutput.write(
                        this.zipReader.buffer, this.zipReader.getPosition(), var6.compressedSize);
                    bufferedOutput.flush();
                  }
                }
              }
            }
          } while (!(var7 = (var6 = (ZipEntryInfo) var10.elementAt(var5++)).toString())
              .endsWith(".class"));
        } while ((this.enableList || this.enableListMainPage) && var7.endsWith("List.class"));

        var8 = var6.localHeaderOffset;
        this.zipReader.seek(var8 + 26);
        zipWriter.putNextEntry(var6);
        zipWriter.closeCurrentEntry();
        this.zipReader.seek(var8 + 26);
        this.zipReader.skip((long) (this.zipReader.readShort() + this.zipReader.readShort()));
        bufferedOutput.write(
            this.zipReader.buffer, this.zipReader.getPosition(), var6.compressedSize);
        bufferedOutput.flush();
      }
    } catch (Throwable var9) {
      var9.printStackTrace();
    }
  }

  private static int patchClassFile(byte[] bytes) {
    int var1;
    int var2 = (var1 = bytes.length) - 35;

    for (int var3 = 10; var3 < var2; ++var3) {
      if (bytes[var3] == 106
          && bytes[var3 + 1] == 97
          && bytes[var3 + 2] == 118
          && bytes[var3 + 3] == 97
          && bytes[var3 + 4] == 120
          && bytes[var3 + 5] == 47
          && bytes[var3 + 6] == 109
          && bytes[var3 + 7] == 105
          && bytes[var3 + 8] == 99
          && bytes[var3 + 9] == 114
          && bytes[var3 + 10] == 111
          && bytes[var3 + 11] == 101
          && bytes[var3 + 12] == 100
          && bytes[var3 + 13] == 105
          && bytes[var3 + 14] == 116
          && bytes[var3 + 15] == 105
          && bytes[var3 + 16] == 111
          && bytes[var3 + 17] == 110
          && bytes[var3 + 18] == 47
          && bytes[var3 + 19] == 108
          && bytes[var3 + 20] == 99
          && bytes[var3 + 21] == 100
          && bytes[var3 + 22] == 117
          && bytes[var3 + 23] == 105
          && bytes[var3 + 24] == 47
          && bytes[var3 + 25] == 76
          && bytes[var3 + 26] == 105
          && bytes[var3 + 27] == 115
          && bytes[var3 + 28] == 116) {
        bytes[var3] = 108;
        bytes[var3 + 1] = 105;
        bytes[var3 + 2] = 98;

        int var4;
        for (var4 = var3 - 1; bytes[var4] != 1; --var4) {}

        int var5 = bytes[var4 + 2] & 255;
        bytes[var4 + 2] = (byte) (var5 - 21);
        System.arraycopy(bytes, var3 + 24, bytes, var3 + 3, var1 - var3 - 24);
        var1 -= 21;
        var2 -= 21;
      }
    }

    return var1;
  }

  private void saveSettings() {
    if (this.useList) {
      try {
        RecordStore.openRecordStore("List", true).closeRecordStore();
      } catch (Throwable var8) {
      }
    } else {
      try {
        RecordStore.deleteRecordStore("List");
      } catch (Throwable var7) {
      }
    }

    try {
      RecordStore var1 = RecordStore.openRecordStore("setting", true);
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      DataOutputStream var3;
      (var3 = new DataOutputStream(var2)).writeUTF(this.iconField.getString());
      var3.writeUTF(this.programNameField.getString());
      var3.writeUTF(this.displayNameField.getString());
      var3.writeUTF(this.vendorNameField.getString());
      var3.writeUTF(this.savePathField.getString());
      var3.writeBoolean(this.enableScreenshots);
      var3.writeBoolean(this.enableList);
      var3.writeBoolean(this.enableListMainPage);
      var3.writeBoolean(this.addBackground);
      var3.flush();
      byte[] var4 = var2.toByteArray();
      var2.reset();
      int var5;
      var3.writeInt(var5 = this.listChoice.size());

      for (int var6 = 0; var6 < var5; ++var6) {
        var3.writeUTF(this.listChoice.getString(var6));
      }

      var3.flush();
      var3.close();
      byte[] var10 = var2.toByteArray();
      if (var1.getNumRecords() > 0) {
        var1.setRecord(1, var4, 0, var4.length);
        var1.setRecord(2, var10, 0, var10.length);
      } else {
        var1.addRecord(var4, 0, var4.length);
        var1.addRecord(var10, 0, var10.length);
      }

      var1.closeRecordStore();
    } catch (Throwable var9) {
    }

    if (this.useList && !(this.fileListScreen instanceof CanvasList)) {
      this.createCustomUI();
    } else {
      if (!this.useList && this.fileListScreen instanceof CanvasList) {
        this.createStandardUI();
      }
    }
  }

  private static byte[] processClassFile(byte[] bytes, char c) {
    DataInputStream var2 = new DataInputStream(new ByteArrayInputStream(bytes));

    try {
      if (var2.readInt() != -889275714) {
        var2.close();
        return bytes;
      }
    } catch (Exception var4) {
    }

    classModifier.readConstantPool(var2);
    classOutputBuffer.reset();
    classModifier.readAndModifyMethods(var2, c);

    try {
      classModifier.writeClassFile(classOutputWriter);
      classOutputWriter.flush();
    } catch (Exception var3) {
    }

    return classOutputBuffer.toByteArray();
  }

  private void showJarDetails(JarInfo jarInfo) {
    this.currentJarInfo = jarInfo;
    if (jarInfo.displayNames == null) {
      System.out.println("Parsing manifest for " + jarInfo.jarPath);
      jarInfo.parseManifest(this.readZipEntryData((ZipEntryInfo) jarInfo.entries.elementAt(0)));
      this.zipReader.close();
    }

    for (int var2 = 0; var2 < 26; ++var2) {
      ((TextField) this.addToSetForm.get(var2)).setString(jarInfo.displayNames[var2]);
    }

    display.setCurrent(this.addToSetForm);
  }

  private void updateJarDisplayNames(JarInfo jarInfo) {
    for (int var2 = 0; var2 < 26; ++var2) {
      jarInfo.displayNames[var2] = ((TextField) this.addToSetForm.get(var2)).getString();
    }
  }

  static {
    statusAlert = new Alert("Tips", "", (Image) null, AlertType.INFO);
    classModifier = new ClassModifier();
    classOutputBuffer = new ByteArrayOutputStream();
    classOutputWriter = new DataOutputStream(classOutputBuffer);
    jarCache = new Hashtable();
    failedJars = new Hashtable();
    mergedEntries = new Vector();
    currentDirectory = "file:///";
    BASE64_CHARS =
        new char[] {
          'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
          'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
          'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1',
          '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
        };
    BASE64_DECODE_TABLE = new byte[256];

    int var0;
    for (var0 = 0; var0 < 255; ++var0) {
      BASE64_DECODE_TABLE[var0] = -1;
    }

    for (var0 = 0; var0 < 64; ++var0) {
      BASE64_DECODE_TABLE[BASE64_CHARS[var0]] = (byte) var0;
    }

    readConnections = new Hashtable(200);
    writeConnections = new Hashtable(200);
  }
}
