package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class NameAndTypeEntry extends ConstantPoolEntry {
    int nameIndex;
    int descriptorIndex;

    NameAndTypeEntry(int nameIndex, int descriptorIndex) {
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(12);
            out.writeShort(this.nameIndex);
            out.writeShort(this.descriptorIndex);
        } catch (IOException e) {
        }
    }
}
