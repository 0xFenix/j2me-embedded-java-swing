package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class Utf8Entry extends ConstantPoolEntry {
    String value;
    String originalValue;
    boolean isModified;

    Utf8Entry(String value) {
        this.originalValue = this.value = value;
        this.isModified = false;
    }

    public final void write(DataOutputStream out) {
        try {
            out.write(1);
            out.writeUTF(this.value);
        } catch (IOException e) {
        }
    }
}
