package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.simple.JSONArray;

class ProxyTCPInstance extends Thread {
	private ProxyMain father;
	private Socket sock;
	private BufferedReader read;
	private PrintWriter write;
	
	ProxyTCPInstance(ProxyMain f, Socket s) throws IOException {
		this.father = f;
		this.sock = s;
		read = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		write = new PrintWriter(sock.getOutputStream(), true);
	}
	
	@Override
	public void run() {
		try {
			String username = read.readLine().trim();
			JSONArray arr = father.getMessageList(username);
			if (arr != null) write.println(arr.toString());
			write.close();
			read.close();
			sock.close();
		} catch (IOException e) {
			System.err.println("Error in sending message list to " + sock.getInetAddress());
		}
	}
}
