package mzmod.zip;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import mzmod.compress.Deflater;

public final class ZipWriter extends OutputStream {
   private Vector entries = new Vector();
   private int crc32;
   private ZipEntryInfo currentEntry = null;
   private int compressionMethod;
   private int uncompressedSize;
   private static int[] crcTable;
   private byte[] outputBuffer;
   private int bufferSize;
   private Deflater deflater;
   private BufferedOutput outputStream;

   public ZipWriter(BufferedOutput var1) {
      Deflater var3 = new Deflater();
      this.outputStream = var1;
      this.outputBuffer = new byte[10240];
      this.bufferSize = 10240;
      this.deflater = var3;
      this.crc32 = 0;
      this.compressionMethod = 8;
       this.deflater.setCompressionLevel(9);
   }

   public final void closeCurrentEntry() {
      this.currentEntry = null;
   }

   public final void putNextEntry(String var1) {
      ZipEntryInfo var6;
      (var6 = new ZipEntryInfo(var1)).versionNeeded = 8;
      var6.compressionMethod = (short)this.compressionMethod;
      long var4 = System.currentTimeMillis();
      Calendar var3 = Calendar.getInstance();
      if (var4 > 0L) {
         var3.setTime(new Date(var4));
      }

      var6.lastModifiedTime = var3.get(1) - 1980 << 25 | var3.get(2) + 1 << 21 | var3.get(5) << 16 | var3.get(11) << 11 | var3.get(12) << 5 | var3.get(13) >> 1;
      this.putNextEntry(var6);
   }

   public final void putNextEntry(ZipEntryInfo var1) {
      if (this.currentEntry != null) {
         this.closeEntry();
      }

      this.currentEntry = var1;
      this.entries.addElement(var1);
      var1.localHeaderOffset = this.outputStream.getPosition();
      this.outputStream.writeInt(67324752);
      this.outputStream.writeShort(var1.compressionMethod == 0 ? 10 : 20);
      this.writeLocalHeader(var1);
      this.outputStream.write(this.currentEntry.getNameBytes());
      this.crc32 = 0;
      this.deflater.reset();
      this.uncompressedSize = 0;
   }

   public final void write(byte[] var1) {
      this.write(var1, 0, var1.length);
   }

   public final void write(int var1) {
      this.write(new byte[]{(byte)var1, 0, 1});
   }

   public final void closeEntry() {
      if (this.currentEntry != null) {
         int var2;
         if (this.compressionMethod == 8) {
            ZipWriter var1 = this;
            this.deflater.finish();

             while(!var1.deflater.isFinished() && (var2 = var1.deflater.deflate(var1.outputBuffer, 0, var1.bufferSize)) > 0) {
               var1.outputStream.write(var1.outputBuffer, 0, var2);
            }
         }

         ZipEntryInfo var3;
         (var3 = this.currentEntry).uncompressedSize = this.uncompressedSize;
         var3.compressedSize = this.compressionMethod == 8 ? this.deflater.getChecksum() : this.uncompressedSize;
         var3.crc32 = this.crc32;
         var2 = this.outputStream.getPosition();
         if (!this.outputStream.seekToPosition(var3.localHeaderOffset + 6)) {
            this.outputStream.writeInt(134695760);
            this.outputStream.writeInt(var3.crc32);
            this.outputStream.writeInt(var3.compressedSize);
            this.outputStream.writeInt(var3.uncompressedSize);
         } else {
            var3.versionNeeded = 0;
            this.writeLocalHeader(var3);
            this.outputStream.setPosition(var2);
         }

         this.outputStream.flush();
         this.currentEntry = null;
      }
   }

   public final void write(byte[] var1, int var2, int var3) {
      if (this.compressionMethod == 0) {
         this.outputStream.write(var1, var2, var3);
      } else {
         this.deflater.setInput(var1, var2, var3);
         ZipWriter var6 = this;

         int var7;
         while(!var6.deflater.needsInput() && (var7 = var6.deflater.deflate(var6.outputBuffer, 0, var6.bufferSize)) > 0) {
            var6.outputStream.write(var6.outputBuffer, 0, var7);
         }
      }

      int var10002 = var2;
      var2 = var3;
      int var8 = var10002;
      byte[] var5 = var1;
      int var9 = ~this.crc32;
      int[] var10 = crcTable;

      while(true) {
         --var2;
         if (var2 < 0) {
            this.crc32 = ~var9;
            this.uncompressedSize += var3;
            return;
         }

         var9 = var10[(var9 ^ var5[var8++]) & 255] ^ var9 >>> 8;
      }
   }

   private void writeLocalHeader(ZipEntryInfo var1) {
      this.outputStream.writeShort(var1.versionNeeded);
      this.outputStream.writeShort(var1.compressionMethod);
      this.outputStream.writeInt(var1.lastModifiedTime);
      this.outputStream.writeInt(var1.crc32);
      this.outputStream.writeInt(var1.compressedSize);
      this.outputStream.writeInt(var1.uncompressedSize);
      this.outputStream.writeShort(var1.nameLength);
      this.outputStream.writeShort(0);
   }

   public final void finish() {
      if (this.entries != null) {
         if (this.currentEntry != null) {
            this.closeEntry();
         }

         int var1 = this.outputStream.getPosition();
         int var2 = 0;
         int var3 = 0;

         ZipEntryInfo var5;
         for(Enumeration var4 = this.entries.elements(); var4.hasMoreElements(); var3 += 46 + var5.nameLength) {
            short var6 = (var5 = (ZipEntryInfo)var4.nextElement()).compressionMethod;
            this.outputStream.writeInt(33639248);
            this.outputStream.writeShort(var6 == 0 ? 10 : 20);
            this.outputStream.writeShort(0);
            this.writeLocalHeader(var5);
            this.outputStream.writeShort(0);
            this.outputStream.writeInt(0);
            this.outputStream.writeInt(0);
            this.outputStream.writeInt(var5.localHeaderOffset);
            this.outputStream.write(var5.getNameBytes());
            ++var2;
         }

         this.outputStream.writeInt(101010256);
         this.outputStream.writeInt(0);
         this.outputStream.writeShort(var2);
         this.outputStream.writeShort(var2);
         this.outputStream.writeInt(var3);
         this.outputStream.writeInt(var1);
         this.outputStream.writeShort(0);
         this.outputStream.flush();
         this.entries = null;
      }
   }

   static {
      int[] var0 = new int[256];

      for(int var1 = 0; var1 < 256; ++var1) {
         int var2 = var1;
         int var3 = 8;

         while(true) {
            --var3;
            if (var3 < 0) {
               var0[var1] = var2;
               break;
            }

            if ((var2 & 1) != 0) {
               var2 = -306674912 ^ var2 >>> 1;
            } else {
               var2 >>>= 1;
            }
         }
      }

      crcTable = var0;
   }
}
