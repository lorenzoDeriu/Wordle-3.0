import java.util.Scanner;

/**
 * Classe fa partire il thread server
 * si mette in attesa di un input da tastiera per terminare il server
 * 
 * @author Lorenzo Deriu
 */
public class ServerStarterMain {
	public static void main(String[] args) {
		WordleServer server = new WordleServer();
		Thread serverThread = new Thread(server);

		serverThread.start();

		System.out.println("press enter to shutdown the server.");
		Scanner input = new Scanner(System.in);

		input.nextLine();
		input.close();

		server.shutdown();

		while (serverThread.isAlive()) ;
		System.out.println("System termineted");
	}
}
