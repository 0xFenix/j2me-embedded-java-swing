package mzmod.compress;

public final class HuffmanDecoder {
    private short[] lookupTable;
    public static HuffmanDecoder LITERAL_LENGTH_TABLE;
    public static HuffmanDecoder DISTANCE_TABLE;

    public HuffmanDecoder(byte[] codeLengths, int numSymbols) {
        buildLookupTable(codeLengths, numSymbols);
    }

    private void buildLookupTable(byte[] codeLengths, int numSymbols) {
        int[] blCount = new int[16];
        int[] nextCode = new int[16];

        for (int i = 0; i < numSymbols; i++) {
            int len = codeLengths[i];
            if (len > 0) {
                blCount[len]++;
            }
        }

        int code = 0;
        int tableSize = 512;

        for (int bits = 1; bits <= 15; bits++) {
            nextCode[bits] = code;
            code += blCount[bits] << (16 - bits);
            if (bits >= 10) {
                int start = nextCode[bits] & 130944;
                int end = code & 130944;
                tableSize += (end - start) >> (16 - bits);
            }
        }

        if (code != 65536) {
            throw new RuntimeException("Code lengths not properly, code = " + code);
        }

        this.lookupTable = new short[tableSize];
        int subTableOffset = 512;

        for (int bits = 15; bits >= 10; bits--) {
            int end = code & 130944;
            code -= blCount[bits] << (16 - bits);
            int start = code & 130944;

            for (int idx = start; idx < end; idx += 128) {
                this.lookupTable[Deflater.reverseBits(idx)] = (short) (-subTableOffset << 4 | bits);
                subTableOffset += 1 << (bits - 9);
            }
        }

        for (int symbol = 0; symbol < numSymbols; symbol++) {
            byte len = codeLengths[symbol];
            if (len != 0) {
                int reversedCode = Deflater.reverseBits(code = nextCode[len]);
                if (len <= 9) {
                    do {
                        this.lookupTable[reversedCode] = (short) (symbol << 4 | len);
                    } while ((reversedCode += 1 << len) < 512);
                } else {
                    short subTable = this.lookupTable[reversedCode & 511];
                    int subTableSize = 1 << (subTable & 15);
                    int subTableBase = -(subTable >> 4);

                    do {
                        this.lookupTable[subTableBase | reversedCode >> 9] = (short) (symbol << 4 | len);
                    } while ((reversedCode += 1 << len) < subTableSize);
                }

                nextCode[len] = code + (1 << (16 - len));
            }
        }
    }

    public final int decodeSymbol(Inflater inflater) {
        int bits;
        short entry;

        if ((bits = inflater.peekBits(9)) >= 0) {
            if ((entry = this.lookupTable[bits]) >= 0) {
                inflater.skipBits(entry & 15);
                return entry >> 4;
            } else {
                int subTableBase = -(entry >> 4);
                int extraBits = entry & 15;
                if ((bits = inflater.peekBits(extraBits)) >= 0) {
                    entry = this.lookupTable[subTableBase | bits >> 9];
                    inflater.skipBits(entry & 15);
                    return entry >> 4;
                } else {
                    int available = inflater.getAvailableBits();
                    bits = inflater.peekBits(available);
                    if (((entry = this.lookupTable[subTableBase | bits >> 9]) & 15) <= available) {
                        inflater.skipBits(entry & 15);
                        return entry >> 4;
                    } else {
                        return -1;
                    }
                }
            }
        } else {
            int available = inflater.getAvailableBits();
            bits = inflater.peekBits(available);
            if ((entry = this.lookupTable[bits]) >= 0 && (entry & 15) <= available) {
                inflater.skipBits(entry & 15);
                return entry >> 4;
            } else {
                return -1;
            }
        }
    }

    static {
        byte[] literalLengthLengths = new byte[288];
        int i = 0;
        for (; i < 144; literalLengthLengths[i++] = 8) {}
        while (i < 256) { literalLengthLengths[i++] = 9; }
        while (i < 280) { literalLengthLengths[i++] = 7; }
        while (i < 288) { literalLengthLengths[i++] = 8; }

        LITERAL_LENGTH_TABLE = new HuffmanDecoder(literalLengthLengths, 288);

        byte[] distanceLengths = new byte[32];
        for (i = 0; i < 32; distanceLengths[i++] = 5) {}

        DISTANCE_TABLE = new HuffmanDecoder(distanceLengths, 32);
    }
}
