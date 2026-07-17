package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class StringRefEntry extends ConstantPoolEntry {
    int stringIndex;

    StringRefEntry(int stringIndex) {
        this.stringIndex = stringIndex;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(8);
            out.writeShort(this.stringIndex);
        } catch (IOException e) {
        }
    }
}
