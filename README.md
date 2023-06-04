# Worlde 3.0
### Author: Lorenzo Deriu

# Program Description
The Wordle 3.0 system is divided into two main components, the client and the server. The client and server communicate using a set of ClientRequest-ServerResponse codes, each representing a specific action.

## 1.1 Server
The Wordle 3.0 server reads the configuration file at startup to obtain essential information for execution, such as the port to listen for new requests or the name of the file containing the fundamental vocabulary for word generation.

The server supports user login and registration functionality, allowing users 12 attempts to guess the word while keeping track of related statistics. It also supports a sharing mechanism, where each user has the option to share a message representing the outcome of the game with all connected users at the end of the game.

### 1.1.1 Login and Registration Management
The server handles each incoming request using the Multiplexing I/O approach. For both login and registration, the server receives the credentials from the client, verifies their validity, and sends a code representing the operation's result.

In the case of successful login, the server starts a separate Session thread to handle the logged-in user's session, passing the client's connection to it.

### 1.1.2 Secret Word Management
The secret word is changed by the server every time a specific time interval, read from the configuration file, elapses.

The mechanism to determine if the word should be changed is based on a comparison between System.currentTimeMillis and nextTimeToChange, which represents the moment (in milliseconds) when the next word change should occur.

If the system performs the check beyond nextTimeToChange, the word is updated by randomly choosing a word from the Vocabulary object. Then, all sessions are notified of the change using the wordChanged flag, and the user's attempt-related state is reset.

## 1.2 Client
The client allows the user to choose from various actions based on the context.

Before logging in, the options are limited to accessing a previously created account or registering a new account.

After logging in, the user can choose to guess the word, view their statistics, view others' shares, or log out.

### 1.2.1 Login and Registration
For user registration, the system connects to the server and, once the user enters their credentials, sends the specific request code, username, and password. It then waits for a response from the server. In case of success, the server informs the user of the operation's outcome and initiates the login process. In case of failure, the server requests the credentials again.

For login as well, the system connects to the server, sends the specific request code, username, and password. If the response is positive, the client displays possible actions to the user, including:

Guessing the word
Requesting statistics
Viewing shares
Logging out
After the login confirmation, the notificationHandlerThread is launched, which remains waiting for notifications from other users.

# Used Data Structures
## 2.1 Server-side Data Structures
The system stores all main data in a dedicated WordleData object.
WordleData encapsulates an ArrayList containing all user-related data and keeps track of their username, password, the number of attempts for the current secret word, and the list of all games played by the user. It also stores the current secret word and the next word change time. WordleData also tracks all sent messages.

WordleData is saved in a ".json" file whenever the server is shut down, or after a user registration or a secret word modification, to recover all information upon startup.

To obtain word feedback, the server uses the Vocabulary object, which provides an interface to access the ArrayList containing ten-character English words.

## 2.2 Client-side Data Structures
The client uses two data structures: the Vocabulary object and the messageReceived ArrayList, which stores all received messages since connecting to the server.

# Activated Threads
## 3.1 Server-side Threads
To start the server, we use the ServerStarterMain program, which initiates the WordleServer thread responsible for running the server.
ServerStarterMain shuts down as soon as the user presses the Enter key.

The WordleServer thread remains running until ServerStarterMain signals the system shutdown.

The WordleServer thread, in turn, starts Session threads whose purpose is to handle the sessions of each logged-in user, while all connections aimed only at authentication are handled by the server.

## 3.2 Client-side Threads
The WordleClientMain program's purpose is to present the various possible actions to the user and communicate the decisions to the server.

Upon receiving a successful login operation, WordleClientMain starts the NotificationHandlerThread, which remains waiting for new notifications as long as the client is connected and collects them in the dedicated messageReceived data structure.

# Synchronization Mechanisms
The WordleData object is implemented as a monitor since it is shared among all Session threads that access user data, the secret word, and the next change time for both read and write operations.
In particular, the need for synchronization arises from critical operations such as:

Registering a new user when two users with the same username are attempting registration simultaneously.
Modifying the secret word when a Session thread tries to access the word after the guessing time has expired and the server has not yet notified the change.
Compilation Instructions
To compile the program, execute the following commands:
```
$ cd Wordle/src/
~/Wordle/src/ $ javac -cp "./../libs/gson-2.10.jar" ./*.java -d ./../bin
```

The above command will create the ".class" files inside the bin folder, from which the system can be executed.

After compilation, the server and client ".jar" files can be created.

For the server, use the following commands:

```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ jar cfm Server.jar Server-Manifest.txt *.class
```

For the client:
```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ jar cfm Client.jar Client-Manifest.txt *.class
```
# Execution Instructions
Both the client and the server have configuration files available, "configClient.txt" and "configServer.txt," respectively. Their structure is as follows:
### configClient.txt:
```
server_address
server_port
word_file
multicast_address
```

### configServer.txt:
```
server_port
word_file
secret_word_interval_millisec
multicast_address
```
The configuration files are read at startup, and the data is verified.

There is also a backup file, "dataBackup.json," containing all information related to users, the secret word, and the next word change time.

If the dataBackup file is empty, the server initializes it again at startup, populating it with new information generated during execution.

The commands to execute the server are as follows:
```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ java -cp ".:./../libs/gson-2.10.jar" ServerStarterMain
```
The server will start, and to terminate its execution, simply press the Enter key. The server will be notified of the intention to terminate, shut down all Session threads, and perform a data backup.

To execute the client, use the following commands:
```
~ $ cd Wordle/bin/
~/Wordle/bin/ $ java -cp ".:./../libs/gson-2.10.jar" WordleClientMain
```
For executing the ".jar" files:

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
