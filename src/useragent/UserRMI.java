package useragent;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import common.Address;
import common.UAOperations;
import common.User;

class UserRMI extends UnicastRemoteObject implements UAOperations,
		Runnable {
	
	private static final long serialVersionUID = 1L;
	private int port;
	private volatile boolean running = true;
	private UserAgent father;
	private String address;
	
	UserRMI (int p, String address, UserAgent a) throws RemoteException {
		super();
		port = p;
		this.address = new String(address);
		father = a;
	}
	
	void shutdown() {
		running = false;
	}

	@Override
	public void response(String s) throws RemoteException {
		System.out.println("Registry: " + s);
	}

	@Override
	public void updateAllowList(List<String> l) throws RemoteException {
		father.updateAllowList(new Vector<String>(l));
	}

	@Override
	public void updateFriendList(List<User> l) throws RemoteException {
		List<String> friend = new Vector<String>();
		Map<String, Address> m = new ConcurrentHashMap<String, Address>();
		for (User u: l) {
			friend.add(u.getName());
			m.put(u.getName(), u.getAddress());
			System.out.println("added user " + u.getName() + " with address " + u.getAddress());
		}
		father.updateFriendList(friend, m);
	}
	
	@Override
	public void run() {
		try {
			System.setProperty("java.rmi.server.hostname", address);
			Registry reg = LocateRegistry.createRegistry(port);
			reg.rebind("GossipUAService", this);
			while (running) {}
			unexportObject(reg, true);
			System.out.println("Shutting down...");
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void updateFriendAddress(String name, Address addr)
			throws RemoteException {
		father.updateFriendAddress(name, addr);
		
	}

}
