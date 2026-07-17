package mzmod.zip;

import java.io.InputStream;

public final class ByteArrayReader extends InputStream {
   private int position = 0;
   public byte[] buffer;

   public final void seek(int var1) {
      this.position = var1;
   }

   public ByteArrayReader(byte[] var1) {
      this.buffer = var1;
   }

   public final int getPosition() {
      return this.position;
   }

   public final void close() {
      this.buffer = null;
   }

   public final void readBytes(byte[] var1, int var2) {
      System.arraycopy(this.buffer, this.position, var1, 0, var2);
      this.position += var2;
   }

   public final long skip(long var1) {
      this.position = (int)((long)this.position + var1);
      return var1;
   }

   public final int read(byte[] var1, int var2, int var3) {
      System.arraycopy(this.buffer, this.position, var1, var2, var3);
      this.position += var3;
      return var3;
   }

   public final int read(byte[] var1) {
      return this.read(var1, 0, var1.length);
   }

   public final int readInt() {
      return (this.buffer[this.position++] & 255) << 24 | (this.buffer[this.position++] & 255) << 16 | (this.buffer[this.position++] & 255) << 8 | this.buffer[this.position++] & 255;
   }

   public final int readShort() {
      return this.buffer[this.position++] & 255 | (this.buffer[this.position++] & 255) << 8;
   }

   public final int readLittleEndianInt() {
      return this.buffer[this.position++] & 255 | (this.buffer[this.position++] & 255) << 8 | (this.buffer[this.position++] & 255) << 16 | (this.buffer[this.position++] & 255) << 24;
   }

   public final int read() {
      return this.buffer[this.position++];
   }
}
