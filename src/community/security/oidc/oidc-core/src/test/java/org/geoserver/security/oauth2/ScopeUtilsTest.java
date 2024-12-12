/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.*;

import org.geoserver.security.oauth2.login.ScopeUtils;
import org.junit.Test;

/** Tests for {@link ScopeUtils}. */
public class ScopeUtilsTest {

    @Test
    public void testValueOf() {
        assertArrayEquals(new String[] {"a"}, ScopeUtils.valueOf("a"));
        assertArrayEquals(new String[] {"a"}, ScopeUtils.valueOf(" a "));
        assertArrayEquals(new String[] {"a", "b"}, ScopeUtils.valueOf("a b"));
        assertArrayEquals(new String[] {"a", "b"}, ScopeUtils.valueOf(" a, b "));
        assertArrayEquals(new String[] {}, ScopeUtils.valueOf(" "));
        assertArrayEquals(new String[] {}, ScopeUtils.valueOf(null));
    }
}
