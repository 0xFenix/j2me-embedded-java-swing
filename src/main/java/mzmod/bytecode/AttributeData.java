package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class AttributeData {
    private int nameIndex;
    private int dataLength;
    private byte[] data;

    AttributeData(int nameIndex, int dataLength, byte[] data) {
        this.nameIndex = nameIndex;
        this.dataLength = dataLength;
        this.data = data;
    }

    public final void write(DataOutputStream out) {
        try {
            out.writeShort(this.nameIndex);
            out.writeInt(this.dataLength);
            out.write(this.data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
