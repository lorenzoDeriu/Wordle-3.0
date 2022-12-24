import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

/**
 * Classe che rappresenta il server del gioco Wordle
 * implenta Runnable per poter essere eseguito in un thread
 * 
 * @author Lorenzo Deriu
 */
public class WordleServer implements Runnable {
	private int port;
	
	private WordleData data;

	private Vocabulary vocabulary;
	private long wordLifeTime;

	private boolean shutdown = false;

	private ArrayList<Session> activeSession;

	private InetSocketAddress multicastGroup;
	private MulticastSocket multicastSocket;

	private ExecutorService threadPool = null;

	private static final String BACKUP_FILE_NAME = "dataBackup.json";

	/**
	 * Implementazione del metodo run() di Runnable
	 */
	public void run() {
		configureServer("configServer.txt", "dataBackup.json");
		
		ServerSocketChannel socket = null;
		Selector selector = null;
		
		try { // apertura del socket e registrazione al selector
			socket = ServerSocketChannel.open();
			socket.bind(new InetSocketAddress(port));
			
			selector = Selector.open();
			
			socket.configureBlocking(false);
			socket.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			System.out.println(ServerOutputMessage.SocketOpeningError);
		}

		ByteBuffer buffer = ByteBuffer.allocate(1024);

		System.out.printf(ServerOutputMessage.ServerListening, port);
		
		// accettazione di nuove connessioni e gestione delle richieste
		while (!shutdown || !threadPool.isTerminated()) { 
			int readyKey = 0;
			
			try { readyKey = selector.selectNow(); } 
			catch(IOException e) { e.printStackTrace(); }

			if (data.isTimeToChangeWord()) {
				changeWord();
				data.doBackup(BACKUP_FILE_NAME);
			}

			if (shutdown) {
				threadPool.shutdownNow();
				changeWord();
			}

			if (readyKey == 0) {
				continue;
			}

			Iterator<SelectionKey> selectedKey = selector.selectedKeys().iterator();

			while (selectedKey.hasNext()) {
				SelectionKey key = selectedKey.next();

				if (key.isAcceptable()) {
					acceptNewConnection(socket, selector);
				} else if (key.isReadable()) {
					SocketChannel client = (SocketChannel) key.channel();
					
					buffer.clear();
					
					try { client.read(buffer); }
					catch (IOException e) { e.printStackTrace(); }

					buffer.flip();
					
					int operation = buffer.getInt();

					switch(operation) {
						case ClientRequest.REGISTER:
						handleRegistration(client, buffer);
						data.doBackup(BACKUP_FILE_NAME);
						break;

						case ClientRequest.LOGIN:
						try { 
							handleLogin(key, client, buffer); 
						}
						catch(IOException e) {e.printStackTrace();}
						break;
					}
					key.cancel();
				}
				selectedKey.remove();
			}
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		data.doBackup(BACKUP_FILE_NAME);
		System.out.println(ServerOutputMessage.TerminationMessage);
	}

	/**
	 * Metodo che accetta una nuova connessione, crea un nuovo SocketChannel per il client e lo registra al Selector
	 * @param socket è il ServerSocketChannel che ha ricevuto la richiesta di connessione
	 * @param selector è il Selector a cui verrà aggiunto il SocketChannel del client
	 */
	private void acceptNewConnection(ServerSocketChannel socket, Selector selector) {
		try {
			SocketChannel client = (SocketChannel) socket.accept();
			System.out.println("new Connection established");

			client.configureBlocking(false);
			client.register(selector, SelectionKey.OP_READ);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metodo che gestisce la registrazione di un nuovo utente, controlla che il nome utente non sia già
	 * presente e che la password abbia una lunghezza maggiore di 1, in caso affermativo invia un codice di Successo al client, e aggiunge l'oggetto 
	 * User alla lista degli utenti, altrimenti invia un codice di Fallimento
	 * @param client è il SocketChannel del client che ha richiesto la registrazione
	 * @param buffer è il ByteBuffer che contiene i dati inviati dal client
	 */
	private void handleRegistration(SocketChannel client, ByteBuffer buffer) {
		String clientUsername = getNextString(buffer, buffer.getInt());
		String clientPassword = getNextString(buffer, buffer.getInt());

		int result = 0;

		if ( (!data.usernameAlreadyPresent(clientUsername)) && clientPassword.length() > 1 ) {
			data.newUser(clientUsername, clientPassword, 0, false);
			result = ServerResponse.SUCCESS;
		} else {
			result = ServerResponse.FAILURE;
		}

		try { sendCode(result, buffer, client); }
		catch (IOException e) {System.out.println(ServerOutputMessage.ConnectionError);}
	}

	/**
	 * Metodo che gestisce il login di un utente, controlla che il nome utente sia presente e che la password sia corretta, 
	 * in caso affermativo invia un codice di Successo al client e fa partire il thread per la gestione della sessione, altrimenti invia un codice di Fallimento
	 * @param key è la SelectionKey del client che ha richiesto il login
	 * @param client è il SocketChannel del client che ha richiesto il login
	 * @param buffer è il ByteBuffer che contiene i dati inviati dal client
	 * @throws IOException
	 */
	private void handleLogin(SelectionKey key, SocketChannel client, ByteBuffer buffer) throws IOException {
		String clientUsername = getNextString(buffer, buffer.getInt());
		String clientPassword = getNextString(buffer, buffer.getInt());

		if (data.passwordVerification(clientUsername, clientPassword)) {
			buffer.clear();
			buffer.putInt(ServerResponse.SUCCESS);
			buffer.putInt(data.getUser(clientUsername).getAttempt());
			buffer.flip();

			client.write(buffer);
			
			key.cancel();
			key.channel().configureBlocking(true);

			Session session = new Session((SocketChannel) key.channel(), data, data.getUser(clientUsername), multicastSocket, multicastGroup);
			threadPool.execute(session);
			
			activeSession.add(session);
			
		} else {
			sendCode(ServerResponse.FAILURE, buffer, client);
		}
	}

	/**
	 * Metodo che invia un codice di risposta al client. Il buffer viene prima pulito, poi viene inserito il codice e infine viene inviato al client
	 * @param code è il codice da inviare
	 * @param buffer è il ByteBuffer che verrà utilizzato per inviare il codice
	 * @param connection è il SocketChannel del client a cui verrà inviato il codice
	 * @throws IOException
	 */
	private static void sendCode(int code, ByteBuffer buffer, SocketChannel connection) throws IOException {
		buffer.clear();
		buffer.putInt(code);
		buffer.flip();

		connection.write(buffer);
	}

	/**
	 * Metodo che restituisce la stringa contenuta in un ByteBuffer
	 * @param buffer è il ByteBuffer da cui verrà estratta la stringa
	 * @param stringLength è la lunghezza della stringa
	 * @return la stringa contenuta nel ByteBuffer
	 */
	private String getNextString(ByteBuffer buffer, int stringLength) {
		byte[] byteArray = new byte[stringLength];
		buffer.get(byteArray);

		return new String(byteArray);
	}

	/**
	 * Metodo che cambia la parola segreta scegliendone una nuova a caso e notificando l'avvenuta modifica a tutte le sessioni
	 */
	private void changeWord() {
		data.setSecreteWord(vocabulary.randomWord(), this.wordLifeTime);
		updateAllSession();
	}
	
	/**
	 * Metodo che aggiorna tutte le sessioni comunicando 
	 * l'avvenuta modifica della parola segreta
	 */
	private void updateAllSession() {
		for (Session session : activeSession) {
			session.wordIsChanged();
		}
	}

	/**
	 * Metodo che abilita la chiusura del server
	 */
	public void shutdown() {
		shutdown = true;
	}
	
	/**
	 * Metodo che configura il server leggendo i dati dal file di configurazione e dal file di backup.
	 * Inizializza il MulticastSocket per l'invio delle notifiche agli utenti.
	 * Inizializza il ThreadPool per la gestione delle sessioni.
	 * @param configurationFileName contiene il nome del file di configurazione
	 * @param backupFileName contiene il nome del file di backup
	 */
	private void configureServer(String configurationFileName, String backupFileName) {
		File file = new File(configurationFileName);
		String vocabularyFileName = "";
		String multicastAddress = "";
	
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			port = Integer.parseInt(bufferedReader.readLine());
			vocabularyFileName = bufferedReader.readLine();
			wordLifeTime = Long.parseLong(bufferedReader.readLine());
			multicastAddress = bufferedReader.readLine();
	
			bufferedReader.close();
			fileReader.close();
			
			fileReader = new FileReader(new File(backupFileName));
			Gson gson = new Gson();
			data = gson.fromJson(fileReader, WordleData.class);
			fileReader.close();
		
		} catch (FileNotFoundException e) {
			System.out.println(ServerOutputMessage.configErrorMessage);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (port < 1024 || port > 65535) {
			System.out.println(ServerOutputMessage.PortErrorMessage);
			System.exit(1);
		}

		if (wordLifeTime < 60000) {
			System.out.println(ServerOutputMessage.WordLifeTimeErrorMessage);
			System.exit(1);
		}
		
		vocabulary = new Vocabulary(vocabularyFileName);
		threadPool = Executors.newCachedThreadPool();
		activeSession = new ArrayList<>();
		
		if (data == null) { // Il file dataBackup.json è vuoto
			data = new WordleData();
		}

		try {
			InetAddress multicastInetAddress = InetAddress.getByName(multicastAddress);
			multicastGroup = new InetSocketAddress(multicastInetAddress, port);
			NetworkInterface networkInterface = NetworkInterface.getByName("lo0");
			
			multicastSocket = new MulticastSocket(port);
	
			multicastSocket.joinGroup(new InetSocketAddress(multicastInetAddress, 0), networkInterface);
		} catch (UnknownHostException | SocketException e) {
			System.out.println(ServerOutputMessage.MulticastSocketErrorMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}