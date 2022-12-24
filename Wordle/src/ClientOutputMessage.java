/**
 * Classe che contiene tutti i messaggi di output del client
 * 
 * @author Lorenzo Deriu
 */
public class ClientOutputMessage {
	public static final String ClientConfigFlieNotFound = "Il file di configurazione del client non esiste";

	public static final String RegistrationConfirmed = "Registrazione confermata, effettua il login per giocare.\n";
	public static final String RegistrationErrorMessage = "La registrazione non è andata a buon fine riprova con credenziali diverse.\n";
	public static final String StartingMessage = "----Wordle----\n[1] Registrati\n[2] Effettua il log in\n[3] Esci\n=> ";
	public static final String GamePlayInstruction = "[1] Prova a indovinare la parola\n[2] Mostra ciò che gli altri hanno condiviso\n[3] mostra le mie statistiche\n[4] Esegui il logout\n=> ";
	
	public static final String Welcome = "\nBentornato";
	public static final String CredentialError = "Credenziali errate";
	public static final String InvalidPassword = "La password deve avere almeno 1 carattere";
	public static final String ServerConnectionError = "Impossibile connettersi al server";

	public static final String RequestUsername = "Username (o STOP per annullare): ";
	public static final String RequestPassword = "Password: ";
	public static final String RequestWord = "Inserisci la parola oppure inserisci 'exit' per uscire. Ti rimangono %d tentaivi: ";
	public static final String VocabularyError = "La parola non è presente nel vocabolario, riprova";

	public static final String Result = "-------Result-------\n%s\n%s\n-------Result-------\n";
	public static final String WordHasChanged = "\n!!!La parola è cambiata, hai 12 tentativi per indovinarla!!!\n";
	public static final String WordGuessed = "Hai indovinato la parola!";
	public static final String TooMuchAttempt = "Hai finito i tentativi. Vuoi aspettare alla prossima parola, mancano %d minuti? [Y|N] => ";
	public static final String RequestSharing = "Vuoi condividere i risultati? [Y|N] => ";
	public static final String RequestWaitNextWord = "Vuoi aspettare la prossima parola? [Y|N] => ";
	public static final String WaitingMessage = "Avrai la possibilità di giocare la prossima parola non appena sarà disponibile. Mancano %d minuti\n";

	public static final String StatisticRecord = "parola segreta: \"%s\" | Numero di tentativi: %d => %s\n";
	public static final String WinAndLoseCounts = "Vittorie: %d - Sconfitte: %d\n";
	public static final String ConfigurationFileNotFoundError = "parola segreta: \"%s\" | Numero di tentativi: %d => %s\n";

	public static final String InvalidPort = "Il numero di porta deve essere compreso tra 1024 e 65535";
	public static final String InvalidHost = "L'indirizzo IP non è valido";

	public static final String MulticastSocketErrorMessage = "Impossibile usare il socket multicast";
}
