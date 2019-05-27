package registry;

import java.util.Iterator;
import java.util.Vector;

import common.ProxyException;
import common.User;

class ProxyPool implements Iterable<Proxy> {
	private Vector<Proxy> pool;
	private int count;
	
	public ProxyPool() {
		pool = new Vector<Proxy>();
		count = 0;
	}
	
	private class ProxyIter implements Iterator<Proxy> {
		private int n;
		
		ProxyIter () {
			if (pool != null) n = 0;
		}
		
		public boolean hasNext() {
			return n < pool.size();
		}
		
		public Proxy next() {
			int i = n;
			n++;
			return pool.get(i);
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	void addProxy(Proxy p) {
		p.setID(count++);
		pool.add(p);
	}
	
	boolean removeProxy(int id) {
		int idx = -1;
		for (int i = 0; i < pool.size(); i++) {
			if (pool.get(i).getID() == id) idx = i;
		}
		if (idx == -1) return false;
		else {
			pool.remove(idx);
			return true;
		}
	}
	
	private int getLowestLoadProxy() {
		int min = 0, idx;
		if (pool.size() > 0) {
			min = pool.get(0).getLoad();
			idx = 0;
		}
		else idx = -1;
		for (int i = 1; i < pool.size(); i++) {
			if (min > pool.get(i).getLoad()) {
				min = pool.get(i).getLoad();
				idx = i;
			}
		}
		return idx;
	}
	
	Proxy assign(User u) throws ProxyException {
		int i = this.getLowestLoadProxy();
		if (i < 0) throw new ProxyException("Assign: no available proxy");
		else {
			Proxy p = pool.get(i);
			p.addUser(u);
			pool.set(i, p);
			System.out.println("Assigned proxy " + p.getID() + " to user " + u.getName());
			return p;
		}
		
	}
	
	void revoke(User u) {
		boolean removed = false;
		for (Proxy p: pool) {
			if (!removed) {
				removed = p.removeUser(u);
			}
		}
	}
	
	void proxyPoll() {
		for (Proxy p: pool) System.out.println(p);
	}
	
	Proxy assignedProxy(String s) throws ProxyException {
		System.out.println("Searching for " + s);
		for (int i = 0; i < pool.size(); i++) {
			System.out.println(pool.get(i));
			if (pool.get(i).hasUser(s)) return pool.get(i);
		}
		throw new ProxyException("No proxy has this user");
	}

	@Override
	public Iterator<Proxy> iterator() {
		return new ProxyIter();
	}
}
