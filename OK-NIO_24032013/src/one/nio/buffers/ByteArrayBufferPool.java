package one.nio.buffers;

public class ByteArrayBufferPool implements BufferPool<byte[]> {
	private final int bufferSize;

	public ByteArrayBufferPool(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	public byte[] getBuffer(int size) {
		return new byte[size];
	}

	public void returnBuffer(byte[] buffer) {
		// Do mothing
	}
}
