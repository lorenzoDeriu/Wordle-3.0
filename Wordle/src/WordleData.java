import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Classe che rappresenta i dati del gioco
 * 
 * @author Lorenzo Deriu
 */
public class WordleData { 	
	private String currentSecreteWord;
	private long nextWordChangeTime;
	
	private ArrayList<User> user;
	
	private ArrayList<String> notificationSended;

	/**
	 * Costruttore della classe.
	 * Inizializza le liste user e notificationSended, e imposta il tempo di aggiornamento della parola segreta in modo tale che venga aggiornata subito
	 */
	public WordleData() {
		user = new ArrayList<>();
		notificationSended = new ArrayList<>();

		nextWordChangeTime = System.currentTimeMillis();
	}

	/**
	 * Metodo che restituisce la parola segreta
	 * 
	 * @return parola segreta
	 */
	public String getSecreteWord() {
		return this.currentSecreteWord;
	}

	/**
	 * Metodo che effettua il backup dei dati del gioco
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @param backupFileName nome del file di backup
	 */
	public synchronized void doBackup(String backupFileName) {
		File backupFile = new File(backupFileName);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try {
			FileWriter fileWriter = new FileWriter(backupFile);
			fileWriter.write(gson.toJson(this));

			fileWriter.close();
		} catch (IOException e) {
			System.out.println("Si è verificato un problema nel salvataggio dei dati");
		}
	}

	/**
	 * Metodo che aggiunge un nuovo utente alla lista user
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @param username username dell'utente
	 * @param password password dell'utente
	 * @param attempt numero di tentativi rimasti
	 * @param wordGuessed indica se l'utente ha indovinato la parola segreta
	 */
	public synchronized void newUser(String username, String password, int attempt, Boolean wordGuessed) {
		user.add(new User(username, password, attempt, wordGuessed));
	}

	/**
	 * Metodo che controlla se l'utente è già presente nella lista user
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @param username username dell'utente
	 * @return true se l'utente è già presente, false altrimenti
	 */
	public synchronized Boolean usernameAlreadyPresent(String username) {
		for (int i = 0; i < user.size(); i++) {
			if (user.get(i).getUsername().equals(username)) {
				return true;
			}
		}
	
		return false;
	}

	/**
	 * Metodo che controlla se la password inserita dall'utente è corretta
	 * 
	 * @param username username dell'utente
	 * @param password password dell'utente
	 * @return true se la password è corretta, false se l'utente non è presente nella lista user o se la password è errata
	 */
	public Boolean passwordVerification(String username, String password) {
		User user;
		
		if ((user = getUser(username)) != null) {
			return user.checkPassword(password);
		}

		return false;
	}

	/**
	 * Metodo che restituisce l'utente con username uguale a quello passato come parametro
	 * 
	 * @param username username dell'utente
	 * @return utente con username uguale a quello passato come parametro se presente nella lista user, null altrimenti
	 */
	public User getUser(String username) {
		for (int i = 0; i < user.size(); i++) {
			if (user.get(i).getUsername().equals(username)) {
				return user.get(i);
			}
		}

		return null;
	}

	/**
	 * Metodo che imposta la parola segreta e il tempo di aggiornamento della parola segreta, inoltre resetta lo stato di tutti gli utenti
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @param newWord nuova parola segreta
	 * @param wordLifeTime intervallo di aggiornamento della parola segreta
	 */
	public synchronized void setSecreteWord(String newWord, long wordLifeTime) {
		user.forEach((user) -> user.reset());

		this.currentSecreteWord = newWord;
		this.nextWordChangeTime = System.currentTimeMillis() + wordLifeTime;
	}

	/**
	 * Metodo che restituisce la attuale parola segreta
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @return parola segreta
	 */
	public synchronized String getCurrentSecreteWord() {
		return currentSecreteWord;
	}

	/**
	 * Metodo che restituisce il prossimo momento di aggiornamento della parola segreta
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @return il prossimo momento di aggiornamento della parola segreta
	 */
	public synchronized long getNextWordChangeTime() {
		return nextWordChangeTime;
	}

	/**
	 * Metodo che controlla se è il momento di aggiornare la parola segreta
	 * implementato in modo sincronizzato per evitare problemi di concorrenza
	 * @return true se è il momento di aggiornare la parola segreta, false altrimenti
	 */
	public synchronized Boolean isTimeToChangeWord() {
		return (System.currentTimeMillis() > nextWordChangeTime);
	}

	/* Metodo che restituisce la lista degli utenti
	 * 
	 * @return lista degli utenti
	 */
	public ArrayList<User> getUsers() {
		return user;
	}

	/**
	 * Metodo che aggiunge una notifica alla lista notificationSended
	 * @param notification notifica da aggiungere
	 */
	public void addNotification(String notification) {
		if (notificationSended == null) {
			notificationSended = new ArrayList<>();
		}
		notificationSended.add(notification);
	}
}