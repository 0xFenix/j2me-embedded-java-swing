package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class DoubleEntry extends ConstantPoolEntry {
    private long value;

    DoubleEntry(long value) {
        this.value = value;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(5);
            out.writeLong(this.value);
        } catch (IOException e) {
        }
    }
}
