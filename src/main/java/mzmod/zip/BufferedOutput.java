package mzmod.zip;

import java.io.IOException;
import java.io.OutputStream;

public final class BufferedOutput extends OutputStream {
  private OutputStream outputStream;
  private byte[] buffer;
  private int bufferPosition;
  private int totalBytesWritten;

  public BufferedOutput(OutputStream var1) {
    this.outputStream = var1;
    this.buffer = new byte['ꀀ'];
    this.bufferPosition = 0;
    this.totalBytesWritten = 0;
  }

  public final boolean seekToPosition(int var1) {
    int var2;
    if ((var2 = this.totalBytesWritten - var1) <= this.bufferPosition) {
      this.bufferPosition -= var2;
      this.totalBytesWritten = var1;
      return true;
    } else {
      return false;
    }
  }

  public final int getPosition() {
    return this.totalBytesWritten;
  }

  public final void setPosition(int var1) {
    this.bufferPosition += var1 - this.totalBytesWritten;
    this.totalBytesWritten = var1;
  }

  public final void flush() {
    if (this.bufferPosition > 0) {
      try {
        this.outputStream.write(this.buffer, 0, this.bufferPosition);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      this.bufferPosition = 0;
    }
  }

  public final void close() {
    this.flush();
    this.buffer = null;
    try {
      this.outputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public final void write(byte[] var1, int var2, int var3) {
    if (var3 < this.buffer.length - this.bufferPosition) {
      System.arraycopy(var1, var2, this.buffer, this.bufferPosition, var3);
      this.bufferPosition += var3;
    } else {
      this.flush();
      try {
        this.outputStream.write(var1, var2, var3);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    this.totalBytesWritten += var3;
  }

  public final void write(byte[] var1) {
    this.write(var1, 0, var1.length);
  }

  public final void writeShort(int var1) {
    if (this.buffer.length < this.bufferPosition + 2) {
      this.flush();
    }

    this.totalBytesWritten += 2;
    this.buffer[this.bufferPosition++] = (byte) var1;
    this.buffer[this.bufferPosition++] = (byte) (var1 >> 8);
  }

  public final void writeInt(int var1) {
    if (this.buffer.length < this.bufferPosition + 4) {
      this.flush();
    }

    this.totalBytesWritten += 4;
    this.buffer[this.bufferPosition++] = (byte) var1;
    this.buffer[this.bufferPosition++] = (byte) (var1 >> 8);
    this.buffer[this.bufferPosition++] = (byte) (var1 >> 16);
    this.buffer[this.bufferPosition++] = (byte) (var1 >> 24);
  }

  public final void write(int var1) {
    if (this.buffer.length < this.bufferPosition + 1) {
      this.flush();
    }

    ++this.totalBytesWritten;
    this.buffer[this.bufferPosition++] = (byte) var1;
  }
}
