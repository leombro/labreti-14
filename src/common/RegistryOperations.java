package common;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import common.UserException;

public interface RegistryOperations extends Remote {
	
	public void register(String name) throws RemoteException, UserException;
	
	public void login(String name, String addr, int port, String callbackRegister) 
		throws RemoteException, UserException, UnknownHostException, MalformedURLException, NotBoundException;
	
	public Address getProxyAddress(String name) throws UserException, RemoteException;
	
	public void logout(String name) 
		throws RemoteException, UserException, UnknownHostException, ProxyException;
	
	public void allow(String myName, String itsName) throws RemoteException, UserException;
	
	public void disallow(String myName, String itsName) throws RemoteException, UserException;
	
	public void friend(String myName, String itsName) throws RemoteException, UserException;
	
	public void unfriend(String myName, String itsName) throws RemoteException, UserException;
}
