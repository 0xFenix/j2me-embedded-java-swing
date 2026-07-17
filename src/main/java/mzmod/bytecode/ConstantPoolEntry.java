package mzmod.bytecode;

import java.io.DataOutputStream;

public abstract class ConstantPoolEntry {
    public abstract void write(DataOutputStream out);
}
