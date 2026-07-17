package mzmod.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public final class ClassModifier {
   private int thisClassIndex;
   private int superClassIndex;
   private Vector constantPool;
   private String superClassName;
   private FieldInfo[] fields;
   private AttributeData[] classAttributes;
   private int[] interfaces;
   private int accessFlags;
   private int modifiedAccessFlags;
   private Hashtable utf8Map = new Hashtable();
   private Hashtable classRefMap = new Hashtable();
   private static final Hashtable LIBRARY_MAPPINGS = new Hashtable(500);
   private static Vector pendingModifications = new Vector(500);
   public static final Vector MODIFIED_CLASSES = new Vector(500);
   private Vector methods = new Vector(100);
   private static int modificationCount;
   private boolean hasStaticFields;
   private boolean hasStaticInit;
   private ByteArrayOutputStream codeOutputStream = new ByteArrayOutputStream();
   private DataOutputStream codeDataOutputStream;

   public ClassModifier() {
      this.codeDataOutputStream = new DataOutputStream(this.codeOutputStream);
   }

   public static Hashtable initLibraryMappings() {
      MODIFIED_CLASSES.removeAllElements();
      modificationCount = 0;
      Hashtable var0;
      (var0 = LIBRARY_MAPPINGS).clear();
      var0.put("javax/microedition/io/Connector", "lib/Connector");
      var0.put("javax/microedition/lcdui/Form", "lib/Form");
      var0.put("javax/microedition/lcdui/List", "lib/List");
      var0.put("javax/microedition/lcdui/Alert", "lib/Alert");
      var0.put("javax/microedition/lcdui/Canvas", "lib/Canvas");
      var0.put("com/nokia/mid/ui/FullCanvas", "lib/FullCanvas");
      var0.put("javax/microedition/midlet/MIDlet", "lib/MIDlet");
      var0.put("javax/microedition/lcdui/Display", "lib/Display");
      var0.put("javax/microedition/lcdui/TextBox", "lib/TextBox");
      var0.put("javax/microedition/lcdui/AlertType", "lib/AlertType");
      var0.put("javax/microedition/lcdui/game/GameCanvas", "lib/GameCanvas");
      return var0;
   }

   public final void readConstantPool(DataInputStream var1) {
      try {
         this.utf8Map.clear();
         this.classRefMap.clear();
         this.accessFlags = var1.readInt();
         int var2 = var1.readUnsignedShort();
         Vector var3;
         (var3 = this.constantPool = new Vector(var2 + 20)).addElement(NullEntry.INSTANCE);

         for(int var4 = 1; var4 < var2; ++var4) {
            switch (var1.read()) {
               case 1:
                  String var5 = var1.readUTF();
                  var3.addElement(new Utf8Entry(var5));
                  this.utf8Map.put(var5, var4);
               case 2:
               default:
                  break;
               case 3:
                  var3.addElement(new IntEntry(var1.readInt()));
                  break;
               case 4:
                  var3.addElement(new FloatEntry(var1.readInt()));
                  break;
               case 5:
                  var3.addElement(new DoubleEntry(var1.readLong()));
                  var3.addElement(NullEntry.INSTANCE);
                  ++var4;
                  break;
               case 6:
                  var3.addElement(new LongEntry(var1.readLong()));
                  var3.addElement(NullEntry.INSTANCE);
                  ++var4;
                  break;
               case 7:
                  var3.addElement(new ClassRefEntry(var1.readUnsignedShort()));
                  break;
               case 8:
                  var3.addElement(new StringRefEntry(var1.readUnsignedShort()));
                  break;
               case 9:
                  var3.addElement(new FieldRefEntry(var1.readUnsignedShort(), var1.readUnsignedShort()));
                  break;
               case 10:
                  var3.addElement(new MethodRefEntry(var1.readUnsignedShort(), var1.readUnsignedShort()));
                  break;
               case 11:
                  var3.addElement(new InterfaceMethodRefEntry(var1.readUnsignedShort(), var1.readUnsignedShort()));
                  break;
               case 12:
                  var3.addElement(new NameAndTypeEntry(var1.readUnsignedShort(), var1.readUnsignedShort()));
            }
         }

         this.modifiedAccessFlags = var1.readUnsignedShort();
         this.modifiedAccessFlags = (this.modifiedAccessFlags |= 1) & -7;
         this.thisClassIndex = var1.readUnsignedShort();
         this.superClassIndex = var1.readUnsignedShort();
         this.superClassName = (String)LIBRARY_MAPPINGS.get(((Utf8Entry)var3.elementAt(((ClassRefEntry)var3.elementAt(this.thisClassIndex)).nameIndex)).value);
      } catch (Exception var6) {
      }
   }

   public final void readAndModifyMethods(DataInputStream var1, char var2) {
      this.hasStaticFields = false;
      this.hasStaticInit = false;
      this.methods.removeAllElements();

      try {
         Vector var3;
         int var4 = (var3 = this.constantPool).size();

         int var5;
         Object var6;
         String var9;
         Utf8Entry var25;
         for(var5 = 1; var5 < var4; ++var5) {
            if ((var6 = var3.elementAt(var5)) instanceof ClassRefEntry) {
if (!(var25 = (Utf8Entry)var3.elementAt(((ClassRefEntry)var6).nameIndex)).isModified) {
                   var25.isModified = true;
                   String var12 = var25.value;
                   if (LIBRARY_MAPPINGS.containsKey(var12)) {
                      var25.value = (String)LIBRARY_MAPPINGS.get(var12);
                  } else {
                     int var13;
                     String var16;
                     if (var12.startsWith("[") && (var13 = var12.indexOf(76) + 1) > 0 && (var16 = (String)LIBRARY_MAPPINGS.get(var12.substring(var13, var12.length() - 1))) != null) {
                        var25.value = var12.substring(0, var13) + var16 + ';';
                     }
                  }
               }
            } else if (var6 instanceof MethodRefEntry) {
               MethodRefEntry var7 = (MethodRefEntry)var6;
               NameAndTypeEntry var8 = (NameAndTypeEntry)var3.elementAt(var7.nameAndTypeIndex);
               var9 = ((Utf8Entry)var3.elementAt(var8.nameIndex)).value;
               String var10;
               Utf8Entry var11;
               if ((var10 = ((Utf8Entry)var3.elementAt(((ClassRefEntry)var3.elementAt(var7.classIndex)).nameIndex)).value).equals("java/lang/Class") && var9.equals("forName") && var2 != 'a') {
                  var7.classIndex = this.addClassReference("lib/Class");
                  (var11 = (Utf8Entry)var3.elementAt(var8.nameIndex)).isModified = true;
                  var11.originalValue = "forName".replace('f', var2);
               } else if (var10.equals("javax/microedition/rms/RecordStore")) {
                  if ((var9 = var9.substring(1)).equals("penRecordStore") || var9.equals("istRecordStores") || var9.equals("eleteRecordStore")) {
                     var7.classIndex = this.addClassReference("lib/RecordStore");
                     ((Utf8Entry)var3.elementAt(var8.nameIndex)).value = var2 + var9;
                  }
               } else if (var9.equals("paint") && !var10.equals("javax/microedition/lcdui/game/Sprite")) {
                  (var11 = (Utf8Entry)var3.elementAt(var8.nameIndex)).isModified = true;
                  var11.originalValue = "PAINT";
               } else {
                  resolveDescriptorReferences((Utf8Entry)var3.elementAt(var8.descriptorIndex));
               }
            } else if (var6 instanceof InterfaceMethodRefEntry) {
               resolveDescriptorReferences((Utf8Entry)var3.elementAt(((NameAndTypeEntry)var3.elementAt(((InterfaceMethodRefEntry)var6).nameAndTypeIndex)).descriptorIndex));
            } else if (var6 instanceof FieldRefEntry) {
               resolveTypeReference((Utf8Entry)var3.elementAt(((NameAndTypeEntry)var3.elementAt(((FieldRefEntry)var6).nameAndTypeIndex)).descriptorIndex));
            }
         }

         var4 = var3.size();

         for(var5 = 1; var5 < var4; ++var5) {
            if (!((var6 = var3.elementAt(var5)) instanceof NameAndTypeEntry)) {
               Utf8Entry var19;
if (var6 instanceof StringRefEntry && (var19 = (Utf8Entry)var3.elementAt(((StringRefEntry)var6).stringIndex)).isModified && !var19.originalValue.equals(var19.value)) {
                   ((StringRefEntry)var6).stringIndex = this.resolveModifiedName(var19);
               }
            } else {
               NameAndTypeEntry var18 = (NameAndTypeEntry)var6;
               Utf8Entry var21;
               if ((var9 = (var21 = (Utf8Entry)var3.elementAt(var18.nameIndex)).value).equals("keyReleased") || var9.equals("keyPressed")) {
                  var21.originalValue = var9.replace('k', 'l');
                  var21.isModified = true;
               }

               if (var21.isModified && !var21.originalValue.equals(var9)) {
                  var18.nameIndex = this.resolveModifiedName(var21);
               }

               if ((var21 = (Utf8Entry)var3.elementAt(var18.descriptorIndex)).isModified && var21.value.indexOf(47) == 1) {
                  var18.descriptorIndex = this.resolveModifiedName(var21);
               }
            }
         }

         var5 = var1.readUnsignedShort();
         this.interfaces = new int[var5];

         int var17;
         for(var17 = 0; var17 < var5; ++var17) {
            this.interfaces[var17] = var1.readUnsignedShort();
         }

         var5 = var1.readUnsignedShort();
         this.fields = new FieldInfo[var5];
         pendingModifications.removeAllElements();

         int var20;
         int var22;
         int var24;
         for(var17 = 0; var17 < var5; ++var17) {
            var20 = var1.readUnsignedShort();
            var22 = var1.readUnsignedShort();
            var24 = var1.readUnsignedShort();
            if ((var25 = (Utf8Entry)var3.elementAt(var22)).isModified && !var25.originalValue.equals(var25.value)) {
               var22 = this.resolveModifiedName(var25);
            }

            if ((var25 = (Utf8Entry)var3.elementAt(var24)).isModified && var25.value.indexOf(47) == 1) {
               var24 = this.resolveModifiedName(var25);
            }

            resolveTypeReference((Utf8Entry)var3.elementAt(var24));
            AttributeData[] var26;
            if ((var26 = this.readAttributes(var1)).length == 0 && (var20 & 8) != 0 && (this.modifiedAccessFlags & 512) == 0) {
               this.hasStaticFields = true;
               Vector var10000 = pendingModifications;
               IntPair var29;
               (var29 = new IntPair()).classIndex = var22;
               var29.nameAndTypeIndex = var24;
               var10000.addElement(var29);
            }

            this.fields[var17] = new FieldInfo(var20, var22, var24);
            this.fields[var17].attributes = var26;
         }

         var5 = var1.readUnsignedShort();

         for(var17 = 0; var17 < var5; ++var17) {
            var20 = var1.readUnsignedShort();
            var22 = var1.readUnsignedShort();
            var24 = var1.readUnsignedShort();
            String var27;
            if (!(var27 = (var25 = (Utf8Entry)var3.elementAt(var22)).value).equals("keyReleased") && !var27.equals("keyPressed")) {
               if (var27.equals("paint")) {
                  var25 = (Utf8Entry)var3.elementAt(var22 = this.addNewUtf8("PAINT"));
               }
            } else {
               var25.originalValue = var27.replace('k', 'l');
               var25.isModified = true;
            }

            if (var25.isModified && !var25.originalValue.equals(var27)) {
               var22 = this.resolveModifiedName(var25);
            }

            resolveDescriptorReferences((Utf8Entry)var3.elementAt(var24));
            MethodInfo var15;
            (var15 = new MethodInfo(var20, var22, var24)).attributes = this.readAttributes(var1);
            this.methods.addElement(var15);
            if (var25.originalValue.equals("<clinit>") && (this.modifiedAccessFlags & 512) == 0) {
               var15.nameIndex = this.getOrAddUtf8("cinitclone");
               var15.accessFlags = 9;
               this.hasStaticInit = true;
               (var15 = new MethodInfo(var20, var22, var24)).attributes = this.generateStaticInitCode(var2);
               this.methods.addElement(var15);
            }
         }

         this.classAttributes = this.readAttributes(var1);
         MethodInfo var23;
         if (this.hasStaticFields && !this.hasStaticInit) {
            this.hasStaticInit = true;
            MethodInfo var31 = var23 = new MethodInfo(9, this.getOrAddUtf8("cinitclone"), this.getOrAddUtf8("()V"));
            AttributeData[] var28 = new AttributeData[1];
            this.codeOutputStream.reset();
            this.codeDataOutputStream.writeShort(0);
            this.codeDataOutputStream.writeShort(1);
            this.codeDataOutputStream.writeInt(1);
            this.codeDataOutputStream.writeByte(177);
            this.codeDataOutputStream.writeInt(0);
            byte[] var30 = this.codeOutputStream.toByteArray();
            var28[0] = new AttributeData(this.getOrAddUtf8("Code"), var30.length, var30);
            var31.attributes = var28;
            this.methods.addElement(var23);
            (var23 = new MethodInfo(8, this.getOrAddUtf8("<clinit>"), this.getOrAddUtf8("()V"))).attributes = this.generateStaticInitCode(var2);
            this.methods.addElement(var23);
         }

         if (this.hasStaticFields || this.hasStaticInit) {
            (var23 = new MethodInfo(9, this.getOrAddUtf8("clears"), this.getOrAddUtf8("()V"))).attributes = this.generateFieldClearCode(pendingModifications);
            this.methods.addElement(var23);
            ++modificationCount;
            MODIFIED_CLASSES.addElement(this.superClassName);
         }

      } catch (Exception var14) {
      }
   }

   private AttributeData[] generateStaticInitCode(char var1) {
      this.codeOutputStream.reset();
     try {
       this.codeDataOutputStream.writeShort(1);
       this.codeDataOutputStream.writeShort(0);
       this.codeDataOutputStream.writeInt(modificationCount < 6 ? 8 : 10);
       if (modificationCount < 6) {
          this.codeDataOutputStream.writeByte(3 + modificationCount);
       } else {
          this.codeDataOutputStream.writeByte(17);
          this.codeDataOutputStream.writeShort(modificationCount);
       }

       this.codeDataOutputStream.writeByte(184);
       String var10002 = var1 == 'a' ? "Static" : var1 + "/Static";
       String var4 = "(I)V";
       String var3 = "regClass";
       String var2 = var10002;
       this.codeDataOutputStream.writeShort(this.addMethodRef(this.addClassReference(var2), var3, var4));
       this.codeDataOutputStream.writeByte(184);
       this.codeDataOutputStream.writeShort(this.addMethodRef(this.thisClassIndex, "cinitclone", "()V"));
       this.codeDataOutputStream.writeByte(177);
       this.codeDataOutputStream.writeInt(0);
     } catch (IOException ex) {
       throw new RuntimeException(ex);
     }
     byte[] var5 = this.codeOutputStream.toByteArray();
      return new AttributeData[]{new AttributeData(this.getOrAddUtf8("Code"), var5.length, var5)};
   }

   private AttributeData[] generateFieldClearCode(Vector var1) {
      try {
         this.codeOutputStream.reset();
         int var2 = var1.size();
         this.codeDataOutputStream.writeShort(2);
         this.codeDataOutputStream.writeShort(0);
         int var3 = (var2 << 2) + 1;
         this.codeDataOutputStream.writeInt(var3);

         for(var3 = 0; var3 < var2; ++var3) {
            IntPair var4 = (IntPair)var1.elementAt(var3);
            IntPair var6 = var4;
            ClassModifier var5 = this;
            Vector var7;
            int var8 = (var7 = this.constantPool).size();
            int var9 = 0;

            int var10000;
            label54:
            while(true) {
               Object var10;
               NameAndTypeEntry var18;
               if (var9 >= var8) {
                  for(var9 = 0; var9 < var8; ++var9) {
                     if ((var10 = var7.elementAt(var9)) instanceof NameAndTypeEntry && (var18 = (NameAndTypeEntry)var10).nameIndex == var6.classIndex && var18.descriptorIndex == var6.nameAndTypeIndex) {
                        var10000 = var5.addStaticFieldRef(var9);
                        break label54;
                     }
                  }

                  int var16 = var6.nameAndTypeIndex;
                  int var15 = var6.classIndex;
                  var5.constantPool.addElement(new NameAndTypeEntry(var15, var16));
                  var10000 = var5.addStaticFieldRef(var5.constantPool.size() - 1);
                  break;
               }

               FieldRefEntry var17;
               if ((var10 = var7.elementAt(var9)) instanceof FieldRefEntry && (var17 = (FieldRefEntry)var10).classIndex == var5.thisClassIndex && (var18 = (NameAndTypeEntry)var7.elementAt(var17.nameAndTypeIndex)).nameIndex == var6.classIndex && var18.descriptorIndex == var6.nameAndTypeIndex) {
                  var10000 = var9;
                  break;
               }

               ++var9;
            }

            int var14 = var10000;
            byte var13;
            switch (((Utf8Entry)this.constantPool.elementAt(var4.nameAndTypeIndex)).value.charAt(0)) {
               case 'D':
                  var13 = 14;
                  break;
               case 'F':
                  var13 = 11;
                  break;
               case 'J':
                  var13 = 9;
                  break;
               case 'L':
               case '[':
                  var13 = 1;
                  break;
               default:
                  var13 = 3;
            }

            this.codeDataOutputStream.writeByte(var13);
            this.codeDataOutputStream.writeByte(179);
            this.codeDataOutputStream.writeShort(var14);
         }

         this.codeDataOutputStream.writeByte(177);
         this.codeDataOutputStream.writeInt(0);
         this.codeDataOutputStream.flush();
         byte[] var12 = this.codeOutputStream.toByteArray();
         return new AttributeData[]{new AttributeData(this.getOrAddUtf8("Code"), var12.length, var12)};
      } catch (Exception var11) {
         return null;
      }
   }

   private int addClassReference(String var1) {
      if (this.classRefMap.containsKey(var1)) {
         return (Integer)this.classRefMap.get(var1);
      } else {
         this.constantPool.addElement(new ClassRefEntry(this.getOrAddUtf8(var1)));
         int var2 = this.constantPool.size() - 1;
         this.classRefMap.put(var1, var2);
         return var2;
      }
   }

   private int resolveModifiedName(Utf8Entry var1) {
      return this.getOrAddUtf8(var1.originalValue);
   }

   private int addMethodRef(int var1, String var2, String var3) {
      Vector var10000 = this.constantPool;
      this.constantPool.addElement(new NameAndTypeEntry(this.getOrAddUtf8(var2), this.getOrAddUtf8(var3)));
      var10000.addElement(new MethodRefEntry(var1, this.constantPool.size() - 1));
      return this.constantPool.size() - 1;
   }

   private int getOrAddUtf8(String var1) {
      if (this.utf8Map.containsKey(var1)) {
         int var2 = (Integer)this.utf8Map.get(var1);
         Utf8Entry var3;
         if ((var3 = (Utf8Entry)this.constantPool.elementAt(var2)).isModified && !var3.value.equals(var1)) {
            this.utf8Map.put(var3.value, var2);
            var2 = this.addNewUtf8(var1);
            this.utf8Map.put(var1, var2);
         }

         return var2;
      } else {
         return this.addNewUtf8(var1);
      }
   }

   private int addNewUtf8(String var1) {
      int var2 = this.constantPool.size();
      this.constantPool.addElement(new Utf8Entry(var1));
      this.utf8Map.put(var1, var2);
      return var2;
   }

   public final void writeClassFile(DataOutputStream var1) {
     try {
       var1.writeInt(-889275714);
       var1.writeInt(this.accessFlags);
       Vector var2;
       int var3 = (var2 = this.constantPool).size();
       var1.writeShort(var3);

       int var4;
       for(var4 = 1; var4 < var3; ++var4) {
          ((ConstantPoolEntry)var2.elementAt(var4)).write(var1);
       }

       var1.writeShort(this.modifiedAccessFlags);
       var1.writeShort(this.thisClassIndex);
       var1.writeShort(this.superClassIndex);
       int[] var8 = this.interfaces;
       var1.writeShort(var3 = var8.length);

       int var5;
       for(var5 = 0; var5 < var3; ++var5) {
          var1.writeShort(var8[var5]);
       }

       FieldInfo[] var6;
       var3 = (var6 = this.fields).length;
       var1.writeShort(var3);

       for(var4 = 0; var4 < var3; ++var4) {
          var6[var4].write(var1);
       }

       Vector var9 = this.methods;
       var1.writeShort(var9.size());

       for(var5 = 0; var5 < var9.size(); ++var5) {
          ((MethodInfo)var9.elementAt(var5)).write(var1);
       }

       AttributeData[] var7 = this.classAttributes;
       var1.writeShort(var3 = var7.length);

       for(var4 = 0; var4 < var3; ++var4) {
          var7[var4].write(var1);
       }
     } catch (IOException ex) {
       throw new RuntimeException(ex);
     }

   }

   private AttributeData[] readAttributes(DataInputStream var1) {
      int var2;
     AttributeData[] var3 = null;
     try {
       var3 = new AttributeData[var2 = var1.readUnsignedShort()];

       for(int var4 = 0; var4 < var2; ++var4) {
          int var5 = var1.readUnsignedShort();
          Utf8Entry var6;
if ((var6 = (Utf8Entry)this.constantPool.elementAt(var5)).isModified && !var6.originalValue.equals(var6.value)) {
              var5 = this.resolveModifiedName(var6);
          }

          int var8;
          byte[] var7 = new byte[var8 = var1.readInt()];
          var1.readFully(var7);
          var3[var4] = new AttributeData(var5, var8, var7);
       }
     } catch (IOException ex) {
       throw new RuntimeException(ex);
     }

     return var3;
   }

   private static void resolveTypeReference(Utf8Entry var0) {
      if (!var0.isModified) {
         var0.isModified = true;
         String var1;
         int var2;
         String var3;
         if (((var1 = var0.value).startsWith("[") || var1.startsWith("L")) && (var2 = var1.indexOf(76) + 1) > 0 && (var3 = (String)LIBRARY_MAPPINGS.get(var1.substring(var2, var1.length() - 1))) != null) {
            var0.value = var1.substring(0, var2) + var3 + ';';
         }
      }

   }

   private static void resolveDescriptorReferences(Utf8Entry var0) {
      if (!var0.isModified) {
         var0.isModified = true;
         String var1;
         if (!(var1 = var0.value).startsWith("(") || var1.indexOf(59) < 0) {
            return;
         }

         int var2;
         StringBuffer var3;
         for(var3 = new StringBuffer(); (var2 = var1.indexOf(76) + 1) > 0; var1 = var1.substring(var2)) {
            var3.append(var1.substring(0, var2));
            if ((var2 = (var1 = var1.substring(var2)).indexOf(59) + 1) <= 0) {
               break;
            }

            String var4;
            if ((var4 = (String)LIBRARY_MAPPINGS.get(var1.substring(0, var2 - 1))) != null) {
               var3.append(var4).append(';');
            } else {
               var3.append(var1.substring(0, var2));
            }
         }

         var3.append(var1);
         var0.value = var3.toString();
      }

   }

   private int addStaticFieldRef(int var1) {
      this.constantPool.addElement(new FieldRefEntry(this.thisClassIndex, var1));
      return this.constantPool.size() - 1;
   }
}
