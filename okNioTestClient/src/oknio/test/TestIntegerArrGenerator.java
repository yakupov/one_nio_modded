package oknio.test;

import java.util.ArrayList;

public class TestIntegerArrGenerator extends TestDataGenerator {
	@Override
	public Object[] generateTestData() {
		ArrayList<Integer> tTestData = new ArrayList<Integer>();
		for (int i = 0; i < 100; ++i) {
			for (int j = 2; j < 6; ++j) {
				tTestData.add(i * j);
			}
		}
		return testData = tTestData.toArray(new Integer[0]);
	}
}
