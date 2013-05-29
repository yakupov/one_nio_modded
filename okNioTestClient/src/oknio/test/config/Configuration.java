package oknio.test.config;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import oknio.test.TestDataGenerator;
import one.nio.net.ConnectionString;
import one.nio.rpc.AbstractRpcClient;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Configuration {
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface AddToLog {
	}
	
	//tested method
	protected TestDataGenerator testDataGenerator;
	@XmlElement
	protected String tdgClassName;
	@AddToLog
	protected Method testedMethod;
	@XmlElement
	protected String methEnclosingClassName;
	@XmlElement
	protected String methName;
	
	//thread
	@AddToLog
	@XmlElement
	protected int runsCount;
	@AddToLog
	@XmlElement
	protected int sleepDelay;
	
	//client
	@AddToLog
	@XmlElement
	protected int bufferSize;
	@AddToLog
	@XmlElement
	protected int maxPoolSize;
	@AddToLog
	@XmlElement
	protected int clientsCount;
	@AddToLog
	@XmlElement
	protected int threadsPerClient;
	@AddToLog
	@XmlElement
	protected ClientType clientType;
	
	//server
	protected ConnectionString connString;
	@XmlElement
	protected String host;
	@XmlElement
	protected int port;	
	
	//misc
	@AddToLog
	@XmlElement
	protected String comment;

	
	/**
	 * Run this method ONLY AFTER this class was deserialized by JAXB
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public void init() throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, NoSuchMethodException, SecurityException {
		this.testDataGenerator = (TestDataGenerator) Class.forName(tdgClassName).newInstance();
		Object[] args = testDataGenerator.generateTestData();
		Class<?> methEnclosingClass = Class.forName(methEnclosingClassName);
		if (args != null)
			this.testedMethod = methEnclosingClass.getDeclaredMethod(methName, args.getClass());
		else
			this.testedMethod = methEnclosingClass.getDeclaredMethod(methName);
		
		this.connString = new ConnectionString("http://" + host + ":" + String.valueOf(port));
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
	
	public String getComment() {
		return this.comment;
	}
	
	public AbstractRpcClient<?> createRpcClient() throws IOException {
		return clientType.getCreator().createClient(this);
	}
}
