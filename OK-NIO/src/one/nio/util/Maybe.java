package one.nio.util;

public class Maybe {
	protected Object value;
	
	public Maybe(Object value) {
		this.value = value;
	}
	
	public Object get() {
		return value;
	}
}
