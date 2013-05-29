package one.nio.rpc;

import java.io.IOException;
import java.nio.ByteBuffer;

import one.nio.buffers.BufferPool;
import one.nio.net.Session;
import one.nio.net.Socket;
import one.nio.serial.CalcSizeStream;
import one.nio.serial.SerializerNotFoundException;

public abstract class NewRpcSession<Q, R, T> extends Session<T> {
	//private static final Log log = LogFactory.getLog(RpcSession.class);
    private final RpcServer<Q, R> server;
	private final BufferPool<T> bufferPool;
	protected final int id;
	protected T requestBuffer;
	
	
    protected abstract int readFromSocket(T buffer, int offset, int count) throws IOException;
	protected abstract Q deserialize(T buffer) throws SerializerNotFoundException, ClassNotFoundException, IOException;
    protected abstract void serialize(T buffer, int size, int id, Object response) throws IOException; 
	
    public NewRpcSession(Socket socket, RpcServer<Q, R> server, BufferPool<T> bufferPool, int sessionId) 
    		throws IOException {
    	super(socket);

    	this.server = server;
        this.bufferPool = bufferPool;
        this.id = sessionId;
        
        int size = readInt(socket);
        T pooledBuffer = bufferPool.getBuffer(size); //TODO: get large enough buffer; 
        int bytesRead = readFromSocket(pooledBuffer, 0, size);
        if (bytesRead < size) {
        	bufferPool.returnBuffer(pooledBuffer);
        	return;
        }

        requestBuffer = pooledBuffer;
        events |= Session.READABLE;        
    }

    protected void processRead(T unusedBuffer) throws Exception {
    	if (requestBuffer != null) {
    		try {
    			Q request = null;
    			try {
    				request = deserialize(requestBuffer);
    			} catch (SerializerNotFoundException e) {
    				writeResponse(e);
    				return;
    			}
    			writeResponse(server.invoke(request));
    		} finally {
    			bufferPool.returnBuffer(requestBuffer);
    		}
    	}
    }
    
	protected int readInt(Socket socket) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		socket.readFully(buf);
		
		if (buf.get(0) != 0) {
			throw new IOException("Invalid request header or request too large");
		}

		return buf.asIntBuffer().get();
	}
    
    protected void writeResponse(Object response) throws IOException {
        CalcSizeStream calcSizeStream = new CalcSizeStream();
        calcSizeStream.writeObject(response);
        int size = calcSizeStream.count();
        calcSizeStream.close();
        
        T pooledBuffer = bufferPool.getBuffer(size + 4 + 4);
        try {
	        serialize(pooledBuffer, size, id, response);
	        super.write(pooledBuffer, 0, size + 4 + 4, false);
        } finally {
        	bufferPool.returnBuffer(pooledBuffer);
        }
    }
    
    @Override
    public synchronized void close() {
        if (socket.isOpen()) {
            socket.close();
        }
    }
}
