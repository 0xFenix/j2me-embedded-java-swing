package mzmod.merger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Bytecode processor (ASM 9.x) that rewrites class files for the JAR merger.
 *
 * <p>Each JAR's classes are relocated under a unique prefix path (e.g. {@code a/}, {@code b/}) to
 * avoid name collisions. J2ME library classes are redirected to {@code lib/} shim classes and a
 * handful of collision-prone method names are rewritten.
 */
public class BytecodeProcessor {

  /** Library class remapping table: J2ME/Platform class -> custom lib/ shim. */
  private static final Map<String, String> LIBRARY_MAPPINGS = new HashMap<>();

  static {
    LIBRARY_MAPPINGS.put("javax/microedition/io/Connector", "lib/Connector");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/Form", "lib/Form");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/List", "lib/List");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/Alert", "lib/Alert");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/Canvas", "lib/Canvas");
    LIBRARY_MAPPINGS.put("com/nokia/mid/ui/FullCanvas", "lib/FullCanvas");
    LIBRARY_MAPPINGS.put("javax/microedition/midlet/MIDlet", "lib/MIDlet");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/Display", "lib/Display");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/TextBox", "lib/TextBox");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/AlertType", "lib/AlertType");
    LIBRARY_MAPPINGS.put("javax/microedition/lcdui/game/GameCanvas", "lib/GameCanvas");
  }

  /** Set of all classes that have been processed / registered, for the Static class to manage. */
  public static final Set<String> MODIFIED_CLASSES = new CopyOnWriteArraySet<>();

  private final String prefix; // e.g. "", "a/", "b/"  (first JAR gets "")
  private final char prefixChar; // e.g. 'a', 'b'  (first JAR defaults to 'a')

  public BytecodeProcessor(int jarIndex) {
    if (jarIndex < 0) {
      throw new IllegalArgumentException("jarIndex must be >= 0");
    }
    if (jarIndex == 0) {
      this.prefix = "";
      this.prefixChar = 'a';
    } else {
      char c = (char) ('a' + (jarIndex - 1));
      this.prefix = String.valueOf(c) + "/";
      this.prefixChar = c;
    }
  }

  /**
   * Process a class file's bytecode.
   *
   * @param classBytes original {@code .class} file bytes
   * @param originalName original internal class name (e.g. {@code "com/example/MyClass"})
   * @return modified class bytes, or {@code null} if processing fails
   */
  public byte[] processClass(byte[] classBytes, String originalName) {
    try {
      ClassReader reader = new ClassReader(classBytes);
      ClassNode node = new ClassNode();
      reader.accept(node, 0);

      String origName = node.name;

      // 1. Rename the class itself, its superclass and implemented interfaces.
      node.name = remapClassName(origName);
      if (node.superName != null) {
        node.superName = remapClassName(node.superName);
      }
      if (node.interfaces != null) {
        for (int i = 0; i < node.interfaces.size(); i++) {
          node.interfaces.set(i, remapClassName(node.interfaces.get(i)));
        }
      }
      // Generic signatures are dropped: they are extremely rare in J2ME class files
      // and rewriting them correctly is expensive; nulling avoids stale class refs.
      node.signature = null;
      if (node.outerClass != null) {
        node.outerClass = remapClassName(node.outerClass);
      }

      // 2. Fields.
      if (node.fields != null) {
        for (FieldNode f : node.fields) {
          f.desc = remapDescriptor(f.desc);
          f.signature = null;
        }
      }

      // 3. Methods.
      if (node.methods != null) {
        for (MethodNode m : node.methods) {

          m.desc = remapDescriptor(m.desc);
          m.name = remapMethodName(origName, m.name, m.desc);

          m.signature = null;
          if (m.exceptions != null) {
            for (int i = 0; i < m.exceptions.size(); i++) {
              m.exceptions.set(i, remapClassName(m.exceptions.get(i)));
            }
          }
          if (m.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : m.tryCatchBlocks) {
              if (tcb.type != null) {
                tcb.type = remapClassName(tcb.type);
              }
            }
          }
          rewriteInstructions(m);
        }
      }

      // 4. Inner-classes attribute: rewrite internal names if present.
      if (node.innerClasses != null) {
        for (InnerClassNode ic : node.innerClasses) {
          if (ic.name != null) {
            ic.name = remapClassName(ic.name);
          }
          if (ic.outerName != null) {
            ic.outerName = remapClassName(ic.outerName);
          }
        }
      }

      // 5. Write it back. Frames are recomputed so renamed owners stay consistent.
      ClassWriter writer =
          new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
              try {
                return super.getCommonSuperClass(type1, type2);
              } catch (Throwable t) {
                return "java/lang/Object";
              }
            }
          };
      node.accept(writer);
      byte[] result = writer.toByteArray();

      // 6. Register the renamed class so the Static class can manage it.
      registerModifiedClass(node.name);

      return result;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void rewriteInstructions(MethodNode m) {
    if (m.instructions == null) {
      return;
    }
    for (AbstractInsnNode ain : m.instructions) {
      if (ain instanceof TypeInsnNode) {
        TypeInsnNode t = (TypeInsnNode) ain;
        t.desc = remapClassName(t.desc);
      } else if (ain instanceof FieldInsnNode) {
        FieldInsnNode f = (FieldInsnNode) ain;
        f.desc = remapDescriptor(f.desc);
        f.owner = remapClassName(f.owner);
      } else if (ain instanceof MethodInsnNode) {
        MethodInsnNode mi = (MethodInsnNode) ain;
        String origOwner = mi.owner;
        String origDesc = mi.desc;
        mi.name = remapMethodName(origOwner, mi.name, origDesc);
        mi.owner = remapClassName(origOwner);

        // Redirect RMS method calls -> lib/RecordStore
        if ("javax/microedition/rms/RecordStore".equals(origOwner)) {
          if (mi.name.contains("penRecordStore")
              || mi.name.contains("istRecordStores")
              || mi.name.contains("eleteRecordStore")) {
            mi.owner = "lib/RecordStore";
          }
        }
        mi.desc = remapDescriptor(origDesc);
      } else if (ain instanceof InvokeDynamicInsnNode) {
        InvokeDynamicInsnNode ind = (InvokeDynamicInsnNode) ain;
        ind.desc = remapDescriptor(ind.desc);
        // leave bootstrap method handle as-is; J2ME never uses invokedynamic.
      } else if (ain instanceof LdcInsnNode) {
        LdcInsnNode ldc = (LdcInsnNode) ain;
        if (ldc.cst instanceof Type) {
          Type t = (Type) ldc.cst;
          String remapped = remapDescriptor(t.getDescriptor());
          ldc.cst =
              "L".equals(t.getDescriptor())
                      || t.getDescriptor().startsWith("L")
                      || t.getDescriptor().startsWith("[")
                  ? Type.getType(remapped)
                  : t;
        }
      }
    }
  }

  /**
   * Remap a class internal name (no {@code L} prefix / {@code ;} suffix).
   *
   * <ul>
   *   <li>Classes present in {@link #LIBRARY_MAPPINGS} are redirected to the {@code lib/} shim.
   *   <li>Array descriptors ({@code [Lcom/Foo;}) are remapped element-wise.
   *   <li>{@code java/} and {@code javax/} platform classes are left untouched (they are provided
   *       by the runtime / shimmed separately).
   *   <li>Everything else receives the {@link #prefix}.
   * </ul>
   */
  private String remapClassName(String className) {
    if (className == null) {
      return null;
    }
    // Array descriptors share format with type descriptors.
    if (className.startsWith("[")) {
      return remapDescriptor(className);
    }
    // Library shim remapping takes priority.
    String mapped = LIBRARY_MAPPINGS.get(className);
    if (mapped != null) {
      return mapped;
    }

    // Platform-provided classes that must keep their original names.
    if (className.startsWith("java/")
        || className.startsWith("javax/")
        || className.startsWith("lib/")) {
      return className;
    }
    // Application class: relocate under the prefix path.
    return prefix + className;
  }

  /** Remap a field/method descriptor, rewriting every {@code L...;} class reference. */
  private String remapDescriptor(String desc) {
    if (desc == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(desc.length());
    int i = 0;
    int len = desc.length();
    while (i < len) {
      char c = desc.charAt(i);
      if (c == 'L') {
        int end = desc.indexOf(';', i);
        if (end < 0) {
          sb.append(c);
          i++;
          continue;
        }
        String internal = desc.substring(i + 1, end);
        sb.append('L').append(remapClassName(internal)).append(';');
        i = end + 1;
      } else {
        sb.append(c);
        i++;
      }
    }
    return sb.toString();
  }

  /** Remap a method name based on the collision-avoidance rules. */
  private String remapMethodName(String owner, String name, String desc) {
    if (name == null) {
      return null;
    }
    switch (name) {
      case "keyPressed":
        return "leyPressed";
      case "keyReleased":
        return "leyReleased";
      case "paint":
        // Paint on Sprite must keep its original name (framework calls it).
        if (!"javax/microedition/lcdui/game/Sprite".equals(owner)) {
          return "PAINT";
        }
        return name;
      case "forName":
        if ("java/lang/Class".equals(owner)) {
          return prefixChar + "orName";
        }
        return name;
      case "openRecordStore":
        if ("javax/microedition/rms/RecordStore".equals(owner)) {
          return prefixChar + "penRecordStore";
        }
        return name;
      case "listRecordStores":
        if ("javax/microedition/rms/RecordStore".equals(owner)) {
          return prefixChar + "istRecordStores";
        }
        return name;
      case "deleteRecordStore":
        if ("javax/microedition/rms/RecordStore".equals(owner)) {
          return prefixChar + "eleteRecordStore";
        }
        return name;
      default:
        return name;
    }
  }

  /** Remap a field reference's owner class. */
  private String remapFieldRef(String owner) {
    return remapClassName(owner);
  }

  /** Register a successfully processed class so the Static manager is aware of it. */
  private void registerModifiedClass(String renamedName) {
    if (renamedName != null) {
      MODIFIED_CLASSES.add(renamedName);
    }
  }

  /** Getter for the renamed form of a given original internal class name. */
  public String getRenamedClass(String originalName) {
    return prefix + originalName;
  }
}
