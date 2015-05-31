package org.openhab.binding.customzigbee.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomZigBeeActivator implements BundleActivator {

	private static Logger logger = LoggerFactory.getLogger(CustomZigBeeActivator.class); 
	
	private static BundleContext context;
	
	/**
	 * Called whenever the OSGi framework starts our bundle
	 */
	@Override
	public void start(BundleContext bc) throws Exception {
		context = bc;
		logger.debug("CustomZigBee binding has been started.");
	}

	/**
	 * Called whenever the OSGi framework stops our bundle
	 */
	@Override
	public void stop(BundleContext bc) throws Exception {
		context = null;
		logger.debug("CustomZigBee binding has been stopped.");
	}
	
	/**
     * Returns the bundle context of this bundle
     * @return the bundle context
     */
    public static BundleContext getContext() {
        return context;
    }
}
