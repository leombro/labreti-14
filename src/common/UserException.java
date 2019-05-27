package common;

public class UserException extends Exception {
	private static final long serialVersionUID = 1L;

	public UserException() {
		this("default");
	}
	
	public UserException(String s) {
		super(s);
	}
}
