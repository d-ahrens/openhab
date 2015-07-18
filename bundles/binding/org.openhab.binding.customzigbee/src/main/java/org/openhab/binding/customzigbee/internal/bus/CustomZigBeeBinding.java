/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.customzigbee.internal.bus;

import gnu.io.NoSuchPortException;

import java.util.Dictionary;

import org.openhab.binding.customzigbee.CustomZigBeeBindingProvider;
import org.openhab.binding.customzigbee.internal.InitializationException;
import org.openhab.binding.customzigbee.internal.SerialDevice;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class implements a binding of serial devices to openHAB.
 * The binding configurations are provided by the {@link GenericItemProvider}.</p>
 * 
 * <p>The format of the binding configuration is simple and looks like this:</p>
 * serial="&lt;port&gt;" where &lt;port&gt; is the identification of the serial port on the host system, e.g.
 * "COM1" on Windows, "/dev/ttyS0" on Linux or "/dev/tty.PL2303-0000103D" on Mac
 * <p>Switch items with this binding will receive an ON-OFF update on the bus, whenever data becomes available on the serial interface<br/>
 * String items will receive the submitted data in form of a string value as a status update, while openHAB commands to a Switch item is
 * sent out as data through the serial interface.</p>
 * 
 * @author Kai Kreuzer
 *
 */
public class CustomZigBeeBinding extends AbstractBinding<CustomZigBeeBindingProvider> implements ManagedService {
	
	private static final Logger logger = LoggerFactory.getLogger(CustomZigBeeBinding.class);

    private static final String CONFIG_KEY_SERIAL_PORT = "serialPort";
    
    private static final String CONFIG_KEY_BAUDRATE = "baudrate";
	
	// TODO: update class to AbstractBinding, so that we can get information about serial port and baudrate from config

	//private Map<String, SerialDevice> serialDevices = new HashMap<String, SerialDevice>();

	/** stores information about the which items are associated to which port. The map has this content structure: itemname -> port */ 
	//private Map<String, String> itemMap = new HashMap<String, String>();
	
	/** stores information about the context of items. The map has this content structure: context -> Set of itemNames */ 
	//private Map<String, Set<String>> contextMap = new HashMap<String, Set<String>>();

	private EventPublisher eventPublisher = null;
	
	private SerialDevice serialDevice;
	
	private String serialPort;
	
	private int baudrate = 38400;
	
	@Override
	public void activate() {
		if (serialDevice != null) {
            try {
            	serialDevice.initialize();
            } catch (Exception e) {
                logger.error("Could not connect to " + serialPort, e);
            }
        }
	}
	
	@Override
	public void deactivate() {
		serialDevice.close();
	}
	
	@Override
    protected void internalReceiveCommand(String itemName, Command command) {
		byte[] output = new byte[3];
		for (CustomZigBeeBindingProvider provider : providers) {
			if( provider.providesBindingFor(itemName) ) {
				if (command instanceof OnOffType) {
					OnOffType cmd = (OnOffType) command;
					//String param = cmd.equals(OnOffType.ON) ? "ON" : "OFF";
					Integer led = provider.getLED(itemName);
					
					//serialDevice.writeString(led + " " + param);
					output[0]='L';
					output[1]=led.byteValue();
					output[2]=(byte) (cmd.equals(OnOffType.ON) ? 1 : 0);
					serialDevice.writeBytes(output);
				}
			}
		}
	}
	
	protected void internalReceiveUpdate(String itemName, State newState) {
		// TODO: do we need an update of states?
	}
	
	@Override
	public void setEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		
		if(serialDevice != null)
			serialDevice.setEventPublisher(eventPublisher);
	}
	
	@Override
	public void unsetEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = null;
		
		if(serialDevice != null)
			serialDevice.setEventPublisher(null);
	}
	
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		if (config == null) {
            return;
        }
        serialPort = (String) config.get(CONFIG_KEY_SERIAL_PORT);
        
        if(config.get(CONFIG_KEY_BAUDRATE) != null) {
        	baudrate = Integer.parseInt( (String) config.get(CONFIG_KEY_BAUDRATE) );
        }

        if (serialDevice != null) {
        	serialDevice.close();
        }
        try {
            connect();
        } catch (InitializationException e) {
            if (e.getCause() instanceof NoSuchPortException) {
                throw new ConfigurationException(CONFIG_KEY_SERIAL_PORT, e.getMessage());
            } else {
                throw new RuntimeException(e);
            }
        }
	}

	@Override
	public void allBindingsChanged(BindingProvider provider) {
		// TODO: update all items registered in serialDevice
	}

	@Override
	public void bindingChanged(BindingProvider provider, String itemName) {
		// TODO: update binding changes in serialDevice
	}
	
	private void connect() throws InitializationException {
        logger.info("Connecting to ZigBee [serialPort='{}' ].", new Object[] { serialPort });

        serialDevice = new SerialDevice(serialPort, baudrate);
        serialDevice.setEventPublisher(eventPublisher);
        
        serialDevice.initialize();

        // TODO: we could init LEDs here (e.g. some LEDs have initial state ON)
    }
}
