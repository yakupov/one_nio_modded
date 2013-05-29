package one.nio.rpc;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.serial.DeserializeStream;
import one.nio.serial.SerializeStream;

public class ByteArrayRpcClient extends AbstractRpcClient<byte []> {
	public ByteArrayRpcClient(ConnectionString conn) throws IOException {
		super(conn);
	}

	@Override
	protected byte [] getLargeEnoughBuffer(int size, byte[] poolBuffer) {
		if (poolBuffer != null && size <= poolBuffer.length) {
			return poolBuffer;
		}
		return new byte[size];
	}

	@Override
	protected ObjectOutput getSerializeStream(byte[] buffer) {
		return new SerializeStream(buffer);
	}

	@Override
	protected ObjectInput getDeserializeStream(byte[] buffer) {
		return new DeserializeStream(buffer);
	}

	@Override
	protected void sendRequest(Socket socket, byte[] buffer) throws IOException {
		socket.writeFully(buffer, 0, buffer.length);
	}

	@Override
	protected byte[] readResponse(Socket socket, byte[] poolBuffer) throws IOException {
		int responseSize = readSize(socket);
		byte[] buffer = poolBuffer;
		if (buffer == null || buffer.length < responseSize) {
			buffer = new byte[responseSize];

		}

		socket.readFully(buffer, 0, responseSize);
		return buffer;
	}
}
