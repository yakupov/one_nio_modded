package one.nio.net;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

final class JavaSelector extends Selector {
    private final java.nio.channels.Selector impl;
    private final ConcurrentLinkedQueue<Session> pendingSessions;

    JavaSelector() throws IOException {
        this.impl = java.nio.channels.Selector.open();
        this.pendingSessions = new ConcurrentLinkedQueue<Session>();
    }

    @Override
    public final int size() {
        return impl.keys().size();
    }

    @Override
    public final void close() {
        try {
            impl.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    @Override
    public final void register(Session session) {
        session.selector = this;
        pendingSessions.add(session);
        impl.wakeup();
        
        System.out.println(session + " registered by JavaSelector");
    }

    @Override
    public final void unregister(Session session) {
        ((JavaSocket) session.socket).ch.keyFor(impl).cancel();
    }

    @Override
    public final void listen(Session session, int events) {
        ((JavaSocket) session.socket).ch.keyFor(impl).interestOps(events);
    }

    @Override
    public final Iterator<Session> iterator() {
        return iteratorFor(impl.keys());
    }

    @Override
    public final Iterator<Session> select() {
        try {
            do {
                registerPendingSessions();
            } while (impl.select() == 0);
        } catch (Exception e) {
            return iteratorFor(Collections.<SelectionKey>emptySet());
        }

        Set<SelectionKey> selectedKeys = impl.selectedKeys();
        Iterator<Session> result = iteratorFor(selectedKeys);
        selectedKeys.clear();
        return result;
    }

    public void registerPendingSessions() throws ClosedChannelException {
        for (Session session; (session = pendingSessions.poll()) != null; ) {
            SelectionKey sk = ((JavaSocket) session.socket).ch.register(impl, Session.READABLE, session);
            System.out.println("JS register: " + session);
            System.out.println("SK " + sk);
            System.out.println(impl.selectedKeys().size());
            
            System.out.println(((JavaSocket) session.socket).ch.isBlocking());
            System.out.println(((JavaSocket) session.socket).ch.isConnected());
            System.out.println(((JavaSocket) session.socket).ch.isConnectionPending());
            System.out.println(((JavaSocket) session.socket).ch.isOpen());
            System.out.println(((JavaSocket) session.socket).ch.isRegistered());
            try {
				System.out.println(((JavaSocket) session.socket).ch.getRemoteAddress());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            
            
            int i = -1;
			try {
				i = impl.select();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            System.out.println(i + " selected");
            
            
            System.out.println(sk.isAcceptable());
            System.out.println(sk.isConnectable());
            System.out.println(sk.isReadable());
            System.out.println(sk.isValid());
            System.out.println(sk.isWritable());
        }
    }

    private static Iterator<Session> iteratorFor(Set<SelectionKey> keys) {
        final Session[] sessions = new Session[keys.size() + 1];
        int i = 0;
        for (SelectionKey key : keys) {
            if (key.isValid()) {
                Session session = (Session) key.attachment();
                session.events = key.readyOps();
                sessions[i++] = session;
            }
        }

        return new Iterator<Session>() {
            private int next = 0;

            @Override
            public final boolean hasNext() {
                return sessions[next] != null;
            }

            @Override
            public final Session next() {
                return sessions[next++];
            }

            @Override
            public final void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
