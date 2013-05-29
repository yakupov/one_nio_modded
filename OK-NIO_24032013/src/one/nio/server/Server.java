package one.nio.server;

import one.nio.net.ConnectionString;
import one.nio.net.Session;
import one.nio.net.Socket;
import one.nio.util.Management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

public abstract class Server implements ServerMXBean, Thread.UncaughtExceptionHandler {
    private static final Log log = LogFactory.getLog(Server.class);

    private final SelectorStats selectorStats;
    private final QueueStats queueStats;

    private volatile boolean running;

    protected AcceptorThread acceptor;
    protected SelectorThread[] selectors;
    protected WorkerPool workers;
    protected CleanupThread cleanup;
    protected boolean useWorkers;

    public Server(ConnectionString conn) throws IOException {
        InetAddress address = InetAddress.getByName(conn.getHost());
        int port = conn.getPort();
        int backlog = conn.getIntParam("backlog", 128);
        int buffers = conn.getIntParam("buffers", 0);
        int selectorCount = conn.getIntParam("selectors", 32);
        int minWorkers = conn.getIntParam("minWorkers", 0);
        int maxWorkers = conn.getIntParam("maxWorkers", 1000);
        long queueTime = conn.getLongParam("queueTime", 0);
        int keepAlive = conn.getIntParam("keepalive", 0);

        this.acceptor = new AcceptorThread(this, address, port, backlog, buffers);
        this.selectors = new SelectorThread[selectorCount];
        this.workers = new WorkerPool(this, minWorkers, maxWorkers, queueTime);
        this.useWorkers = conn.getStringParam("minWorkers") != null || conn.getStringParam("maxWorkers") != null;

        for (int i = 0; i < selectorCount; i++) {
            this.selectors[i] = new SelectorThread(this, i);
        }

        if (keepAlive > 0) {
            this.cleanup = new CleanupThread(this, keepAlive);
        }

        this.selectorStats = new SelectorStats();
        this.queueStats = new QueueStats();
        Management.registerMXBean(this, "type=Server,port=" + port);
    }

    public boolean reconfigure(ConnectionString conn) throws IOException {
        if (acceptor.port != conn.getPort()) {
            return false;
        }

        workers.setCorePoolSize(conn.getIntParam("minWorkers", 0));
        workers.setMaximumPoolSize(conn.getIntParam("maxWorkers", 1000));
        workers.setQueueTime(conn.getLongParam("queueTime", 0));
        useWorkers = conn.getStringParam("minWorkers") != null || conn.getStringParam("maxWorkers") != null;

        int selectorCount = conn.getIntParam("selectors", 32);
        if (selectorCount > selectors.length) {
            SelectorThread[] newSelectors = Arrays.copyOf(selectors, selectorCount);
            for (int i = selectors.length; i < selectorCount; i++) {
                newSelectors[i] = new SelectorThread(this, i);
                newSelectors[i].start();
            }
            selectors = newSelectors;
        }

        return true;
    }

    public void start() {
        running = true;
        for (SelectorThread selector : selectors) {
            selector.start();
        }
        acceptor.start();
        if (cleanup != null) {
            cleanup.start();
        }
    }

    public void stop() {
        running = false;
        if (cleanup != null) {
            cleanup.shutdown();
            cleanup = null;
        }
        if (acceptor != null) {
            acceptor.shutdown();
            acceptor = null;
        }
        if (selectors != null) {
            for (SelectorThread selector : selectors) {
                selector.shutdown();
            }
            selectors = null;
        }
        if (workers != null) {
            workers.shutdownNow();
            workers = null;
        }
    }


    public abstract Session createSession(Socket socket); /*{
        return new Session(socket);
    }*/

    @Override
    public final boolean isRunning() {
        return running;
    }

    @Override
    public int getConnections() {
        int result = 0;
        for (SelectorThread selector : selectors) {
            result += selector.selector.size();
        }
        return result;
    }

    @Override
    public boolean getWorkersUsed() {
        return useWorkers;
    }

    @Override
    public int getWorkers() {
        return workers.getPoolSize();
    }

    @Override
    public int getWorkersActive() {
        return workers.getActiveCount();
    }

    @Override
    public long getAcceptedSessions() {
        return acceptor.acceptedSessions;
    }

    @Override
    public int getSelectorCount() {
        return selectors.length;
    }

    @Override
    public double getSelectorAvgReady() {
        return selectorStats.getAvgReady();
    }

    @Override
    public int getSelectorMaxReady() {
        return selectorStats.getMaxReady();
    }

    @Override
    public long getSelectorOperations() {
        return selectorStats.getOperations();
    }

    @Override
    public long getSelectorSessions() {
        return selectorStats.getSessions();
    }

    @Override
    public double getQueueAvgLength() {
        return queueStats.getAvgLength();
    }

    @Override
    public long getQueueAvgBytes() {
        return queueStats.getAvgBytes();
    }

    @Override
    public long getQueueMaxLength() {
        return queueStats.getMaxLength();
    }

    @Override
    public long getQueueMaxBytes() {
        return queueStats.getMaxBytes();
    }

    @Override
    public void reset() {
        acceptor.acceptedSessions = 0;
        for (SelectorThread selector : selectors) {
            selector.operations = 0;
            selector.sessions = 0;
            selector.maxReady = 0;
        }
    }

    public final void asyncExecute(Runnable command) {
        workers.execute(command);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.fatal("Fatal error in " + t, e);
    }

    private class SelectorStats {
        private long expireTime;
        private long operations;
        private long sessions;
        private int maxReady;

        synchronized long getOperations() {
            ensureRecent();
            return operations;
        }

        synchronized long getSessions() {
            ensureRecent();
            return sessions;
        }

        synchronized double getAvgReady() {
            ensureRecent();
            return operations == 0 ? 0 : ((double) sessions) / operations;
        }

        synchronized int getMaxReady() {
            ensureRecent();
            return maxReady;
        }

        private void ensureRecent() {
            long currentTime = System.currentTimeMillis();
            if (currentTime < expireTime) {
                return;
            }

            long operations = 0;
            long sessions = 0;
            int maxSelected = 0;
            for (SelectorThread selector : selectors) {
                operations += selector.operations;
                sessions += selector.sessions;
                maxSelected = Math.max(maxSelected, selector.maxReady);
            }

            this.operations = operations;
            this.sessions = sessions;
            this.maxReady = maxSelected;
            this.expireTime = currentTime + 1000;
        }
    }

    private class QueueStats {
        private long expireTime;
        private long totalLength;
        private long totalBytes;
        private long maxLength;
        private long maxBytes;
        private int sessions;

        synchronized double getAvgLength() {
            ensureRecent();
            return sessions == 0 ? 0.0 : ((double) totalLength) / sessions;
        }

        synchronized long getAvgBytes() {
            ensureRecent();
            return sessions == 0 ? 0 : totalBytes / sessions;
        }

        synchronized long getMaxLength() {
            ensureRecent();
            return maxLength;
        }

        synchronized long getMaxBytes() {
            ensureRecent();
            return maxBytes;
        }

        private void ensureRecent() {
            long currentTime = System.currentTimeMillis();
            if (currentTime < expireTime) {
                return;
            }

            int sessions = 0;
            long totalLength = 0;
            long totalBytes = 0;
            long maxLength = 0;
            long maxBytes = 0;
            long[] stats = new long[2];

            for (SelectorThread selector : selectors) {
                for (Session session : selector.selector) {
                    session.getQueueStats(stats);
                    sessions++;
                    totalLength += stats[0];
                    totalBytes += stats[1];
                    if (stats[0] > maxLength) maxLength = stats[0];
                    if (stats[1] > maxBytes) maxBytes = stats[1];
                }
            }

            this.totalLength = totalLength;
            this.totalBytes = totalBytes;
            this.maxLength = maxLength;
            this.maxBytes = maxBytes;
            this.sessions = sessions;
            this.expireTime = currentTime + 1000;
        }
    }
}
