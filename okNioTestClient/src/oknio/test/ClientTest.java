package oknio.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import oknio.test.config.Configuration;
import one.nio.os.NativeLibrary;
import one.nio.rpc.AbstractRpcClient;
import one.nio.rpc.RemoteMethodCall;
import sun.org.mozilla.javascript.internal.NativeArray;


public class ClientTest {	
	public static double THRESHOLD = 1.1; //10%
	
	private static ArrayList<AbstractRpcClient<?>> clients;
	
	public static void main(String[] args) throws Exception {	
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
		
		String confFileName = "conf2.xml";
		if (args.length > 0 && args[0] != null && args[0].length() > 0)
			confFileName = args[0];
		
		String resultsFileName = "results_last.csv";
		if (args.length > 1 && args[1] != null && args[1].length() > 0)
			resultsFileName = args[1];
		
		if (args.length > 2 && args[2] != null)
			THRESHOLD = Double.parseDouble(args[2]);
		
		Unmarshaller um = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
		Configuration conf = (Configuration) um.unmarshal(new File(confFileName)); 
		conf.init();
		
		System.out.println("Native: " + NativeLibrary.IS_SUPPORTED);
		
		String[] results = parallelRun(conf);
		sendResults(resultsFileName, results, conf);
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
			//client.stop();
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
		
		//for (AbstractRpcClient<?> cl : clients) {
		//	cl.stop();
		//}
		
		double worstAvgLatency = -1;
		double worstAvgThroughputArg = Double.POSITIVE_INFINITY;
		double worstAvgThroughputRet = Double.POSITIVE_INFINITY;
		double avgAvgLatency = 0;
		double avgAvgThroughputArg = 0;
		double avgAvgThroughputRet = 0;
		for (TestThread runner : runners) {
			double currAvgLatency = runner.getAvgLatency(THRESHOLD);
			double currAvgThroughputArg = runner.getAvgThroughputArg(THRESHOLD);
			double currAvgThroughputRet = runner.getAvgThroughputRet(THRESHOLD);
			worstAvgLatency = Math.max(worstAvgLatency, currAvgLatency);			
			worstAvgThroughputArg = Math.min(worstAvgThroughputArg, currAvgThroughputArg);
			worstAvgThroughputRet = Math.min(worstAvgThroughputRet, currAvgThroughputRet);
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
		clients = new ArrayList<AbstractRpcClient<?>>();
		
		for (int i = 0; i < conf.getClientsCount(); ++i) {
			AbstractRpcClient<?> client = conf.createRpcClient();
			clients.add(client);
			for (int j = 0; j < conf.getThreadsPerClient(); ++j) {
				runners.add(new TestThread(client, conf));
			}
		}
		
		return runners;
	}
}
