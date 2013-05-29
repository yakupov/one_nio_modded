package one.nio.serial;

import one.nio.util.ByteBufferStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

public class BBSerializeStream extends ByteBufferStream {
    static final byte REF_NULL      = -1;
    static final byte REF_RECURSIVE = -2;

    protected IdentityHashMap<Object, Integer> context;
    
    public BBSerializeStream(ByteBuffer input) {
        super(input);
        this.context = new IdentityHashMap<Object, Integer>(16); 
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
        	write(REF_NULL);
        } else {
            Integer index = context.put(obj, context.size());
            if (index != null) {
                context.put(obj, index);
            	write(REF_RECURSIVE);
                writeShort(index.shortValue());
            } else {
                Serializer serializer = Repository.get(obj.getClass());
                if (serializer.uid < 0) {
                	write((byte) serializer.uid);
                } else {
                    writeLong(serializer.uid);
                }
                serializer.write(obj, this);
            }
        }
    }

    @Override
    public void close() {
        context = null;
    }
}
