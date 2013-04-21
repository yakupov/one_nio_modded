package one.nio.serial;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

class CharacterArraySerializer extends Serializer<char[]> {
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    CharacterArraySerializer() {
        super(char[].class);
    }

    @Override
    public void write(char[] obj, ObjectOutput out) throws IOException {
        out.writeInt(obj.length);
        for (char v : obj) {
            out.writeChar(v);
        }
    }

    @Override
    public char[] read(ObjectInput in) throws IOException {
        int length = in.readInt();
        if (length > 0) {
            char[] result = new char[length];
            for (int i = 0; i < length; i++) {
                result[i] = in.readChar();
            }
            return result;
        } else {
            return EMPTY_CHAR_ARRAY;
        }
    }

    @Override
    public void skip(ObjectInput in) throws IOException {
        int length = in.readInt();
        if (length > 0) {
            in.skipBytes(length << 1);
        }
    }
}
