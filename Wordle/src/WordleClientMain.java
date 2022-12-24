import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Classe che rappresenta il client del gioco Wordle
 * 
 * @author Lorenzo Deriu
 */
public class WordleClientMain {
	private static final String clientConfigFileName = "configClient.txt";

	private static InetAddress host;
	private static int port;
	
	private static Scanner scanner = new Scanner(System.in);
	private static Vocabulary vocabulary;

	private static NotificationHandler notificationHandler;
	private static Thread notificationHandlerThread;

	private static final String WINNING_RESPONSE = "++++++++++";
	private static int attemptRemaining;

	public static void main(String[] args) {
		configureClient(clientConfigFileName);

		Boolean done = false;
		
		while (!done) {
			System.out.print(ClientOutputMessage.StartingMessage);
			String choice = scanner.nextLine();
	
			switch (Integer.parseInt(choice)) {
				case UserChoice.REGISTER:
				int responseCode;
				while ((responseCode = Register()) == ServerResponse.FAILURE) {
					System.out.println(ClientOutputMessage.RegistrationErrorMessage);
				}
				
				if (responseCode == -1) break;

				System.out.println(ClientOutputMessage.RegistrationConfirmed);

				case UserChoice.LOGIN:
				try {
					SocketChannel connection = LogIn();
					if (connection == null) break;

					System.out.println(ClientOutputMessage.Welcome);

					startReceivingNotification();
					
					while(playGame(connection) != -1) ;

				} catch (BufferUnderflowException | IOException e) {
					System.out.println(ClientOutputMessage.ServerConnectionError);
				}
				break;
	
				case UserChoice.EXIT:
				done = true;
				notificationHandler.stop();
				break;
			}
		}

		scanner.close();
	}

	/**
	 * Metodo che permette di registrare un nuovo utente
	 * 
	 * @return 0 se la registrazione è andata a buon fine, -1 se si è verificato un errore
	 */
	private static int Register() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int responseCode;
		
		buffer.putInt(ClientRequest.REGISTER);
		if (getCredential(buffer) == -1) return -1;
		
		try (SocketChannel connection = SocketChannel.open(new InetSocketAddress(host, port))) {	
			sendBufferContent(buffer, connection);
			responseCode = receiveResponseCode(buffer, connection); 
		} catch (IOException e) {
			System.out.println(ClientOutputMessage.ServerConnectionError);
			return -1;
		}

		return responseCode;
	}

	/**
	 * Metodo che permette di effettuare il login di un utente
	 * 
	 * @return SocketChannel se il login è andato a buon fine, null se si è verificato un errore
	 * @throws IOException
	 */
	private static SocketChannel LogIn() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		SocketChannel connection = null;

		buffer.putInt(ClientRequest.LOGIN);
		if (getCredential(buffer) == -1) {
			return null;
		}

		connection = SocketChannel.open(new InetSocketAddress(host, port));	

		sendBufferContent(buffer, connection);
		receiveResponse(buffer, connection);
		
		int responseCode = buffer.getInt();
		
		
		if (responseCode == ServerResponse.FAILURE) {
			System.out.println(ClientOutputMessage.CredentialError);
		} else {
			attemptRemaining = 12 - buffer.getInt();
		}

		return (responseCode == ServerResponse.SUCCESS) ? connection : null;
	}

	/**
	 * Metodo che gestisce la connessione con il server una volta effettuato il login
	 * Chiede all'utente di scegliere se provare a indovinare una parola, vedere le statistiche, vedere i messaggi ricevuti o uscire
	 * @param connection SocketChannel connesso al server
	 * @return 0 se il l'utente non deve essere riindirizzato al menu di login, -1 altrimenti
	 * @throws IOException
	 * @throws BufferUnderflowException
	 */
	private static int playGame(SocketChannel connection) throws IOException, BufferUnderflowException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		System.out.print(ClientOutputMessage.GamePlayInstruction);

		String choice = scanner.nextLine();
		Boolean  guessed = false;

		int backToMainMenu = 0; // varrà 0 se l'utente non deve essere riindirizzato al menu di login, -1 altrimenti

		switch (Integer.parseInt(choice)) {
			case UserChoice.PLAY:
			Boolean done = false;
			int minuteRemaining = 0;
			long nextTimeToChange = 0;

			// gestione del tentativo di indovinare una parola
			while (!done) { 
				System.out.printf(ClientOutputMessage.RequestWord, attemptRemaining);
				String word = scanner.nextLine().toLowerCase();
				
				if (word.equals("exit")) {
					done = true;
					continue;
				}

				if (!wordValid(word)) {
					System.out.println(ClientOutputMessage.VocabularyError);
					continue;
				}
				
				sendWord(word, buffer, connection);
				receiveResponse(buffer, connection);
				
				int responseCode = buffer.getInt();

				Boolean waitNextWord = false;

				// Gestione delle risposte del server
				switch (responseCode) {
					case ServerResponse.WORD_CHANGED:
					System.out.println(ClientOutputMessage.WordHasChanged);
					attemptRemaining = 12;
					guessed = false;
					break;

					case ServerResponse.SUCCESS:
					attemptRemaining--;

					byte[] byteArray = new byte[10];
					buffer.get(byteArray);
					String response = new String(byteArray);

					System.out.printf(ClientOutputMessage.Result, word, response);
					
					if (response.equals(WINNING_RESPONSE)) {
						nextTimeToChange = buffer.getLong();
						System.out.println(ClientOutputMessage.WordGuessed);
						
						attemptRemaining = 0;

						done = true;
						guessed = true;
					}
					break;

					case ServerResponse.FAILURE:
					nextTimeToChange = buffer.getLong();
					
					minuteRemaining = (int) (((nextTimeToChange - System.currentTimeMillis()) / 1000) / 60);
					
					System.out.printf(ClientOutputMessage.TooMuchAttempt, minuteRemaining);
					choice = scanner.nextLine();

					done = choice.toUpperCase().equals("N");
					waitNextWord = !done;
					break;
				}

				// Se l'utente ha indovinato la parola, gli viene chiesto se vuole condividere 
				// il risultato e se vuole aspettare la prossima parola
				if (guessed) {
					guessed = false;

					System.out.print(ClientOutputMessage.RequestSharing);
					choice = scanner.nextLine();

					if (choice.toUpperCase().equals("Y")) {
						sendCode(ClientRequest.SHARE, buffer, connection);
					}

					System.out.print(ClientOutputMessage.RequestWaitNextWord);
					choice = scanner.nextLine();
					
					done = choice.toUpperCase().equals("N");
					waitNextWord = !done;
				}
				
				// Controlla se l'utente deve aspettare la prossima parola, in tal caso gli 
				// viene mostrato il tempo rimanente e viene inviata la richiesta al server
				// quando il server risponde, l'utente può riprendere a giocare
				if (waitNextWord) {
					minuteRemaining = (int) (((nextTimeToChange - System.currentTimeMillis()) / 1000) / 60);

					System.out.printf(ClientOutputMessage.WaitingMessage, minuteRemaining);
					
					sendCode(ClientRequest.WAITING_NEXT_WORD, buffer, connection);
					receiveResponseCode(buffer, connection);

					guessed = false;
					attemptRemaining = 12;
				}

				backToMainMenu = 0;
			}
			break;

			case UserChoice.GET_NOTIFICATION:
			notificationHandler.printNotification();
			backToMainMenu = 0;
			break;
			
			case UserChoice.GET_STATISTICS:
			sendCode(ClientRequest.SEND_STATISTICS, buffer, connection);
			buffer.clear();
			ObjectInputStream objectInputStream = new ObjectInputStream(connection.socket().getInputStream());
			
			try {
				ArrayList<GameRecord> records = ((GameHistory) objectInputStream.readObject()).getRecords();
				printStatistics(records);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			backToMainMenu = 0;
			break;

			case UserChoice.LOGOUT:
			sendCode(ClientRequest.LOGOUT, buffer, connection);
			connection.close();
			notificationHandler.stop();
			backToMainMenu = -1;
			break;
		}

		return backToMainMenu;
	}

	/**
	 * Classe privata rappresentante le scelte dell'utente
	 */
	private class UserChoice {
		public static final int REGISTER = 1;
		public static final int LOGIN = 2;
		public static final int EXIT = 3;

		public static final int PLAY = 1;
		public static final int GET_NOTIFICATION = 2;
		public static final int GET_STATISTICS = 3;
		public static final int LOGOUT = 4;

	}

	/**
	 * Metodo che avvia il thread per la ricezione delle notifiche
	 */
	private static void startReceivingNotification() {
		notificationHandlerThread = new Thread(notificationHandler);

		notificationHandlerThread.start();
	}

	/**
	 * Metodo che invia prepara il buffer per la lettura e invia il suo contenuto alla SocketChannel connection
	 * @param buffer contenente i dati da inviare
	 * @param connection connessione a cui inviare i dati contenuti nel buffer
	 * @throws IOException
	 */
	private static int sendBufferContent(ByteBuffer buffer, SocketChannel connection) throws IOException {		
		buffer.flip();
		connection.write(buffer);
		buffer.clear();
		
		return 0;
	}
	
	/**
	 * Metodo chiede all'utente le credenziali e le inserisce nel buffer
	 * @param buffer che verrà riempito con le credenziali
	 * @return 0 se le credenziali sono valide, -1 se l'utente ha inserito STOP oppure ha inserito delle credenziali non valide
	 */
	private static int getCredential(ByteBuffer buffer) {
		System.out.print(ClientOutputMessage.RequestUsername);
		String username = scanner.nextLine();
		
		buffer.putInt(username.length());
		buffer.put(username.getBytes());
		
		if (username.equals("STOP") || username.length() == 0) return -1;
		
		System.out.print(ClientOutputMessage.RequestPassword);
		String password = scanner.nextLine();

		if (password.length() == 0) {
			System.out.println(ClientOutputMessage.InvalidPassword);
			return -1;
		}

		buffer.putInt(password.length());
		buffer.put(password.getBytes());

		return 0;
	}

	/**
	 * Metodo che controlla se la parola inserita dall'utente è valida, quindi se è presente nel dizionario e se è lunga 10 caratteri
	 * @param word parola da controllare
	 * @return true se la parola è valida, false altrimenti
	 */
	private static Boolean wordValid(String word) {
		return (word.length() == 10) && vocabulary.isPresent(word);
	}

	/**
	 * Metodo che invia la parola da indovinare al server insieme a un codice che indica l'invio di una parola
	 * @param word parola da inviare
	 * @param buffer da riempire con i dati da inviare
	 * @param connection connessione a cui inviare i dati contenuti nel buffer
	 * @throws IOException
	 */
	private static void sendWord(String word, ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		buffer.putInt(ClientRequest.PLAY);
		buffer.put(word.getBytes());

		sendBufferContent(buffer, connection);
	}

	/**
	 * Metodo che invia il codice di richiesta al server
	 * @param code codice da inviare
	 * @param buffer da riempire con i dati da inviare
	 * @param connection connessione a cui inviare i dati contenuti nel buffer
	 * @throws IOException
	 */
	private static void sendCode(int code, ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		buffer.putInt(code);
		buffer.flip();

		connection.write(buffer);
	}

	/**
	 * Metodo che riceve la risposta dal server e la inserisce nel buffer
	 * @param buffer da riempire con i dati ricevuti
	 * @param connection connessione da cui ricevere i dati
	 * @throws IOException
	 */
	private static void receiveResponse(ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		connection.read(buffer);
		buffer.flip();
	}

	/**
	 * Metodo che riceve il codice di risposta dal server e la inserisce nel buffer
	 * @param buffer da riempire con il codice ricevuto
	 * @param connection connessione da cui ricevere i dati
	 * @return il codice di risposta ricevuto dal server
	 * @throws IOException
	 */
	private static int receiveResponseCode(ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		connection.read(buffer);
		buffer.flip();

		int responseCode = buffer.getInt();

		return responseCode;
	}

	/**
	 * Metodo che stampa le statistiche di gioco dell'utente
	 * @param records lista delle partite giocate dall'utente
	 */
	private static void printStatistics(ArrayList<GameRecord> records) {
		int[] winningDistribution = new int[12];
		int winCount = 0;
		int loseCount = 0;

		for (GameRecord record : records) {
			if (record.guessed()) {
				winCount++;
				winningDistribution[record.getAttemptsCount()-1]++;
			} else {
				loseCount++;
			}
		}

		for (int i = 0; i < winningDistribution.length; i++) {
			System.out.print((i+1) + ": " + winningDistribution[i] + "\t");
			
			for (int j = 0; j < winningDistribution[i]; j++) System.out.print("|");
			System.out.print("\n");
		}

		System.out.printf(ClientOutputMessage.WinAndLoseCounts, winCount, loseCount);
	}

	/**
	 * Metodo che configura il client leggendo il file di configurazione
	 * Inizializza le variabili host, port, vocabulary e multicastAddress
	 * Inizializza l'oggetto vocabulary e l'oggetto notificationHandler
	 * @param configurationFileName
	 */
	private static void configureClient(String configurationFileName) {
		File file = new File(configurationFileName);

		String vocabularyFileName = "";
		String multicastAddress = "";

		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			host = InetAddress.getByName(bufferedReader.readLine());
			port = Integer.parseInt(bufferedReader.readLine());
			vocabularyFileName = bufferedReader.readLine();
			multicastAddress = bufferedReader.readLine();

			bufferedReader.close();
			fileReader.close();
		
		} catch (FileNotFoundException e) {
			System.out.println(ClientOutputMessage.ClientConfigFlieNotFound);
			System.exit(1);
		} catch (UnknownHostException e) {
			System.out.println(ClientOutputMessage.InvalidHost);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (port < 1024 || port > 65535) {
			System.out.println(ClientOutputMessage.InvalidPort);
			System.exit(1);
		}

		vocabulary = new Vocabulary(vocabularyFileName);
		notificationHandler = new NotificationHandler(multicastAddress, port);
	}
}