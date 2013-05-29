package oknio.test;

import java.util.ArrayList;

public class TestStringArrGenerator extends TestDataGenerator {
	@Override
	public Object[] generateTestData() {
		ArrayList<String> tTestData = new ArrayList<String>();
		for (int i = 0; i < 100; ++i) {
			for (int j = 2; j < 6; ++j) {
				StringBuilder sb = new StringBuilder();
				for (int k = 0; k < j; ++k)
					sb.append("HA");
				tTestData.add(sb.toString());
			}
		}
		return testData = tTestData.toArray(new String[0]);
	}

}
