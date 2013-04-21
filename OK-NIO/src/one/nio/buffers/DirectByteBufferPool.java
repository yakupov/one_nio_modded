package one.nio.buffers;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DirectByteBufferPool implements BufferPool<ByteBuffer> {
	private final int bufferSize;
	private final BlockingQueue<ByteBuffer> queue;

	public DirectByteBufferPool(int bufferSize, int poolSize) {
		this.bufferSize = bufferSize;
		queue = new ArrayBlockingQueue<ByteBuffer>(poolSize);
		for (int i = 0; i < poolSize; i++) {
			queue.add(ByteBuffer.allocateDirect(bufferSize));
		}
	}
	
	public ByteBuffer getBuffer(int size) {
		if (size > bufferSize)
			return ByteBuffer.allocate(size);
		
		ByteBuffer bb = queue.poll();
		bb.clear();
		return bb;
	}

	public void returnBuffer(ByteBuffer bb) {
		if (bb != null && bb.isDirect() && bb.capacity() == bufferSize)
			queue.add(bb);
	}
}
