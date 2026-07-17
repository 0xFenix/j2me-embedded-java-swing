package mzmod.bytecode;

import java.io.DataOutputStream;

public final class ClassRefEntry extends ConstantPoolEntry {
    int nameIndex;

    ClassRefEntry(int nameIndex) {
        this.nameIndex = nameIndex;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(7);
            out.writeShort(this.nameIndex);
        } catch (Throwable e) {
        }
    }
}
