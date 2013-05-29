package one.nio.server;

import one.nio.net.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

final class SelectorThread extends Thread {
	private static final Log log = LogFactory.getLog(SelectorThread.class);
	final Server server;
	long operations;
	long sessions;
	protected static int REPLACE_ME_BY_SETTINGS_REQUEST_QUEUE_SIZE = 1000; //TODO: replace
	protected final BlockingQueue<Session<?>> requestQueue;

	SelectorThread(Server server, int num) throws IOException {
		super("NIO Selector #" + num);
		setUncaughtExceptionHandler(server);
		this.server = server;

		requestQueue = new ArrayBlockingQueue<Session<?>>(REPLACE_ME_BY_SETTINGS_REQUEST_QUEUE_SIZE);
	}

	void shutdown() {
		try {
			join();
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	@Override
	public void run() {
		while (server.isRunning()) {
			Session<?> session;
			try {
				session = requestQueue.take();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return;
			}
			try {
				session.process(null);
			} catch (SocketException e) {
				if (server.isRunning() && log.isDebugEnabled()) {
					log.debug("Connection closed: " + session.clientIp());
				}
				session.close();
			} catch (Throwable e) {
				if (server.isRunning()) {
					log.error("Cannot process session from " + session.clientIp(), e);
				}
				session.close();
			}
		}

		operations++;
	}
	
	public void register(Session<?> session) {
		requestQueue.add(session);
	}
}
