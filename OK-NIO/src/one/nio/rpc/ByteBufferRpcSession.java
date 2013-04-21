package one.nio.rpc;

import java.io.IOException;
import java.nio.ByteBuffer;

import one.nio.buffers.BufferPool;
import one.nio.net.Socket;
import one.nio.serial.BBDeserializeStream;
import one.nio.serial.BBSerializeStream;

public class ByteBufferRpcSession extends RpcSession<RemoteMethodCall, Object, ByteBuffer>{
	public ByteBufferRpcSession(Socket socket, RpcServer<RemoteMethodCall, Object> server, BufferPool<ByteBuffer> bufferPool) {
        super(socket, server, bufferPool);
	}

	@Override
	protected int readFromSocket(ByteBuffer buffer, int offset, int count)
			throws IOException {
		return socket.read(buffer);
	}

	@Override
	protected RemoteMethodCall deserialize(ByteBuffer buffer) throws ClassNotFoundException, IOException {
		BBDeserializeStream ds = new BBDeserializeStream(buffer);
		try { 
			return (RemoteMethodCall) ds.readObject();
		} finally {
			ds.close();
		}
	}

	@Override
	protected void serialize(ByteBuffer buffer, int size, Object response) throws IOException {
		buffer.clear();
		BBSerializeStream ss = new BBSerializeStream(buffer);
        ss.writeInt(size);
        ss.writeObject(response);
        ss.close();
	}	
	
	@Override
	protected int writeToSocket(ByteBuffer data, int offset, int count) throws IOException {
		return socket.write(data);
	}
}
