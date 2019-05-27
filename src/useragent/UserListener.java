package useragent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.Message;

class UserListener extends Thread {
	private int port;
	private volatile boolean end = false;
	
	UserListener(int port) {
		this.port = port;
	}
	
	void shutdown() {
		this.end = true;
	}
	
	public void run() {
		byte[] buf = null;
		DatagramPacket pack = null;
		DatagramSocket sck = null;
		try {
			sck = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Error in opening socket, exiting...");
			System.exit(-1);
		}
		while (!end) {
			buf = new byte[1024];
			pack = new DatagramPacket(buf, buf.length);
			try {
				sck.receive(pack);
				String parsable = new String(pack.getData()).trim();
				JSONObject obj = (JSONObject) new JSONParser().parse(parsable);
				Message m = Message.convertFromJSON(obj);
				System.out.println(m.getSender() + ": " + m.getContent());
				buf = null;
				pack = null;
			} catch (IOException | ParseException e) {
				e.printStackTrace();
				System.err.println("There was a problem in receiving a message.");
			}
		}
		sck.close();
	}
}
