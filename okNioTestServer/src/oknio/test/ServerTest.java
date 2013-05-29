package oknio.test;

import one.nio.net.ConnectionString;
import one.nio.rpc.ByteArrayDefaultRpcServer;
import one.nio.rpc.ByteBufferDefaultRpcServer;
import one.nio.rpc.DefaultRpcServer;

public class ServerTest {
	public static void main(String[] args) throws Exception {
		String port = "12346";
		if (args.length > 0 && args[0].length() > 0) {
			port = args[0];
		}
		
		String addr = "192.168.1.249";
		if (args.length > 1 && args[1].length() > 0) {
			addr = args[1];
		}
		
		final ConnectionString connection = new ConnectionString("http://" + addr + ":" + port);
		final DefaultRpcServer server = new ByteArrayDefaultRpcServer(connection, new TestService(), 8000);
		//final DefaultRpcServer server = new ByteBufferDefaultRpcServer(connection, new TestService(), 8000, 1000);
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
