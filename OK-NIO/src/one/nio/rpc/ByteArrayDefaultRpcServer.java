package one.nio.rpc;

import java.io.IOException;
import one.nio.buffers.BufferPool;
import one.nio.buffers.ByteArrayBufferPool;
import one.nio.net.ConnectionString;
import one.nio.net.Session;
import one.nio.net.Socket;

public final class ByteArrayDefaultRpcServer extends DefaultRpcServer {
	private BufferPool<byte[]> bufferPool;
	
	public ByteArrayDefaultRpcServer(ConnectionString conn, Class<?> serviceClass, int bufferSize) 
			throws IOException {
        super(conn, serviceClass);
        bufferPool = new ByteArrayBufferPool(bufferSize);
    }

    public ByteArrayDefaultRpcServer(ConnectionString conn, Object serviceInstance, int bufferSize) 
    		throws IOException {
        super(conn, serviceInstance);
        bufferPool = new ByteArrayBufferPool(bufferSize);
    }
    
	@Override
	public Session<byte[]> createSession(Socket socket) {
		return new ByteArrayRpcSession(socket, this, bufferPool);
	}
}
