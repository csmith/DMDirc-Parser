/*
 * Copyright (c) 2006-2017 DMDirc Developers
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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.events.ChannelNamesEvent;
import com.dmdirc.parser.events.ChannelTopicEvent;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ModeManager;
import com.dmdirc.parser.irc.PrefixModeManager;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Process a Names reply.
 */
public class ProcessNames extends IRCProcessor {

    /** The manager to use to access prefix modes. */
    private final PrefixModeManager prefixModeManager;
    /** Mode manager to use for user modes. */
    private final ModeManager userModeManager;

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param prefixModeManager The manager to use to access prefix modes.
     * @param userModeManager Mode manager to use for user modes.
     */
    @Inject
    public ProcessNames(final IRCParser parser, final PrefixModeManager prefixModeManager,
            @Named("user") final ModeManager userModeManager) {
        super(parser, "353", "366");
        this.prefixModeManager = prefixModeManager;
        this.userModeManager = userModeManager;
    }

    /**
     * Process a Names reply.
     *
     * @param sParam Type of line to process ("366", "353")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final LocalDateTime time, final String sParam, final String... token) {
        final IRCChannelInfo iChannel;
        if ("366".equals(sParam)) {
            // End of names
            iChannel = getChannel(token[3]);
            if (iChannel == null) {
                return;
            }

            if (!iChannel.hadTopic()) {
                callChannelTopic(time, iChannel, true);
            }

            iChannel.setAddingNames(false);
            callChannelGotNames(time, iChannel);

            if (!iChannel.hasAskedForListModes()
                    && parser.getAutoListMode()) {
                iChannel.requestListModes();
            }
        } else {
            // Names

            iChannel = getChannel(token[4]);

            if (iChannel == null) {
                return;
            }

            // If we are not expecting names, clear the current known names - this is fresh stuff!
            if (!iChannel.isAddingNames()) {
                iChannel.emptyChannel();
            }
            iChannel.setAddingNames(true);

            final String[] sNames = token[token.length - 1].split(" ");
            String sName = "";
            StringBuilder sModes = new StringBuilder();
            for (String sName1 : sNames) {
                // If name is empty (ie there was an extra space) ignore it.
                if (sName1.isEmpty()) {
                    continue;
                }
                // This next bit of code allows for any ircd which decides to use @+Foo in names
                for (int i = 0; i < sName1.length(); i++) {
                    final char cMode = sName1.charAt(i);
                    if (prefixModeManager.isPrefix(cMode)) {
                        sModes.append(prefixModeManager.getModeFor(cMode));
                    } else {
                        sName = sName1.substring(i);
                        break;
                    }
                }
                callDebugInfo(IRCParser.DEBUG_INFO, "Name: %s Modes: \"%s\"", sName,
                        sModes.toString());

                IRCClientInfo iClient = getClientInfo(sName);
                if (iClient == null) {
                    iClient = new IRCClientInfo(parser, userModeManager, sName);
                    parser.addClient(iClient);
                }
                iClient.setUserBits(sName, false); // Will do nothing if this isn't UHNAMES
                final IRCChannelClientInfo iChannelClient = iChannel.addClient(iClient);
                iChannelClient.setChanMode(sModes.toString());

                sName = "";
                sModes = new StringBuilder();
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelTopic Callback.
     *
     * @param cChannel Channel that topic was set on
     * @param bIsJoinTopic True when getting topic on join, false if set by user/server
     */
    protected void callChannelTopic(final LocalDateTime time, final ChannelInfo cChannel, final boolean bIsJoinTopic) {
        ((IRCChannelInfo) cChannel).setHadTopic();
        getCallbackManager().publish(
                new ChannelTopicEvent(parser, time, cChannel, bIsJoinTopic));
    }

    /**
     * Callback to all objects implementing the ChannelGotNames Callback.
     *
     * @param cChannel Channel which the names reply is for
     */
    protected void callChannelGotNames(final LocalDateTime time, final ChannelInfo cChannel) {
        getCallbackManager().publish(new ChannelNamesEvent(parser, time, cChannel));
    }

}
