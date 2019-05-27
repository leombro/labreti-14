package common;

import java.net.InetAddress;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

/**
 * La classe <code>Message</code> incapsula le informazioni di un messaggio
 * inviato mediante GOSSIP e fornisce metodi di utilità per convertire queste
 * informazioni da e verso oggetti <code>JSONObject</code>.
 * 
 * @author Orlando Leombruni
 * @version 1.0
 * @see InetAddress
 *
 */
public class Message implements JSONAware {
	private String content;
	private String sender;
	private String receiver;
	
	public Message (String cont, String send, String rec) {
		this.content = new String(cont);
		this.sender = new String(send);
		this.receiver = new String(rec);
	}
	
	public static Message convertFromJSON (JSONObject o) {
		String cont = (String) o.get("content");
		String send = (String) o.get("sender");
		String rec = (String) o.get("receiver");
		return new Message(cont, send, rec);
	}

	public String getContent() {
		return content;
	}

	public String getSender() {
		return sender;
	}

	public String getReceiver() {
		return receiver;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getJSON() {
		JSONObject obj = new JSONObject();
		obj.put("content", content);
		obj.put("sender", sender);
		obj.put("receiver", receiver);
		return obj;
	}

	@Override
	public String toJSONString() {
		return this.getJSON().toString();
	}
	
	

}
