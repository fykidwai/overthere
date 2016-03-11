/*
 * Copyright (C)2016 - Jeroen van Erp <jeroen@hierynomus.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xebialabs.overthere.proxy;

import com.hierynomus.sshj.backport.JavaVersion;
import com.hierynomus.sshj.backport.Jdk7HttpProxySocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.*;

/**
 * Copied from https://github.com/hierynomus/smbj.git
 */
public class ProxySocketFactory extends SocketFactory {
    private static final Logger logger = LoggerFactory.getLogger(ProxySocketFactory.class);
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    private Proxy proxy;
    private int connectTimeout;

    public ProxySocketFactory() {
        this(Proxy.NO_PROXY, DEFAULT_CONNECT_TIMEOUT);
    }

    public ProxySocketFactory(String proxyAddress, int proxyPort) {
        this(getHttpProxy(proxyAddress, proxyPort), DEFAULT_CONNECT_TIMEOUT);
    }

    public ProxySocketFactory(Proxy proxy) {
        this(proxy, DEFAULT_CONNECT_TIMEOUT);
    }

    public ProxySocketFactory(int connectTimeout) {
        this(Proxy.NO_PROXY, connectTimeout);
    }

    public ProxySocketFactory(Proxy proxy, int connectTimeout) {
        this.proxy = proxy;
        this.connectTimeout = connectTimeout;
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket socket;
        // Special case, pre-java8 HTTP proxies were not supported by Socket
        if (JavaVersion.isJava7OrEarlier() && proxy.type() == Proxy.Type.HTTP) {
            logger.trace("Using {} proxy {} (Java 7 mode)", proxy.type(), proxy.address());
            socket = new Jdk7HttpProxySocket(proxy);
        } else {
            logger.trace("Using {} proxy {}", proxy.type(), proxy.address());
            socket = new Socket(proxy);
        }
        return socket;
    }

    @Override
    public Socket createSocket(String address, int port) throws IOException, UnknownHostException {
        return createSocket(new InetSocketAddress(address, port), null);
    }

    @Override
    public Socket createSocket(String address, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        return createSocket(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort));
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(new InetSocketAddress(address, port), null);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort));
    }

    private Socket createSocket(InetSocketAddress address, InetSocketAddress bindAddress) throws IOException {
        Socket socket = createSocket();

        if (bindAddress != null) {
            logger.trace("Using bind address to {}", bindAddress);
            socket.bind(bindAddress);
        }

        logger.trace("Connecting to {}", address);
        socket.connect(address, connectTimeout);
        return socket;
    }

    private static Proxy getHttpProxy(String proxyAddress, int proxyPort) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort));
    }
}