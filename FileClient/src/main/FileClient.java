package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class FileClient implements Runnable{
	
	static int port = 15000;
	static String text;
	static Socket commSocket;
	static BufferedReader consoleInput;
	static BufferedReader inputStream;
	static PrintStream outputStream;
	static String consoleText;
	public static void main(String[] args) {
		try {
			consoleInput = new BufferedReader(new InputStreamReader(System.in));
			commSocket = new Socket("localhost", port);
			inputStream = new BufferedReader(new InputStreamReader(commSocket.getInputStream()));
			consoleInput = new BufferedReader(new InputStreamReader(System.in));
			outputStream = new PrintStream(commSocket.getOutputStream());
			new Thread(new FileClient()).start();
			
			while(true) {
				consoleText = consoleInput.readLine();
				
				outputStream.println(consoleText);
				if(consoleText.startsWith("/quit")) {
					System.exit(0);

					
				}
			}
			
		} catch (UnknownHostException e) {
			System.out.println("Host is unknown.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Connection terminated.");
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		String serverText;
		try {
			while(true) {
				serverText = inputStream.readLine();				
				System.out.println(serverText);
				
				if(serverText.startsWith("[SERVER] Sending file.")) {
					File file = new File(inputStream.readLine());
					PrintWriter printToFile = new PrintWriter(new FileWriter(file.getAbsoluteFile()));
					String text;
					while(!(text = inputStream.readLine()).contains("[SERVER] File sent.")) {
						printToFile.println(text);
					}
					printToFile.close();
					System.out.println("File saved.");
				}
				if(serverText.startsWith("[SERVER] Bye!")) {
					
					outputStream.close();
					inputStream.close();
					System.exit(0);
				
				}
					
			}
		} catch (IOException e) {
			System.out.println("Connection terminated.");
			e.printStackTrace();
		}
	}
}
