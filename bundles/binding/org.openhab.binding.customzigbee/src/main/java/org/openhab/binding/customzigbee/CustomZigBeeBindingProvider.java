package org.openhab.binding.customzigbee;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

/**
 * @author d-ahrens
 * @since 1.7.0
 */
public interface CustomZigBeeBindingProvider extends BindingProvider {
	Item getItem(String itemName);
	
	int getLED(String itemName);
}
