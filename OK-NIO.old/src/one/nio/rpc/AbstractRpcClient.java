package one.nio.rpc;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.nio.ByteBuffer;

import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.pool.SocketPool;
import one.nio.serial.CalcSizeStream;
import one.nio.serial.Repository;
import one.nio.serial.Serializer;
import one.nio.serial.SerializerNotFoundException;
import one.nio.util.JavaInternals;

public abstract class AbstractRpcClient<T> implements RpcService<Object,Object>, InvocationHandler {
	public static final Method provideSerializerMethod =
			JavaInternals.getMethod(Repository.class, "provideSerializer", Serializer.class);
	public static final Method requestSerializerMethod =
			JavaInternals.getMethod(Repository.class, "requestSerializer", long.class);
	public static final int INT_SIZE = 4;

	protected final SocketPool socketPool;

	public AbstractRpcClient(ConnectionString conn) throws IOException {
		socketPool = new SocketPool(conn);
	}

	abstract protected T getLargeEnoughBuffer(int size, T poolBuffer);
	
	abstract protected ObjectOutput getSerializeStream(T buffer);
	
	abstract protected ObjectInput getDeserializeStream(T buffer);
	
	abstract protected void sendRequest(Socket socket, T buffer) throws SocketException, IOException;
	
	abstract protected T readResponse(Socket socket, T poolBuffer) throws IOException;
	
	protected T getPooledBuffer() {
		return null;
	}
	
	protected void returnPooledBuffer(T buffer) {
	}
		
	@Override
	public Object invoke(Object request) throws Exception {
		while (true) {
			Object response = invokeRaw(request);
			if (response instanceof SerializerNotFoundException) {
				provideSerializer(((SerializerNotFoundException) response).getUid());
			} else if (response instanceof Exception) {
				throw (Exception) response;
			} else {
				return response;
			}
		}
	}     

	protected Object invokeRaw(Object request) throws Exception {
		while (true) {
			final T pooledBuffer = getPooledBuffer();
			try {
				T requestBuffer = serializeAndWrite(request, pooledBuffer);
				Socket socket = socketPool.borrowObject();
				try {
					try {
						sendRequest(socket, requestBuffer);
					} catch (SocketException e) {
						// Stale connection? Retry on a fresh socket
						socketPool.destroyObject(socket);
						socket = socketPool.createObject();
						sendRequest(socket, requestBuffer);
					}

					try {
						T responseBuffer = readResponse(socket, pooledBuffer);
						socketPool.returnObject(socket);
						return getDeserializeStream(responseBuffer).readObject();
					} catch (SerializerNotFoundException e) {
						requestSerializer(e.getUid());
					}
				} catch (Exception e) {
					socketPool.invalidateObject(socket);
					throw e;
				}			
			} finally {
				returnPooledBuffer(pooledBuffer);
			}
		}
	}

	@Override
	public Object invoke(Object proxy, Method m, Object[] args) throws Exception {
		return invoke(getInvocationObject(m, args));
	}

	protected Object getInvocationObject(Method m, Object... args) throws Exception {
		return new RemoteMethodCall(m, args);
	}

	protected void provideSerializer(long uid) throws Exception {
		Serializer<?> serializer = Repository.requestSerializer(uid);
		Object remoteMethodCall = getInvocationObject(provideSerializerMethod, serializer);
		invokeRaw(remoteMethodCall);
	}

	protected void requestSerializer(long uid) throws Exception {
		Object remoteMethodCall = getInvocationObject(requestSerializerMethod, uid);
		Serializer<?> serializer = (Serializer<?>) invokeRaw(remoteMethodCall);
		Repository.provideSerializer(serializer);
	}

	protected T serializeAndWrite(Object request, T poolBuffer) throws IOException {
		int requestSize = calculateSize(request);
		T buffer = getLargeEnoughBuffer(requestSize + INT_SIZE, poolBuffer);  
		ObjectOutput ss = getSerializeStream(buffer);
		ss.writeInt(requestSize);
		ss.writeObject(request);
		return buffer;
	}

	protected int calculateSize(Object request) throws IOException {
		CalcSizeStream calcSizeStream = new CalcSizeStream();
		try {
			calcSizeStream.writeObject(request);
			return calcSizeStream.count();
		} finally {
			calcSizeStream.close();
		}
	}
	
	protected int readSize(Socket socket) throws IOException {
		ByteBuffer sizeBuf = ByteBuffer.allocate(4);
		socket.readFully(sizeBuf);

		int responseSize = sizeBuf.asIntBuffer().get();
		return responseSize;
	}
}
