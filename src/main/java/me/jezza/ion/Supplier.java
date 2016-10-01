package me.jezza.ion;

import java.io.IOException;
import java.util.Scanner;

import me.jezza.ion.cluster.IonCluster;

/**
 * Just a test class.
 *
 * @author Jezza
 */
public class Supplier {
	public static void main(String[] args) throws IOException {
		IonCluster cluster = Ion.cluster("myUniqueNetwork").join(6457);

		try {
			String msg = "Greeting from test";
			cluster.sendData(msg);
			Scanner scanner = new Scanner(System.in);
			while (true) {
				msg = scanner.nextLine();
				cluster.sendData(msg);
				System.out.println("Sending: '" + msg + '\'');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
