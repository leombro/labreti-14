package useragent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.Address;
import common.Message;
import common.ProxyException;
import common.RegistryOperations;
import common.UserException;

class UserAgent {
	
	private static final String registryRMI =
			"rmi://172.241.0.1:49555/GossipRegistryService";
	
	private Map<String, Address> friendCache = null;
	private List<String> allowList = null;
	private List<String> friendList = null;
	private String myName = null;
	private String myCallback = null;
	private String address = null;
	private UserRMI rmi = null;
	private Thread rmithread = null;
	private UserListener listener = null;
	private Thread listthread = null;
	private boolean online = false;
	
	void updateAllowList(List<String> l) {
		allowList = l;
	}
	
	void updateFriendList(List<String> l, Map<String, Address> m) {
		friendList = l;
		friendCache = m;
	}
	
	void updateFriendAddress(String name, Address addr) {
		friendCache.put(name, addr);
	}
	
	private void startRMI(Address addr) throws RemoteException {
		int port = addr.getPort() + 1;
		myCallback = new String("rmi://" + addr.getAddress() + ":" + port + "/GossipUAService");
		
		rmi = new UserRMI(port, addr.getAddress(), this);
		rmithread = new Thread(rmi);
		rmithread.start();
	}
	
	private void startListen(int port) {
		listener = new UserListener(port);
		listthread = new Thread(listener);
		listthread.start();
	}
	
	private List<String> extractMessages(JSONArray a) {
		List<String> lis = new Vector<String>();
		
		for (Object o: a) {
			Message m = Message.convertFromJSON((JSONObject) o);
			String s = new String(m.getSender() + ": " + m.getContent());
			lis.add(s);
		}
		return lis;
	}
	
	private void getOfflineMessages(Address a) throws IOException, ParseException {
		JSONArray arr = null;
		Socket sock = new Socket(a.getAddr(), a.getPort());
		BufferedReader b = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		PrintWriter w = new PrintWriter(sock.getOutputStream(), true);
		w.println(this.myName);
		String s = null;
		while ((s = b.readLine()) != null) {
			s = s.trim();
			arr = (JSONArray) new JSONParser().parse(s);
			if (arr.size() != 0) {
				System.out.println("You received messages while offline!");
				for (String e: this.extractMessages(arr)) {
					System.out.println(e);
				}
			}
		}
		w.close();
		b.close();
		sock.close();
	}
	
	private void run(String[] args) throws IOException {
		RegistryOperations regOps = null;
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		String input;
		boolean end = false;
		boolean notConnected;
		Address addr = null;
		
		if (args.length < 1 || args.length > 1) {
			System.err.println("Invalid number of arguments!");
			System.err.println("Usage: java -jar gossip <port>");
			System.exit(-1);
		}
		
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			System.err.println("Could not resolve an IP address. You can specify it, or leave blank to exit");
			address = bf.readLine().trim();
			if (address == null) System.exit(-1);
		}
		try {
			addr = new Address(address, Integer.parseInt(args[0]));
		} catch (NumberFormatException e1) {
			System.err.println("Wrong port format: it must be an integer.");
			System.err.println("Usage: java -jar gossip <port>");
			System.exit(-1);
		} catch (UnknownHostException e1) {
			System.err.println("Fatal error. Exiting...");
			System.exit(-1);
		}
		friendCache = new ConcurrentHashMap<String, Address>();
		
		System.out.println("**WELCOME TO GOSSIP!**");
		System.out.println("Write \"help\" (without quotes) to see the list of possible commands.");
		try {
			regOps = (RegistryOperations) Naming.lookup(registryRMI);
			notConnected = false;
			this.startRMI(addr);
			this.startListen(addr.getPort());
		} catch (NotBoundException | RemoteException e2) {
			System.err.println("The GOSSIP service is not available at the moment. It could be a problem on your side,\n"
					+ "specifically a wrong IP address. Try the \"settings\" command to change it.");
			notConnected = true;
		}
		
		while (!end) {
			
			if (online) System.out.print(myName + ">$ ");
			else System.out.print("$ ");
			System.out.flush();
			input = bf.readLine();
			String[] toParse = input.trim().split(" ");
			toParse[0] = toParse[0].toLowerCase();
			switch(toParse[0]) {
			
			case "register":
				
				if (!notConnected) {
					if (toParse.length != 2) {
						System.err.println("Invalid number of arguments.");
						System.err.println("Usage: register <username>");
					} else {
						try {
							regOps.register(toParse[1].trim());
							System.out.println("Registry: done");
						} catch (UserException e) {
							System.err.println("Registry: ERROR/"
									+ e.getMessage());
						}
					}
				}
				else {
					System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				}
				break;
				
			case "login":
				if (!notConnected) {
					if (online) {
						System.err.println("You are already logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: login <username>");
						} else {
							myName = new String(toParse[1].trim());
							try {
								Address a = null;
								try {
									a = regOps.getProxyAddress(myName.trim());
									this.getOfflineMessages(a);
								} catch (UserException e) {
									e.printStackTrace();
									System.err.println("Registry: ERROR/" + e.getMessage());
								} catch (ParseException e) {
									e.printStackTrace();
									System.err.println("Error in receiving offline messages.");
								}
								regOps.login(myName, addr.getAddress(),
										addr.getPort(), myCallback);
								online = true;
								System.out.println("Registry: done");
							} catch (UserException e) {
								e.printStackTrace();
								System.err.println("Registry: ERROR/ "
										+ e.getMessage());
								myName = null;
							} catch (NotBoundException e) {
								e.printStackTrace();
								System.err.println("Something's gone wrong with the connection (callback error). Exiting...");
								System.exit(-1);
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "logout":
				if (!notConnected) {
					if (!online) {
						System.err.println("You are not logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: logout <username>");
						} else {
							try {
								regOps.logout(toParse[1].trim());
								online = false;
								myName = null;
								System.out.println("Registry: done");
							} catch (UserException e) {
								System.err.println("Registry: ERROR/ "
										+ e.getMessage());
							} catch (ProxyException e) {
								System.err.println("Registry: WARNING/ "
										+ e.getMessage());
								online = false;
								myName = null;
								System.out.println("Registry: done");
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "send":
				if (!notConnected) {
					if (!online) {
						System.err.println("You are not logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: send <username>");
						} else {
							System.out.println("Write your message to "
									+ toParse[1].trim() + ":");
							String msg = bf.readLine().trim();
							String otherUser = toParse[1].trim();
							Message m = new Message(msg, myName, otherUser);
							if (friendList.contains(otherUser)) {
								Address a = friendCache.get(otherUser);
								try (DatagramSocket s = new DatagramSocket(addr.getPort() + 1)) {
									msg = m.toJSONString();
									byte[] buf = msg.trim().getBytes();
									DatagramPacket pack = new DatagramPacket(
											buf, buf.length, a.getAddr(),
											a.getPort());
									s.send(pack);
								} catch (IOException e) {
									e.printStackTrace();
									System.err
											.println("Something's gone wrong while sending the message. Please retry.");
									System.err
											.println("If the problem persists, try setting the IP address manually\nwith the 'settings' command.");
								}
							} else {
								System.err.println("You don't have "
										+ otherUser + " in your friend list.");
								System.err.println("Add it with the 'friend "
										+ otherUser + "' command.");
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "allow":
				if (!notConnected) {
					if (!online) {
						System.err.println("You are not logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: allow <username>");
						} else {
							try {
								regOps.allow(myName, toParse[1].trim());
								allowList.add(toParse[1].trim());
								System.out.println("Done. Current allow list:");
								System.out.println(allowList);
							} catch (UserException e) {
								System.err.println("Registry: ERROR/ "
										+ e.getMessage());
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "disallow":
				if (!notConnected) {
					if (!online) {
						System.err.println("You are not logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: disallow <username>");
						} else {
							try {
								regOps.disallow(myName, toParse[1].trim());
								allowList.remove(toParse[1].trim());
								System.out.println("Done. Current allow list:");
								System.out.println(allowList);
							} catch (UserException e) {
								System.err.println("Registry: ERROR/ "
										+ e.getMessage());
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "friend":
				if (!notConnected) {
					if (!online) {
						System.err.println("You are not logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: friend <username>");
						} else {
							try {
								regOps.friend(myName, toParse[1].trim());
								friendList.add(toParse[1].trim());
								System.out
										.println("Done. Current friend list:");
								System.out.println(friendList);
							} catch (UserException e) {
								System.err.println("Registry: ERROR/"
										+ e.getMessage());
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "unfriend":
				if (!notConnected) {
					if (!online) {
						System.err.println("You are not logged in.");
					} else {
						if (toParse.length != 2) {
							System.err.println("Invalid number of arguments.");
							System.err.println("Usage: disallow <username>");
						} else {
							try {
								regOps.unfriend(myName, toParse[1].trim());
								friendList.remove(toParse[1].trim());
								System.out
										.println("Done. Current friend list:");
								System.out.println(friendList);
							} catch (UserException e) {
								System.err.println("Registry: ERROR/"
										+ e.getMessage());
							}
						}
					}
				}
				else System.err.println("You are not connected. Troubleshoot your problems with the \"settings\" command.");
				break;
				
			case "exit":
				if (online) {
					System.out.println("Logging you off...");
					try {
						regOps.logout(myName);
						online = false;
						myName = null;
						System.out.println("Registry: done");
					} catch (UserException e) {
						System.err.println("Registry: ERROR/ " + e.getMessage());
					} catch (ProxyException e) {
						System.err.println("Registry: WARNING/ " + e.getMessage());
						online = false;
						myName = null;
						System.out.println("Registry: done");
					}					
				}
				rmi.shutdown();
				listener.shutdown();
				System.out.println("Bye! ;)");
				end = true;
				break;
				
			case "help":
				System.out.println("Available commands:\n");
				System.out.println("register <username>\nRegisters <username> to the service\n");
				System.out.println("login <username>\nLogs in <username>\n");
				System.out.println("logout <username>\nLogs out <username>\n");
				System.out.println("send <username>\nSends a message to <username> (must be logged in, <username> must be in your friend list)\n");
				System.out.println("allow <username>\nAllows <username> to send you messages (must be logged in)\n");
				System.out.println("disallow <username>\nRevokes <username>'s permission to send you messages (must be logged in)\n");
				System.out.println("friend <username>\nAdds <username> to your friend list (must be logged in, <username> must have allowed you)\n");
				System.out.println("unfriend <username>\nRemoves <username> from your friend list (must be logged in)\n");
				System.out.println("exit\nLogs you out (if you are logged in), then closes the GOSSIP client\n");
				System.out.println("help\nThis section\n");
				System.out.println("settings\nHere you can view your IP address or specify one yourself (if you have problems connecting)\n");
				break;
				
			case "settings":
				System.out.println("Your current IP address: " + address);
				if (!online) {
					System.out.println("Do you want to change it? (y/n)");
					input = bf.readLine().trim().toLowerCase();
					switch (input) {
						case "y":
							System.out.println("Insert your IP address:");
							address = bf.readLine().trim();
							try {
								addr = new Address(address, Integer.parseInt(args[0]));
								if (rmi != null) rmi.shutdown();
								if (listener != null) listener.shutdown();
								rmi = null;
								rmithread = null;
								listener = null;
								listthread = null;
								try {
									regOps = (RegistryOperations) Naming.lookup(registryRMI);
									this.startRMI(addr);
									this.startListen(addr.getPort());
									notConnected = false;
								} catch (NotBoundException | RemoteException e2) {
									System.err.println("The GOSSIP service is not available at the moment. It could be a problem on your side,\n"
											+ "specifically a wrong IP address. Try the \"settings\" command to change it.");
									notConnected = true;
								}
							} catch (UnknownHostException e1) {
								System.err.println("This is not a valid address. Returning to the main menu...");
							}
							break;
							
						case "n":
							break;
						
						default:
							break;
					}
				}
				break;
			
			default:
				break;
			}
		}
		System.exit(0);
	}
	
	public static void main(String[] args) throws IOException {
		new UserAgent().run(args);
	}
}