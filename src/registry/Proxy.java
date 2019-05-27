package registry;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;

import common.User;
import common.Address;

class Proxy {
	private Address addr;
	private List<User> users;
	private int _id = 0;
	
	Proxy (String a, int p) throws UnknownHostException {
		addr = new Address(a, p);
		users = new Vector<User>();
	}
	
	public void setID(int i) {
		this._id = i;
	}
	
	public int getID() {
		return this._id;
	}
	
	public Address getAddress() {
		return addr;
	}
	
	public int getLoad() {
		return users.size();
	}
	
	boolean addUser(User u) {
		if (users.contains(u)) return false;
		else {
			if (users.add(u)) {
				return true;
			}
			else return false;
			
		}
	}
	
	boolean removeUser(User u) {
		return users.remove(u);
	}
	
	boolean hasUser(String s) {
		for (User u: users) {
			if (u.getName().equals(s)) return true;
		}
		return false;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("Proxy n.: ");
		sb.append(_id);
		sb.append(" with address ");
		sb.append(addr.toString());
		sb.append("\nUsers assigned to this proxy: ");
		sb.append(users);
		return sb.toString();
	}
}
