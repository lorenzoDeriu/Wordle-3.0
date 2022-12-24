import java.io.Serializable;
import java.util.ArrayList;

/**
 * Classe che rappresenta lo storico delle partite giocate
 * 
 * @author Lorenzo Deriu
 */
public class GameHistory implements Serializable {
	private ArrayList<GameRecord> record;

	/**
	 * Costruttore della classe
	 * @param record lista delle partite giocate
	 */
	public GameHistory(ArrayList<GameRecord> record) {
		this.record = record;
	}

	/**
	 * Metodo che restituisce la lista delle partite giocate
	 * @return lista delle partite giocate
	 */
	public ArrayList<GameRecord> getRecords() {
		return record;
	}
}
