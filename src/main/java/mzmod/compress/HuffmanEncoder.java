package mzmod.compress;

final class HuffmanEncoder {
   short[] frequencies;
   private short[] codes;
   byte[] codeLengths;
   private int[] blCount;
   private int minSymbols;
   int maxCode;
   private int maxCodeBits;
   private int numSymbols;
   private Deflater deflater;

   HuffmanEncoder(int var1, int var2, int var3, Deflater var4) {
      this.deflater = var4;
      this.minSymbols = var2;
      this.maxCodeBits = var3;
      this.numSymbols = var1;
      this.frequencies = new short[var1];
      this.blCount = new int[var3];
   }

   public final int getWeightedPathLength() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.numSymbols; ++var2) {
         var1 += this.frequencies[var2] * this.codeLengths[var2];
      }

      return var1;
   }

   public final void reset() {
      for(int var1 = this.numSymbols - 1; var1 >= 0; --var1) {
         this.frequencies[var1] = 0;
      }

      this.codes = null;
      this.codeLengths = null;
   }

   public final void writeCode(int var1) {
      this.deflater.writeBits(this.codes[var1] & '\uffff', this.codeLengths[var1]);
   }

   public final void setCodes(short[] var1, byte[] var2) {
      this.codes = var1;
      this.codeLengths = var2;
   }

   public final void buildCodes() {
      int[] var1 = new int[this.maxCodeBits];
      int var2 = 0;
      this.codes = new short[this.numSymbols];

      int var3;
      for(var3 = 0; var3 < this.maxCodeBits; ++var3) {
         var1[var3] = var2;
         var2 += this.blCount[var3] << 15 - var3;
      }

      for(var3 = 0; var3 < this.maxCode; ++var3) {
         byte var4;
         if ((var4 = this.codeLengths[var3]) > 0) {
            this.codes[var3] = Deflater.reverseBits(var1[var4 - 1]);
            var1[var4 - 1] += 1 << 16 - var4;
         }
      }

   }

   public final void buildTree() {
      int var1;
      int[] var2 = new int[var1 = this.numSymbols];
      int var3 = 0;
      int var4 = 0;

      int var5;
      int var6;
      int var7;
      for(var6 = 0; var6 < var1; ++var6) {
         if ((var5 = this.frequencies[var6]) != 0) {
            for(var4 = var3++; var4 > 0 && this.frequencies[var2[var7 = (var4 - 1) / 2]] > var5; var4 = var7) {
               var2[var4] = var2[var7];
            }

            var2[var4] = var6;
            var4 = var6;
         }
      }

      int var10001;
      int var10002;
      for(; var3 < 2; var2[var10001] = var10002) {
         var10001 = var3++;
         if (var4 < 2) {
            ++var4;
            var10002 = var4;
         } else {
            var10002 = 0;
         }
      }

      this.maxCode = Math.max(var4 + 1, this.minSymbols);
      int[] var13 = new int[var6 = (var3 << 2) - 2];
      int[] var14 = new int[(var3 << 1) - 1];
      var1 = var3;

      int var8;
      for(var5 = 0; var5 < var3; var2[var5] = var5++) {
         var8 = var2[var5];
         var13[var5 << 1] = var8;
         var13[(var5 << 1) + 1] = -1;
         var14[var5] = this.frequencies[var8] << 8;
      }

      label147:
      do {
         var5 = var2[0];
         --var3;
         var8 = var2[var3];
         int var9 = 0;

         int var10;
         for(var10 = 1; var10 < var3; var10 = (var10 << 1) + 1) {
            if (var10 + 1 < var3 && var14[var2[var10]] > var14[var2[var10 + 1]]) {
               ++var10;
            }

            var2[var9] = var2[var10];
            var9 = var10;
         }

         int var11 = var14[var8];

         while(true) {
            var10 = var9;
            if (var9 <= 0 || var14[var2[var9 = (var9 - 1) / 2]] <= var11) {
               var2[var9] = var8;
               var9 = var2[0];
               var8 = var1++;
               var13[var8 << 1] = var5;
               var13[(var8 << 1) + 1] = var9;
               var10 = Math.min(var14[var5] & 255, var14[var9] & 255);
               var14[var8] = var11 = var14[var5] + var14[var9] - var10 + 1;
               var9 = 0;

               for(var10 = 1; var10 < var3; var10 = (var10 << 1) + 1) {
                  if (var10 + 1 < var3 && var14[var2[var10]] > var14[var2[var10 + 1]]) {
                     ++var10;
                  }

                  var2[var9] = var2[var10];
                  var9 = var10;
               }

               while(true) {
                  var10 = var9;
                  if (var9 <= 0 || var14[var2[var9 = (var9 - 1) / 2]] <= var11) {
                     var2[var9] = var8;
                     continue label147;
                  }

                  var2[var10] = var2[var9];
               }
            }

            var2[var10] = var2[var9];
         }
      } while(var3 > 1);

      var2 = var13;
      HuffmanEncoder var12;
      this.codeLengths = new byte[(var12 = this).numSymbols];
      var4 = ((var3 = var6 / 2) + 1) / 2;
      var5 = 0;

      for(var6 = 0; var6 < var12.maxCodeBits; ++var6) {
         var12.blCount[var6] = 0;
      }

      int[] var15;
      (var15 = new int[var3])[var3 - 1] = 0;
      --var3;

      for(; var3 >= 0; --var3) {
         if (var2[(var3 << 1) + 1] != -1) {
            if ((var7 = var15[var3] + 1) > var12.maxCodeBits) {
               var7 = var12.maxCodeBits;
               ++var5;
            }

            var15[var2[var3 << 1]] = var15[var2[(var3 << 1) + 1]] = var7;
         } else {
            var7 = var15[var3];
            var10002 = var12.blCount[var7 - 1]++;
            var12.codeLengths[var2[var3 << 1]] = (byte)var15[var3];
         }
      }

      if (var5 != 0) {
         var3 = var12.maxCodeBits - 1;

         do {
            do {
               --var3;
            } while(var12.blCount[var3] == 0);

            do {
               var10002 = var12.blCount[var3]--;
               ++var3;
               var10002 = var12.blCount[var3]++;
            } while((var5 -= 1 << var12.maxCodeBits - 1 - var3) > 0 && var3 < var12.maxCodeBits - 1);
         } while(var5 > 0);

         int[] var16 = var12.blCount;
         var10001 = var12.maxCodeBits - 1;
         var16[var10001] += var5;
         var16 = var12.blCount;
         var10001 = var12.maxCodeBits - 2;
         var16[var10001] -= var5;
         var7 = var4 << 1;

         for(var5 = var12.maxCodeBits; var5 != 0; --var5) {
            var3 = var12.blCount[var5 - 1];

            while(var3 > 0) {
               var4 = 2 * var2[var7++];
               if (var2[var4 + 1] == -1) {
                  var12.codeLengths[var2[var4]] = (byte)var5;
                  --var3;
               }
            }
         }
      }

   }

   public final void countBitLengthFrequencies(HuffmanEncoder var1) {
      byte var4 = -1;
      int var5 = 0;

      while(var5 < this.maxCode) {
         int var3 = 1;
         short var2;
         byte var6;
         if ((var6 = this.codeLengths[var5]) == 0) {
            var2 = 138;
         } else {
            var2 = 6;
            if (var4 != var6) {
               ++var1.frequencies[var6];
               var3 = 0;
            }
         }

         var4 = var6;
         ++var5;

         while(var5 < this.maxCode && var4 == this.codeLengths[var5]) {
            ++var5;
            ++var3;
            if (var3 >= var2) {
               break;
            }
         }

         if (var3 < 3) {
            short[] var10000 = var1.frequencies;
            var10000[var4] = (short)(var10000[var4] + var3);
         } else if (var4 != 0) {
            ++var1.frequencies[16];
         } else if (var3 <= 10) {
            ++var1.frequencies[17];
         } else {
            ++var1.frequencies[18];
         }
      }

   }

   public final void writeBitLengths(HuffmanEncoder var1) {
      byte var4 = -1;
      int var5 = 0;

      while(true) {
         while(var5 < this.maxCode) {
            int var3 = 1;
            short var2;
            byte var6;
            if ((var6 = this.codeLengths[var5]) == 0) {
               var2 = 138;
            } else {
               var2 = 6;
               if (var4 != var6) {
                  var1.writeCode(var6);
                  var3 = 0;
               }
            }

            var4 = var6;
            ++var5;

            while(var5 < this.maxCode && var4 == this.codeLengths[var5]) {
               ++var5;
               ++var3;
               if (var3 >= var2) {
                  break;
               }
            }

            if (var3 < 3) {
               while(var3-- > 0) {
                  var1.writeCode(var4);
               }
            } else if (var4 != 0) {
               var1.writeCode(16);
               this.deflater.writeBits(var3 - 3, 2);
            } else if (var3 <= 10) {
               var1.writeCode(17);
               this.deflater.writeBits(var3 - 3, 3);
            } else {
               var1.writeCode(18);
               this.deflater.writeBits(var3 - 11, 7);
            }
         }

         return;
      }
   }
}
