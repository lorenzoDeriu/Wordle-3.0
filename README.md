# Worlde 3.0

Author: Lorenzo Deriu 

# 1. Descrizione del programma

Il sistema Wordle 3.0 è diviso in due componenti principali, il client e il server.
Client e Server comunicano con un insieme di codici ClientRequest-ServerResponse ognuno rappresentante una specifica azione.

## 1.1 Server

Il server di Wordle 3.0 all’avvio legge il file di configurazioni per ottenere informazioni fondamentali per l’esecuzione, quali la porta su cui rimanere in attesa di nuove richieste o il nome del file in cui si trova il vocabolario fondamentale per la generazione delle parole.

Il server supporta le funzionalità di log-in e registrazione degli utenti, concede agli utenti 12 tentativi per indovinare la parola, tenendo traccia delle relative statistiche.
Supporta inoltre un meccanismo di condivisione per cui ogni utente, al termine della partita, ha la possibilità di condividere a tutti gli utenti connessi un messaggio rappresentante l’esito della partita.

### 1.1.1 Gestione del Log-in e della Registrazione

Il server gestisce ogni richiesta in entrata con l’approccio Multiplexing I/O.
Sia per log-in che registrazione, il server riceverà dal client le credenziali ne verificherà la validità e invierà un codice rappresentante l’esito dell’operazione.

Nel caso di Log-in effettuato con successo il server avvierà un’apposito thread *******Session*******, a cui verrà passata la connessione del client, e che gestirà la sessione dell’utente loggato.

### 1.1.2 Gestione della parola segreta

La parola segreta viene cambiata dal server ogni volta che scorre uno specifico intervallo di tempo che leggerà dal file di configurazione.

Il meccanismo per stabilire se la parola va cambiata si basa su un confronto tra *System.currentTimeMillis* e il ****************nextTimeToChange**************** cioè il momento (rappresentato in millisecondi) in cui deve avvenire il prossimo cambio di parola.

Se il sistema effettua il controllo oltre il *nextTimeToChange* allora la parola viene aggiornata, scegliendo in maniera random una parola dall’oggetto *Vocabulary*, dopodiché tutte le sessioni vengono notificate tramite la flag ***********wordChanged*********** del cambio, e lo stato degli utenti relativo al numero di tentativi viene resettato.

## 1.2 Client

Il client permette all’utente di scegliere le possibili azioni in base al contesto.

Prima del log-in le possibilità si limitano ad accedere con un account creato in precedenza oppure alla registrazione di un nuovo account.

Dopo il log-in l’utente ha la possibilità di scegliere se indovinare la parola, vedere le proprie statistiche o vedere le condivisione degli altri.

### 1.2.1 Log-in e Registrazione

Per la registrazione dell’utente il sistema, si collega al server e, una volta che l’utente ha inserito le credenziali, invia il codice della richiesta specifico, username e password.
Dopodichè rimane in attesa di una risposta da parte del client, in caso di successo il server comunica all’utente l’esito dell’operazione e inizia il processo di Log-in.
In caso di esito negativo il server chiede nuovamente le credenziali.

Anche per il log-in il sistema, si collega al server, invia il codice della richiesta specifico, username e password.
Se la risposta ha esito positvo il client mostra all’utente le possibili azioni tra cui:
  • Tentativo di indovinare la parola
  • Richiesta delle statistiche
  • Mostrare le condivisioni
  • Effettuare log-out

Dopo la conferma di log-in viene fatto partire il *notificationHandlerThread* che si occupa di rimanere in attesa di notifiche provenienti dagli altri utenti.


# 2. Strutture dati utilizzate

## 2.1 Strutture dati lato Server

Il sistema memorizza tutti i dati principali in un oggetto apposito di tipo *WordleData*.

WordleData incapsula un’ArrayList contenente tutti i dati relativi agli utenti e, per ognuno di essi mantiene il suo username, la sua password, il numero di tentativi per la parola segreta corrente e la lista di tutte le partite da lui giocate.
Conserva, inoltre, la parola segreta corrente e, il prossimo momento in cui la parola deve cambiare.
WordleData tiene traccia anche di tutti i messaggi che sono stati inviati.

WordleData viene salvato in un file *“.json”* ogni volta che il server viene spento, oppure successivamente alla registrazione di un utente o la modifica della parola segreta in modo da poter recuperare tutte le informazioni all’accensione.

Infine per ottenere il riscontro con le parole, il server utilizza l’oggetto *Vocabulary*, che fornisce un’interfaccia per l’accesso all’ArrayList contenente le parole della lingua inglese composte da dieci caratteri.

## 2.2 Strutture dati lato Client

Il client utilizza due strutture dati: l’oggetto *Vocabulary* e l’ArrayList *messageReceived* che conserva tutti i messaggi ricevuti dal momento in cui si è collegato al server.


# 3. Thread Attivati

## 3.1 Thread lato Server

Per avviare il server usiamo il programma *ServerStarterMain*, il quale avvia il thread ************WordleServer************ che andrà a eseguire il server.

*ServerStarterMain* si spegnerà non appena l’utente premerà il tasto *Enter.*

Il thread *WordleServer* rimarrà in esecuzione finchè *ServerStarterMain* non comunicherà la volontà di spegnere il sistema.

Il thread *WordleServer* avvierà, a sua volta, dei thread *Session*, il cui scopo è quello di gestire le sessioni di ogni utente una volta loggati, mentre tutte le connessioni che hanno come scopo la sola autenticazione vengono gestite dal server.

## 3.2 Thread lato Client

Il programma *WordleClientMain* ha lo scopo di illustrare all’utente le varie azioni possibili e comunicare al server le decisioni prese.

*WordleClientMain* avvia, una volta ricevuto esito positivo dall’operazione di log-in, il thread *NotificationHandlerThread* che rimane in attesa per tutto il tempo in cui il client è connesso di nuove notifiche e le raccoglie all’interno della struttura dati apposita messageReceived.


# 4. Meccanismi di sincronizzazione

L’oggetto *WordleData* è implementato come un Monitor, poichè viene condiviso tra tutti i thread Session che accedono in letture e in scrittura ai dati degli utenti, alla parola segreta e al momento in cui dovrà cambiare.

In particolare la necessità di sincronizzare gli accessi nasce da operazioni critiche quali:

- La registrazione di un nuovo utente, nel caso in cui due utenti con lo stesso username stiano cercando di registrarsi contemporaneamente.
- La modifica della parola segreta, nel caso in cui un thread *Session* cercasse di accedere alla parola quando il tempo per poterla indovinare è scaduto, e il server non ha ancora notificato il cambio.


# 5. Istruzioni di compilazione

Per compilare il programma è necessario eseguire i seguenti comandi:

```
$ cd Wordle/src/
~/Wordle/src/ $ javac -cp "./../libs/gson-2.10.jar" ./*.java -d ./../bin
```

Quest’ultimo comando avrà l’effetto di creare all’interno della cartella bin tutti i file *“.class”*, da cui poi potremmo andare a eseguire il sistema*.*

Dopo la compilazione è possibile andare a effettuare la creazione del file *“.jar”* del server e del client.

Per quanto riguarda il server i comandi sono i seguenti:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ jar cfm Server.jar Server-Manifest.txt *.class
```

Per il client:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ jar cfm Client.jar Client-Manifest.txt *.class
```

# 6. Istruzioni di esecuzione

Sia il client che il server hanno a disposizione un file di configurazione, “configClient.txt” e configServer.txt, la loro struttura è la seguente:

*configClient.txt*

```
indirizzo_server
porta_server
file_parole
indirizzo_multicast
```

*configServer.txt*

```
porta_server
file_parole
intervallo_parola_segreta_millisec
indirizzo_multicast
```

I file di configurazione vengono letti al momento di avvio, e i dati vengono verificati.

È presente anche un file di backup, *“dataBackup.json”* contenente tutte le informazioni relative agli utenti la parola segreta e il prossimo momento in cui deve cambiare la parola.

Se **********dataBackup********** viene svuotato, al momento dell’avvio il server lo inizializza nuovamente, riempiendolo con le nuove informazioni generate durante l’esecuzione.

I comandi per eseguire il server sono i seguenti:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ java -cp ".:./../libs/gson-2.10.jar" ServerStarterMain
```

Il server verrà avviato è per terminare la sua esecuzione sarà sufficente premere il tasto *Enter*;
verrà comunicato al server la volontà di terminare l’esecuzione, che terminerà tutti i thread *Session* e effettureà un backup dei dati.

Per eseguire il client i comandi sono:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ java -cp ".:./../libs/gson-2.10.jar" WordleClientMain
```

Per l’esecuzione dei file “.jar”:

Server:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ java -jar Server.jar
```

Client:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ java -jar Client.jar
```
