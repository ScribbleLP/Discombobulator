package com.minecrafttas.discombobulator.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Lock a port with a socket. Prevents running a code twice at the same time
 * @author Pancake
 */
public class SocketLock {

	/**
	 * Server locking the port
	 */
	private ServerSocket socket;

	/**
	 * Locked port
	 */
	private int port;

	/**
	 * Creates a socket lock for a port
	 * @param port Port to lock
	 */
	public SocketLock(int port) {
		this.port = port;
		System.out.println(String.format("Locking Port \033[0;32m%s\033[0;37m\n", port));
		try {
			this.socket = new ServerSocket();
		} catch (IOException e) {
			throw new RuntimeException("Unable to create lock");
		}
	}

	/**
	 * Tries the lock the port and throws a RuntimeException if unsuccessful
	 */
	public void tryLock() {
		try {
			this.socket.bind(new InetSocketAddress(this.port));
		} catch (IOException e) {
			throw new RuntimeException("Unable to lock port " + this.port + ". Is another instance of this program already running?");
		}
	}

	/**
	 * Unlocks the port
	 */
	public void unlock() {
		try {
			this.socket.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to unlock port " + this.port + ". Wait what?!");
		}
	}

}
