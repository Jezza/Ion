package me.jezza.ion;

import me.jezza.ion.cluster.IonClusterBuilder;

/**
 * @author Jezza
 */
public final class Ion {

	private Ion() {
		throw new IllegalStateException();
	}

	static IonClusterBuilder cluster(String name) {
		System.out.println("Configuring '" + name + "' Cluster...");
		return new IonClusterBuilder(name);
	}
}
