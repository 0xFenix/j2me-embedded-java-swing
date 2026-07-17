package mzmod.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public final class FieldInfo {
    private int accessFlags;
    private int nameIndex;
    private int descriptorIndex;
    AttributeData[] attributes;

    FieldInfo(int accessFlags, int nameIndex, int descriptorIndex) {
        this.accessFlags = accessFlags;
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
    }

    final void write(DataOutputStream out) {
        try {
            out.writeShort(this.accessFlags);
            out.writeShort(this.nameIndex);
            out.writeShort(this.descriptorIndex);
            if (this.attributes == null) {
                out.writeShort(0);
            } else {
                int count = this.attributes.length;
                out.writeShort(count);

                for (int i = 0; i < count; ++i) {
                    this.attributes[i].write(out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
