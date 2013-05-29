package oknio.test.config;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import oknio.test.TestDataGenerator;
import one.nio.net.ConnectionString;
import one.nio.rpc.AbstractRpcClient;

public class Configuration {
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface AddToLog {
	}
	
	//test
	protected TestDataGenerator testDataGenerator;
	@AddToLog
	protected Method testedMethod;
	
	//thread
	@AddToLog
	protected int runsCount;
	@AddToLog
	protected int sleepDelay;
	
	//client
	@AddToLog
	protected int bufferSize;
	@AddToLog
	protected int maxPoolSize;
	@AddToLog
	protected int clientsCount;
	@AddToLog
	protected int threadsPerClient;
	protected ConnectionString connString;
	@AddToLog
	protected ClientType clientType;
	
	public Configuration(TestDataGenerator gen, Method meth, int runsCount,
			int sleepDelay, int bufferSize, int maxPoolSize, int clientsCount,
			int threadsPerClient, ConnectionString connString, ClientType clientType) {
		super();
		this.testDataGenerator = gen;
		this.testedMethod = meth;
		this.runsCount = runsCount;
		this.sleepDelay = sleepDelay;
		this.bufferSize = bufferSize;
		this.maxPoolSize = maxPoolSize;
		this.clientsCount = clientsCount;
		this.threadsPerClient = threadsPerClient;
		this.connString = connString;
		this.clientType = clientType;
	}

	public TestDataGenerator getTestDataGenerator() {
		return testDataGenerator;
	}

	public Method getTestedMethod() {
		return testedMethod;
	}

	public int getRunsCount() {
		return runsCount;
	}

	public int getSleepDelay() {
		return sleepDelay;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public int getClientsCount() {
		return clientsCount;
	}

	public int getThreadsPerClient() {
		return threadsPerClient;
	}
	
	public String getMethName() {
		if (testedMethod == null)
			return null;
		
		return getTestedMethod().getDeclaringClass().getName() + "." + getTestedMethod().getName();
	}
	
	public String[] getLog(String[] addFields) {
		ArrayList<String> res = new ArrayList<String>();
		
		for (Field f : this.getClass().getDeclaredFields()) {
			if (f.getAnnotation(AddToLog.class) != null) {
				try {
					res.add(f.getName() + " = " +  f.get(this).toString());
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}
		}
		
		String[] resFull = new String[addFields.length + res.size()];
		System.arraycopy(addFields, 0, resFull, 0, addFields.length);
		System.arraycopy(res.toArray(), 0, resFull, addFields.length, res.size());
		return resFull;
	}

	public ConnectionString getConnString() {
		return this.connString;
	}
	
	public AbstractRpcClient<?> createRpcClient() throws IOException {
		return clientType.getCreator().createClient(this);
	}
}
