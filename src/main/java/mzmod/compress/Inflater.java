package mzmod.compress;

public final class Inflater {
   private static final int[] LENGTH_BASE = new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258};
   private static final int[] LENGTH_EXTRA_BITS = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0};
   private static final int[] DISTANCE_BASE = new int[]{1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577};
   private static final int[] DISTANCE_EXTRA_BITS = new int[]{0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13};
   private int inflateState;
   private int adlerChecksum;
   private int extraBits;
   private int availableBits;
   private boolean isFinalBlock;
   private int storedBlockLength;
   private int totalBytesIn;
   private int extraLength;
   private int distance;
   private boolean hasHeader = true;
   private HuffmanDecoder literalDecoder;
   private HuffmanDecoder distanceDecoder;
   private byte[] slidingWindow = new byte[32768];
   private int windowWritePos = 0;
   private int windowUsed = 0;
   private static final int[] CODE_LENGTH_ORDER = new int[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
   private byte[] inputBuffer;
   private int inputReadPos;
   private int inputWritePos;
   private HuffmanDecoder codeLengthDecoder;
   private int numLiterals;
   private int numDistances;
   private int numCodeLengths;
   private int totalCodeLengthSymbols;
   private int repeatCount;
   private int repeatSymbol;
   private int codeLengthIndex;
   private byte lastCodeLength;
   private byte[] codeLengthBuffer;
   private int bitBuffer = 0;
   private int bitCount = 0;
   private int bytesRead = 0;

   public Inflater() {
      this.inflateState = this.hasHeader ? 2 : 0;
   }

   public final int inflate(byte[] var1, int var2, int var3) {
      if (var3 <= 0) {
         return 0;
      } else {
         int var4 = 0;

         boolean var10000;
         do {
            do {
               int var7;
               int var9;
               int var10;
               int var11;
               if (this.inflateState != 11) {
                  int var8 = var3;
                  var7 = var2;
                  var9 = this.windowWritePos;
                  if (var3 > this.windowUsed) {
                     var8 = this.windowUsed;
                  } else {
                     var9 = this.windowWritePos - this.windowUsed + var3 & 32767;
                  }

                  var10 = var8;
                  if ((var11 = var8 - var9) > 0) {
                     System.arraycopy(this.slidingWindow, 32768 - var11, var1, var2, var11);
                     var7 = var2 + var11;
                     var8 = var9;
                  }

                  System.arraycopy(this.slidingWindow, var9 - var8, var1, var7, var8);
                  this.windowUsed -= var10;
                  var2 += var10;
                  var4 += var10;
                  this.totalBytesIn += var10;
                  if ((var3 -= var10) == 0) {
                     return var4;
                  }
               }

               Inflater var6;
               int var13;
               Inflater var14;
               label304:
               switch (this.inflateState) {
                  case 0:
                     if ((var9 = this.peekBits(16)) >= 0) {
                        this.skipBits(16);
                        if (((var9 << 8 | var9 >> 8) & '\uffff' & 32) == 0) {
                           this.inflateState = 2;
                        } else {
                           this.inflateState = 1;
                           this.extraBits = 32;
                        }

                        var10000 = true;
                        continue;
                     }
                     break;
                  case 1:
                     for(var14 = this; var14.extraBits > 0 && (var9 = var14.peekBits(8)) >= 0; var14.extraBits -= 8) {
                        var14.skipBits(8);
                        var14.adlerChecksum = var14.adlerChecksum << 8 | var9;
                     }

                     var10000 = false;
                     continue;
                  case 2:
                     if (this.isFinalBlock) {
                        if (!this.hasHeader) {
                           this.alignToByteBoundary();
                           this.extraBits = 32;
                           this.inflateState = 11;
                           var10000 = true;
                           continue;
                        }

                        this.inflateState = 12;
                     } else if ((var13 = this.peekBits(3)) >= 0) {
                        this.skipBits(3);
                        if ((var13 & 1) != 0) {
                           this.isFinalBlock = true;
                        }

                        switch (var13 >> 1) {
                           case 0:
                              this.alignToByteBoundary();
                              this.inflateState = 3;
                              break;
                           case 1:
                               this.literalDecoder = HuffmanDecoder.LITERAL_LENGTH_TABLE;
                               this.distanceDecoder = HuffmanDecoder.DISTANCE_TABLE;
                              this.inflateState = 7;
                              break;
                           case 2:
                              this.decodeDynamicHeader();
                              this.inflateState = 6;
                        }

                        var10000 = true;
                        continue;
                     }
                     break;
                  case 3:
                     if ((this.storedBlockLength = this.peekBits(16)) < 0) {
                        break;
                     }

                     this.skipBits(16);
                     this.inflateState = 4;
                  case 4:
                     if (this.peekBits(16) < 0) {
                        break;
                     }

                     this.skipBits(16);
                     this.inflateState = 5;
                  case 5:
                     var9 = this.storedBlockLength;
                     var9 = Math.min(Math.min(var9, 32768 - this.windowUsed), (var6 = this).bitCount - var6.bitBuffer + (var6.bytesRead >> 3));
                     var11 = 32768 - this.windowWritePos;
                     if (var9 > var11) {
                        if ((var10 = this.copyRawBytes(this.slidingWindow, this.windowWritePos, var11)) == var11) {
                           var10 += this.copyRawBytes(this.slidingWindow, 0, var9 - var11);
                        }
                     } else {
                        var10 = this.copyRawBytes(this.slidingWindow, this.windowWritePos, var9);
                     }

                     this.windowWritePos = this.windowWritePos + var10 & 32767;
                     this.windowUsed += var10;
                     this.storedBlockLength -= var10;
                     if (this.storedBlockLength == 0) {
                        this.inflateState = 2;
                        var10000 = true;
                     } else {
                        var10000 = this.bitBuffer != this.bitCount;
                     }
                     continue;
                  case 6:
                     var14 = this;

                     label301:
                     while(true) {
                        label339: {
                           label292:
                           while(true) {
                              switch (var14.codeLengthIndex) {
                                 case 0:
                                    var14.numLiterals = var14.peekBits(5);
                                    if (var14.numLiterals < 0) {
                                       var10000 = false;
                                       break label301;
                                    }

                                    var14.numLiterals += 257;
                                    var14.skipBits(5);
                                    var14.codeLengthIndex = 1;
                                 case 1:
                                    var14.numDistances = var14.peekBits(5);
                                    if (var14.numDistances < 0) {
                                       var10000 = false;
                                       break label301;
                                    }

                                    ++var14.numDistances;
                                    var14.skipBits(5);
                                    var14.totalCodeLengthSymbols = var14.numLiterals + var14.numDistances;
                                    var14.codeLengthBuffer = new byte[var14.totalCodeLengthSymbols];
                                    var14.codeLengthIndex = 2;
                                 case 2:
                                    var14.numCodeLengths = var14.peekBits(4);
                                    if (var14.numCodeLengths < 0) {
                                       var10000 = false;
                                       break label301;
                                    }

                                    var14.numCodeLengths += 4;
                                    var14.skipBits(4);
                                    var14.codeLengthBuffer = new byte[19];
                                    var14.codeLengthIndex = 3;
                                 case 3:
                                    break;
                                 case 4:
                                    break label292;
                                 case 5:
                                    break label339;
                                 default:
                                    continue;
                              }

                              while(var14.codeLengthIndex < var14.numCodeLengths) {
                                 if ((var9 = var14.peekBits(3)) < 0) {
                                    var10000 = false;
                                    break label301;
                                 }

                                 var14.skipBits(3);
                                 var14.codeLengthBuffer[CODE_LENGTH_ORDER[var14.codeLengthIndex]] = (byte)var9;
                                 ++var14.codeLengthIndex;
                              }

                           var14.codeLengthDecoder = new HuffmanDecoder(var14.codeLengthBuffer, 19);
                           var14.codeLengthBuffer = null;
                              var14.codeLengthIndex = 4;
                              break;
                           }

                           while(((var9 = var14.codeLengthDecoder.decodeSymbol(var14)) & -16) == 0) {
                              var14.codeLengthBuffer[var14.codeLengthIndex++] = var14.lastCodeLength = (byte)var9;
                              if (var14.codeLengthIndex == var14.totalCodeLengthSymbols) {
                                 var10000 = true;
                                 break label301;
                              }
                           }

                           if (var9 < 0) {
                              var10000 = false;
                              break;
                           }

                           if (var9 >= 17) {
                              var14.lastCodeLength = 0;
                           }

                           var14.repeatSymbol = var9 - 16;
                           var14.codeLengthIndex = 5;
                        }

                        var9 = var14.repeatSymbol > 1 ? 7 : var14.repeatSymbol + 2;
                        if ((var10 = var14.peekBits(var9)) < 0) {
                           var10000 = false;
                           break;
                        }

                        var14.skipBits(var9);

                        for(var10 += var14.repeatSymbol > 1 ? 11 : 3; var10-- > 0; var14.codeLengthBuffer[var14.codeLengthIndex++] = var14.lastCodeLength) {
                        }

                        if (var14.codeLengthIndex == var14.totalCodeLengthSymbols) {
                           var10000 = true;
                           break;
                        }

                        var14.codeLengthIndex = 4;
                     }

                     if (!var10000) {
                        break;
                     }

                     byte[] var15 = new byte[this.numLiterals];
                     System.arraycopy(this.codeLengthBuffer, 0, var15, 0, this.numLiterals);
                     this.literalDecoder = new HuffmanDecoder(var15, this.numLiterals);
                     Inflater var17 = var14 = this;
                     var15 = new byte[var17.numDistances];
                     System.arraycopy(var14.codeLengthBuffer, var14.numLiterals, var15, 0, var14.numDistances);
                     var17.distanceDecoder = new HuffmanDecoder(var15, var14.numDistances);
                     this.inflateState = 7;
                  case 7:
                  case 8:
                  case 9:
                  case 10:
                     var14 = this;
                     var9 = 32768 - this.windowUsed;

                     label251:
                     while(var9 >= 258) {
                        label330: {
                           label364: {
                              switch (var14.inflateState) {
                                 case 7:
                                    while(true) {
                                       if (((var10 = var14.literalDecoder.decodeSymbol(var14)) & -256) != 0) {
                                          if (var10 < 257) {
                                             if (var10 < 0) {
                                                break label304;
                                             }

                                             var14.distanceDecoder = null;
                                             var14.literalDecoder = null;
                                             var14.inflateState = 2;
                                             break label251;
                                          }

                                          var14.extraLength = LENGTH_BASE[var10 - 257];
                                          var14.extraBits = LENGTH_EXTRA_BITS[var10 - 257];
                                          break;
                                       }

                                       ++var14.windowUsed;
                                       var14.slidingWindow[var14.windowWritePos++] = (byte)var10;
                                       var14.windowWritePos &= 32767;
                                       --var9;
                                       if (var9 < 258) {
                                          break label251;
                                       }
                                    }
                                 case 8:
                                    break;
                                 case 9:
                                    break label364;
                                 case 10:
                                    break label330;
                                 default:
                                    continue;
                              }

                              if (var14.extraBits > 0) {
                                 var14.inflateState = 8;
                                 if ((var11 = var14.peekBits(var14.extraBits)) < 0) {
                                    break label304;
                                 }

                                 var14.skipBits(var14.extraBits);
                                 var14.extraLength += var11;
                              }

                              var14.inflateState = 9;
                           }

                           if ((var10 = var14.distanceDecoder.decodeSymbol(var14)) < 0) {
                              break label304;
                           }

                           var14.distance = DISTANCE_BASE[var10];
                           var14.extraBits = DISTANCE_EXTRA_BITS[var10];
                        }

                        if (var14.extraBits > 0) {
                           var14.inflateState = 10;
                           if ((var11 = var14.peekBits(var14.extraBits)) < 0) {
                              break label304;
                           }

                           var14.skipBits(var14.extraBits);
                           var14.distance += var11;
                        }

                        var7 = var14.distance;
                        int var5 = var14.extraLength;
                        var6 = var14;
                        var14.windowUsed += var5;
                        var10 = var14.windowWritePos - var7 & 32767;
                        var11 = 32768 - var5;
                        if (var10 <= var11 && var14.windowWritePos < var11) {
                           if (var5 > var7) {
                              while(var5-- > 0) {
                                 var6.slidingWindow[var6.windowWritePos++] = var6.slidingWindow[var10++];
                              }
                           } else {
                              byte[] var10002 = var14.slidingWindow;
                              System.arraycopy(var10002, var10, var10002, var14.windowWritePos, var5);
                              var14.windowWritePos += var5;
                           }
                        } else {
                           var7 = var5;
                           var13 = var10;

                           for(Inflater var12 = var14; var7-- > 0; var13 &= 32767) {
                              var12.slidingWindow[var12.windowWritePos++] = var12.slidingWindow[var13++];
                              var12.windowWritePos &= 32767;
                           }
                        }

                        var9 -= var14.extraLength;
                        var14.inflateState = 7;
                     }

                     var10000 = true;
                     continue;
                  case 11:
                     for(var14 = this; var14.extraBits > 0; var14.extraBits -= 8) {
                        if ((var9 = var14.peekBits(8)) < 0) {
                           break label304;
                        }

                        var14.skipBits(8);
                        var14.adlerChecksum = var14.adlerChecksum << 8 | var9;
                     }

                     var14.inflateState = 12;
                     break;
                  case 12:
                     var10000 = false;
                     continue;
               }

               var10000 = false;
            } while(var10000);
         } while(this.windowUsed > 0 && this.inflateState != 11);

         return var4;
      }
   }

   public final void reset() {
      this.inflateState = this.hasHeader ? 2 : 0;
      this.totalBytesIn = this.bytesRead = 0;
      this.bitBuffer = this.bitCount = this.bitCount = this.bytesRead = 0;
      this.windowUsed = this.windowWritePos = 0;
      this.decodeDynamicHeader();
      this.literalDecoder = null;
      this.distanceDecoder = null;
      this.isFinalBlock = false;
   }

   public final void setInput(byte[] var1, int var2, int var3) {
      int var4 = var2;
      this.bitCount = var2 + var3;
      if ((var3 & 1) != 0) {
         var4 = var2 + 1;
         this.bitBuffer |= (var1[var2] & 255) << this.bytesRead;
         this.bytesRead += 8;
      }

      this.inputBuffer = var1;
      this.bitBuffer = var4;
      this.totalBytesIn += var3;
   }

   private void decodeDynamicHeader() {
      this.codeLengthBuffer = null;
      this.codeLengthDecoder = null;
      this.codeLengthIndex = this.numLiterals = this.numDistances = this.numCodeLengths = this.totalCodeLengthSymbols = this.repeatCount = this.repeatSymbol = 0;
      this.lastCodeLength = 0;
   }

   public final int peekBits(int var1) {
      if (this.bytesRead < var1) {
         if (this.bitBuffer == this.bitCount) {
            return -1;
         }

         this.bitBuffer |= (this.inputBuffer[this.bitBuffer++] & 255 | (this.inputBuffer[this.bitBuffer++] & 255) << 8) << this.bytesRead;
         this.bytesRead += 16;
      }

      return this.bitBuffer & (1 << var1) - 1;
   }

   public final void skipBits(int var1) {
      this.bitBuffer >>>= var1;
      this.bytesRead -= var1;
   }

   public final int getAvailableBits() {
      return this.bytesRead;
   }

   private void alignToByteBoundary() {
      this.bitBuffer >>= this.bytesRead & 7;
      this.bytesRead &= -8;
   }

   private int copyRawBytes(byte[] var1, int var2, int var3) {
      int var4;
      for(var4 = 0; this.bytesRead > 0 && var3 > 0; ++var4) {
         var1[var2++] = (byte)this.bitBuffer;
         this.bitBuffer >>>= 8;
         this.bytesRead -= 8;
         --var3;
      }

      if (var3 == 0) {
         return var4;
      } else {
         var3 = Math.min(var3, this.bitCount - this.bitBuffer);
         System.arraycopy(this.inputBuffer, this.bitBuffer, var1, var2, var3);
         this.bitBuffer += var3;
         if ((this.bitBuffer - this.bitCount & 1) != 0) {
            this.bitBuffer = this.inputBuffer[this.bitBuffer++] & 255;
            this.bytesRead = 8;
         }

         return var4 + var3;
      }
   }
}
