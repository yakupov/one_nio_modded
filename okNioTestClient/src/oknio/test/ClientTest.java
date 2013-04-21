package oknio.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import one.nio.net.ConnectionString;
import one.nio.rpc.AbstractRpcClient;
import one.nio.rpc.ByteArrayRpcClient;
import one.nio.rpc.ByteBufferRpcClient;
import one.nio.rpc.RemoteMethodCall;


public class ClientTest {
	public static final String host = "192.168.1.249";
	public static final int port = 12346;
	
	public static void main(String[] args) throws Exception {
		final String fileName = "results.csv";
		
		TestDataGenerator gen = new TestStringArrGenerator();
		Configuration conf = new Configuration(
				gen, 
				getMethod(MethodTrashBin.class, "concatShortRes", gen), 
				100, 3, 8000, 100, 1, 15);
		String[] results = parallelRun(conf, "ByteArrays, -server flag on server");
		sendResults(fileName, results);
	}
	
	
	/**
	 * 
	 * @param containerClass
	 * @param methodName
	 * @param gen
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Method getMethod (Class<?> containerClass, String methodName, TestDataGenerator gen) 
			throws NoSuchMethodException, SecurityException {
		Object[] args = gen.generateTestData();
		if (args != null)
			return containerClass.getDeclaredMethod(methodName, gen.generateTestData().getClass());
		return containerClass.getDeclaredMethod(methodName);
	}
	
	
	/**
	 * Send results to server in order to store them there
	 * @param fileName
	 * @param data
	 */
	private static void sendResults(String fileName, String[] data) {
		String connString = "http://" + host + ":" + String.valueOf(port);
		ConnectionString connection = null;
		connection = new ConnectionString(connString);
		
		try {
			AbstractRpcClient<?> client = new ByteBufferRpcClient(connection, 8000, 100);
			client.invoke(new RemoteMethodCall(MethodTrashBin.class.getDeclaredMethod("saveToCsvFile", 
					String.class, String[].class), new Object[] {fileName, data}));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Run #conf.threadsPerClient requests on #conf.clientsCount clients sequentially #conf.runsCount times
	 * 
	 * @param conf
	 * @param comment - for log
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static String[] parallelRun(Configuration conf, String comment) 
			throws InterruptedException, IOException {
		List<TestThread> runners = createRunners(conf);
		List<Thread> threads = startThreads(runners);
		
		System.out.printf("%d clients with %d threads each were started at %s, the executed method is %s\n", 
				conf.getClientsCount(), conf.getThreadsPerClient(), new Date().toString(), conf.getMethName());
		for (Thread currThread : threads) {
			currThread.join();
		}
		
		System.out.println("All threads have finished their work by " + new Date().toString() + 
				", the executed method is " + conf.getMethName());
		
		double worstAvgLatency = -1;
		double avgAvgLatency = 0;
		double avgAvgThroughputOut = 0;
		for (TestThread runner : runners) {
			worstAvgLatency = Math.max(worstAvgLatency, runner.getAvgLatency());
			avgAvgLatency += runner.getAvgLatency();
			avgAvgThroughputOut += runner.getAvgThroughput();
		}
		avgAvgLatency /= runners.size();
		double avgAvgThroughputIn = ((double)conf.getTestDataGenerator().size()) / avgAvgLatency;
		
		System.out.printf("Worst average latency = %f, the executed method is %s\n", 
				worstAvgLatency, conf.getMethName());
		System.out.printf("Average average latency = %f, the executed method is %s\n",  
				avgAvgLatency, conf.getMethName());
		System.out.printf("Average average throughput out = %f, the executed method is %s\n", 
				avgAvgThroughputOut / 1e6, conf.getMethName());
		System.out.printf("Average average throughput in = %f, the executed method is %s\n", 
				avgAvgThroughputIn / 1e6, conf.getMethName());
		
		return conf.getLog(new String[] {
			String.format("%f", worstAvgLatency), 
			String.format("%f", avgAvgLatency), 
			String.format("%f", avgAvgThroughputOut / 1e6), 
			String.format("%f", avgAvgThroughputIn / 1e6), 
			new Date().toString(), 
			comment
		});
	}
	
	private static List<Thread> startThreads(List<TestThread> runners) {
		List<Thread> threads = new ArrayList<Thread>();
		for (TestThread runner : runners) {
			Thread currThread = new Thread(runner);
			threads.add(currThread);
			currThread.start();
		}
		return threads;
	}

	private static List<TestThread> createRunners(Configuration conf) throws IOException {
		List<TestThread> runners = new ArrayList<TestThread>();
		
		String connString = "http://" + host + ":" + String.valueOf(port);
		ConnectionString connection = new ConnectionString(connString);

		for (int i = 0; i < conf.getClientsCount(); ++i) {
			AbstractRpcClient<?> client = new ByteBufferRpcClient(connection, conf.getBufferSize(), 
					conf.getMaxPoolSize());
			client = new ByteArrayRpcClient(connection);
			
			for (int j = 0; j < conf.getThreadsPerClient(); ++j) {
				runners.add(new TestThread(client, conf));
			}
		}
		
		return runners;
	}
}
