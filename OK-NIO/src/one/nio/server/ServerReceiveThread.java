package one.nio.server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.Random;

import one.nio.net.Session;
import one.nio.net.Socket;

public class ServerReceiveThread implements Runnable {
	protected final Server server;
    protected final Random random;
	protected long acceptedSessions;
	protected final Socket socket;
	protected final SelectionKey sk;
	
	public ServerReceiveThread(Socket socket, Server server, SelectionKey sk) {
		this.server = server;
        this.random = new Random();
        this.socket = socket;
        acceptedSessions = 0L;
        System.err.println(socket);
        this.sk = sk;
	}

	@Override
	public void run() {
		while (true) {
			try {
				try {
					processRead();
				} catch (SocketException e1) {
					e1.printStackTrace();
					return;
				} catch (ClosedChannelException e1) {
					e1.printStackTrace();
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void processRead() throws IOException {
//		while (!sk.isReadable()) {
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//				return;
//			}
//			server.selector.select();
//		}
		
		int sessionId = readInt(socket);
		Session<?> session = server.createSession(socket, sessionId);
        getSmallestSelector().register(session);
        acceptedSessions++;
        
        server.selector.select();
	}

    protected SelectorThread getSmallestSelector() {
    	SelectorThread[] selectors = server.selectors;
    	SelectorThread st1 = selectors[random.nextInt(selectors.length)];
    	SelectorThread st2 = selectors[random.nextInt(selectors.length)];
    	
    	return st1.requestQueue.size() < st2.requestQueue.size() ? st1 : st2; 
	}
    
    protected int readInt(Socket socket) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		socket.readFully(buf);
		
		if (buf.get(0) != 0) {
			throw new IOException("Invalid request ID or ID is too large. ID = " + buf.asIntBuffer().get());
		}
		
		return buf.asIntBuffer().get();
	}
}
