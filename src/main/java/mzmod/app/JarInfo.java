package mzmod.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import mzmod.zip.ZipEntryInfo;

final class JarInfo {
  String jarPath;
  Vector entries;
  String[] displayNames;
  String iconPath;
  String tempIconName;
  String tempIniName;
  private Hashtable manifestEntries;
  private String midlet1Value;
  private String midlet2Value;
  private int mergeCount;
  private static byte[] lineBuffer = new byte[4096];

  JarInfo(String jarPath, Vector entries) {
    this.entries = entries;
    this.jarPath = jarPath;
  }

  final boolean parseManifest(byte[] var1) {
    try {
      this.manifestEntries = parseManifestEntries(var1);
      this.displayNames = new String[26];
      this.displayNames[0] = "10";
      String var7;
      int var2 =
          (var7 = this.midlet1Value = (String) this.manifestEntries.get("MIDlet-1")).indexOf(44)
              + 1;

      try {
        this.iconPath = var7.substring(var2, var7.indexOf(44, var2)).trim().substring(1);
      } catch (Exception var8) {
      }

      long var3 = System.currentTimeMillis();
      this.tempIconName = var3 + ".png";
      this.tempIniName = var3 + ".ini";
      var7 = var7.substring(0, var2 - 1).trim();
      int ii = 1;

      for (int var11 = 1; var11 < 26; ++var11) {
        this.displayNames[var11] = ii + "." + var7;
        ++ii;
      }

      if ((var7 = this.midlet2Value = (String) this.manifestEntries.get("MIDlet-2")) != null) {
        var7 = var7.substring(0, var7.indexOf(44)).trim();
        this.displayNames[25] = var7;
      }

      return true;
    } catch (Exception var9) {
      return false;
    }
  }

  private static Hashtable parseManifestEntries(byte[] var0) {
    Hashtable var1 = new Hashtable();
    ByteArrayInputStream var6 = new ByteArrayInputStream(var0);
    String var3 = "";

    try {
      String var2;
      while ((var2 = readLine((InputStream) var6)) != null) {
        int var4;
        if ((var4 = var2.indexOf(58)) > 0) {
          var1.put(var3 = var2.substring(0, var4), var2.substring(var4 + 1).trim());
        } else if ((var2 = var2.trim()).length() > 0) {
          var1.put(var3, var1.get(var3) + var2);
        }
      }

      var6.close();
    } catch (Exception ignored) {
    }

    return var1;
  }

  private static String readLine(InputStream var0) {
    try {
      int var1 = 0;

      while (true) {
        int var2;
        switch (var2 = var0.read()) {
          case -1:
          case 10:
            return var2 == -1 && var1 == 0 ? null : new String(lineBuffer, 0, var1, "utf-8");
          case 13:
            var0.read();
            return var2 == -1 && var1 == 0 ? null : new String(lineBuffer, 0, var1, "utf-8");
          default:
            lineBuffer[var1++] = (byte) var2;
        }
      }
    } catch (UnsupportedEncodingException var3) {
      var3.printStackTrace();
    } catch (IOException var4) {
      var4.printStackTrace();
    }

    return null;
  }

  final Vector getResourceEntries() {
    Vector var1 = this.entries;
    int var2 = this.entries.size() - 1;
    Vector var3 = new Vector(var2);

    for (int var4 = 1; var4 <= var2; ++var4) {
      ZipEntryInfo var5;
      if (!(var5 = (ZipEntryInfo) var1.elementAt(var4)).toString().endsWith(".class")) {
        ZipEntryInfo var6;
        (var6 = new ZipEntryInfo(var5.toString())).nameLength = var5.nameLength;
        var6.localHeaderOffset = var5.localHeaderOffset;
        var6.crc32 = var5.crc32;
        var6.lastModifiedTime = var5.lastModifiedTime;
        var6.uncompressedSize = var5.uncompressedSize;
        var6.compressionMethod = var5.compressionMethod;
        var6.compressedSize = var5.compressedSize;
        var3.addElement(var6);
      }
    }

    return var3;
  }

  final int getMergeCount() {
    return this.mergeCount =
        Math.min(Integer.parseInt(this.displayNames[0]), this.midlet2Value == null ? 25 : 24);
  }

  final String buildManifestEntries(Vector var1, int var2) {
    String var3 = this.midlet1Value.substring(this.midlet1Value.lastIndexOf(44) + 1).trim();
    int var4 = this.mergeCount + var2;

    for (int var5 = var2; var5 < var4; ++var5) {
      var1.addElement(
          this.displayNames[var5 - var2 + 1]
              + ",/"
              + this.tempIconName
              + ","
              + (var5 == 0 ? "" : (char) (var5 + 97) + ".")
              + var3
              + ",/"
              + this.tempIniName);
    }

    var3 = this.midlet2Value;
    if (this.midlet2Value != null) {
      var1.addElement(
          this.displayNames[25]
              + ",/"
              + this.tempIconName
              + ","
              + (char) (var2 + 97)
              + "."
              + var3.substring(var3.lastIndexOf(44) + 1).trim()
              + ",/"
              + this.tempIniName);
    }

    StringBuffer var10 = new StringBuffer();
    Enumeration var6 = this.manifestEntries.keys();

    while (var6.hasMoreElements()) {
      String var7 = (String) var6.nextElement();
      var10.append(var7).append(": ").append(this.manifestEntries.get(var7)).append("\r\n");
    }

    return var10.append("\r\n").toString();
  }
}
