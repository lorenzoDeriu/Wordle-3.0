import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Classe che rappresenta una sessione di gioco
 * implementa Runnable per poter essere eseguita in un thread
 * 
 * @author Lorenzo Deriu
 */
public class Session implements Runnable {
	private SocketChannel connection;
	private User user;
	private WordleData data;

	private Boolean wordChanged;

	private MulticastSocket multicastSocket;
	private InetSocketAddress multicastGroup;
	
	private final String WINNING_RESULT = "++++++++++";

	/**
	 * Costruttore della classe Session
	 * 
	 * @param connection SocketChannel di connessione
	 * @param data Oggetto WordleData contenente dati come: dizionario, lista di utenti, parola segreta, e successivo aggiornamento della parola
	 * @param user Oggetto User, rappresenta i dati dell'utente che ha effettuato la connessione
	 * @param multicastSocket Socket per la multicast
	 * @param multicastGroup Gruppo di multicast
	 */
	public Session(SocketChannel connection, WordleData data, User user, MulticastSocket multicastSocket, InetSocketAddress multicastGroup) {
		this.connection = connection;
		this.data = data;
		this.user = user;
		this.wordChanged = false;
		this.multicastSocket = multicastSocket;
		this.multicastGroup = multicastGroup;
	}

	/**
	 * Implementazione del metodo run() di Runnable
	 */
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		Boolean done = false;
		
		while (!done) {
			try {
				int request = receiveCode(buffer, connection);

				switch(request) {
					case ClientRequest.PLAY:
					if (wordChanged) {
						sendCode(ServerResponse.WORD_CHANGED, buffer, connection);
						wordChanged = false;
					} else handlePlay(buffer);
					break;

					case ClientRequest.LOGOUT:
					done = true;
					break;

					case ClientRequest.SHARE:
					GameRecord lastGameRecord = user.getLastGameRecord();
					String packetContent = messageComposer(user, lastGameRecord);
					
					DatagramPacket datagramPacket = new DatagramPacket(packetContent.getBytes(StandardCharsets.UTF_8), packetContent.length(), multicastGroup);
					multicastSocket.send(datagramPacket);

					data.addNotification(packetContent);
					break;

					case ClientRequest.SEND_STATISTICS:
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection.socket().getOutputStream());
					objectOutputStream.writeObject(new GameHistory(user.getGameRecord()));
					break;

					case ClientRequest.WAITING_NEXT_WORD:
					while (!wordChanged) ;
					sendCode(ServerResponse.WORD_CHANGED, buffer, connection);
					wordChanged = false;
					break;
				}
			} catch (BufferUnderflowException | IOException e) {
				System.out.println(ServerOutputMessage.ConnectionError);
				done = true;
			}
		}

		try { this.connection.close(); } 
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Metodo che invia il codice di risposta al client
	 * @param code Codice di risposta
	 * @param buffer Buffer di scrittura
	 * @param connection SocketChannel a cui inviare il codice
	 * @throws IOException
	 */
	private static void sendCode(int code, ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		buffer.putInt(code);
		buffer.flip();

		connection.write(buffer);
	}

	/**
	 * Metodo che riceve il codice di richiesta dal client
	 * @param buffer Buffer di lettura
	 * @param connection SocketChannel da cui ricevere il codice
	 * @return Codice di risposta
	 * @throws IOException
	 */
	private static int receiveCode(ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		connection.read(buffer);
		buffer.flip();

		int responseCode = buffer.getInt();

		return responseCode;
	}

	/**
	 * Metodo che si occupa di comporre il messaggio da inviare tramite multicast a tutti gli utenti
	 * @param user Utente che vuole inviare il messaggio
	 * @param record ultima partita giocata
	 * @return
	 */
	private static String messageComposer(User user, GameRecord record) {
		String message = ""; 
		message += user.getUsername() +  " ha indovinato \"";
		message += record.getWordToGuess() + "\"" + " in " + record.getAttemptsCount() + " tentativi";

		return message;
	}

	/**
	 * Metodo che gestisce la richiesta di indovinare la parola dell'utente
	 * @param buffer Buffer di lettura
	 * @throws IOException
	 */
	private void handlePlay(ByteBuffer buffer) throws IOException {		
		if (user.getAttempt() >= 12 || user.wordGuessed()) {
			buffer.clear();
			buffer.putInt(ServerResponse.FAILURE);
			buffer.putLong(data.getNextWordChangeTime());
			buffer.flip();
			
			connection.write(buffer);
			return;
		}
		
		byte[] byteArray = new byte[10];
		
		buffer.get(byteArray);
		buffer.clear();
		
		String word = new String(byteArray);
		String result = compareWithSecreteWord(word);

		buffer.putInt(ServerResponse.SUCCESS);
		buffer.put(result.getBytes());
		
		if (result.equals(WINNING_RESULT)) {
			user.guessed();
			buffer.putLong(data.getNextWordChangeTime());
		}
		
		buffer.flip();
		this.connection.write(buffer);
		

		user.newAttempt();

		if (user.getAttempt() == 12 || user.wordGuessed()) {
			user.updateStats(data.getCurrentSecreteWord());
		}
	}

	/**
	 * Metodo che confronta la parola inserita dall'utente con la parola segreta
	 * @param word Parola inserita dall'utente
	 * @return Stringa contenente i risultati del confronto, condificata nel seguente modo:
	 * "+" -> la lettera è presente nella parola segreta nella giusta posizione,
	 * "-" -> la lettera è presente nella parola segreta ma in una posizione diversa,
	 * "X" -> la lettera non è presente nella parola segreta
	 */
	private String compareWithSecreteWord(String word) {
		String secreteWord = data.getSecreteWord();
		HashMap<String,Integer> occurrence = new HashMap<String,Integer>();

		for (int i = 0; i < secreteWord.length(); i++) {
			String letter = secreteWord.charAt(i) + "";

			if (occurrence.containsKey(letter)) {
				occurrence.compute(letter, (k, v) -> v + 1);
			} else {
				occurrence.put(letter, 1);
			}
		}

		String result = "XXXXXXXXXX";

		result = markCorrectPositions(secreteWord, word, occurrence, result);
		result = markCorrectLetters(secreteWord, word, occurrence, result);

		return result;
	}

	/**
	 * Metodo che marca le lettere inserite dall'utente se sono presenti nella parola segreta nella giusta posizione
	 * @param secreteWord Parola segreta
	 * @param word Parola inserita dall'utente
	 * @param occurrence Mappa che contiene le occorrenze delle lettere nella parola segreta
	 * @param result Stringa contenente i risultati del confronto fin'ora ottenuti
	 * @return Stringa contenente i risultati del confronto
	 */
	private String markCorrectPositions(String secreteWord, String word, HashMap<String,Integer> occurrence, String result) {
		for (int i = 0; i < secreteWord.length(); i++) {
			if (secreteWord.charAt(i) == word.charAt(i)) {
				result = result.substring(0, i) + "+" + result.substring(i + 1);
				
				String c = word.charAt(i) + "";
				occurrence.compute(c, (key, value) -> value = value - 1);
			}
		}

		return result;
	}

	/**
	 * Metodo che marca le lettere inserite dall'utente se sono presenti nella parola segreta ma in una posizione diversa
	 * @param secreteWord Parola segreta
	 * @param word Parola inserita dall'utente
	 * @param occurrence Mappa che contiene le occorrenze delle lettere nella parola segreta
	 * @param result Stringa contenente i risultati del confronto fin'ora ottenuti
	 * @return Stringa contenente i risultati del confronto
	 */
	private String markCorrectLetters(String secreteWord, String word, HashMap<String,Integer> occurrence, String result) {
		for (int i = 0; i < secreteWord.length(); i++) {
			String c = "" + word.charAt(i);

			if (occurrence.containsKey(c) && occurrence.get(c) > 0 && result.charAt(i) == 'X') {
				result = result.substring(0, i) + "?" + result.substring(i + 1);
				occurrence.compute(c, (key, value) -> value = value - 1);
			}
		}

		return result;
	}

	/**
	 * Metodo che comunica l'aggiornamento della parola segreta
	 * modifica il flag wordChanged a true
	 */
	public void wordIsChanged() {
		wordChanged = true;
	}
}