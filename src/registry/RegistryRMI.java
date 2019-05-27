package registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import common.UserException;
import common.RegistryOperations;
import common.User;
import common.UAOperations;
import common.Address;
import common.ProxyException;


class RegistryRMI extends UnicastRemoteObject implements RegistryOperations, Runnable {
	
	private static final long serialVersionUID = 1L;
	private SortedSet<User> users;
	private ProxyPool pp;
	private int port;
	private String address;
	private volatile boolean running = true;
	
	RegistryRMI(String addr, ProxyPool prpo) throws RemoteException {
		this(49555, addr, prpo);
	}
	
	private RegistryRMI(int p, String addr, ProxyPool prpo) throws RemoteException {
		super();
		port = p;
		address = new String(addr);
		users = Collections.synchronizedSortedSet(new TreeSet<User>());
		pp = prpo;
	}
	
	private User searchUser(String s) {
		for (User u: users) {
			if (u.equals(s)) return u;
		}
		return null;
	}
	
	void poll() {
		for (User u: users) {
			System.out.println("User " + u.getName() + " isOnline=" + u.getOnlineStatus() + " address " + u.getAddress());
		}
	}
	
	private void notifyProxy(Proxy p, String user) {
		if (p != null) {
			DatagramPacket pack;
			byte[] buf;
			buf = (new String("USR:" + user)).getBytes();
			try (DatagramSocket sock = new DatagramSocket(55246)) {
				pack = new DatagramPacket(buf, buf.length, p.getAddress()
						.getAddr(), p.getAddress().getPort());
				sock.send(pack);
			} catch (IOException e) {
				System.err.println("Proxy/socket error.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void register(String name) throws RemoteException, UserException {
		if (searchUser(name) != null) throw new UserException("User already exists");
		User u = new User(name);
		users.add(u);
		System.out.println("Registered user " + name);
	}

	@Override
	public synchronized void login(String name, String addr, int port, String callbackRegister) 
			throws RemoteException, UserException, UnknownHostException, MalformedURLException, NotBoundException {
		User u = searchUser(name);
		if (u != null) {
			if (u.getOnlineStatus()) throw new UserException("User is already online.");
			else {
				UAOperations uao = (UAOperations) Naming.lookup(callbackRegister);
				pp.revoke(u);
				u.setOnline(addr, port, uao);
				u.getCallback().updateAllowList(u.getInbound());
				u.getCallback().updateFriendList(u.getOutbound());
				for (User f: u.getOthersOutbound()) {
					if (f.getCallback() != null) {
						f.getCallback().updateFriendAddress(u.getName(), u.getAddress());
						f.getCallback().response("User " + name + " is online!");
					}
				}
				System.out.println("User " + name + " logged in with address " + addr + " and port " + port);
			}
		}
		else {
			throw new UserException("The user " + name + " is not registered.");
		}
	}
	
	@Override
	public Address getProxyAddress(String name) throws RemoteException, UserException {
		Proxy p = null;
		try {
			p = pp.assignedProxy(name);
		} catch (ProxyException e) {
			e.printStackTrace();
			throw new UserException(e.getMessage() + " (if this is the first time you log in, this is normal)");
		}
		Address a = p.getAddress();
		System.out.println("Assigned user " + name + " to proxy " + p.getID() + " with address:port " + p.getAddress());
		return a;
	}

	@Override
	public synchronized void logout(String name) throws RemoteException, UserException, UnknownHostException, ProxyException {
		User u = searchUser(name);
		Proxy p = null;
		if (u != null) {
			if (u.getOnlineStatus()) {
				try {
					p = pp.assign(u);
				}
				catch (ProxyException e) {
				}
				if (p != null) {
					u.setOffline(p.getAddress().getAddress(), p.getAddress().getPort());
					notifyProxy(p, u.getName());
				}
				else u.setOffline("0.0.0.0", 0);
				for (User f: u.getOthersOutbound()) {
					if (f.getCallback() != null) {
						f.getCallback().updateFriendAddress(u.getName(), u.getAddress());
						f.getCallback().response("User " + name + " went offline.");
					}
				}
				System.out.println("User "  + name + " logged out");
				if (p == null) throw new ProxyException("No available proxy");
			}
			else throw new UserException("User is already offline.");
		}
		else throw new UserException("The user " + name + " is not registered.");
	}

	@Override
	public synchronized void allow(String myName, String itsName) throws RemoteException, UserException {
		if (myName.equals(itsName)) throw new UserException("You can't allow yourself!");
		User me = searchUser(myName);
		User it = searchUser(itsName);
		if (it == null) throw new UserException("The user " + itsName + " is not registered.");
		if (!me.addInbound(it)) throw new UserException("User " + itsName + " already in " + myName + "'s allow list.");
		if (it.getCallback() != null)
			it.getCallback().response("User " + myName + " allowed you to send messages to him/her.");
		System.out.println("User " + myName + " has allowed " + itsName);
	}

	@Override
	public synchronized void disallow(String myName, String itsName) throws RemoteException, UserException {
		if (myName.equals(itsName)) throw new UserException("You can't disallow yourself!");
		User me = searchUser(myName);
		User it = searchUser(itsName);
		if (it == null) throw new UserException("The user " + itsName + " is not registered.");
		if (!me.remInbound(it)) throw new UserException("User " + itsName + " is not in " + myName + "'s allow list.");
		me.remOthersOutbound(it);
		it.remOutbound(me);
		if (it.getCallback() != null) {
			it.getCallback().updateFriendList(it.getOutbound());
			it.getCallback().response("User " + myName + " disallowed you to send messages to him/her.");
		}
		System.out.println("User " + myName + " has disallowed " + itsName);
	}

	@Override
	public synchronized void friend(String myName, String itsName) throws RemoteException, UserException {
		if (myName.equals(itsName)) throw new UserException("You can't friend yourself!");
		User me = searchUser(myName);
		User it = searchUser(itsName);
		if (it == null) throw new UserException("The user " + itsName + " is not registered.");
		if (!it.isInInbound(me)) throw new UserException("You don't have the permission to add " + itsName + " as a friend."); 
		if (!me.addOutbound(it)) throw new UserException("User " + itsName + " already in " + myName + "'s friend list.");
		if (me.getCallback() != null)
			me.getCallback().updateFriendAddress(it.getName(), it.getAddress());
		it.addOthersOutbound(me);
		if (it.getCallback() != null)
			it.getCallback().response("User " + myName + " has added you into his/her friend list.");
		System.out.println("User " + myName + " has friended " + itsName);
	}

	@Override
	public synchronized void unfriend(String myName, String itsName) throws RemoteException, UserException {
		if (myName.equals(itsName)) throw new UserException("You can't unfriend yourself!");
		User me = searchUser(myName);
		User it = searchUser(itsName);
		if (it == null) throw new UserException("The user " + itsName + " is not registered.");
		if (!me.remOutbound(it)) throw new UserException("User " + itsName + " is not in " + myName + "'s friend list.");
		it.remOthersOutbound(me);
		if (it.getCallback() != null)
			it.getCallback().response("User " + myName + " has removed you from his/her friend list.");
		System.out.println("User " + myName + " has unfriended " + itsName);
	}
	
	void shutdown() {
		running = false;
	}
	
	
	@Override
	public void run() {
		try {
			System.setProperty("java.rmi.server.hostname", address);
			Registry reg = LocateRegistry.createRegistry(port);
			reg.rebind("GossipRegistryService", this);
			while (running) {}
			unexportObject(reg, true);
			System.out.println("RegistryRMI has stopped.");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
}
