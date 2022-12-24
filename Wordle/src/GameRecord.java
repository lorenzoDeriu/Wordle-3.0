import java.io.Serializable;

/**
 * Classe che rappresenta le informazioni del gioco: numero di tentativi, parola da indovinare, se è stata indovinata
 * Implementa Serializable per poter essere serializzato e inviato tramite socket
 * @author Lorenzo Deriu
 */
public class GameRecord implements Serializable {
	private int attemptsCount;
	private Boolean guessed;
	private String wordToGuess;

	/**
	 * Costruttore della classe
	 * @param attemptsCount numero di tentativi
	 * @param guessed se la parola è stata indovinata
	 * @param wordToGuess parola da indovinare
	 */
	public GameRecord(int attemptsCount, Boolean guessed, String wordToGuess) {
		this.attemptsCount = attemptsCount;
		this.guessed = guessed;
		this.wordToGuess = wordToGuess;
	}

	/**
	 * Metodo che restituisce il numero di tentativi
	 * @return numero di tentativi
	 */
	public int getAttemptsCount() {
		return attemptsCount;
	}

	/**
	 * Metodo che restituisce l'esito della partita
	 * @return true se la parola è stata indovinata, 0 altrimenti
	 */
	public Boolean guessed() {
		return guessed;
	}
	
	/**
	 * Metodo che restituisce quale parola l'utente ha cercato di indovinare
	 * @return parola da indovinare
	 */
	public String getWordToGuess() {
		return wordToGuess;
	}
}
