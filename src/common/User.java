package common;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import common.UAOperations;

public class User implements Serializable, Comparable<User> {
	private static final long serialVersionUID = 1L;

	private String name;
	private SortedSet<User> inbound;
	private SortedSet<User> outbound;
	private SortedSet<User> othersOutbound;
	private boolean isOnline;
	private Address proxyAddr;
	private Address myAddr;
	private UAOperations callback;
	
	public User (String s) {
		name = new String(s);
		inbound = Collections.synchronizedSortedSet(new TreeSet<User>());
		outbound = Collections.synchronizedSortedSet(new TreeSet<User>());
		othersOutbound = Collections.synchronizedSortedSet(new TreeSet<User>());
		isOnline = false;
		proxyAddr = null;
		myAddr = null;
		callback = null;
	}
	
	public UAOperations getCallback() {
		return callback;
	}
	
	public void setName(String s) {
		name = new String(s);
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isInInbound(User u) {
		boolean ret = false;
		synchronized (inbound) {
			for (User f: inbound) {
				if (f.equals(u)) ret = true;
			}
		}
		return ret;
	}
	
	public boolean addInbound(User u) {
		boolean ret;
		synchronized (inbound) {
			ret = inbound.add(u);
		}
		return ret;
	}
	
	public boolean addOutbound(User u) {
		boolean ret;
		synchronized (outbound) {
			ret = outbound.add(u);
		}
		return ret;
	}
	
	public boolean addOthersOutbound(User u) {
		boolean ret;
		synchronized (othersOutbound) {
			ret = othersOutbound.add(u);
		}
		return ret;
	}
	
	public boolean remInbound(User u) {
		boolean ret;
		synchronized (inbound) {
			ret = inbound.remove(u);
		}
		return ret;
	}
	
	public boolean remOutbound(User u) {
		boolean ret;
		synchronized (outbound) {
			ret = outbound.remove(u);
		}
		return ret;
	}
	
	public boolean remOthersOutbound(User u) {
		boolean ret;
		synchronized (othersOutbound) {
			ret = othersOutbound.remove(u);
		}
		return ret;
	}
	
	public List<String> getInbound() {
		List<String> l = new Vector<String>();
		synchronized (inbound) {
			for(User u: inbound) {
				l.add(u.getName());
			}
		}
		return l;
	}
	
	public List<User> getOutbound() {
		List<User> l = new Vector<User>();
		synchronized (outbound) {
			for (User u: outbound) {
				l.add(u);
			}
		}
		return l;
	}
	
	public List<User> getOthersOutbound() {
		List<User> l = new Vector<User>();
		synchronized (othersOutbound) {
			for (User u: othersOutbound) {
				l.add(u);
			}
		}
		return l;
	}

	public boolean getOnlineStatus() {
		return isOnline;
	}
	
	public Address getAddress() {
		if (isOnline) return myAddr;
		else return proxyAddr;
	}
	
	public void setOnline(String a, int p, Object cb) throws UnknownHostException {
		proxyAddr = null;
		myAddr = new Address(a, p);
		isOnline = true;
		callback = (UAOperations) cb;
	}
	
	public void setOffline(String pra, int prp) throws UnknownHostException {
		isOnline = false;
		proxyAddr = new Address(pra, prp);
		myAddr = null;
		callback = null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof User) {
			if (name.equals(((User) o).getName())) return true;
		}
		if (o instanceof String) {
			if (name.equals((String) o)) return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public int compareTo(User anotherUser) {
		return this.getName().compareTo(anotherUser.getName());
	}
	
	
}
