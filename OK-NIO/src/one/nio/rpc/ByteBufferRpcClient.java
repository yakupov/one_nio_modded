package one.nio.rpc;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

import one.nio.buffers.DirectByteBufferPool;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.serial.BBDeserializeStream;
import one.nio.serial.BBSerializeStream;

public class ByteBufferRpcClient extends AbstractRpcClient<ByteBuffer> {
	private DirectByteBufferPool directByteBufferPool;
	private int bufferSize;	
	
	public ByteBufferRpcClient(ConnectionString conn, int bufferSize, int maxPoolSize) throws IOException {
		super(conn);
		this.bufferSize = bufferSize;
		directByteBufferPool = new DirectByteBufferPool (bufferSize, maxPoolSize);
	}

	@Override
	protected ByteBuffer getLargeEnoughBuffer(int size, ByteBuffer poolBuffer) {
		if (poolBuffer != null && size <= poolBuffer.capacity()) {
			return poolBuffer;
		}
		return ByteBuffer.allocate(size);
	}

	@Override
	protected ObjectOutput getSerializeStream(ByteBuffer buffer) {
		return new BBSerializeStream(buffer);
	}

	@Override
	protected ObjectInput getDeserializeStream(ByteBuffer buffer) {
		return new BBDeserializeStream(buffer);
	}

	@Override
	protected void sendRequest(Socket socket, ByteBuffer buffer) throws IOException {
		buffer.flip();
		socket.write(buffer);
	}

	@Override
	protected Pair readResponse(Socket socket, ByteBuffer poolBuffer)
			throws IOException {
		int requestId = readInt(socket);
		int responseSize = readInt(socket);
		
		ByteBuffer buffer = poolBuffer;
		if (buffer != null && buffer.capacity() >= responseSize) {
			buffer.clear();
		} else {
			buffer = ByteBuffer.allocate(responseSize);
		}
		buffer.limit(responseSize);
		socket.readFully(buffer);
		return new Pair(buffer, requestId);
	}

	@Override
	protected ByteBuffer getPooledBuffer() {
		while (directByteBufferPool == null)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		return directByteBufferPool.getBuffer(bufferSize);
	}
	
	@Override
	protected void returnPooledBuffer(ByteBuffer buffer) {
		directByteBufferPool.returnBuffer(buffer);
	}
}
