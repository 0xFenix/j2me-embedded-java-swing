package mzmod.zip;

public final class ZipEntryInfo {
   private byte[] nameBytes = null;
   public int nameLength;
   public int lastModifiedTime;
   public int uncompressedSize;
   public int compressedSize;
   public int localHeaderOffset;
   public int crc32;
   public short compressionMethod;
   public String name;
   public int versionNeeded;

   public ZipEntryInfo(String var1) {
      if (var1 != null) {
         this.name = var1.replace('\\', '/');
         this.nameBytes = this.name.getBytes();
         this.nameLength = this.nameBytes.length;
      }

   }

   public final void setNameFromBytes(byte[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         if (var1[var2] == 92) {
            var1[var2] = 47;
         }
      }

      this.nameLength = var1.length;
      this.nameBytes = var1;
      this.name = new String(var1);
   }

   public final byte[] getNameBytes() {
      return this.nameBytes;
   }

   public final String toString() {
      return this.name;
   }
}
