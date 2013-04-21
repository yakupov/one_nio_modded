package one.nio.serial;

import one.nio.util.ByteBufferStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BBDeserializeStream extends ByteBufferStream {
    protected ArrayList<Object> context;
    
    public BBDeserializeStream(ByteBuffer input) {
        super(input);
        this.context = new ArrayList<Object>(16);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object readObject() throws IOException, ClassNotFoundException {
        Serializer<Object> serializer;
        byte b = readByte();
        if (b >= 0) {
        	buf.position(buf.position() - 1);
            serializer = Repository.requestSerializer(readLong());
        } else if (b == SerializeStream.REF_NULL) {
            return null;
        } else if (b == SerializeStream.REF_RECURSIVE) {
            return context.get(readUnsignedShort());
        } else {
            serializer = Repository.requestBootstrapSerializer(b);
        }
        Object obj = serializer.read(this);
        context.add(obj);
        serializer.fill(obj, this);
        return obj;
    }

    @Override
    public void close() {
        context = null;
    }
}