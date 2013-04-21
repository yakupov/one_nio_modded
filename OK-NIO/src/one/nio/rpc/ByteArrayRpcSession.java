package one.nio.rpc;

import java.io.IOException;

import one.nio.buffers.BufferPool;
import one.nio.net.Socket;
import one.nio.serial.DeserializeStream;
import one.nio.serial.SerializeStream;

public class ByteArrayRpcSession extends RpcSession<RemoteMethodCall, Object, byte[]> {
	public ByteArrayRpcSession(Socket socket, RpcServer<RemoteMethodCall, Object> server, BufferPool<byte[]> bufferPool) {
        super(socket, server, bufferPool);
	}

	@Override
	protected int readFromSocket(byte[] buffer, int offset, int count)
			throws IOException {
		return socket.read(buffer, offset, count);
	}

	@Override
	protected RemoteMethodCall deserialize(byte[] buffer) throws ClassNotFoundException, IOException {
		DeserializeStream ds = new DeserializeStream(buffer);
		try { 
			return (RemoteMethodCall) ds.readObject();
		} finally {
			ds.close();
		}
	}

	@Override
	protected void serialize(byte[] buffer, int size, Object response) throws IOException {
		SerializeStream ss = new SerializeStream(buffer);
        ss.writeInt(size);
        ss.writeObject(response);
        ss.close();	
	}

	@Override
	protected int writeToSocket(byte[] data, int offset, int count) throws IOException {
		return socket.write(data, offset, count);
	}
}
