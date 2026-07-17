package mzmod.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public final class StaticClassGenerator {
   private static final MethodInfo[] methodInfos = new MethodInfo[5];
   private static final Vector constantPool = new Vector(2048);
   private static final Vector additionalEntries = new Vector(2048);
   private static final Vector classList = new Vector(200);
   private static final Vector methodRefs = new Vector(1024);
   private static final Hashtable utf8Map = new Hashtable(1024);
   private static final Hashtable classRefMap = new Hashtable(1024);
   private static final Hashtable stringMap = new Hashtable(1024);
   private static int baseIndex;
   private static int initMethodIndex;
   private static int staticFieldIndex;
   private static final ByteArrayOutputStream codeBuffer = new ByteArrayOutputStream();
   private static final DataOutputStream codeWriter;
   private static final ByteArrayOutputStream stackMapBuffer;
   private static final DataOutputStream stackMapWriter;

   private static void writeClassFile(DataOutputStream var0) throws IOException {
      var0.writeInt(-889275714);
      var0.writeInt(196653);
      Vector var1;
      int var2 = (var1 = constantPool).size();
      var0.writeShort(var2);

      try {
         for(int var3 = 1; var3 < var2; ++var3) {
             ((ConstantPoolEntry)var1.elementAt(var3)).write(var0);
         }
      } catch (Exception var4) {
      }

      var0.writeShort(17);
      var0.writeShort(1);
      var0.writeShort(3);
      var0.writeShort(0);
      Vector var6 = methodRefs;
      var0.writeShort(var2 = var6.size());

      int var5;
      for(var5 = 0; var5 < var2; ++var5) {
         ((FieldInfo)var6.elementAt(var5)).write(var0);
      }

      var0.writeShort(5);

      for(var5 = 0; var5 < 5; ++var5) {
         methodInfos[var5].write(var0);
      }

      var0.writeShort(0);
   }

   public static byte[] generate(Vector classNames, String superName) {
      if (classNames.size() < 0) {
         return null;
      } else {
         classRefMap.clear();
         additionalEntries.removeAllElements();
         methodRefs.removeAllElements();
         classList.removeAllElements();
         constantPool.removeAllElements();
         Hashtable var2;
         (var2 = stringMap).clear();
         var2.put("()V", "");
         var2.put("Code", "");
         var2.put("(I)V", "");
         var2.put("java/util/Vector", "");
         var2.put("java/lang/Integer", "");
         var2.put("<init>", "");
         var2.put("clears", "");
         var2.put("<clinit>", "");
         var2.put("StackMap", "");
         var2.put("regClass", "");
         var2.put("cinitclone", "");
         var2.put("addElement", "");
         var2.put("cinitclones", "");
         var2.put("staticvector", "");
         var2.put("lib/Stack", "");
         var2.put("Ljava/util/Vector;", "");
         var2.put("(Ljava/util/Vector;)V", "");
         var2.put("(Ljava/lang/Object;)V", "");
         (var2 = utf8Map).clear();
         var2.put("<init>()V", new NameValuePair("<init>", "()V"));
         var2.put("<init>(I)V", new NameValuePair("<init>", "(I)V"));
         var2.put("clears()V", new NameValuePair("clears", "()V"));
         var2.put("regClass(I)V", new NameValuePair("regClass", "(I)V"));
         var2.put("cinitclone()V", new NameValuePair("cinitclone", "()V"));
         var2.put("<init>(Ljava/util/Vector;)V", new NameValuePair("<init>", "(Ljava/util/Vector;)V"));
         var2.put("staticvectorLjava/util/Vector;", new NameValuePair("staticvector", "Ljava/util/Vector;"));
         var2.put("addElement(Ljava/lang/Object;)V", new NameValuePair("addElement", "(Ljava/lang/Object;)V"));

         try {
            var2 = stringMap;
            Vector var3 = classList;
            Hashtable var4 = utf8Map;
            var2.put(superName, "");
            var3.addElement(superName);
            var3.addElement("java/lang/Integer");
            var3.addElement("lib/Stack");
            var3.addElement("java/util/Vector");
            int var5 = classNames.size();

            int var12;
            for(var12 = 0; var12 < var5; ++var12) {
               var3.addElement(classNames.elementAt(var12));
            }

            Vector var6 = additionalEntries;
            var12 = baseIndex = var3.size() + 6 + var4.size() + (classNames.size() << 1);

            Enumeration var10;
            for(var10 = var2.keys(); var10.hasMoreElements(); ++var12) {
               Object var7 = var10.nextElement();
               var2.put(var7, var12);
               var6.addElement(var7);
            }

            Vector var16;
            (var16 = constantPool).removeAllElements();
            var16.addElement(NullEntry.INSTANCE);
            var12 = 0;

            for(var5 = var3.size(); var12 < var5; ++var12) {
               var16.addElement(new ClassRefEntry(getOrAddConstant(var3.elementAt(var12))));
            }

            var12 = var5;
            var10 = var4.keys();

            while(var10.hasMoreElements()) {
               Object var13 = var10.nextElement();
               NameValuePair var15 = (NameValuePair)var4.get(var13);
               var16.addElement(new NameAndTypeEntry(getOrAddConstant((Object)var15.name), getOrAddConstant((Object)var15.descriptor)));
               ++var12;
               var4.put(var13, new Integer(var12));
            }

            methodRefs.addElement(new FieldInfo(10, getOrAddConstant((Object)"staticvector"), getOrAddConstant((Object)"Ljava/util/Vector;")));
            var16.addElement(new FieldRefEntry(1, (Integer)var4.get("staticvectorLjava/util/Vector;")));
            ++var12;
            staticFieldIndex = var12;
            var16.addElement(new MethodRefEntry(4, (Integer)var4.get("<init>()V")));
            ++var12;
            var16.addElement(new MethodRefEntry(3, (Integer)var4.get("<init>(Ljava/util/Vector;)V")));
            ++var12;
            initMethodIndex = var12;
            var16.addElement(new MethodRefEntry(2, (Integer)var4.get("<init>(I)V")));
            var16.addElement(new MethodRefEntry(4, (Integer)var4.get("addElement(Ljava/lang/Object;)V")));
            int var14 = var3.size();
            int var11 = (Integer)var4.get("cinitclone()V");

            for(var12 = 5; var12 <= var14; ++var12) {
               var16.addElement(new MethodRefEntry(var12, var11));
            }

            var11 = (Integer)var4.get("clears()V");

            for(var12 = 5; var12 <= var14; ++var12) {
               var16.addElement(new MethodRefEntry(var12, var11));
            }

            var11 = var6.size();

            for(var12 = 0; var12 < var11; ++var12) {
               var16.addElement(new Utf8Entry((String)var6.elementAt(var12)));
            }

            var11 = getOrAddConstant((Object)"()V");
            methodInfos[0] = new MethodInfo(1, getOrAddConstant((Object)"<init>"), var11);
            methodInfos[0].attributes = new AttributeData[]{generateInitMethod()};
            methodInfos[4] = new MethodInfo(9, getOrAddConstant((Object)"<clinit>"), var11);
            methodInfos[4].attributes = new AttributeData[]{generateStaticInitMethod()};
            var11 = getOrAddConstant((Object)"(I)V");
            methodInfos[1] = new MethodInfo(9, getOrAddConstant((Object)"regClass"), var11);
            methodInfos[1].attributes = new AttributeData[]{generateRegClassMethod()};
            methodInfos[2] = new MethodInfo(17, getOrAddConstant((Object)"cinitclones"), var11);
            methodInfos[2].attributes = new AttributeData[]{generateCinitCloneMethod()};
            methodInfos[3] = new MethodInfo(17, getOrAddConstant((Object)"clears"), var11);
            methodInfos[3].attributes = new AttributeData[]{generateClearsMethod()};
         } catch (Exception var9) {
            var9.printStackTrace();
         }

         codeBuffer.reset();

         try {
            writeClassFile(codeWriter);
            return codeBuffer.toByteArray();
         } catch (Exception var8) {
            var8.printStackTrace();
            return null;
         }
      }
   }

   private static int getOrAddConstant(Object var0) {
      if (!stringMap.containsKey(var0)) {
         int var1 = additionalEntries.size() + baseIndex;
         stringMap.put(var0, var1);
         additionalEntries.addElement(var0);
         return var1;
      } else {
         return (Integer)stringMap.get(var0);
      }
   }

   private static AttributeData generateInitMethod() {
      try {
         codeBuffer.reset();
         codeWriter.writeShort(2);
         codeWriter.writeShort(1);
         codeWriter.writeInt(8);
         codeWriter.writeByte(42);
         codeWriter.writeByte(178);
         codeWriter.writeShort(staticFieldIndex);
         codeWriter.writeByte(183);
         codeWriter.writeShort(initMethodIndex);
         codeWriter.writeByte(177);
         codeWriter.writeShort(0);
         codeWriter.writeShort(0);
         codeWriter.flush();
         byte[] var0 = codeBuffer.toByteArray();
         return new AttributeData(getOrAddConstant((Object)"Code"), var0.length, var0);
      } catch (Exception var1) {
         var1.printStackTrace();
         return null;
      }
   }

   private static AttributeData generateStaticInitMethod() {
      try {
         codeBuffer.reset();
         codeWriter.writeShort(2);
         codeWriter.writeShort(0);
         codeWriter.writeInt(11);
         codeWriter.writeByte(187);
         codeWriter.writeShort(4);
         codeWriter.writeByte(89);
         codeWriter.writeByte(183);
         codeWriter.writeShort(initMethodIndex - 1);
         codeWriter.writeByte(179);
         codeWriter.writeShort(staticFieldIndex);
         codeWriter.writeByte(177);
         codeWriter.writeShort(0);
         codeWriter.writeShort(0);
         codeWriter.flush();
         byte[] var0 = codeBuffer.toByteArray();
         return new AttributeData(getOrAddConstant((Object)"Code"), var0.length, var0);
      } catch (Exception var1) {
         var1.printStackTrace();
         return null;
      }
   }

   private static AttributeData generateRegClassMethod() {
      try {
         codeBuffer.reset();
         codeWriter.writeShort(4);
         codeWriter.writeShort(1);
         codeWriter.writeInt(15);
         codeWriter.writeByte(178);
         codeWriter.writeShort(staticFieldIndex);
         codeWriter.writeByte(187);
         codeWriter.writeShort(2);
         codeWriter.writeByte(89);
         codeWriter.writeByte(26);
         codeWriter.writeByte(183);
         codeWriter.writeShort(initMethodIndex + 1);
         codeWriter.writeByte(182);
         codeWriter.writeShort(initMethodIndex + 2);
         codeWriter.writeByte(177);
         codeWriter.writeInt(0);
         codeWriter.flush();
         byte[] var0 = codeBuffer.toByteArray();
         return new AttributeData(getOrAddConstant((Object)"Code"), var0.length, var0);
      } catch (Exception var1) {
         var1.printStackTrace();
         return null;
      }
   }

   private static AttributeData generateCinitCloneMethod() {
      try {
         codeBuffer.reset();
         stackMapBuffer.reset();
         int var0 = classList.size() - 4;
         stackMapWriter.writeShort(getOrAddConstant((Object)"StackMap"));
         stackMapWriter.writeInt(var0 * 10 + 12);
         stackMapWriter.writeShort(var0 + 1);
         codeWriter.writeShort(1);
         codeWriter.writeShort(2);
         int var1 = (var0 << 3) + 16;
         codeWriter.writeInt(var1);
         codeWriter.writeByte(27);
         codeWriter.writeByte(170);
         int var2 = (var0 << 2) + 15;
         var1 -= 2;
         codeWriter.writeShort(0);
         codeWriter.writeInt(var1);
         codeWriter.writeInt(0);
         codeWriter.writeInt(var0 - 1);

         int var3;
         for(var3 = 0; var3 < var0; ++var3) {
            codeWriter.writeInt(var2);
            stackMapWriter.writeShort(var2 + 1);
            var2 += 4;
            stackMapWriter.writeShort(2);
            stackMapWriter.writeByte(7);
            stackMapWriter.writeShort(1);
            stackMapWriter.writeByte(1);
            stackMapWriter.writeShort(0);
         }

         stackMapWriter.writeShort(var1 + 1);
         stackMapWriter.writeShort(2);
         stackMapWriter.writeByte(7);
         stackMapWriter.writeShort(1);
         stackMapWriter.writeByte(1);
         stackMapWriter.writeShort(0);
         var3 = var0 + initMethodIndex + 3;

         for(var0 = initMethodIndex + 3; var0 < var3; ++var0) {
            codeWriter.writeByte(184);
            codeWriter.writeShort(var0);
            codeWriter.writeByte(177);
         }

         codeWriter.writeShort(0);
         codeWriter.writeShort(1);
         codeWriter.write(stackMapBuffer.toByteArray());
         codeWriter.flush();
         byte[] var5 = codeBuffer.toByteArray();
         return new AttributeData(getOrAddConstant((Object)"Code"), var5.length, var5);
      } catch (Exception var4) {
         var4.printStackTrace();
         return null;
      }
   }

   private static AttributeData generateClearsMethod() {
      try {
         codeBuffer.reset();
         stackMapBuffer.reset();
         int var0 = classList.size() - 4;
         stackMapWriter.writeShort(getOrAddConstant((Object)"StackMap"));
         stackMapWriter.writeInt(var0 * 10 + 12);
         stackMapWriter.writeShort(var0 + 1);
         codeWriter.writeShort(1);
         codeWriter.writeShort(2);
         int var1 = (var0 << 3) + 16;
         codeWriter.writeInt(var1);
         codeWriter.writeByte(27);
         codeWriter.writeByte(170);
         int var2 = (var0 << 2) + 15;
         var1 -= 2;
         codeWriter.writeShort(0);
         codeWriter.writeInt(var1);
         codeWriter.writeInt(0);
         codeWriter.writeInt(var0 - 1);

         int var3;
         for(var3 = 0; var3 < var0; ++var3) {
            codeWriter.writeInt(var2);
            stackMapWriter.writeShort(var2 + 1);
            var2 += 4;
            stackMapWriter.writeShort(2);
            stackMapWriter.writeByte(7);
            stackMapWriter.writeShort(1);
            stackMapWriter.writeByte(1);
            stackMapWriter.writeShort(0);
         }

         stackMapWriter.writeShort(var1 + 1);
         stackMapWriter.writeShort(2);
         stackMapWriter.writeByte(7);
         stackMapWriter.writeShort(1);
         stackMapWriter.writeByte(1);
         stackMapWriter.writeShort(0);
         var3 = (var0 << 1) + initMethodIndex + 3;

         for(var0 += initMethodIndex + 3; var0 < var3; ++var0) {
            codeWriter.writeByte(184);
            codeWriter.writeShort(var0);
            codeWriter.writeByte(177);
         }

         codeWriter.writeShort(0);
         codeWriter.writeShort(1);
         codeWriter.write(stackMapBuffer.toByteArray());
         codeWriter.flush();
         byte[] var5 = codeBuffer.toByteArray();
         return new AttributeData(getOrAddConstant((Object)"Code"), var5.length, var5);
      } catch (Exception var4) {
         var4.printStackTrace();
         return null;
      }
   }

   static {
      codeWriter = new DataOutputStream(codeBuffer);
      stackMapBuffer = new ByteArrayOutputStream();
      stackMapWriter = new DataOutputStream(stackMapBuffer);
   }
}
