import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
/**
 * Classe che rappresenta il thread che si occupa di ricevere le notifiche
 * Implementa Runnable per poter essere eseguito in un thread
 * 
 * @author Lorenzo Deriu
 */
public class NotificationHandler implements Runnable {
	private String multicastAddress;
	private int port;

	private ArrayList<String> messageReceived;

	private Boolean stop;

	/**
	 * Costruttore della classe
	 * @param multicastAddress indirizzo multicast a cui connettersi
	 * @param port porta a cui connettersi
	 */
	public NotificationHandler(String multicastAddress, int port) {
		this.multicastAddress = multicastAddress;
		this.port = port;
	}

	/**
	 * Implementazione del metodo run() di Runnable
	 */
	public void run() {
		try {
			MulticastSocket multicastSocket = new MulticastSocket(port);
			InetAddress multicastInetAddress = InetAddress.getByName(multicastAddress);
			InetSocketAddress multicastGroup = new InetSocketAddress(multicastInetAddress, port);
			NetworkInterface networkInterface = NetworkInterface.getByName("lo0");

			multicastSocket.joinGroup(new InetSocketAddress(multicastInetAddress, 0), networkInterface);

			multicastSocket.setSoTimeout(10);
			
			this.messageReceived = new ArrayList<>();
			stop = false;
			
			while (!stop) {
				try {
					DatagramPacket receivedPacket = new DatagramPacket(new byte[512], 512);
					multicastSocket.receive(receivedPacket);
					messageReceived.add(new String(receivedPacket.getData()));
				} catch (SocketTimeoutException e) {}
			}

			multicastSocket.leaveGroup(multicastGroup, networkInterface);
			multicastSocket.close();
		
		} catch (UnknownHostException | SocketException e) {
			System.out.println(ClientOutputMessage.MulticastSocketErrorMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metodo che stampa le notifiche ricevute
	 */
	public void printNotification() {
		messageReceived.forEach((msg) -> System.out.println(msg));

		System.out.print("\n");
	}

	/**
	 * Metodo che porta allo spegnimento del thread
	 */
	public void stop() {
		stop = true;
	}
}
