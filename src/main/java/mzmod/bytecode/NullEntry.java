package mzmod.bytecode;

import java.io.DataOutputStream;

public final class NullEntry extends ConstantPoolEntry {
    public static final NullEntry INSTANCE = new NullEntry();

    public final void write(DataOutputStream out) {
    }
}
