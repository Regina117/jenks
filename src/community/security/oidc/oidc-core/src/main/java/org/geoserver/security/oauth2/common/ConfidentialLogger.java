/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Used to log confidential information, if enabled.
 *
 * @author awaterme
 */
public class ConfidentialLogger {

    private static Logger LOGGER = Logging.getLogger(ConfidentialLogger.class);
    private static boolean enabled = false;

    public static void log(Level level, String msg, Object params[]) {
        if (!enabled) {
            return;
        }
        LOGGER.log(level, msg, params);
    }

    public static boolean isLoggable(Level level) {
        return enabled && LOGGER.isLoggable(level);
    }

    /** @param pEnabled the enabled to set */
    public static void setEnabled(boolean pEnabled) {
        enabled = pEnabled;
    }
}
