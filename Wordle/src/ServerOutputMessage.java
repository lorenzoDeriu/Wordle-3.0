/**
 * Classe che contiene i messaggi di output del server
 * 
 * @author Lorenzo Deriu
 */
public class ServerOutputMessage {
	public static final String jsonErrorMessage = "Il file di backup non è strutturato correttamente";
	public static final String configErrorMessage = "Il file di configurazione del Server non esiste";
	public static final String ConnectionError = "Il client si è disconnesso";
	public static final String TerminationMessage = "Il sistema è stato terminato correttamente";
	public static final String InterrputedExceptionMessage = "Il thread è stato interrotto";
	public static final String ServerListening = "Il server è in ascolto sulla porta %d\n";
	public static final String SocketOpeningError = "Si è verificato un errore nell'apertura del socket";
	
	public static final String PortErrorMessage = "La porta deve essere un numero compreso tra 1024 e 65535";
	public static final String WordLifeTimeErrorMessage = "Il life time della parola deve essere un numero maggiore o uguale a 60000";
	public static final String MulticastSocketErrorMessage = "Si è verificato un errore nell'apertura del socket multicast";
}
