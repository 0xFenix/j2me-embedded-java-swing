package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class FieldRefEntry extends ConstantPoolEntry {
    int nameAndTypeIndex;
    int classIndex;

    FieldRefEntry(int classIndex, int nameAndTypeIndex) {
        this.nameAndTypeIndex = nameAndTypeIndex;
        this.classIndex = classIndex;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(9);
            out.writeShort(this.classIndex);
            out.writeShort(this.nameAndTypeIndex);
        } catch (IOException e) {
        }
    }
}
