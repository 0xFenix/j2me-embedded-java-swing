package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class FloatEntry extends ConstantPoolEntry {
    private int value;

    FloatEntry(int value) {
        this.value = value;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(4);
            out.writeInt(this.value);
        } catch (IOException e) {
        }
    }
}
