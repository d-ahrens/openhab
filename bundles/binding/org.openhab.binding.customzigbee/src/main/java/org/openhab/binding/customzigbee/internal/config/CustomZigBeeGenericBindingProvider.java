/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.customzigbee.internal.config;

import org.openhab.binding.customzigbee.CustomZigBeeBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author d-ahrens
 * @since 1.7.0
 */
public class CustomZigBeeGenericBindingProvider extends AbstractGenericBindingProvider implements CustomZigBeeBindingProvider {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomZigBeeGenericBindingProvider.class);
    //private Map<String, Item> items = new HashMap<String, Item>();

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "customzigbee";
	}
	
	@Override
    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only SwitchItems are allowed - please check your *.items configuration");
		}
    }

    @Override
    public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
    	super.processBindingConfiguration(context, item, bindingConfig);
    	
		CustomZigBeeBindingConfig config = new CustomZigBeeBindingConfig();
		
		// TODO: check that every LED is only instantiated once
		if(!bindingConfig.startsWith("LED"))
			throw new BindingConfigParseException("An CustomZigBee Entry has to be formatted like 'LEDx'! Please check your items-list!");
		
		String led_number = bindingConfig.substring(3);
		
		config.led = Integer.parseInt(led_number);
		
		logger.debug("Adding LED " + led_number + " to CustomZigbeeBinding ...");
		
		addBindingConfig(item, config);	
    }

//    @Override
//    protected void addBindingConfig(Item item, BindingConfig config) {
//        items.put(item.getName(), item);
//        super.addBindingConfig(item, config);
//    }

//    @Override
//    public void removeConfigurations(String context) {
//        Set<Item> configuredItems = contextMap.get(context);
//        if (configuredItems != null) {
//            for (Item item : configuredItems) {
//                items.remove(item.getName());
//            }
//        }
//        super.removeConfigurations(context);
//    }

	@Override
	public int getLED(String itemName) {
		CustomZigBeeBindingConfig config = (CustomZigBeeBindingConfig) bindingConfigs.get(itemName);
        return config != null ? config.led : null;
	}
	
	
	/**
	 * This is a helper class holding binding specific configuration details
	 * 
	 * @author d-ahrens
	 * @since 1.7.0
	 */
	class CustomZigBeeBindingConfig implements BindingConfig {
		// TODO: map led => room, e.g. 1 => livingroom
		public int led;
	}
	
	
}
