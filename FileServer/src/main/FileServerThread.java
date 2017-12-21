package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;

public class FileServerThread extends Thread {

	// promenljive:
	Socket socket;
	File users = new File("usrs.txt");
	File userFiles = new File("userFiles.txt");
	BufferedReader fileInput;
	FileWriter userFileOutput;
	PrintWriter printToFile, printToUserFiles;
	String userName, pw;
	boolean opDone = false;
	int reg = 0;
	boolean done = false;
	int option = 0;
	//File dir;

	public FileServerThread(Socket socket) {
		this.socket = socket;
	}

	// Tester za quit
	String readText(Socket socket, BufferedReader inputStream) throws IOException {
		String text = inputStream.readLine();
		if (text.equals("/quit")) {
			PrintStream outputStream = new PrintStream(socket.getOutputStream());
			outputStream.println("Bye!");
			socket.getOutputStream().close();
			socket.getInputStream().close();
		}

		return text;

	}

	// Random code generator
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static SecureRandom rnd = new SecureRandom();

	String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	@Override
	public void run() {

		System.out.println("Connection established.");
		try {
			if (!(users.exists())) {
				users.createNewFile();

			}
			printToFile = new PrintWriter(new FileWriter(users.getAbsoluteFile(), true));
			fileInput = new BufferedReader(new FileReader(users));
			PrintStream outputStream = new PrintStream(socket.getOutputStream());
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (true) {
				outputStream.println("[SERVER] Connection established.");
				outputStream.println("[SERVER] 1.Log in\n2.Register\n3.Quit\nType \"/quit\" to quit anytime.");
				String rez = readText(socket, inputStream);
				if (isNumeric(rez)) {
					reg = Integer.parseInt(rez);
					break;
				}
			}
			if (reg != 3) {
				while (!done) {
					outputStream.println("[SERVER] User name:");
					userName = readText(socket, inputStream);
					System.out.println("Read " + userName);
					outputStream.println("[SERVER] Password:");
					pw = readText(socket, inputStream);
					System.out.println("Read " + pw);

					if (reg == 2) {
						String fileText;
						fileInput = new BufferedReader(new FileReader(users)); // resetovanje citaca
						while (true) {

							fileText = fileInput.readLine();

							if (fileText != null && fileText.substring(0, fileText.indexOf(":")).equals(userName)) {

								outputStream.println("[SERVER] User name is taken! Please try again.");
								break;
							} else if (fileText == null) {
								printToFile.println(userName + ":" + pw);
								done = true;
								break;
							}
							System.out.println(fileText.substring(0, fileText.indexOf(":")));
						}

					} else if (reg == 1) {
						String fileText;
						fileInput = new BufferedReader(new FileReader(users)); // resetovanje citaca

						while ((fileText = fileInput.readLine()) != null) {
							if (fileText.substring(0, fileText.indexOf(":")).equals(userName)) {
								if (fileText.substring(fileText.indexOf(":")).equals(":" + pw)) {
									outputStream.println("[SERVER] Log in successful!");
									done = true;
								} else {
									outputStream.println("[SERVER] Wrong password! Try again!");

									break;
								}
							}
						}
						if (fileText == null && done == false) {
							outputStream.println("[SERVER] Username doesn't exist! Please register!");
							reg = 2;
						}
					}
				}
				printToFile.close();
				fileInput.close();
				
				while (!opDone) {
					outputStream.println("[SERVER] Options:\n1.UPLOAD\n2.DOWNLOAD\n3.LIST FILES\n4.QUIT");
					String rez = readText(socket, inputStream);
					if (isNumeric(rez)) {
						option = Integer.parseInt(rez);
					} else {
						option = -1;
					}

					if (option == 1) {
						int n = 0;
						String codeStr = randomString(10);
						outputStream.println("[SERVER] Enter file name:");
						String fileName = readText(socket, inputStream) + ".txt";
						File file = new File(codeStr + ".." + fileName);
						
						PrintWriter printToFileW = new PrintWriter(new FileWriter(file.getAbsoluteFile()));

						outputStream
								.println("[SERVER] Enter your content (type \"/end\" to finish and save the file):");

						while (true) {

							String tex = readText(socket, inputStream);
							if (tex.endsWith("/end")) {
								System.out.println("Writing content...");
								outputStream.println("[SERVER] Your file is saved.");
								break;
							}
							n += tex.length();
							System.out.println("Read: " + tex);
							printToFileW.println(tex);

							if (n > 500) {
								n = 0;
								PrintWriter clearFile = new PrintWriter(file.getAbsoluteFile());
								clearFile.close();
								printToFileW.flush();
								printToFileW.close();
								printToFileW = new PrintWriter(new FileWriter(file.getAbsoluteFile()));
								outputStream.println(
										"[SERVER] Your content cannot be longer than 500 characters!\n Try again.");

							}

						}

						printToFileW.close();

						System.out.println("[SERVER] File saved.");
						
						userFileOutput = new FileWriter(userFiles.getAbsoluteFile(), true);
						printToUserFiles = new PrintWriter(userFileOutput);
						printToUserFiles.println(userName + ":" + file.getName() + ":" + codeStr);
						printToUserFiles.close();
						outputStream.println("[SERVER] Your secret code for the file: " + codeStr);
					}
					if (option == 2) {
						Boolean dlDone = false;
						outputStream.println("[SERVER] Enter your private code: ");
						String code = readText(socket, inputStream);
						String line;
						fileInput = new BufferedReader(new FileReader(userFiles));
						while ((line = fileInput.readLine()) != null) {
							if (line.startsWith(userName + ":") && line.contains(code)) {
								
								String foundFileName = line.substring(line.indexOf(":") + 1, line.lastIndexOf(":"));
								File foundFile = new File(foundFileName);
								fileInput = new BufferedReader(new FileReader(foundFile));
								outputStream.println("[SERVER] Sending file.");
								outputStream.println(foundFileName.substring(foundFileName.indexOf("..")+2));

								while ((line = fileInput.readLine()) != null) {
									outputStream.println(line);
								}
								outputStream.println("[SERVER] File sent.");

								dlDone = true;
							}
						}
						if (!dlDone)
							outputStream.println("[SERVER] File with this code doesn't exist! Try again.");
						fileInput.close();
					}
					if (option == 3) {
						fileInput = new BufferedReader(new FileReader(userFiles));
						String line;
						Boolean found = false;
						while ((line = fileInput.readLine()) != null) {
							if (line.startsWith(userName + ":")) {
								outputStream.println(line.substring(line.indexOf("..") + 2));
								found = true;
							}
						}
						if (found == false) {
							outputStream.println("[SERVER] There are no files uploaded for this username.");

						}
					}
					if (option == 4) {
						opDone = true;
					}
				}
			}
			outputStream.println("[SERVER] Bye!");
			outputStream.close();
			inputStream.close();

			System.out.println("Connection closed with " + userName);

		} catch (SocketException e) {
			if (userName != null) {
				System.out.println("Connection terminated with " + userName);
			} else {
				System.out.println("Connection terminated.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
