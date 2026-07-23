package mzmod.merger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/**
 * Merges multiple JAR files into one output JAR.
 *
 * <p>Each input JAR's classes are relocated under a unique prefix path (e.g. {@code a/}, {@code
 * b/}) to avoid name collisions, and bytecode is rewritten via {@link BytecodeProcessor} to fix all
 * internal references.
 */
public class JarMerger {

  // ---- Merge settings ----
  public String programName = "Merged App";
  public String displayName = "Merged App";
  public String vendorName = "MZMOD";
  public String iconPath = ""; // path to custom icon file
  public String savePath = "Embedded.jar";
  // Feature toggles (mirror original J2ME options)
  public boolean enableScreenshots = false;
  public boolean enableList = false;
  public boolean enableListMainPage = false;
  public boolean addBackground = false;

  // ---- JAR list ----
  private File sourceJar; // the single selected JAR
  public int copyCount = 1; // number of copies to merge (e.g. 6)

  // ---- Progress callback ----
  public interface ProgressListener {
    void onProgress(int current, int total, String message);

    void onComplete(boolean success, String message);
  }

  private ProgressListener listener;

  // ---- Manifest parsing helpers ----
  private static final char ENTRY_GROUP_SEP = ',';

  // ------------------------------------------------------------------
  //  Public API
  // ------------------------------------------------------------------

  public void setSourceJar(File jar) {
    if (jar != null && jar.isFile() && jar.getName().toLowerCase().endsWith(".jar")) {
      this.sourceJar = jar;
    }
  }

  public File getSourceJar() {
    return sourceJar;
  }

  public void clearSourceJar() {
    this.sourceJar = null;
  }

  /** Effective number of JAR entries to process (the same source JAR repeated copyCount times). */
  private int effectiveJarCount() {
    return (sourceJar != null) ? Math.max(1, copyCount) : 0;
  }

  public void setProgressListener(ProgressListener l) {
    this.listener = l;
  }

  /** Run the merge on a background thread. */
  public void mergeAsync() {
    Thread t = new Thread(this::doMerge, "MZMOD-Merge");
    t.setDaemon(true);
    t.start();
  }

  // ------------------------------------------------------------------
  //  Core merge logic
  // ------------------------------------------------------------------

  private void doMerge() {
    int total = effectiveJarCount();
    if (total == 0) {
      notifyComplete(false, "No JAR file selected.");
      return;
    }

    // Clear global state from previous runs
    BytecodeProcessor.MODIFIED_CLASSES.clear();

    Path outputPath = Paths.get(savePath);
    try {
      // Ensure parent directory exists (handle relative paths with no parent)
      Path parent = outputPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      if(outputPath.toFile().exists()) {
        Files.delete(outputPath);
      }

      long startTime = System.currentTimeMillis();

      try (ZipOutputStream zos =
          new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputPath)))) {

        // 1. Write the merged manifest
        writeManifest(zos, total);

        // 2. Write custom icon if specified
        writeIcon(zos);

        // 3. Process each copy of the source JAR (same file repeated total times)
        // Resources are collected (not written yet) and emitted once at the end
        // under a shared "res/" folder to avoid duplicate entries.
        Map<String, byte[]> collectedResources = new LinkedHashMap<>();

        for (int i = 0; i < total; i++) {
          File jarFile = sourceJar;
          notifyProgress(
              i, total, "Processing copy " + (i + 1) + "/" + total + ": " + jarFile.getName());

          BytecodeProcessor processor = new BytecodeProcessor(i);
          processJar(zos, jarFile, processor, i, collectedResources);
        }

        // 4. Merge embedded.jar (library bundled as a resource) into the output jar.
        // All of its classes/resources are added verbatim (no renaming / relocation).
        mergeEmbeddedJar(zos, collectedResources);

        // 5. Write Static registration class
        if (!BytecodeProcessor.MODIFIED_CLASSES.isEmpty()) {
          writeStaticClass(zos, BytecodeProcessor.MODIFIED_CLASSES);
        }
      }

      long elapsed = System.currentTimeMillis() - startTime;
      notifyComplete(
          true,
          String.format(
              "Success! Merged %d copy(ies) of 1 JAR in %d ms\nOutput: %s",
              total, elapsed, outputPath));
    } catch (Exception e) {
      e.printStackTrace();
      notifyComplete(false, "Error: " + e.getMessage());
    }
  }

  /**
   * Merge all entries from the bundled {@code embedded.jar} resource into the output jar.
   *
   * <p>Entries are written verbatim (classes are not relocated). Resource entries that were already
   * collected from the source jar are skipped to avoid duplicate entries.
   */
  private void mergeEmbeddedJar(ZipOutputStream zos, Map<String, byte[]> collectedResources)
      throws IOException {
    InputStream embedded = getClass().getResourceAsStream("/embedded.jar");
    if (embedded == null) {
      // No embedded library to merge
      return;
    }
    notifyProgress(0, 1, "Merging embedded library: embedded.jar");
    try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(embedded))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();
        if (entry.isDirectory()) continue;
        if (name.equalsIgnoreCase("META-INF/MANIFEST.MF")) continue;

        byte[] data = readAllBytes(zis);
        zis.closeEntry();

        if (name.endsWith(".class")) {
          writeEntry(zos, name, data);
        } else {
          if (collectedResources.putIfAbsent(name, data) == null) {
            writeEntry(zos, name, data);
          }
        }
      }
    }
  }

  private void processJar(
      ZipOutputStream zos,
      File jarFile,
      BytecodeProcessor processor,
      int jarIndex,
      Map<String, byte[]> collectedResources)
      throws IOException {
    try (ZipInputStream zis =
        new ZipInputStream(new BufferedInputStream(Files.newInputStream(jarFile.toPath())))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();

        // Read entire entry data
        byte[] data = readAllBytes(zis);
        zis.closeEntry();

        // Skip directories and the manifest (we write our own)
        if (entry.isDirectory()) continue;

        if (name.endsWith(".class")) {
          // Process bytecode and rename
          String className = name.substring(0, name.length() - 6);
          byte[] processed = processor.processClass(data, className);
          String newName = processor.getRenamedClass(className) + ".class";
          writeEntry(zos, newName, processed != null ? processed : data);
        } else if (name.equalsIgnoreCase("META-INF/MANIFEST.MF") && collectedResources.putIfAbsent(name, data) == null) {
          writeEntry(zos, "manifest.ini", data);
        } else {
          // Resource (images, data, etc.): keep at its original location, written
          // exactly once. The first copy wins; later copies are skipped to avoid
          // duplicate entries when the same source JAR is merged multiple times.
          if (collectedResources.putIfAbsent(name, data) == null) {
            writeEntry(zos, name, data);
          }
        }
      }
    }
  }

  // ------------------------------------------------------------------
  //  Manifest generation
  // ------------------------------------------------------------------

  private void writeManifest(ZipOutputStream zos, int totalJars) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("Manifest-Version: 1.0\r\n");
    sb.append("MicroEdition-Configuration: CLDC-1.1\r\n");
    sb.append("MicroEdition-Profile: MIDP-2.0\r\n");
    sb.append("MIDlet-Version: 2.5\r\n");
    sb.append("MIDlet-Vendor: ").append(vendorName).append("\r\n");
    sb.append("MIDlet-Name: ").append(programName).append("\r\n");
    sb.append("MIDlet-1: ")
        .append(programName)
        .append(",/MZMOD/mid.png,MZMOD.MZMOD")
        .append("\r\n");
    // Collect MIDlet entries from each copy of the source JAR's manifest
    int midletNumber = 0;
    Map<String, String> manifest = readManifest(sourceJar);
    for (int i = 0; i < totalJars; i++) {
      // Extract MIDlet-1 (the main entry)
      String midlet1 = manifest.get("MIDlet-1");
      if (midlet1 != null) {
        // Format: "DisplayName, Icon, ClassName"
        String[] parts = splitMidlet(midlet1);
        String className = (i == 0) ? parts[2] : (char) ('a' + i - 1) + "." + parts[2];
        String displayName = parts[0];
        String icon = (iconPath != null && !iconPath.isEmpty()) ? "/icon" : parts[1];

        sb.append(midletNumber++).append(": ");
        sb.append(displayName).append("(").append(midletNumber).append(")").append(",");
        sb.append(icon.isEmpty() ? "" : icon).append(",");
        sb.append(className).append(",");
        sb.append("/manifest.ini");
        sb.append("\r\n");
      }
    }

    writeEntry(zos, "META-INF/MANIFEST.MF", sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** Parse a JAR's manifest into a key-value map. */
  private Map<String, String> readManifest(File jarFile) {
    Map<String, String> entries = new LinkedHashMap<>();
    try (ZipInputStream zis =
        new ZipInputStream(new BufferedInputStream(Files.newInputStream(jarFile.toPath())))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
          byte[] data = readAllBytes(zis);
          parseManifest(new String(data, "UTF-8"), entries);
          break;
        }
        zis.closeEntry();
      }
    } catch (IOException e) {
      // ignore - return empty map
    }
    return entries;
  }

  private void parseManifest(String content, Map<String, String> out) {
    String[] lines = content.split("\r?\n");
    String currentKey = "";
    for (String line : lines) {
      if (line.isEmpty()) continue;
      int colon = line.indexOf(':');
      if (colon > 0 && !line.startsWith(" ")) {
        currentKey = line.substring(0, colon).trim();
        out.put(currentKey, line.substring(colon + 1).trim());
      } else {
        // continuation line
        String v = out.get(currentKey);
        if (v != null) {
          out.put(currentKey, v + line.trim());
        }
      }
    }
  }

  private String[] splitMidlet(String midletValue) {
    // Format: "DisplayName,IconPath,ClassName"
    String[] parts = new String[3];
    int first = midletValue.indexOf(',');
    int second = midletValue.indexOf(',', first + 1);
    if (second < 0) {
      parts[0] = midletValue.substring(0, first).trim();
      parts[1] = "";
      parts[2] = midletValue.substring(first + 1).trim();
    } else {
      parts[0] = midletValue.substring(0, first).trim();
      parts[1] = midletValue.substring(first + 1, second).trim();
      parts[2] = midletValue.substring(second + 1).trim();
    }
    return parts;
  }

  // ------------------------------------------------------------------
  //  Icon
  // ------------------------------------------------------------------

  private void writeIcon(ZipOutputStream zos) throws IOException {
    byte[] iconData = null;
    if (iconPath != null && !iconPath.isEmpty()) {
      try {
        iconData = Files.readAllBytes(Paths.get(iconPath));
      } catch (IOException e) {
        // Use default icon from resources
        InputStream is = getClass().getResourceAsStream("/icon.png");
        if (is != null) {
          iconData = readAllBytes(is);
          is.close();
        }
      }
    } else {
      InputStream is = getClass().getResourceAsStream("/icon.png");
      if (is != null) {
        iconData = readAllBytes(is);
        is.close();
      }
    }
    if (iconData != null) {
      writeEntry(zos, "icon", iconData);
    }
  }

  // ------------------------------------------------------------------
  //  Static class (class registration)
  // ------------------------------------------------------------------

  private void writeStaticClass(ZipOutputStream zos, Set<String> classes) throws IOException {
    // Generate a simple Static class that registers all modified classes.
    // This class has a <clinit> that a vector staticvector which holds all class names.
    // In the original J2ME app, this was generated via raw bytecode.
    // Here, we use ASM to generate it.

    ClassWriter cw = new ClassWriter(0);
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Static", null, "java/lang/Object", null);

    // static field: staticvector (java/util/Vector)
    cw.visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "staticvector",
            "Ljava/util/Vector;",
            null,
            null)
        .visitEnd();

    // <init>
    {
      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    // <clinit>
    {
      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
      mv.visitCode();
      mv.visitTypeInsn(Opcodes.NEW, "java/util/Vector");
      mv.visitInsn(Opcodes.DUP);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Vector", "<init>", "()V", false);
      mv.visitFieldInsn(Opcodes.PUTSTATIC, "Static", "staticvector", "Ljava/util/Vector;");

      for (String className : classes) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "Static", "staticvector", "Ljava/util/Vector;");
        mv.visitLdcInsn(className.replace('/', '.'));
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/Class",
            "forName",
            "(Ljava/lang/String;)Ljava/lang/Class;",
            false);
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/util/Vector",
            "addElement",
            "(Ljava/lang/Object;)V",
            false);
      }

      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(3, 0);
      mv.visitEnd();
    }

    // regClass(I)V - stub (delegate to Static registration runtime if needed)
    {
      MethodVisitor mv =
          cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "regClass", "(I)V", null, null);
      mv.visitCode();
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(0, 1);
      mv.visitEnd();
    }

    cw.visitEnd();
    writeEntry(zos, "Static.class", cw.toByteArray());
  }

  // ------------------------------------------------------------------
  //  Utility methods
  // ------------------------------------------------------------------

  private void writeEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
    ZipEntry ze = new ZipEntry(name);
    zos.putNextEntry(ze);
    zos.write(data);
    zos.closeEntry();
  }

  private byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = is.read(buf)) > 0) {
      bos.write(buf, 0, n);
    }
    return bos.toByteArray();
  }

  private void notifyProgress(int current, int total, String msg) {
    if (listener != null) {
      listener.onProgress(current, total, msg);
    }
  }

  private void notifyComplete(boolean success, String msg) {
    if (listener != null) {
      listener.onComplete(success, msg);
    }
  }
}
