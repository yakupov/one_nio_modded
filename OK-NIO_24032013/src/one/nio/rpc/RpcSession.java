package one.nio.rpc;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;

import one.nio.buffers.BufferPool;
import one.nio.net.Session;
import one.nio.net.Socket;
import one.nio.serial.CalcSizeStream;
import one.nio.serial.SerializerNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class RpcSession<Q, R, T> extends Session<T> {
    private static final Log log = LogFactory.getLog(RpcSession.class);

    private final RpcServer<Q, R> server;

	private final BufferPool<T> bufferPool;

    public RpcSession(Socket socket, RpcServer<Q, R> server, BufferPool<T> bufferPool) {
        super(socket);
        this.server = server;
        this.bufferPool = bufferPool;
    }

    @Override
    protected void processRead(T unusedBuffer) throws Exception {
    	int requestSize = readSize(socket);
        T pooledBuffer = bufferPool.getBuffer(requestSize);
        
        try {
	        // Read request
	        int bytesRead = readFromSocket(pooledBuffer, 0, requestSize);
	        if (bytesRead < requestSize) {
	            return;
	        }
	
	        // Request is complete - deserialize it
	        final Q request;
	        try {
	            request = deserialize(pooledBuffer);
	        } catch (SerializerNotFoundException e) {
	            writeResponse(e);
	            return;
	        }
	
	        // Perform the invocation
	        if (server.getWorkersUsed()) {
	            server.asyncExecute(new AsyncRequest(request));
	        } else {
	            writeResponse(server.invoke(request));
	        }
        } finally {
        	bufferPool.returnBuffer(pooledBuffer);
        }
    }
    
	protected int readSize(Socket socket) throws IOException {
		ByteBuffer sizeBuf = ByteBuffer.allocate(4);
		socket.read(sizeBuf);
		if (sizeBuf.get(0) != 0) {
			throw new IOException("Invalid response header or response too large");
		}

		int responseSize = sizeBuf.asIntBuffer().get();
		return responseSize;
	}
    
    protected abstract int readFromSocket(T buffer, int offset, int count) throws IOException;
	protected abstract Q deserialize(T buffer) throws SerializerNotFoundException, ClassNotFoundException, IOException;
    protected abstract void serialize(T buffer, int size, Object response) throws IOException; 
    	

    protected void writeResponse(Object response) throws IOException {
        CalcSizeStream calcSizeStream = new CalcSizeStream();
        calcSizeStream.writeObject(response);
        int size = calcSizeStream.count();
        calcSizeStream.close();
        
        T pooledBuffer = bufferPool.getBuffer(size + 4);
        try {
	        serialize(pooledBuffer, size, response);
	        super.write(pooledBuffer, 0, size + 4, false);
        } finally {
        	bufferPool.returnBuffer(pooledBuffer);
        }
    }

	private class AsyncRequest implements Runnable {
        private final Q request;

        AsyncRequest(Q request) {
            this.request = request;
        }

        @Override
        public void run() {
            try {
                writeResponse(server.invoke(request));
            } catch (SocketException e) {
                if (server.isRunning() && log.isDebugEnabled()) {
                    log.debug("Connection closed: " + clientIp());
                }
                close();
            } catch (Throwable e) {
                if (server.isRunning()) {
                    log.error("Cannot process session from " + clientIp(), e);
                }
                close();
            }
        }
    }
}
