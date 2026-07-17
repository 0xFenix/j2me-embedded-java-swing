package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class IntEntry extends ConstantPoolEntry {
    private int value;

    IntEntry(int value) {
        this.value = value;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(3);
            out.writeInt(this.value);
        } catch (IOException e) {
        }
    }
}
