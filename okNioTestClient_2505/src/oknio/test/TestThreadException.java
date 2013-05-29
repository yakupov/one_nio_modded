package oknio.test;

public class TestThreadException extends RuntimeException {
	private static final long serialVersionUID = 5786184887002640757L;

	public TestThreadException(String message) {
		super(message);
	}

	public TestThreadException(String message, Throwable cause) {
		super(message, cause);
	}
}
