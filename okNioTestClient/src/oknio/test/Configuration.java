package oknio.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Configuration {
	//test
	protected TestDataGenerator testDataGenerator;
	protected Method testedMethod;
	
	//thread
	protected int runsCount;
	protected int sleepDelay;
	
	//client
	protected int bufferSize;
	protected int maxPoolSize;
	protected int clientsCount;
	protected int threadsPerClient;
	
	public Configuration(TestDataGenerator gen, Method meth, int runsCount,
			int sleepDelay, int bufferSize, int maxPoolSize, int clientsCount,
			int threadsPerClient) {
		super();
		this.testDataGenerator = gen;
		this.testedMethod = meth;
		this.runsCount = runsCount;
		this.sleepDelay = sleepDelay;
		this.bufferSize = bufferSize;
		this.maxPoolSize = maxPoolSize;
		this.clientsCount = clientsCount;
		this.threadsPerClient = threadsPerClient;
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
			try {
				res.add(f.getName() + " = " +  f.get(this).toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String[] resFull = new String[addFields.length + res.size()];
		System.arraycopy(addFields, 0, resFull, 0, addFields.length);
		System.arraycopy(res.toArray(), 0, resFull, addFields.length, res.size());
		return resFull;
	}
}
