import java.util.ArrayList;

/**
 * Classe che rappresenta un utente del gioco
 * 
 * @author Lorenzo Deriu
 */
public class User {
	private String username;
	private String password;

	private ArrayList<GameRecord> gameRecord; 

	private int attempt;

	private Boolean wordGuessed;

	/**
	 * Costruttore della classe
	 * @param username nome utente
	 * @param password password
	 * @param attempt numero di tentativi
	 * @param wordGuessed se la parola è stata indovinata
	 */
	public User(String username, String password, int attempt, Boolean wordGuessed) {
		this.username = username;
		this.password = password;

		this.attempt = attempt;
		this.wordGuessed = wordGuessed;
		this.gameRecord = new ArrayList<>();
	}

	/**
	 * Costruttore della classe
	 * @param username nome utente
	 * @param password password
	 * @param attempt numero di tentativi
	 * @param wordGuessed se la parola è stata indovinata
	 * @param gameRecord lista delle partite giocate
	 */
	public User(String username, String password, int attempt, Boolean wordGuessed, ArrayList<GameRecord> gameRecord) {
		this.username = username;
		this.password = password;

		this.attempt = attempt;
		this.wordGuessed = wordGuessed;
		this.gameRecord = gameRecord;
	}

	/**
	 * Metodo che cambia il flag wordGuessed a true
	 * wordGuessed indica se la parola è stata indovinata nella partita corrente
	 */
	public void guessed() {
		this.wordGuessed = true;
	}

	/**
	 * Metodo che restituisce il numero di tentativi
	 * @return numero di tentativi
	 */
	public int getAttempt() {
		return attempt;
	}

	/**
	 * Metodo che incrementa il numero di tentativi
	 */
	public void newAttempt() {
		this.attempt++;
	}

	/**
	 * Metodo che restituisce il nome utente
	 * @return nome utente
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Metodo che controlla se la password passata come parametro è corretta
	 * @return true se la password è corretta, false altrimenti
	 */
	public Boolean checkPassword(String password) {
		return this.password.equals(password);
	}

	/**
	 * Metodo che aggiorna le statistiche del gioco
	 * @param oldWord parola che l0utente ha cercato di indovinare
	 */
	public void updateStats(String oldWord) {
		this.gameRecord.add(new GameRecord(this.attempt, this.wordGuessed, oldWord));
	}
	
	/**
	 * Metodo i dati di gioco dell'utente
	 */
	public void reset() {
		this.attempt = 0;
		this.wordGuessed = false;
	}
	
	/**
	 * Metodo che restituisce se la parola è stata indovinata
	 * @return true se la parola è stata indovinata, false altrimenti
	 */
	public Boolean wordGuessed() {
		return wordGuessed;
	}

	/**
	 * Metodo che restituisce la lista delle partite giocate
	 * @return lista delle partite giocate
	 */
	public ArrayList<GameRecord> getGameRecord() {
		return gameRecord;
	}

	/**
	 * Metodo che restituisce l'ultima partita giocata
	 * @return ultima partita giocata
	 */
	public GameRecord getLastGameRecord() {
		return gameRecord.get(gameRecord.size() - 1);
	}
}
