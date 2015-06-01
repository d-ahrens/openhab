package org.openhab.binding.customzigbee;

import org.openhab.core.binding.BindingProvider;

/**
 * @author d-ahrens
 * @since 1.7.0
 */
public interface CustomZigBeeBindingProvider extends BindingProvider {
	int getLED(String itemName);
}
