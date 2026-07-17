package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class InterfaceMethodRefEntry extends ConstantPoolEntry {
    private int classIndex;
    int nameAndTypeIndex;

    InterfaceMethodRefEntry(int classIndex, int nameAndTypeIndex) {
        this.classIndex = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(11);
            out.writeShort(this.classIndex);
            out.writeShort(this.nameAndTypeIndex);
        } catch (IOException e) {
        }
    }
}
