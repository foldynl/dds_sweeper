
/*
 * ArduinoAnalyzerCom
 *
 * Author:  Ladislav Foldyna
 *
 * Copyright (C) 2018 Ladislav Foldyna
 *
 * This file is part of AntennaAnalyzer.
 *
 * AntennaAnalyzer is free software: you can redistribute it and/or modify
 * it under the terms of either the Apache Software License, version 2, or
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation, version 3 or above.
 *
 * AntennaAnalyzer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of both the GNU Lesser General Public
 * License and the Apache Software License along with jSerialComm. If not,
 * see <http://www.gnu.org/licenses/> and <http://www.apache.org/licenses/>.
 */

import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import com.fazecast.jSerialComm.SerialPort;

public class ArduinoAnalyzerCom {

	static private SerialPort chosenPort;
	static private boolean portconnected = false;
	static private int baudRate;
	static private Scanner scanner;
	private String token;
	private JTextArea arduinoMonitor;

	/**
	 * It prepares a connection to Arduino
	 * 
	 * @param BaudRate
	 * @param textMonitor
	 */
	public ArduinoAnalyzerCom(int BaudRate, JTextArea textMonitor) {
		baudRate = BaudRate;
		arduinoMonitor = textMonitor;
	}

	/**
	 * It establishes a connection to Arduino
	 * 
	 * @param COMPort
	 * @return Was port opened?
	 */
	public boolean openPort(String COMPort) {
		chosenPort = SerialPort.getCommPort(COMPort);
		chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
		chosenPort.setBaudRate(baudRate);

		portconnected = chosenPort.openPort();
		scanner = new Scanner(chosenPort.getInputStream());

		return portconnected;

	}

	/**
	 * Close the Serial port
	 * 
	 * @return Successful or not
	 */
	public boolean closePort() {
		portconnected = false;
		return chosenPort.closePort();
	}

	/**
	 * It sends a command to arduino
	 * 
	 * @param bytes
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void sendCommand(String bytes) throws IOException, InterruptedException {
		if (!portconnected) {
			JOptionPane.showMessageDialog(null, "Cannot sent a command - Port is not connected", "Connection Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		OutputStream out = chosenPort.getOutputStream();
		out.write(bytes.getBytes());
		out.flush();
		Thread.sleep(50);
		arduinoMonitor.append("Send command: " + bytes + "\n");
		arduinoMonitor.setCaretPosition(arduinoMonitor.getDocument().getLength());
	}

	/**
	 * Gets nextlines with Timeout
	 * 
	 * At this moment Timeout is not implemented
	 * 
	 * @param timeout
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public String nextLineTimeout(int timeout) throws InterruptedException, ExecutionException, TimeoutException {

		if (!portconnected) {
			JOptionPane.showMessageDialog(null, "Cannot read a next line - Port is not connected", "Connection Error",
					JOptionPane.ERROR_MESSAGE);
			throw new InterruptedException();
		}

		token = scanner.nextLine();
		arduinoMonitor.append("Received: " + token + "\n");
		arduinoMonitor.setCaretPosition(arduinoMonitor.getDocument().getLength());
		return token;
	}

	/**
	 * Sends One command where command result is not expected (dummy command)
	 * 
	 * Timeout is not implemented
	 * 
	 * @param bytes
	 * @param timeout
	 * @return received string
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public String sendOneShotCommand(String bytes, int timeout)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		if (!portconnected) {
			JOptionPane.showMessageDialog(null, "Cannot sent a command - Port is not connected", "Connection Error",
					JOptionPane.ERROR_MESSAGE);
			throw new InterruptedException();
		}

		String str;

		sendCommand(bytes);
		str = nextLineTimeout(timeout);
		arduinoMonitor.setCaretPosition(arduinoMonitor.getDocument().getLength());

		if (!str.equals("#OK#")) {
			JOptionPane.showMessageDialog(null, "Unexpected command result", "Connection Error",
					JOptionPane.ERROR_MESSAGE);
		}

		return str;
	}
}
