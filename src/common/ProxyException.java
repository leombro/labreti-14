package common;

public class ProxyException extends Exception {
	private static final long serialVersionUID = 1L;

	public ProxyException() {
		this("default");
	}
	
	public ProxyException(String s) {
		super(s);
	}
}
