import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Classe che rappresenta il dizionario con tutte le parole supportate dal gioco
 * 
 * @author Lorenzo Deriu
 */
public class Vocabulary {
	private ArrayList<String> words;
	
	/**
	 * Costruttore della classe
	 * @param fileName nome del file da cui caricare il dizionario
	 */
	public Vocabulary(String fileName) {
		words = new ArrayList<>();
		
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String word;
			while ((word = bufferedReader.readLine()) != null) {
				words.add(word);
			}

			fileReader.close();
			bufferedReader.close();

		} catch (FileNotFoundException e) {
			System.out.println("Il file del dizionario non esiste");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metodo che restituisce true se la parola passata come parametro è presente nel dizionario, false altrimenti
	 * @param word parola da cercare
	 * @return true se la parola è presente nel dizionario, false altrimenti
	 */
	public Boolean isPresent(String word) {
		return words.contains(word);
	}

	/**
	 * Metodo che restituisce una parola casuale presente nel dizionario
	 * @return parola casuale
	 */
	public String randomWord() {
		int index = new Random().nextInt(words.size());

		return words.get(index);
	}
}
