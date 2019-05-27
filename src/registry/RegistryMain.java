package registry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RegistryMain extends Thread { // NO_UCD (unused code)
	
	private DatagramSocket sock;
	private ExecutorService x;
	private ProxyPool pp;
	private int count = 1;
	private volatile boolean running = true;
	
	public RegistryMain(ExecutorService x, ProxyPool pp) throws SocketException {
		sock = new DatagramSocket(55123);
		this.x = x;
		this.pp = pp;
	}
	
	public void shutdown() throws IOException {
		DatagramSocket sock2 = new DatagramSocket(55124);
		sock2.send(new DatagramPacket(new byte[1], 1, InetAddress.getLocalHost(), 55123));
		sock2.close();
		running = false;
	}
	
	public void run() {
		byte[] buf; 
		DatagramPacket p;
		while (running) {
			buf = new byte[1024];
			p = new DatagramPacket(buf, buf.length);
			try {
				sock.receive(p);
				System.out.println("Received a REG request");
				x.execute(new ProxyRegManager(55124 + count++, p, pp));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		sock.close();
		System.out.println("RegistryMain has stopped.");
	}
	

	public static void main(String[] args) throws IOException, InterruptedException {
		ProxyPool pp = new ProxyPool();
		ExecutorService threads = Executors.newFixedThreadPool(12);
		
		RegistryRMI reg = new RegistryRMI(InetAddress.getLocalHost().getHostAddress(), pp);
		Thread regThread = new Thread(reg);
		threads.execute(regThread);
		
		RegistryMain rm = new RegistryMain(threads, pp);
		threads.execute(rm);
		
		System.out.println("Registry set up on address " + InetAddress.getLocalHost().getHostAddress() + ". Write \"shutdown\" to exit.");
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		String s = "lol";
		while (!s.equalsIgnoreCase("shutdown")) {
			s = bf.readLine();
			if (s.equals("poll")) {
				reg.poll();
				pp.proxyPoll();
			}
		}
		rm.shutdown();
		reg.shutdown();
		
		System.out.println("Exiting...");
		threads.awaitTermination(30, TimeUnit.SECONDS);
		System.exit(0);
		
	}

}
