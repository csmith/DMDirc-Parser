/*
 * Copyright (c) 2006-2013 DMDirc Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dmdirc.parser.irc;

import com.dmdirc.harness.parser.TestParser;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChannelClientInfoTest {
        
    @Test
    public void testImportantMode() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");

        final IRCChannelClientInfo cci = parser.getClient("luser").getChannelClients().get(0);

        assertEquals("v", cci.getImportantMode());
        assertEquals("+", cci.getImportantModePrefix());
        assertEquals("+luser", cci.toString());
        assertEquals("+luser", cci.toFullString());
        final long value = cci.getImportantModeValue();

        parser.injectLine(":server MODE #DMDirc_testing +o luser");
        assertEquals("o", cci.getImportantMode());
        assertEquals("@", cci.getImportantModePrefix());
        assertEquals("@luser", cci.toString());
        assertEquals("@+luser", cci.toFullString());
        assertTrue(cci.getImportantModeValue() > value);

        parser.injectLine(":server MODE #DMDirc_testing -ov luser luser");
        assertEquals("", cci.getImportantMode());
        assertEquals("", cci.getImportantModePrefix());
        assertEquals("luser", cci.toString());
        assertEquals("luser", cci.toFullString());
        assertEquals(0L, cci.getImportantModeValue());
    }
    
    @Test
    public void testMap() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        final Map<Object, Object> map1 = new HashMap<>();
        final Map<Object, Object> map2 = new HashMap<>();
        
        final IRCChannelClientInfo cci = parser.getClient("nick").getChannelClients().get(0);
        
        cci.setMap(map1);
        assertSame(map1, cci.getMap());
        
        cci.setMap(map2);
        assertSame(map2, cci.getMap());
    }
    
    @Test
    public void testKick() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.sentLines.clear();
        
        parser.getClient("nick").getChannelClients().get(0).kick("");
        assertEquals(1, parser.sentLines.size());
        assertEquals("KICK #DMDirc_testing nick", parser.sentLines.get(0));
        parser.sentLines.clear();
        
        parser.getClient("nick").getChannelClients().get(0).kick("booya");
        assertEquals(1, parser.sentLines.size());
        assertEquals("KICK #DMDirc_testing nick :booya", parser.sentLines.get(0));
    }

}
