package one.nio.rpc;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.serial.CalcSizeStream;
import one.nio.serial.Repository;
import one.nio.serial.Serializer;
import one.nio.serial.SerializerNotFoundException;
import one.nio.util.JavaInternals;
import one.nio.util.Maybe;

public abstract class AbstractRpcClient<T> implements RpcService<Object,Object>, InvocationHandler {
	protected class Pair {
		public T object;
		public int id;
		
		public Pair(T object, int id) {
			this.object = object;
			this.id = id;
		}
	}
	
	protected interface Sink {
		public void process(Object result);
	}

	protected class Request {
		private final Integer id;
		private Object requestData;
		Sink sink;

		public Request(Object requestData, Sink sink) {
			this.id = nextId.incrementAndGet();
			this.requestData = requestData;
			this.sink = sink;

			requestById.put(id, this);
		}

		public void setResult(final Object resultData) {
			requestById.remove(id);
			sentRequestQueue.remove(this);
			sink.process(resultData);
		}
	}

	protected class Sender implements Runnable {
		private final Socket socket;
		private final AtomicBoolean isStopped;

		public Sender(Socket socket) {
			this.socket = socket;
			this.isStopped = new AtomicBoolean(false);
		}

		public void stop() {
			isStopped.set(true);
		}

		@Override
		public void run() {
			while (true) {
				if (isStopped.get()) {
					return;
				}

				try {
					if (requestQueue.isEmpty())
						continue;
					Request r = requestQueue.take();
					sentRequestQueue.add(r);
					send(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
		}

		private void send(Request request) {
			final T pooledBuffer = getPooledBuffer();
			try {
				T requestBuffer;
				try {
					requestBuffer = serializeAndWrite(request.requestData, pooledBuffer, request.id);
				} catch (IOException e1) {
					request.setResult(e1);
					return;
				}
				try {
					try {
						sendRequest(socket, requestBuffer);
					} catch (SocketException e) {
						reCreateSenderAndReceiver(createSocket());
					}
				} catch (Exception e) {
					request.setResult(e);
				}			
			} finally {
				returnPooledBuffer(pooledBuffer);
			}
		}
	}

	protected class Receiver implements Runnable {
		private final Socket socket;
		private final AtomicBoolean isStopped;

		public Receiver(Socket socket) {
			this.socket = socket;
			this.isStopped = new AtomicBoolean(false);
		}

		public void stop() {
			isStopped.set(true);
		}

		@Override
		public void run() {
			final T pooledBuffer = getPooledBuffer();
			try {
				while (true) {
					Pair response;
					try {
						response = readResponse(socket, pooledBuffer);
					} catch (SocketException e) {
						e.printStackTrace();
						return;
					} catch (ClosedChannelException e) {
						System.out.println("Socket closed, stopping receiver thread");
						return;
					} catch (SocketTimeoutException e) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
							return;
						}
						continue;
					} catch (IOException e) {
						e.printStackTrace();
						continue;
						//TODO: restart threads maybe
					}
					
					if (isStopped.get())
						return;
					
					Request request = requestById.get(response.id);
					Object resultObject;
					try {
						resultObject = getDeserializeStream(response.object).readObject();
					} catch (Exception e1) {
						request.setResult(e1);
						continue;
					}
					
					if (resultObject instanceof SerializerNotFoundException) {
						long uid = ((SerializerNotFoundException) resultObject).getUid();
						try {
							provideSerializer(uid, request);
						} catch (SerializerNotFoundException e) {
							request.setResult(e);
						}
					} else {
						request.setResult(resultObject);
					}
				}
			} finally {
				returnPooledBuffer(pooledBuffer);
			}
		}
	}

	public static final Method provideSerializerMethod =
			JavaInternals.getMethod(Repository.class, "provideSerializer", Serializer.class);
	public static final Method requestSerializerMethod =
			JavaInternals.getMethod(Repository.class, "requestSerializer", long.class);
	public static final int INT_SIZE = 4;

	protected final ConnectionString conn;
	protected Socket currSocket;
	protected final AtomicInteger nextId;
	protected final Map<Integer, Request> requestById;
	protected static int REPLACE_ME_BY_SETTINGS_REQUEST_QUEUE_SIZE = 1000; //TODO: replace
	protected final BlockingQueue<Request> requestQueue; 
	protected final BlockingQueue<Request> sentRequestQueue;
	protected Sender sender;
	protected Thread senderThread;
	protected Receiver receiver;
	protected Thread receiverThread;
	
	public AbstractRpcClient(ConnectionString conn) throws IOException {
		this.conn = conn;
		
		nextId = new AtomicInteger();
		requestById = new ConcurrentHashMap<Integer, Request>();
		requestQueue = new ArrayBlockingQueue<Request>(REPLACE_ME_BY_SETTINGS_REQUEST_QUEUE_SIZE);
		sentRequestQueue = new ArrayBlockingQueue<Request>(REPLACE_ME_BY_SETTINGS_REQUEST_QUEUE_SIZE);
		
		reCreateSenderAndReceiver(createSocket());
	}

	abstract protected T getLargeEnoughBuffer(int size, T poolBuffer);
	
	abstract protected ObjectOutput getSerializeStream(T buffer);
	
	abstract protected ObjectInput getDeserializeStream(T buffer);
	
	abstract protected void sendRequest(Socket socket, T buffer) throws SocketException, IOException;
	
	abstract protected Pair readResponse(Socket socket, T poolBuffer) throws IOException;
	
	protected T getPooledBuffer() {
		return null;
	}
	
	protected void returnPooledBuffer(T buffer) {
	}
		
	protected void reCreateSenderAndReceiver(final Socket socket) {
		stop();
		
		requestQueue.addAll(sentRequestQueue);
		sentRequestQueue.clear();
		
		currSocket = socket;
		sender = new Sender(socket);
		senderThread = new Thread(sender);
		senderThread.start();
		receiver = new Receiver(socket);
		receiverThread = new Thread(receiver);
		receiverThread.start();
	}
	
    protected Socket createSocket() throws IOException {    	
        Socket socket = null;
        try {
            socket = Socket.create();
            socket.setKeepAlive(false);
            socket.setNoDelay(true);
            socket.setBlocking(true);
            socket.setTimeout(conn.getIntParam("timeout", 3000));
            socket.connect(conn.getHost(), conn.getPort());
            return socket;
        } catch (IOException e) {
            if (socket != null) socket.close();
            throw new IOException("AbstractRpcClient: unable to create socket", e);
        }
    }
	
	
	@Override
	public Object invoke(Object requestData) throws Exception {
		final BlockingQueue<Maybe> queue = new SynchronousQueue<Maybe>();
		invoke(requestData, new Sink() {
			@Override
			public void process(Object result) {
				queue.add(new Maybe(result));
			}
		});
		Object response = queue.take().get();
		return response;
	}     

	public void invoke(Object requestData, Sink sink) {
		requestQueue.add(new Request(requestData, sink));
	}     
	

	@Override
	public Object invoke(Object proxy, Method m, Object[] args) throws Exception {
		return invoke(getInvocationObject(m, args));
	}

	protected Object getInvocationObject(Method m, Object... args) {
		return new RemoteMethodCall(m, args);
	}

	protected void provideSerializer(final long uid, final Request request) throws SerializerNotFoundException {
		Serializer<?> serializer = Repository.requestSerializer(uid);
		Object remoteMethodCall = getInvocationObject(provideSerializerMethod, serializer);
		invoke(remoteMethodCall, new Sink() {
			@Override
			public void process(Object result) {
				requestQueue.add(request);
			}
		});
	}

	protected void requestSerializer(final long uid, final Request request) {
		Object remoteMethodCall = getInvocationObject(requestSerializerMethod, uid);
		invoke(remoteMethodCall, new Sink() {			
			@Override
			public void process(Object result) {
				Repository.provideSerializer((Serializer<?>) result);
				requestQueue.add(request);
			}
		});
	}

	protected T serializeAndWrite(Object request, T poolBuffer, int requestId) throws IOException {
		int requestSize = calculateSize(request);
		T buffer = getLargeEnoughBuffer(requestSize + INT_SIZE + INT_SIZE, poolBuffer);  
		ObjectOutput ss = getSerializeStream(buffer);
		ss.writeInt(requestId);
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
	
	protected int readInt(Socket socket) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		socket.readFully(buf);
		
		if (buf.get(0) != 0) {
			throw new IOException("Invalid response header or response too large");
		}

		return buf.asIntBuffer().get();
	}
	
	public void stop() {
		if (currSocket != null) {
			sender.stop();
			while (senderThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
			
			receiver.stop();
			currSocket.close();
			while (receiverThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
}
