package mzmod.bytecode;

public final class NameValuePair {
    String name;
    String descriptor;

    NameValuePair(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }
}
