package oknio.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import oknio.test.config.Configuration;
import one.nio.rpc.AbstractRpcClient;
import one.nio.rpc.RemoteMethodCall;

public class TestThread implements Runnable {
	public static class Pair {
		public Pair(double latency, int size) {
			this.latency = latency;
			this.size = size;
		}
		
		public double latency;
		public int size;
	}
	
	protected final AbstractRpcClient<?> 		client;
	protected Object[]				testData;
	protected Method				method;
	protected List<String>			log;
	protected List<Pair>	 		results;
	protected int					runsCount;
	protected int					delay;
	
	public TestThread (AbstractRpcClient<?> client, Configuration conf) throws TestThreadException {
		this.client = client;
		this.testData = conf.getTestDataGenerator().generateTestData();
		this.method = conf.getTestedMethod();
		this.log = new ArrayList<String>();
		this.results = new ArrayList<Pair>();
		this.delay = conf.getSleepDelay();
		this.runsCount = conf.getRunsCount();
	}

	/**
	 * Execute test $numberOfTimes with $sleepInterval milliseconds between the runs
	 * @param numberOfTimes
	 * @param sleepInterval
	 * @throws InterruptedException 
	 */
	public void execTest(int numberOfTimes, int sleepInterval) throws InterruptedException {
		results.clear();
		
		for (int i = 0; i < numberOfTimes; ++i) {
			Pair currRun = execTestOnce();
			results.add(currRun);
			Thread.sleep(sleepInterval);
		}
	}
	
	public Pair execTestOnce() {
		long nanoBefore = System.nanoTime();
		Object rsp = null;
		try {
			if (testData != null)
				rsp = client.invoke(new RemoteMethodCall(method, new Object[] {testData}));
			else
				rsp = client.invoke(new RemoteMethodCall(method));
		} catch (Exception e) {
			throw new TestThreadException ("Failed to invoke " + method.getName() + 
					(testData != null ? " with a single argument of a type " + testData.getClass().getName() : 
					" with no arguments."), e);
		}
		long nanoAfter = System.nanoTime();
		double latency = ((double)(nanoAfter - nanoBefore)) / 1e9;
		
		log.add("latency = " + String.valueOf(latency));
		if (rsp != null)
			log.add("--> response = " + rsp.toString());
		
		return new Pair(latency, Utils.sizeof(rsp));
	}
	
	public List<Pair> getResults() {
		return results;
	}
	
	/**
	 * Get average latency, excluding the first run
	 * @return
	 */
	public double getAvgLatency() {
		double avg = 0;
		for (int i = 1; i < results.size(); ++i) {
			avg += results.get(i).latency;
		}
		avg /= (results.size() - 1);
		return avg;
	}
	
	public String[] getLog() {
		return log.toArray(new String[0]);
	}
	
	/**
	 * Get average size, excluding the first run
	 * @return
	 */
	public double getAvgSize() {
		double avg = 0;
		for (int i = 1; i < results.size(); ++i) {
			avg += results.get(i).size;
		}
		avg /= (results.size() - 1);
		return avg;
	}
	
	public double getAvgThroughput() {
		return getAvgSize() / getAvgLatency();
	}
		
	@Override
	public void run() {
		try {
			execTest(runsCount, delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
