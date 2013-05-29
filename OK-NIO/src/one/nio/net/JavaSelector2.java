package one.nio.net;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.Collections;

public class JavaSelector2 {
	protected final java.nio.channels.Selector impl;
	
	public JavaSelector2() throws IOException {
		impl = java.nio.channels.Selector.open();
	}
	
    public final void close() {
        try {
            impl.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    public final SelectionKey register(Socket socket, int interestSet) throws ClosedChannelException {
    	SelectionKey sk = ((JavaSocket)socket).ch.register(impl, interestSet);
        impl.wakeup();
        return sk;
    }

    public final Collection<SelectionKey> select() {
        try {
        	impl.select();
        } catch (Exception e) {
            return Collections.<SelectionKey>emptySet();
        }

        return impl.selectedKeys();
    }
}
