package mzmod.compress;

public final class Deflater {
   private int compressionLevel;
   private int state;
   private int checksum;
   private boolean hasHeader;
   private byte[] window = new byte[65536];
   private byte[] inputBuffer;
   private short[] hashHead = new short[32768];
   private short[] hashPrev = new short[32768];
   private int hashValue;
   private int matchStart;
   private int lookahead;
   private boolean hasPrevLiteral;
   private int matchLen;
   private int prevMatchStart;
   private int prevMatchLen;
   private int maxChainLength;
   private int goodMatchLen;
   private int maxLazyMatchLen;
   private int maxInsertLen;
   private int inputPos;
   private int inputEnd;
   private int inputOffset;
   private int compressionMode;
   private static final byte[] BL_ORDER = new byte[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
   private static final byte[] BIT_REVERSE_TABLE = new byte[]{0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15};
   private static short[] FIXED_LITERAL_CODES = new short[286];
   private static short[] FIXED_DISTANCE_CODES;
   private static byte[] FIXED_LITERAL_LENGTHS = new byte[286];
   private static byte[] FIXED_DISTANCE_LENGTHS;
   private HuffmanEncoder literalEncoder = new HuffmanEncoder(286, 257, 15, this);
   private HuffmanEncoder distanceEncoder = new HuffmanEncoder(30, 1, 15, this);
   private HuffmanEncoder bitLengthEncoder = new HuffmanEncoder(19, 4, 7, this);
   private short[] pendingDistances = new short[16384];
   private byte[] pendingLengths = new byte[16384];
   private int pendingCount;
   private int pendingExtraBits;
   private byte[] outputBuffer = new byte[65536];
   private int outputWritePos;
   private int outputReadPos;
   private int bitBuffer;
   private int bitCount;

   public Deflater() {
      this.matchLen = this.prevMatchLen = 1;
      this.hasHeader = true;
      this.compressionMode = 0;
      this.setCompressionLevel(0);
      this.reset();
   }

   public final void reset() {
      this.state = this.hasHeader ? 16 : 0;
      this.checksum = 0;
      this.outputWritePos = this.outputReadPos = this.bitBuffer = this.bitCount = 0;
      Deflater var1 = this;
      this.matchLen = this.prevMatchLen = 1;
      this.lookahead = 0;
      this.hasPrevLiteral = false;
      this.prevMatchLen = 2;

      for(int var2 = 32767; var2 >= 0; --var2) {
         var1.hashHead[var2] = 0;
         var1.hashPrev[var2] = 0;
      }

      var1.initBlock();
   }

   public final int getChecksum() {
      return this.checksum;
   }

   public final void finish() {
      this.state |= 12;
   }

   public final boolean isFinished() {
      return this.state == 30 && this.isOutputEmpty();
   }

   public final boolean needsInput() {
      return this.inputEnd == this.inputPos;
   }

   public final void setInput(byte[] var1, int var2, int var3) {
      this.inputBuffer = var1;
      this.inputPos = var2;
      this.inputEnd = var2 + var3;
   }

   public final void setCompressionLevel(int var1) {
      if (this.compressionLevel != var1) {
         this.compressionLevel = var1;
         this.maxInsertLen = 32;
         this.maxLazyMatchLen = 258;
         this.goodMatchLen = 258;
         this.maxChainLength = 4096;
         if (2 != this.compressionMode) {
            switch (this.compressionMode) {
               case 0:
                  if (this.prevMatchLen > this.matchLen) {
                     this.compressStoredBlock(this.window, this.matchLen, this.prevMatchLen - this.matchLen, false);
                     this.matchLen = this.prevMatchLen;
                  }

                  this.updateHash();
                  break;
               case 1:
                  if (this.prevMatchLen > this.matchLen) {
                     this.compressDynamicBlock(this.window, this.matchLen, this.prevMatchLen - this.matchLen, false);
                     this.matchLen = this.prevMatchLen;
                  }
            }

            this.compressionMode = 2;
         }
      }

   }

   public final int deflate(byte[] var1, int var2, int var3) {
      int var4 = var3;
      if (this.state < 16) {
         int var7;
         if ((var7 = this.compressionLevel - 1 >> 1) < 0 || var7 > 3) {
            var7 = 3;
         }

         int var6 = 30720 | var7 << 6;
         if ((this.state & 1) != 0) {
            var6 |= 32;
         }

         var6 += 31 - var6 % 31;
         Deflater var8;
         (var8 = this).outputBuffer[var8.outputWritePos++] = (byte)(var6 >> 8);
         var8.outputBuffer[var8.outputWritePos++] = (byte)var6;
         this.state = 16 | this.state & 12;
      }

      while(true) {
         int var17 = var3;
         if (this.bitCount >= 8) {
            this.outputBuffer[this.outputWritePos++] = (byte)this.bitBuffer;
            this.bitBuffer = this.bitBuffer >> 8 & 16777215;
            this.bitCount -= 8;
         }

         if (var3 > this.outputWritePos - this.outputReadPos) {
            var17 = this.outputWritePos - this.outputReadPos;
            System.arraycopy(this.outputBuffer, this.outputReadPos, var1, var2, var17);
            this.outputReadPos = this.outputWritePos = 0;
         } else {
            System.arraycopy(this.outputBuffer, this.outputReadPos, var1, var2, var3);
            this.outputReadPos += var3;
         }

         var2 += var17;
         this.checksum += var17;
         if ((var3 -= var17) == 0 || this.state == 30) {
            return var4 - var3;
         }

         boolean var10001 = (this.state & 4) != 0;
         boolean var16 = (this.state & 8) != 0;
         boolean var15 = var10001;
         Deflater var5 = this;
         boolean var18 = false;

         do {
            Deflater var9 = var5;
            if (var5.prevMatchLen >= 65274) {
               var5.slideWindow();
            }

            while(var9.lookahead < 262 && var9.inputPos < var9.inputEnd) {
               int var10 = Math.min(65536 - var9.lookahead - var9.prevMatchLen, var9.inputEnd - var9.inputPos);
               System.arraycopy(var9.inputBuffer, var9.inputPos, var9.window, var9.prevMatchLen + var9.lookahead, var10);
               var9.inputPos += var10;
               var9.lookahead += var10;
            }

            if (var9.lookahead > 2) {
               var9.updateHash();
            }

            boolean var19 = var15 && var5.inputPos == var5.inputEnd;
            int var11;
            boolean var10000;
            boolean var20;
            boolean var21;
            switch (var5.compressionMode) {
               case 0:
                  if (!var19 && var5.lookahead == 0) {
                     var10000 = false;
                  } else {
                     label357: {
                        var5.prevMatchLen += var5.lookahead;
                        var5.lookahead = 0;
                        if ((var11 = var5.prevMatchLen - var5.matchLen) >= 65531 || var5.matchLen < 32768 && var11 >= 32506 || var19) {
                           var21 = var16;
                           if (var11 > 65531) {
                              var11 = 65531;
                              var21 = false;
                           }

                           var5.compressStoredBlock(var5.window, var5.matchLen, var11, var21);
                           var5.matchLen += var11;
                           if (var21) {
                              var10000 = false;
                              break label357;
                           }
                        }

                        var10000 = true;
                     }
                  }

                  var18 = var10000;
                  break;
               case 1:
                  var20 = var19;
                  var9 = var5;
                  if (var5.lookahead < 262 && !var19) {
                     var10000 = false;
                  } else {
                     label340: {
                        while(var9.lookahead >= 262 || var20) {
                           if (var9.lookahead == 0) {
                              var9.compressDynamicBlock(var9.window, var9.matchLen, var9.prevMatchLen - var9.matchLen, var16);
                              var9.matchLen = var9.prevMatchLen;
                              var10000 = false;
                              break label340;
                           }

                           if (var9.prevMatchLen > 65274) {
                              var9.slideWindow();
                           }

                           if (var9.lookahead >= 3 && (var11 = var9.findLongestMatch()) != 0 && var9.prevMatchLen - var11 <= 32506 && var9.matchFound(var11)) {
                              var9.emitMatch(var9.prevMatchLen - var9.matchStart, var9.prevMatchLen);
                              var9.lookahead -= var9.prevMatchLen;
                              if (var9.prevMatchLen <= var9.maxLazyMatchLen && var9.lookahead >= 3) {
                                 while(--var9.prevMatchLen > 0) {
                                    ++var9.prevMatchLen;
                                    var9.findLongestMatch();
                                 }

                                 ++var9.prevMatchLen;
                              } else {
                                 var9.prevMatchLen += var9.prevMatchLen;
                                 if (var9.lookahead >= 2) {
                                    var9.updateHash();
                                 }
                              }

                              var9.prevMatchLen = 2;
                           } else {
                              var9.emitLiteral(var9.window[var9.prevMatchLen] & 255);
                              ++var9.prevMatchLen;
                              --var9.lookahead;
                              if (var9.isBlockFull()) {
                                 var21 = var16 && var9.lookahead == 0;
                                 var9.compressDynamicBlock(var9.window, var9.matchLen, var9.prevMatchLen - var9.matchLen, var21);
                                 var9.matchLen = var9.prevMatchLen;
                                 if (var21) {
                                    var10000 = false;
                                    break label340;
                                 }
                                 break;
                              }
                           }
                        }

                        var10000 = true;
                     }
                  }

                  var18 = var10000;
                  break;
               case 2:
                  var20 = var19;
                  var9 = var5;
                  if (var5.lookahead < 262 && !var19) {
                     var10000 = false;
                  } else {
                     label343: {
                        while(var9.lookahead >= 262 || var20) {
                           if (var9.lookahead == 0) {
                              if (var9.hasPrevLiteral) {
                                 var9.emitLiteral(var9.window[var9.prevMatchLen - 1] & 255);
                              }

                              var9.hasPrevLiteral = false;
                              var9.compressDynamicBlock(var9.window, var9.matchLen, var9.prevMatchLen - var9.matchLen, var16);
                              var9.matchLen = var9.prevMatchLen;
                              var10000 = false;
                              break label343;
                           }

                           if (var9.prevMatchLen >= 65274) {
                              var9.slideWindow();
                           }

                           var11 = var9.matchStart;
                           int var12 = var9.prevMatchLen;
                           int var13;
                           if (var9.lookahead >= 3 && (var13 = var9.findLongestMatch()) != 0 && var9.prevMatchLen - var13 <= 32506 && var9.matchFound(var13) && var9.prevMatchLen <= 5 && var9.prevMatchLen == 3 && var9.prevMatchLen - var9.matchStart > 4096) {
                              var9.prevMatchLen = 2;
                           }

                           if (var12 >= 3 && var9.prevMatchLen <= var12) {
                              var9.emitMatch(var9.prevMatchLen - 1 - var11, var12);
                              var12 -= 2;

                              do {
                                 ++var9.prevMatchLen;
                                 --var9.lookahead;
                                 if (var9.lookahead >= 3) {
                                    var9.findLongestMatch();
                                 }

                                 --var12;
                              } while(var12 > 0);

                              ++var9.prevMatchLen;
                              --var9.lookahead;
                              var9.hasPrevLiteral = false;
                              var9.prevMatchLen = 2;
                           } else {
                              if (var9.hasPrevLiteral) {
                                 var9.emitLiteral(var9.window[var9.prevMatchLen - 1] & 255);
                              }

                              var9.hasPrevLiteral = true;
                              ++var9.prevMatchLen;
                              --var9.lookahead;
                           }

                           if (var9.isBlockFull()) {
                              var13 = var9.prevMatchLen - var9.matchLen;
                              if (var9.hasPrevLiteral) {
                                 --var13;
                              }

                              var18 = var16 && var9.lookahead == 0 && !var9.hasPrevLiteral;
                              var9.compressDynamicBlock(var9.window, var9.matchLen, var13, var18);
                              var9.matchLen += var13;
                              if (var18) {
                                 var10000 = false;
                                 break label343;
                              }
                              break;
                           }
                        }

                        var10000 = true;
                     }
                  }

                  var18 = var10000;
            }
         } while(var5.isOutputEmpty() && var18);

         if (!var18) {
            if (this.state == 16) {
               return var4 - var3;
            }

            if (this.state != 20) {
               if (this.state == 28) {
                  this.flushBitBuffer();
                  this.state = 30;
               }
            } else {
               if (this.compressionLevel != 0) {
                  for(int var14 = 8 + (-this.bitCount & 7); var14 > 0; var14 -= 10) {
                     this.writeBits(2, 10);
                  }
               }

               this.state = 16;
            }
         }
      }
   }

   private void updateHash() {
      this.hashValue = this.window[this.prevMatchLen] << 5 ^ this.window[this.prevMatchLen + 1];
   }

   private int findLongestMatch() {
      int var2 = (this.hashValue << 5 ^ this.window[this.prevMatchLen + 2]) & 32767;
      short var1;
      this.hashPrev[this.prevMatchLen & 32767] = var1 = this.hashHead[var2];
      this.hashHead[var2] = (short)this.prevMatchLen;
      this.hashValue = var2;
      return var1 & '\uffff';
   }

   private void slideWindow() {
      System.arraycopy(this.window, 32768, this.window, 0, 32768);
      this.matchStart -= 32768;
      this.prevMatchLen -= 32768;
      this.matchLen -= 32768;

      for(int var2 = 0; var2 < 32768; ++var2) {
         int var1 = this.hashHead[var2] & '\uffff';
         this.hashHead[var2] = var1 >= 32768 ? (short)(var1 - 32768) : 0;
         var1 = this.hashPrev[var2] & '\uffff';
         this.hashPrev[var2] = var1 >= 32768 ? (short)(var1 - 32768) : 0;
      }

   }

   private boolean matchFound(int var1) {
      int var2 = this.maxChainLength;
      int var3 = this.goodMatchLen;
      short[] var4 = this.hashPrev;
      int var5 = this.prevMatchLen;
      int var7 = this.prevMatchLen + this.prevMatchLen;
      int var8 = Math.max(this.prevMatchLen, 2);
      int var9 = Math.max(this.prevMatchLen - 32506, 0);
      int var10 = var5 + 257;
      byte var11 = this.window[var7 - 1];
      byte var12 = this.window[var7];
      if (var8 >= this.maxInsertLen) {
         var2 >>= 2;
      }

      if (var3 > this.lookahead) {
         var3 = this.lookahead;
      }

      do {
         if (this.window[var1 + var8] == var12 && this.window[var1 + var8 - 1] == var11 && this.window[var1] == this.window[var5] && this.window[var1 + 1] == this.window[var5 + 1]) {
            int var6 = var1 + 2;
            var5 += 2;

            byte var10000;
            do {
               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
               if (var10000 != this.window[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.window[var5];
               ++var6;
            } while(var10000 == this.window[var6] && var5 < var10);

            if (var5 > var7) {
               this.matchStart = var1;
               var7 = var5;
               if ((var8 = var5 - this.prevMatchLen) >= var3) {
                  break;
               }

               var11 = this.window[var5 - 1];
               var12 = this.window[var5];
            }

            var5 = this.prevMatchLen;
         }

         if ((var1 = var4[var1 & 32767] & '\uffff') <= var9) {
            break;
         }

         --var2;
      } while(var2 != 0);

      this.prevMatchLen = Math.min(var8, this.lookahead);
      return this.prevMatchLen >= 3;
   }

   static short reverseBits(int var0) {
      return (short)(BIT_REVERSE_TABLE[var0 & 15] << 12 | BIT_REVERSE_TABLE[var0 >> 4 & 15] << 8 | BIT_REVERSE_TABLE[var0 >> 8 & 15] << 4 | BIT_REVERSE_TABLE[var0 >> 12]);
   }

   private void initBlock() {
      this.pendingCount = 0;
      this.pendingExtraBits = 0;
      this.literalEncoder.reset();
      this.distanceEncoder.reset();
      this.bitLengthEncoder.reset();
   }

   private static int getLengthCode(int var0) {
      if (var0 == 255) {
         return 285;
      } else {
         int var1;
         for(var1 = 257; var0 >= 8; var0 >>= 1) {
            var1 += 4;
         }

         return var1 + var0;
      }
   }

   private static int getDistanceCode(int var0) {
      int var1;
      for(var1 = 0; var0 >= 4; var0 >>= 1) {
         var1 += 2;
      }

      return var1 + var0;
   }

   private void emitBlock() {
      for(int var1 = 0; var1 < this.pendingCount; ++var1) {
         int var2 = this.pendingLengths[var1] & 255;
         int var3;
         int var10000 = var3 = this.pendingDistances[var1];
         --var3;
         if (var10000 != 0) {
            int var4 = getLengthCode(var2);
            this.literalEncoder.writeCode(var4);
            if ((var4 = (var4 - 261) / 4) > 0 && var4 <= 5) {
               this.writeBits(var2 & (1 << var4) - 1, var4);
            }

            var2 = getDistanceCode(var3);
            this.distanceEncoder.writeCode(var2);
            if ((var4 = var2 / 2 - 1) > 0) {
               this.writeBits(var3 & (1 << var4) - 1, var4);
            }
         } else {
            this.literalEncoder.writeCode(var2);
         }
      }

      this.literalEncoder.writeCode(256);
   }

   private void compressStoredBlock(byte[] var1, int var2, int var3, boolean var4) {
      this.writeBits(var4 ? 1 : 0, 3);
      this.flushBitBuffer();
      this.writeShort(var3);
      this.writeShort(~var3);
      System.arraycopy(var1, var2, this.outputBuffer, this.outputWritePos, var3);
      this.outputWritePos += var3;
      this.initBlock();
   }

   private void compressDynamicBlock(byte[] var1, int var2, int var3, boolean var4) {
      ++this.literalEncoder.frequencies[256];
      this.literalEncoder.buildTree();
      this.distanceEncoder.buildTree();
      this.literalEncoder.countBitLengthFrequencies(this.bitLengthEncoder);
      this.distanceEncoder.countBitLengthFrequencies(this.bitLengthEncoder);
      this.bitLengthEncoder.buildTree();
      int var5 = 4;

      int var6;
      for(var6 = 18; var6 > var5; --var6) {
         if (this.bitLengthEncoder.codeLengths[BL_ORDER[var6]] > 0) {
            var5 = var6 + 1;
         }
      }

      var6 = 14 + var5 * 3 + this.bitLengthEncoder.getWeightedPathLength() + this.literalEncoder.getWeightedPathLength() + this.distanceEncoder.getWeightedPathLength() + this.pendingExtraBits;
      int var7 = this.pendingExtraBits;

      int var8;
      for(var8 = 0; var8 < 286; ++var8) {
         var7 += this.literalEncoder.frequencies[var8] * FIXED_LITERAL_LENGTHS[var8];
      }

      for(var8 = 0; var8 < 30; ++var8) {
         var7 += this.distanceEncoder.frequencies[var8] * FIXED_DISTANCE_LENGTHS[var8];
      }

      if (var6 >= var7) {
         var6 = var7;
      }

      if (var2 >= 0 && var3 + 4 < var6 >> 3) {
         this.compressStoredBlock(var1, var2, var3, var4);
      } else if (var6 == var7) {
         this.writeBits(2 + (var4 ? 1 : 0), 3);
         this.literalEncoder.setCodes(FIXED_LITERAL_CODES, FIXED_LITERAL_LENGTHS);
         this.distanceEncoder.setCodes(FIXED_DISTANCE_CODES, FIXED_DISTANCE_LENGTHS);
         this.emitBlock();
         this.initBlock();
      } else {
         this.writeBits(4 + (var4 ? 1 : 0), 3);
         var2 = var5;
         Deflater var9 = this;
         this.bitLengthEncoder.buildCodes();
         this.literalEncoder.buildCodes();
         this.distanceEncoder.buildCodes();
         this.writeBits(this.literalEncoder.maxCode - 257, 5);
         this.writeBits(this.distanceEncoder.maxCode - 1, 5);
         this.writeBits(var5 - 4, 4);

         for(var3 = 0; var3 < var2; ++var3) {
            var9.writeBits(var9.bitLengthEncoder.codeLengths[BL_ORDER[var3]], 3);
         }

         var9.literalEncoder.writeBitLengths(var9.bitLengthEncoder);
         var9.distanceEncoder.writeBitLengths(var9.bitLengthEncoder);
         this.emitBlock();
         this.initBlock();
      }
   }

   private boolean isBlockFull() {
      return this.pendingCount == 16384;
   }

   private boolean emitLiteral(int var1) {
      this.pendingDistances[this.pendingCount] = 0;
      this.pendingLengths[this.pendingCount++] = (byte)var1;
      ++this.literalEncoder.frequencies[var1];
      return this.pendingCount == 16384;
   }

   private boolean emitMatch(int var1, int var2) {
      this.pendingDistances[this.pendingCount] = (short)var1;
      this.pendingLengths[this.pendingCount++] = (byte)(var2 - 3);
      var2 = getLengthCode(var2 - 3);
      ++this.literalEncoder.frequencies[var2];
      if (var2 >= 265 && var2 < 285) {
         this.pendingExtraBits += (var2 - 261) / 4;
      }

      var1 = getDistanceCode(var1 - 1);
      ++this.distanceEncoder.frequencies[var1];
      if (var1 >= 4) {
         this.pendingExtraBits += var1 / 2 - 1;
      }

      return this.pendingCount == 16384;
   }

   private boolean isOutputEmpty() {
      return this.outputWritePos == 0;
   }

   private void writeShort(int var1) {
      this.outputBuffer[this.outputWritePos++] = (byte)var1;
      this.outputBuffer[this.outputWritePos++] = (byte)(var1 >> 8);
   }

   private void flushBitBuffer() {
      if (this.bitCount > 0) {
         this.outputBuffer[this.outputWritePos++] = (byte)this.bitBuffer;
         if (this.bitCount > 8) {
            this.outputBuffer[this.outputWritePos++] = (byte)(this.bitBuffer >> 8);
         }
      }

      this.bitBuffer = 0;
      this.bitCount = 0;
   }

   public final void writeBits(int var1, int var2) {
      this.bitBuffer |= var1 << this.bitCount;
      this.bitCount += var2;
      if (this.bitCount >= 16) {
         this.outputBuffer[this.outputWritePos++] = (byte)this.bitBuffer;
         this.outputBuffer[this.outputWritePos++] = (byte)(this.bitBuffer >> 8);
         this.bitBuffer = this.bitBuffer >> 16 & '\uffff';
         this.bitCount -= 16;
      }

   }

   static {
      int var0;
      for(var0 = 0; var0 < 144; FIXED_LITERAL_LENGTHS[var0++] = 8) {
         FIXED_LITERAL_CODES[var0] = reverseBits(var0 + 48 << 8);
      }

      while(var0 < 256) {
         FIXED_LITERAL_CODES[var0] = reverseBits(var0 + 256 << 7);
         FIXED_LITERAL_LENGTHS[var0++] = 9;
      }

      while(var0 < 280) {
         FIXED_LITERAL_CODES[var0] = reverseBits(var0 + -256 << 9);
         FIXED_LITERAL_LENGTHS[var0++] = 7;
      }

      while(var0 < 286) {
         FIXED_LITERAL_CODES[var0] = reverseBits(var0 + -88 << 8);
         FIXED_LITERAL_LENGTHS[var0++] = 8;
      }

      FIXED_DISTANCE_CODES = new short[30];
      FIXED_DISTANCE_LENGTHS = new byte[30];

      for(var0 = 0; var0 < 30; ++var0) {
         FIXED_DISTANCE_CODES[var0] = reverseBits(var0 << 11);
         FIXED_DISTANCE_LENGTHS[var0] = 5;
      }

   }
}
