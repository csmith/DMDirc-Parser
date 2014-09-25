/*
 * Copyright (c) 2006-2014 DMDirc Developers
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

package com.dmdirc.parser.common;

import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.callbacks.CallbackInterface;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements common base functionality for parsers.
 * <p>
 * Implementations of this class must be annotated with {@link ChildImplementations} to define
 * the implementations to use for instances of {@link ClientInfo}, {@link ChannelInfo}, etc.
 */
public abstract class BaseParser extends ThreadedParser {

    /** The URI that this parser was constructed for. */
    private final URI uri;

    /** The ignore list for this parser. */
    private IgnoreList ignoreList;

    /** The ping timer interval for this parser. */
    private long pingTimerInterval;

    /** The ping timer fraction for this parser. */
    private int pingTimerFraction;

    /** The cached name of the server this parser is connected to. */
    private String serverName;

    /** The IP that this parser should bind to. */
    private String bindIp;

    /** The IPv6 IP that this parser should bind to. */
    private String bindIpv6;

    /** The URI of the proxy to use when connecting, if any. */
    private URI proxy;

    /** The callback manager to use for this parser. */
    private final CallbackManager callbackManager;

    /** A map for callers to use to store things for no sane reason. */
    private final Map<Object, Object> map = new HashMap<>();

    /**
     * Creates a new base parser for the specified URI.
     *
     * @param uri The URI this parser will connect to.
     */
    public BaseParser(final URI uri) {
        this.uri = uri;

        final Map<Class<?>, Class<?>> implementations = new HashMap<>();

        for (Class<?> child : getClass().getAnnotation(ChildImplementations.class).value()) {
            for (Class<?> iface : child.getInterfaces()) {
                implementations.put(iface, child);
            }
        }

        this.callbackManager = new CallbackManager(this, implementations);
    }

    @Override
    public void quit(final String reason) {
        disconnect(reason);
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public URI getProxy() {
        if (proxy == null && ProxySelector.getDefault() != null) {
            final List<Proxy> proxies = ProxySelector.getDefault().select(getURI());
            if (!proxies.isEmpty()) {
                final SocketAddress sock = proxies.get(0).address();
                if (sock instanceof InetSocketAddress) {
                    final InetSocketAddress isa = (InetSocketAddress)sock;
                    try {
                        return new URI("proxy://", "", isa.getAddress().getHostAddress(), isa.getPort(), "", "", "");
                    } catch (final URISyntaxException use) { /* Oh well... */ }
                }
            }
        } else {
            return proxy;
        }

        return null;
    }

    @Override
    public void setProxy(final URI proxy) {
        this.proxy = proxy;
    }

    @Override
    public IgnoreList getIgnoreList() {
        return ignoreList;
    }

    @Override
    public void setIgnoreList(final IgnoreList ignoreList) {
        this.ignoreList = ignoreList;
    }

    @Override
    public long getPingTimerInterval() {
        return pingTimerInterval;
    }

    @Override
    public void setPingTimerInterval(final long newValue) {
        pingTimerInterval = newValue;
    }

    @Override
    public int getPingTimerFraction() {
        return pingTimerFraction;
    }

    @Override
    public void setPingTimerFraction(final int newValue) {
        pingTimerFraction = newValue;
    }

    @Override
    public CallbackManager getCallbackManager() {
        return callbackManager;
    }

    /**
     * Gets a proxy object which can be used to despatch callbacks of the
     * specified type. Callers may pass <code>null</code> for the first two
     * arguments of any callback, and these will automatically be replaced
     * by the relevant Parser instance and the current date.
     *
     * @param <T> The type of the callback to retrieve
     * @param callback The callback to retrieve
     * @return A proxy object which can be used to call the specified callback
     */
    protected <T extends CallbackInterface> T getCallback(final Class<T> callback) {
        return callbackManager.getCallback(callback);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Object, Object> getMap() {
        return map;
    }

    @Override
    public void joinChannel(final String channel) {
        joinChannels(new ChannelJoinRequest(channel));
    }

    @Override
    public void joinChannel(final String channel, final String key) {
        joinChannels(new ChannelJoinRequest(channel, key));
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the name of this parser's server.
     *
     * @param serverName The new name for this parser's server
     */
    protected void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String getBindIP() {
        return bindIp;
    }

    @Override
    public void setBindIP(final String ip) {
        this.bindIp = ip;
    }

    @Override
    public String getBindIPv6() {
        return bindIpv6;
    }

    @Override
    public void setBindIPv6(final String ip) {
        this.bindIpv6 = ip;
    }
}