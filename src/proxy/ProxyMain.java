package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.Message;

class ProxyMain {
	private Map<String, JSONArray> db;
	private ProxyTCPDispatcher ptd;
	private ProxyUDPReceiver pur;
	private int id;
	
	public ProxyMain() {
		db = Collections.synchronizedMap(new HashMap<String, JSONArray>());
	}
	
	@SuppressWarnings("unchecked")
	synchronized JSONArray getMessageList(String name) {
		JSONArray ja = new JSONArray();
		JSONArray jb = db.get(name);
		if (jb == null) return null;
		for (Object o: jb) {
			ja.add((JSONObject) o);
		}
		db.remove(name);
		return ja;
	}
	
	synchronized void putNewUser(String name) {
		db.put(name, new JSONArray());
	}
	
	@SuppressWarnings("unchecked")
	synchronized void putMessage(Message m) {
		String name = m.getReceiver();
		JSONArray ja = db.get(name);
		if (ja != null) ja.add(m.getJSON());
	}
	
	public int getID() {
		return this.id;
	}
	
	public void setID (int i) {
		this.id = i;
		System.out.println("Current id:" + this.id);
	}
	
	private void run(String[] args) throws IOException, InterruptedException {
		ExecutorService threads = Executors.newFixedThreadPool(2);
		int port = Integer.parseInt(args[0].trim());
		try {
			ptd = new ProxyTCPDispatcher(port, this);
		} catch (IOException e) {
			System.err.println("Error in setting up TCP socket; exiting...");
			System.exit(-1);
		}
		try {
			pur = new ProxyUDPReceiver(port, this);
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
			System.err.println("Error in setting up UDP socket; exiting...");
			System.exit(-1);
		}
		threads.execute(ptd);
		threads.execute(pur);
		
		System.out.println("GOSSIP Proxy: started. Type \"shutdown\" to exit: ");
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		String s = "lol";
		try {
			while (!s.equalsIgnoreCase("shutdown")) {
				s = bf.readLine();
			}
		} catch (IOException e) {
			System.err.println("Cannot read from command line!");
		}
		
		pur.shutdown();
		ptd.shutdown();
		
		threads.awaitTermination(30, TimeUnit.SECONDS);
		System.out.println("Proxy: exiting...");
		System.exit(0);
		
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		if (args.length != 1) {
			System.err.println("Invalid number of arguments.");
			System.err.println("Usage: java -jar proxy <port>");
			System.exit(-1);
		}
		
		new ProxyMain().run(args);
	}
}
