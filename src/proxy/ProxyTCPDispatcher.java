package proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class ProxyTCPDispatcher extends Thread {
	private ServerSocket sock;
	private ProxyMain father;
	private Executor threads;
	private volatile boolean end = false;
	private int port;
	
	ProxyTCPDispatcher(int port, ProxyMain f) throws IOException {
		this.sock = new ServerSocket(port);
		this.port = port;
		this.father = f;
		threads = Executors.newCachedThreadPool();
	}
	
	void shutdown() throws UnknownHostException, IOException {
		Socket s = new Socket(InetAddress.getLocalHost(), port);
		s.close();
		end = true;
	}
	
	public void run() {
		Socket s = null;
		
		while (!end) {
			try {
				s = sock.accept();
			} catch (IOException e) {
				System.err.println("Could not accept");
			}
			
			try {
				if (s != null) {
					ProxyTCPInstance pti = new ProxyTCPInstance(this.father, s);
					threads.execute(pti);
				}
			} catch (IOException e) {
			}
		}
		
		try {
			sock.close();
		} catch (IOException e) {
			System.err.println("Error in closing stream: " + e.getMessage());
		}
		
		System.out.println("Proxy Dispatcher: exiting...");
	}
		
}
