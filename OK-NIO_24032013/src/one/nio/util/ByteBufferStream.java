package one.nio.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

public class ByteBufferStream implements ObjectInput, ObjectOutput {
    private static final int LONG_LENGTH = 8;
	private static final int INT_LENGTH = 4;
	private static final int SHORT_LENGTH = 2;
	private static final int CHAR_LENGTH = 2;
	protected ByteBuffer buf;

    public ByteBufferStream(ByteBuffer buf) {
    	this.buf = buf;    	
    }
    
    public ByteBufferStream(int capacity, boolean isDirect) {
    	if (isDirect) {
    		this.buf = ByteBuffer.allocateDirect(capacity);
    	} else { 
    		this.buf = ByteBuffer.allocate(capacity);
    	}
    }

    public int count() {
        return buf.position();
    }

    public void write(int b) {
    	buf.put((byte) b);
        
    }

    public void write(byte[] b) {
        System.arraycopy(b, 0, buf, count(), b.length);
    }

    public void write(byte[] b, int off, int len) {
        System.arraycopy(b, off, buf, count(), len);
    }

    public void writeBoolean(boolean v) {
    	buf.put(v ? (byte) 1 : (byte) 0);
        
    }

    public void writeByte(int v) {
    	buf.put((byte)v);
        
    }

    public void writeShort(int v) {
    	buf.put((byte) (v >>> 8));
        buf.put((byte) v);
    }

    public void writeChar(int v) {
    	writeShort(v);
    }

    public void writeInt(int v) {
    	buf.asIntBuffer().put(v);
    	buf.position(buf.position() + INT_LENGTH);
    }

    public void writeLong(long v) {
    	buf.asLongBuffer().put(v);
    	buf.position(buf.position() + LONG_LENGTH);
    }

    public void writeFloat(float v) {
        writeInt(Float.floatToRawIntBits(v));
    }

    public void writeDouble(double v) {
        writeLong(Double.doubleToRawLongBits(v));
    }

    public void writeBytes(String s) {
        buf.put(s.getBytes());
    }

    public void writeChars(String s) {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            int v = s.charAt(i);
        	writeChar(v);
        }
    }
    
    public void writeUTF(String s) {
        int utfLength = Utf8.simulateWrite(s); 
        if (utfLength <= 0x7fff) {
            writeShort(utfLength);
        } else {
            writeInt(utfLength | 0x80000000);
        }
        Utf8.write(s, buf);
    }

    public void writeObject(Object obj) throws IOException {
        throw new UnsupportedOperationException();
    }


    public int read() {
        return buf.get();
    }

    public int read(byte[] b) {
        readFully(b);
        return b.length;
    }

    public int read(byte[] b, int off, int len) {
        readFully(b, off, len);
        return len;
    }

    public void readFully(byte[] b) {
    	buf.get(b);
    }

    public void readFully(byte[] b, int off, int len) {
    	buf.get(b, off, len);
    }

    public long skip(long n) throws IOException {
        buf.position(buf.position() + (int)n);
        return n;
    }

    public int skipBytes(int n) {
    	buf.position(buf.position() + (int)n);
        return n;
    }

    public boolean readBoolean() {
        return buf.get() != 0;
    }

    public byte readByte() {
        return buf.get();
    }

    public int readUnsignedByte() {
        return buf.get() & 0xff;
    }

    public short readShort() {
    	short v = buf.asShortBuffer().get();
    	buf.position(buf.position() + SHORT_LENGTH);
    	return v;
    }

    public int readUnsignedShort() {
    	short v = (short) (buf.asShortBuffer().get() & (2 * Short.MAX_VALUE + 1));
    	buf.position(buf.position() + SHORT_LENGTH);
    	return v;
    }

    public char readChar() {
    	char v = buf.asCharBuffer().get();
    	buf.position(buf.position() + CHAR_LENGTH);
    	return v;
    }

    public int readInt() {
    	int v = buf.asIntBuffer().get();
    	buf.position(buf.position() + INT_LENGTH);
    	return v;
    }

    public long readLong() {
    	long v = buf.asLongBuffer().get();
    	buf.position(buf.position() + LONG_LENGTH);
    	return v;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public String readLine() {
        if (buf.position() < buf.capacity()) {
            StringBuilder sb = new StringBuilder();
            do {
                byte b = buf.get();
                if (b == 10) {
                    break;
                } else if (b != 13) {
                    sb.append((char) b);
                }
            } while (buf.position() < buf.capacity());
            return sb.toString();
        } else {
            return null;
        }
    }

    public String readUTF() {
        int length = readUnsignedShort();
        if (length == 0) {
            return "";
        }
        if (length > 0x7fff) {
            length = (length & 0x7fff) << 16 | readUnsignedShort();
        }
        String result = Utf8.read(buf, buf.position(), length);
        buf.position(buf.position() + length);
        return result;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    public int available() {
        return buf.capacity() - buf.position();
    }

    public void flush() {
        // Nothing to do
    }

    public void close() {
        // Nothing to do
    }
    
    public byte peek() {
    	return buf.get(buf.position());
    }
}
