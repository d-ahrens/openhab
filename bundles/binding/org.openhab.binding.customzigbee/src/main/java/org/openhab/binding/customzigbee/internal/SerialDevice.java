/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.customzigbee.internal;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

import org.apache.commons.io.IOUtils;
import org.openhab.core.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a serial device that is linked to exactly one String item and/or Switch item.
 * 
 * @author Kai Kreuzer
 *
 */
public class SerialDevice implements SerialPortEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SerialDevice.class);

	private String port;
	private int baud = 38400;
	
	private List<String> switchItems;

	private EventPublisher eventPublisher;

	private CommPortIdentifier portId;
	private SerialPort serialPort;

	private InputStream inputStream;

	private OutputStream outputStream;

	public SerialDevice(String port) {
		this.port = port;
	}

	public SerialDevice(String port, int baud) {
		this.port = port;
		this.baud = baud;
	}

	public void setEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void unsetEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = null;
	}

	public List<String> getSwitchItems() {
		return switchItems;
	}
	
	public void removeSwitchItem(String switchItem) {
		switchItems.remove(switchItem);
	}
	
	public void clearSwitchItems() {
		switchItems.clear();
	}

	public void setSwitchItems(List<String> switchItems) {
		this.switchItems = switchItems;
	}
	
	public void addSwitchItem(String switchItem) {
		if(!switchItems.contains(switchItem))
			switchItems.add(switchItem);
	}
	
	public void addSwitchItems(List<String> switchItems) {
		for(String switchItem : switchItems) {
			addSwitchItem(switchItem);
		}
	}

	public String getPort() {
		return port;
	}

	/**
	 * Initialize this device and open the serial port
	 * 
	 * @throws InitializationException if port can not be opened
	 */
	@SuppressWarnings("rawtypes")
	public void initialize() throws InitializationException {
		// parse ports and if the default port is found, initialized the reader
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
			if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (id.getName().equals(port)) {
					logger.debug("Serial port '{}' has been found.", port);
					portId = id;
				}
			}
		}
		if (portId != null) {
			// initialize serial port
			try {
				serialPort = (SerialPort) portId.open("openHAB", 2000);
			} catch (PortInUseException e) {
				throw new InitializationException(e);
			}

			try {
				inputStream = serialPort.getInputStream();
			} catch (IOException e) {
				throw new InitializationException(e);
			}

			try {
				serialPort.addEventListener(this);
			} catch (TooManyListenersException e) {
				throw new InitializationException(e);
			}

			// activate the DATA_AVAILABLE notifier
			serialPort.notifyOnDataAvailable(true);

			try {
				// set port parameters
				serialPort.setSerialPortParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} catch (UnsupportedCommOperationException e) {
				throw new InitializationException(e);
			}

			try {
				// get the output stream
				outputStream = serialPort.getOutputStream();
			} catch (IOException e) {
				throw new InitializationException(e);
			}
		} else {
			StringBuilder sb = new StringBuilder();
			portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements()) {
				CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
				if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					sb.append(id.getName() + "\n");
				}
			}
			throw new InitializationException("Serial port '" + port + "' could not be found. Available ports are:\n" + sb.toString(), new NoSuchPortException());
		}
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			// we get here if data has been received
			StringBuilder sb = new StringBuilder();
			byte[] readBuffer = new byte[20];
			try {
				do {
					// read data from serial device
					while (inputStream.available() > 0) {
						int bytes = inputStream.read(readBuffer);
						sb.append(new String(readBuffer, 0, bytes));
					}
					try {
						// add wait states around reading the stream, so that interrupted transmissions are merged
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// ignore interruption
					}
				} while (inputStream.available() > 0);
				// sent data
				String result = sb.toString();

				// send data to the bus
				logger.debug("Received message '{}' on serial port {}", new String[] { result, port });
				
				
				// TODO: we could handle the serial message here (e.g. update LED states)
				
				
//				if (eventPublisher != null && stringItemName != null) {
//					eventPublisher.postUpdate(stringItemName, new StringType(result));
//				}
//				// if we receive empty values, we treat this to be a switch operation
//				if (eventPublisher != null && switchItemName != null && result.trim().isEmpty()) {
//					eventPublisher.postUpdate(switchItemName, OnOffType.ON);
//					eventPublisher.postUpdate(switchItemName, OnOffType.OFF);
//				}
			} catch (IOException e) {
				logger.debug("Error receiving data on serial port {}: {}", new String[] { port, e.getMessage() });
			}
			break;
		}
	}

	/**
	 * Sends a string to the serial port of this device
	 * 
	 * @param msg the string to send
	 */
	public void writeString(String msg) {
		logger.debug("Writing '{}' to serial port {}", new String[] { msg, port });
		try {
			// write string to serial port
			outputStream.write(msg.getBytes());
			outputStream.flush();
		} catch (IOException e) {
			logger.error("Error writing '{}' to serial port {}: {}", new String[] { msg, port, e.getMessage() });
		}
	}
	
	/**
	 * Sends a string to the serial port of this device
	 * 
	 * @param msg the string to send
	 */
	public void writeBytes(byte[] bytes) {
		String msg = (char)bytes[0] + "" + (char)(bytes[1]+'0') + "" + (char)(bytes[2]+'0');
		logger.debug("Writing '{}' to serial port {}", new String[] { msg, port });
		try {
			// write string to serial port
			outputStream.write(bytes);
			outputStream.flush();
		} catch (IOException e) {
			logger.error("Error writing '{}' to serial port {}: {}", new String[] { msg, port, e.getMessage() });
		}
	}

	/**
	 * Close this serial device
	 */
	public void close() {
		serialPort.removeEventListener();
		IOUtils.closeQuietly(inputStream);
		IOUtils.closeQuietly(outputStream);
		serialPort.close();
	}
}
