package oknio.test;

import one.nio.net.ConnectionString;
import one.nio.rpc.ByteArrayDefaultRpcServer;
import one.nio.rpc.ByteBufferDefaultRpcServer;
import one.nio.rpc.DefaultRpcServer;

public class ServerTest {
	public static void main(String[] args) throws Exception {
		final ConnectionString connection = new ConnectionString("http://192.168.1.249:12346");
		final DefaultRpcServer server = new ByteArrayDefaultRpcServer(connection, new TestService(), 8000);
		//final DefaultRpcServer server = new ByteBufferDefaultRpcServer(connection, new TestService(), 8000, 100);
		server.start();

		server.registerRemoteMethods(MethodTrashBin.class);
		
		while (true) {
			int i = System.in.read();
			if (i == 'q') 
				break;
		}
		
		server.stop();
	}
}
