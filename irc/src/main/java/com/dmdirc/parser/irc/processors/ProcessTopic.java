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

import com.dmdirc.parser.events.ChannelTopicEvent;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;

import java.time.LocalDateTime;

import javax.inject.Inject;

/**
 * Process a topic change.
 */
public class ProcessTopic extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     */
    @Inject
    public ProcessTopic(final IRCParser parser) {
        super(parser, "TOPIC", "332", "333");
    }

    /**
     * Process a topic change.
     *
     * @param date The LocalDateTime that this event occurred at.
     * @param sParam Type of line to process ("TOPIC", "332", "333")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final LocalDateTime date, final String sParam, final String... token) {
        final IRCChannelInfo iChannel;
        switch (sParam) {
            case "332":
                iChannel = getChannel(token[3]);
                if (iChannel == null) {
                    return;
                }   iChannel.setInternalTopic(token[token.length - 1]);
                break;
            case "333":
                if (token.length > 3) {
                    iChannel = getChannel(token[3]);
                    if (iChannel == null) {
                        return;
                    }
                    if (token.length > 4) {
                        iChannel.setTopicUser(token[4]);
                        if (token.length > 5) {
                            iChannel.setTopicTime(Long.parseLong(token[5]));
                        }
                    }
                    callChannelTopic(date, iChannel, true);
                }   break;
            default:
                if (IRCParser.ALWAYS_UPDATECLIENT) {
                    final IRCClientInfo iClient = getClientInfo(token[0]);
                    if (iClient != null && iClient.getHostname().isEmpty()) {
                        iClient.setUserBits(token[0], false);
                    }
                }
                iChannel = getChannel(token[2]);
                if (iChannel == null) {
                    return;
                }
                iChannel.setTopicTime(System.currentTimeMillis() / 1000);
                iChannel.setTopicUser(token[0].charAt(0) == ':' ? token[0].substring(1) : token[0]);
                iChannel.setInternalTopic(token[token.length - 1]);
                callChannelTopic(date, iChannel, false);
                break;
        }
    }

    /**
     * Callback to all objects implementing the ChannelTopic Callback.
     *
     * @param date The LocalDateTime that this event occurred at.
     * @param cChannel Channel that topic was set on
     * @param bIsJoinTopic True when getting topic on join, false if set by user/server
     */
    protected void callChannelTopic(final LocalDateTime date, final ChannelInfo cChannel, final boolean bIsJoinTopic) {
        ((IRCChannelInfo) cChannel).setHadTopic();
        getCallbackManager().publish(
                new ChannelTopicEvent(parser, date, cChannel, bIsJoinTopic));
    }

}
