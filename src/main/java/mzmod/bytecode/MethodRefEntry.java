package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class MethodRefEntry extends ConstantPoolEntry {
    int classIndex;
    int nameAndTypeIndex;

    MethodRefEntry(int classIndex, int nameAndTypeIndex) {
        this.classIndex = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(10);
            out.writeShort(this.classIndex);
            out.writeShort(this.nameAndTypeIndex);
        } catch (IOException e) {
        }
    }
}
