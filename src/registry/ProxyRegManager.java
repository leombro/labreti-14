package registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

class ProxyRegManager extends Thread {
	private DatagramSocket sock;
	private DatagramPacket pack;
	private ProxyPool pp;
	
	ProxyRegManager(int port, DatagramPacket pack, ProxyPool pp) throws SocketException {
		super();
		this.sock = new DatagramSocket(port);
		this.pack = pack;
		this.pp = pp;
	}
	
	private Proxy getProxyFromPacket(DatagramPacket p) throws UnknownHostException {
		return new Proxy(p.getAddress().getHostAddress(), p.getPort()); 
	}
	
	private int getIDFromPacket(DatagramPacket p) {
		String[] s = (new String(p.getData())).trim().split(":");
		return Integer.parseInt(s[1]);
	}
	
	private void link() {
		byte[] buffer = (new String("ACK")).getBytes();
		DatagramPacket p = new DatagramPacket(buffer, buffer.length, pack.getAddress(), pack.getPort());
		try {
			sock.send(p);
			buffer = new byte[1024];
			p = new DatagramPacket(buffer, buffer.length);
			sock.receive(p);
			Proxy pr = getProxyFromPacket(p);
			pp.addProxy(pr);
			buffer = (new String("id:" + pr.getID())).getBytes();
			p = new DatagramPacket(buffer, buffer.length, pack.getAddress(), pack.getPort());
			sock.send(p);
			System.out.println("Proxy " + pr.getID() + " connected, with address " + pr.getAddress());
		} catch (IOException e) {
			System.err.println("Socket error: " + e.getMessage());
		} finally {
			sock.close();
		}
	}
	
	private void unlink() {
		int id = getIDFromPacket(pack);
		pp.removeProxy(id);
		System.out.println("Proxy " + id + " disconnetted");
	}
	
	public void run() {
		String[] s = (new String(pack.getData())).trim().split(":");
		switch(s[0]) {
			case "REG":
				link();
				break;
			
			case "DEL":
				unlink();
				break;
			
			default:
				System.err.println("Proxy " + pack.getAddress() + " sent an unsupported message.");
				break;
		}
	}
	
}
