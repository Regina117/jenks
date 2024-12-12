/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import org.geoserver.security.validation.FilterConfigException;

public class GeoServerOAuth2FilterConfigException extends FilterConfigException {

    /** serialVersionUID */
    private static final long serialVersionUID = -3686715589371356406L;

    public GeoServerOAuth2FilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public GeoServerOAuth2FilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String OAUTH2_WKTS_URL_MALFORMED = "OAUTH2_WKTS_URL_MALFORMED";
    public static final String OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED =
            "OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED";
    public static final String OAUTH2_SCOPE_DELIMITER_MIXED = "OAUTH2_SCOPE_DELIMITER_MIXED";
}
