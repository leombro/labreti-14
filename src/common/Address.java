package common;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * <code>Address</code> è una classe di utilità che incapsula porta e indirizzo IP
 * (sia in formato <code>String</code> che <code>InetAddress</code>) e fornisce metodi per
 * ottenere rapidamente queste informazioni in diversi formati.
 * 
 * @author Orlando Leombruni
 * @version 1.0
 * @see InetAddress
 *
 */
public class Address implements Serializable {
	
	/**
	 * Campo richiesto dall'interfaccia <code>Serializable</code>.
	 * 
	 * @see Serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * L'indirizzo IP, in formato stringa.
	 */
	private String address;
	
	/**
	 * L'indirizzo IP, in formato <code>InetAddress</code>.
	 * 
	 * @see InetAddress
	 */
	private InetAddress addr;
	
	/**
	 * Il numero di porta.
	 */
	private int port;
	
	
	/**
	 * Crea un nuovo oggetto di tipo <code>Address</code>, prendendo in input indirizzo IP e 
	 * numero di porta in modo esplicito.
	 * 
	 * @param a Stringa contenente l'indirizzo IP
	 * @param p Numero di porta
	 * @throws UnknownHostException Se la stringa <code>a</code> non contiene un indirizzo
	 * IP valido (secondo le regole della classe <code>InetAddress</code> 
	 */
	public Address(String a, int p) throws UnknownHostException {
		address = new String(a);
		port = p;
		addr = InetAddress.getByName(address);
	}
	
	/**
	 * Restituisce l'indirizzo IP in formato testuale.
	 * 
	 * @return Una stringa contenente l'indirizzo IP in formato testuale.
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * Modifica l'indirizzo IP contenuto in questo oggetto.
	 * 
	 * @param address Indirizzo IP da inserire nell'oggetto.
	 * @throws UnknownHostException Se la stringa <code>address</code> non contiene un indirizzo
	 * IP valido (secondo le regole della classe <code>InetAddress</code> 
	 * @see InetAddress
	 */
	public void setAddress(String address) throws UnknownHostException {
		this.address = new String(address);
		addr = InetAddress.getByName(this.address);
	}
	
	/**
	 * Restituisce il numero di porta.
	 * 
	 * @return Il numero di porta incapsulato nell'oggetto.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Modifica il numero di porta contenuto in questo oggetto.
	 * 
	 * @param port Numero di porta da inserire nell'oggetto.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Restituisce l'indirizzo IP in formato <code>InetAddress</code>.
	 * 
	 * @return L'indirizzo IP incapsulato nell'oggetto, in formato <code>InetAddress</code>.
	 */
	public InetAddress getAddr() {
		return addr;
	}
	
	/**
	 * Restituisce una rappresentazione testuale del contenuto dell'oggetto. Il formato
	 * della stringa restituita è
	 * <center><code>indirizzoIP:porta</code></center>
	 * Esempio: <code>127.0.0.1:12345</code>
	 * 
	 * @return un oggetto <code>String</code> contenente indirizzo IP e porta nel formato
	 * sopra specificato. 
	 */
	@Override
	public String toString() {
		return new String(address + ":" + port);
	}
	
}
