package proxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.Message;

class ProxyUDPReceiver extends Thread {
	private DatagramSocket sock;
	private ProxyMain father;
	private volatile boolean end = false;
	private String registryAddress = "172.241.0.1";
	private InetAddress regAdd;
	private int registryPort = 55123;
	private int thisPort;
	
	ProxyUDPReceiver(int port, ProxyMain pr) throws SocketException, UnknownHostException {
		this.thisPort = port;
		this.father = pr;
		this.regAdd = InetAddress.getByName(registryAddress);
		try {
			this.sock = new DatagramSocket(port);
		} catch (SocketException e) {
			throw new SocketException("UDP Socket error! " + e.getMessage());
		}
	}
	
	void shutdown() throws IOException {
		end = true;
		DatagramSocket sck = new DatagramSocket(thisPort+1);
		byte[] buffer = new String("POKE").getBytes();
		DatagramPacket pk = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), thisPort);
		sck.send(pk);
		sck.close();
	}
	
	private Message getMessage(String s) throws ParseException {
		JSONObject obj = (JSONObject) new JSONParser().parse(s);
		return Message.convertFromJSON(obj);
	}
	
	private void manageUsers(String s) throws IOException {
		String[] command = s.split(":");
		if (command.length != 2) throw new IOException();
		switch (command[0]) {
			case "USR":
				father.putNewUser(command[1].trim());
				break;
			
			default:
				throw new IOException();
		}
	}
	
	public void run() {
		byte[] buf;
		DatagramPacket pack = null;
		Message m;
		
		buf = new String("REG:newproxy").getBytes();
		pack = new DatagramPacket(buf, buf.length, regAdd, registryPort);
		try {
			sock.send(pack);
			System.out.println("REGREQUEST sent");
			buf = new byte[1024];
			pack = new DatagramPacket(buf, buf.length);
			sock.receive(pack);
			String s = new String(pack.getData()).trim();
			if (!s.equals("ACK")) throw new IOException();
			System.out.println("ACK received");
			buf = new String("pijatestostracciofracico").trim().getBytes();
			pack = new DatagramPacket(buf, buf.length, pack.getAddress(), pack.getPort());
			sock.send(pack);
			System.out.println("Data sent");
			buf = new byte[1024];
			pack = new DatagramPacket(buf, buf.length);
			sock.receive(pack);
			System.out.println("ID Received");
			s = new String(pack.getData()).trim();
			String[] contID = s.split(":");
			father.setID(Integer.parseInt(contID[1].trim()));
		} catch (IOException e2) {
			e2.printStackTrace();
			System.err.println("Cannot connect to registry. Exiting...");
			System.exit(-1);
		}
		
		while(!end) {
			buf = new byte[1024];
			pack = new DatagramPacket(buf, buf.length);
			try {
				sock.receive(pack);
			} catch (IOException e2) {
				e2.printStackTrace();
				System.err.println("Error in receiving message!");
			}
			try {
				m = getMessage(new String(pack.getData()).trim());
				System.out.println("Received a message from " + m.getSender() + " to " + m.getReceiver() + " : " + m.getContent());
				father.putMessage(m);
			} catch (ParseException e) {
				try {
					manageUsers(new String(pack.getData()).trim());
				} catch (IOException e1) {
					if (!new String(pack.getData()).trim().equals("POKE"))
						System.err.println("Cannot parse a message sent by " + pack.getAddress());
				}
			}
		}
		
		buf = new String("DEL:" + father.getID()).getBytes();
		pack = new DatagramPacket(buf, buf.length, regAdd, registryPort);
		try {
			sock.send(pack);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error in sending logout request");
		}
		
		
		sock.close();
		System.out.println("UDP Server stopped!");
	}
	
	
}
