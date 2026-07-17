package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class LongEntry extends ConstantPoolEntry {
    private long value;

    LongEntry(long value) {
        this.value = value;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(6);
            out.writeLong(this.value);
        } catch (IOException e) {
        }
    }
}
