package plugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "plugin"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/**
	 * Log the status.
	 *
	 * @param status the status
	 */
	public static void log(IStatus status) {
		Activator.getDefault().getLog().log(status);
	}

	/**
	 * Log the message.
	 *
	 * @param severity the severity
	 * @param message the message
	 */
	public static void log(int severity, String message) {
		log(new Status(severity, PLUGIN_ID, message));
	}	
	
	/**
	 * Log the exception.
	 *
	 * @param throwable the throwable
	 */
	public static void log(Throwable throwable) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, "An exception occured", throwable)); //$NON-NLS-1$ 
	}	
	
	/**
	 * Log the exception.
	 *
	 * @param message the message
	 * @param throwable the throwable
	 */
	public static void log(String message, Throwable throwable) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, message, throwable)); //$NON-NLS-1$ 
	}	
	
	/**
	 * Debug.
	 *
	 * @param message the message
	 */
	public static void debug(String message) {
		message = "==========="+message+"==============";
		log(new Status(IStatus.INFO, PLUGIN_ID, message));
	}

	public static String getPluginID() {
		return PLUGIN_ID;
	}

}
