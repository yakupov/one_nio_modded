package one.nio.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.util.Random;

import one.nio.net.Socket;
import one.nio.server.Server.ClientConn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

final class AcceptorThread extends Thread {
    private static final Log log = LogFactory.getLog(AcceptorThread.class);
    
    final Server server;
    final InetAddress address;
    final int port;
    final Socket serverSocket;
    final Random random;


    AcceptorThread(Server server, InetAddress address, int port, int backlog, int buffers) throws IOException {
        super("NIO Acceptor");
        setUncaughtExceptionHandler(server);
        this.server = server;
        this.address = address;
        this.port = port;
        this.serverSocket = Socket.createServerSocket();
        this.random = new Random();
        if (buffers != 0) {
            serverSocket.setBufferSize(buffers, buffers);
        }
        serverSocket.setNoDelay(true);
        serverSocket.setReuseAddr(true);
        serverSocket.bind(address, port, backlog);
    }

    void shutdown() {
        serverSocket.close();
        try {
            join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Override
    public void run() {
        while (server.isRunning()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                socket.setBlocking(false);
                SelectionKey sk = server.selector.register(socket, SelectionKey.OP_READ);
                server.aliveClients.put(socket.getRemoteAddress(), new ClientConn(socket, server, null));
            } catch (Exception e) {
                if (server.isRunning()) {
                    log.error("Cannot accept incoming connection", e);
                }
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }
}
