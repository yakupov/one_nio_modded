package one.nio.rpc;

import java.io.IOException;
import java.nio.ByteBuffer;

import one.nio.buffers.BufferPool;
import one.nio.buffers.DirectByteBufferPool;
import one.nio.net.ConnectionString;
import one.nio.net.Session;
import one.nio.net.Socket;

public final class ByteBufferDefaultRpcServer extends DefaultRpcServer {
	private BufferPool<ByteBuffer> bufferPool;
	
	public ByteBufferDefaultRpcServer(ConnectionString conn, Class<?> serviceClass, int bufferSize, int maxPoolSize) 
			throws IOException {
        super(conn, serviceClass);
		bufferPool = new DirectByteBufferPool(bufferSize, maxPoolSize);
    }

    public ByteBufferDefaultRpcServer(ConnectionString conn, Object serviceInstance, int bufferSize, int maxPoolSize) 
    		throws IOException {
        super(conn, serviceInstance);
		bufferPool = new DirectByteBufferPool(bufferSize, maxPoolSize);
    }

	@Override
	public Session<ByteBuffer> createSession(Socket socket, int sessionId) throws IOException {
		return new ByteBufferRpcSession(socket, this, bufferPool, sessionId);
	}
}
