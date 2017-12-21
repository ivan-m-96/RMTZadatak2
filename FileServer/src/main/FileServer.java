package main;

import java.io.IOException;
import java.net.ServerSocket;

public class FileServer extends Thread {
	
	
	
	
	public static void main(String[] args) {
		
		int port = 15000;
		
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while(true) {
				new FileServerThread(serverSocket.accept()).start();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
}
