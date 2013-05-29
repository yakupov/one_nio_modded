package oknio.test;

public abstract class TestDataGenerator {
	protected Object[] testData;
	
	public abstract Object[] generateTestData();

	public int size() {
		return Utils.sizeof(testData);
	}
}
