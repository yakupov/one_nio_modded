package oknio.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import oknio.test.config.ClientType;
import oknio.test.config.Configuration;
import one.nio.net.ConnectionString;
import one.nio.rpc.AbstractRpcClient;
import one.nio.rpc.RemoteMethodCall;


public class ClientTest {	
	public static final double THRESHOLD = 1.1; //10%
	
	public static void main(String[] args) throws Exception {
		String host = "192.168.1.249";
		int port = 12346;
		ConnectionString connString = new ConnectionString("http://" + host + ":" + String.valueOf(port));
		TestDataGenerator gen = new TestStringArrGenerator();
		
		//gen = new TestNullGenerator();
		//succ
//		Configuration conf = new Configuration(
//				gen,
//				//getMethod(MethodTrashBin.class, "hello", gen),
//				getMethod(MethodTrashBin.class, "concatShortRes", gen),
//				50,
//				0,
//				8000,
//				1000,
//				2,
//				200,
//				connString,
//				ClientType.BYTE_BUF_CLIENT
//		);
		
//		Configuration conf = new Configuration(
//				gen,
//				//getMethod(MethodTrashBin.class, "hello", gen),
//				getMethod(MethodTrashBin.class, "concatShortRes", gen),
//				10000,
//				0,
//				8000,
//				100,
//				1,
//				1,
//				connString,
//				ClientType.BYTE_BUF_CLIENT
//		);

//		Configuration conf = new Configuration(
//				gen,
//				//getMethod(MethodTrashBin.class, "hello", gen),
//				getMethod(MethodTrashBin.class, "concatShortRes", gen),
//				100,
//				0,
//				8000,
//				100,
//				2,
//				30,
//				connString,
//				ClientType.BYTE_BUF_CLIENT
//		);
		
//		Configuration conf = new Configuration(
//				gen,
//				//getMethod(MethodTrashBin.class, "hello", gen),
//				getMethod(MethodTrashBin.class, "concatShortRes", gen),
//				1000,
//				3,
//				8000,
//				100,
//				3,
//				15,
//				connString,
//				ClientType.BYTE_BUF_CLIENT
//		);
		
		
//		Configuration conf = new Configuration(
//				gen,
//				getMethod(MethodTrashBin.class, "concatShortRes", gen),
//				3,
//				0,
//				8000,
//				100,
//				1,
//				1,
//				connString,
//				ClientType.BYTE_BUF_CLIENT
//		);
//		Marshaller m = JAXBContext.newInstance(conf.getClass()).createMarshaller();
//		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//		m.marshal(conf, new File("conf.xml"));
//		if (true)
//			return;
		
		Unmarshaller um = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
		Configuration conf = (Configuration) um.unmarshal(new File("conf.xml")); 
		conf.init();
		/*
		Marshaller m = JAXBContext.newInstance(conf.getClass()).createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(conf, System.out);
		
		System.out.println(conf.getConnString().toString());
		
		if (true) {
			return;
		}
		*/
		String[] results = parallelRun(conf);
		final String fileName = "results_last.csv";
		sendResults(fileName, results, conf);
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
	private static void sendResults(String fileName, String[] data, Configuration conf) {
		try {
			AbstractRpcClient<?> client = conf.createRpcClient();
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
	public static String[] parallelRun(Configuration conf) 
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
		double worstAvgThroughputArg = -1;
		double worstAvgThroughputRet = -1;
		double avgAvgLatency = 0;
		double avgAvgThroughputArg = 0;
		double avgAvgThroughputRet = 0;
		for (TestThread runner : runners) {
			double currAvgLatency = runner.getAvgLatency(THRESHOLD);
			double currAvgThroughputArg = runner.getAvgThroughputArg(THRESHOLD);
			double currAvgThroughputRet = runner.getAvgThroughputRet(THRESHOLD);
			worstAvgLatency = Math.max(worstAvgLatency, currAvgLatency);			
			worstAvgThroughputArg = Math.max(worstAvgThroughputArg, currAvgThroughputArg);
			worstAvgThroughputRet = Math.max(worstAvgThroughputRet, currAvgThroughputRet);
			avgAvgLatency += currAvgLatency;
			avgAvgThroughputArg += currAvgThroughputArg;
			avgAvgThroughputRet += currAvgThroughputRet;
		}
		avgAvgLatency /= runners.size();
		avgAvgThroughputArg /= runners.size();
		avgAvgThroughputRet /= runners.size();
		
		System.out.printf("Worst average latency (s) = %f\n", worstAvgLatency);
		System.out.printf("Average average latency (s) = %f\n", avgAvgLatency);
		System.out.printf("Worst average throughput (Mb/s) Arg = %f\n", worstAvgThroughputArg / 1e6);
		System.out.printf("Average average throughput (Mb/s) Arg = %f\n", avgAvgThroughputArg / 1e6);
		System.out.printf("Worst average throughput (Mb/s) Ret = %f\n", worstAvgThroughputRet / 1e6);
		System.out.printf("Average average throughput (Mb/s) Ret = %f\n", avgAvgThroughputRet / 1e6);
		
		return conf.getLog(new String[] {
			String.format("%f", worstAvgLatency), 
			String.format("%f", avgAvgLatency), 
			String.format("%f", worstAvgThroughputArg / 1e6),
			String.format("%f", avgAvgThroughputArg / 1e6), 
			String.format("%f", worstAvgThroughputRet / 1e6),
			String.format("%f", avgAvgThroughputRet / 1e6), 
			new Date().toString(), 
			conf.getComment()
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
		
		for (int i = 0; i < conf.getClientsCount(); ++i) {
			AbstractRpcClient<?> client = conf.createRpcClient();
			for (int j = 0; j < conf.getThreadsPerClient(); ++j) {
				runners.add(new TestThread(client, conf));
			}
		}
		
		return runners;
	}
}
