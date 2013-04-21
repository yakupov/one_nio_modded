package oknio.test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.csvreader.CsvWriter;

import one.nio.rpc.RemoteMethod;

public class MethodTrashBin {
	@RemoteMethod
	public static long sumxRand (Integer[] args) {
		long res = 0;
		
		if (args == null) {
			return res;
		}
		
		for (int i : args) {
			res += i;
		}
		
		return (long) (res * Math.random());
	}

	@RemoteMethod
	public static String concat(String[] args) {
		String res = "";
		
		if (args == null) {
			return res;
		}
		
		for (String s : args) {
			res += s + " ";
		}
		
		return res;
	}
	
	@RemoteMethod
	public static String concatShortRes(String[] args) {
		String res = "";
		
		if (args == null) {
			return res;
		}
		
		for (String s : args) {
			res += s + " ";
		}
		
		return String.valueOf(res.length());
	}
	
	@RemoteMethod
	public static String concatRand (String[] args) {
		String res = "";
		
		if (args == null) {
			return res;
		}
		
		for (String s : args) {
			if (Math.random() > 0.5)
				res += s + " ";
		}
		
		return res;
	}
	
	@RemoteMethod
	public static void saveToFile(String fileName, String str) throws IOException {
		FileOutputStream os = new FileOutputStream (fileName, true);
		os.write(str.getBytes());
		os.close();
	}
	
	@RemoteMethod
	public static void saveToCsvFile(String fileName, String[] data) throws IOException {
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
		CsvWriter csvWriter = new CsvWriter(writer, ',');
		csvWriter.writeRecord(data);
		csvWriter.close();
	}
	
	@RemoteMethod
	public static void oneSecondWorker() {
		fixedTimeWorker(1000);
	}
	
	@RemoteMethod
	public static void fixedTimeWorker(long mills) {
		long timeBefore = System.nanoTime();
		int j;
		while (System.nanoTime() < timeBefore + mills * 1e6) {
			int i = (int) (Math.random() * 100);
			j = i % 2;
			if (j == 3)
				return;
		}
	}
}
