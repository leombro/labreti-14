package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import common.User;

public interface UAOperations extends Remote {
	public void response(String s) throws RemoteException;
	public void updateAllowList(List<String> l) throws RemoteException;
	public void updateFriendList(List<User> l) throws RemoteException;
	public void updateFriendAddress(String name, Address addr) throws RemoteException;
} 

