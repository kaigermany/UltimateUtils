package me.kaigermany.ultimateutils.networking.smarthttp;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;

public class HTTPServerGroup {
	private final HashSet<HTTPClient> active;
	private final LinkedList<HTTPClient> idle;
	
	public HTTPServerGroup(){
		active = new HashSet<HTTPClient>();
		idle = new LinkedList<HTTPClient>();
	}

	public int getNumActiveConnections() {
		int size;
		synchronized (this) {
			size = active.size();
		}
		return size;
	}
	
	public void markInstanceAsUnused(HTTPClient client){
		synchronized (this) {
			active.remove(client);
			idle.add(client);
		}
	}
	
	public boolean cleanup(long currentTime){//return true if no client is stored!
		synchronized (this) {
			LinkedList<HTTPClient> todoDelete = new LinkedList<HTTPClient>();
			
			for(HTTPClient c : idle){
				if(c.canBeDeleted(currentTime)){
					todoDelete.add(c);
				}
			}
			idle.removeAll(todoDelete);
			todoDelete.clear();
			
			for(HTTPClient c : active){
				if(c.isDisabled()){
					todoDelete.add(c);
				}
			}
			active.removeAll(todoDelete);
			
			return idle.isEmpty() && active.isEmpty();
		}
	}

	public HTTPClient getOrCreateClient(String server, int port, boolean ssl, boolean disableCertificateCheck) throws UnknownHostException, IOException {
		synchronized (this) {
			while(!idle.isEmpty()){
				HTTPClient client = idle.removeFirst();
				/*
				if(!client.tryClaim()){//Theoretically impossible...
					System.err.println("WARNING: Invalid HTTPClient instance found!");
					return null;
				}
				*/
				if(client.tryClaim()){
					active.add(client);
					return client;
				}
			}
			//if there is no instance for reuse left then open a new one.
			HTTPClient client = new HTTPClient(server, port, ssl, disableCertificateCheck, this);
			active.add(client);
			return client;
		}
	}
	
	@Override
	public String toString() {
		return "{#active: " + active.size() + ", #idle = " + idle.size() + "}";
	}
}
