package oknio.test;

import java.io.Serializable;
import java.util.concurrent.Callable;

import one.nio.rpc.RemoteMethod;

public class TestService implements Callable<String>, Serializable {
	private static final long serialVersionUID = -6532347254247392808L;
	
	@RemoteMethod
	public String call() throws Exception {
		String threadName = "Running in " + Thread.currentThread().getName();
		System.out.println(threadName);
		return threadName;
	}		
}
