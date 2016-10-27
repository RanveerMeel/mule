/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server;


import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.NetworkUtils;
import org.mule.runtime.module.http.internal.listener.DefaultServerAddress;
import org.mule.runtime.module.http.internal.listener.HttpListenerRegistry;
import org.mule.runtime.module.http.internal.listener.HttpServerManager;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;
import org.mule.service.http.api.server.HttpServerFactory;
import org.mule.service.http.api.server.ServerAddress;
import org.mule.service.http.api.tcp.TcpServerSocketProperties;
import org.mule.services.http.impl.service.server.grizzly.GrizzlyServerManager;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Grizzly based {@link HttpServerFactory}.
 *
 * @since 4.0
 */
public class HttpListenerConnectionManager implements HttpServerFactory, Initialisable, Disposable {

  public static final String SERVER_ALREADY_EXISTS_FORMAT =
      "A server in port(%s) already exists for ip(%s) or one overlapping it (0.0.0.0).";
  private static final String LISTENER_THREAD_NAME_PREFIX = "http.listener";

  private HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
  private HttpServerManager httpServerManager;

  private AtomicBoolean initialized = new AtomicBoolean(false);

  @Override
  public void initialise() throws InitialisationException {
    if (initialized.getAndSet(true)) {
      return;
    }

    //TODO: Analyze how to allow users to configure this
    TcpServerSocketProperties tcpServerSocketProperties = new DefaultTcpServerSocketProperties();

    String threadNamePrefix = LISTENER_THREAD_NAME_PREFIX;
    try {
      httpServerManager = new GrizzlyServerManager(threadNamePrefix, httpListenerRegistry, tcpServerSocketProperties);
    } catch (IOException e) {
      throw new InitialisationException(e, this);
    }

  }

  @Override
  public synchronized void dispose() {
    httpServerManager.dispose();
  }

  @Override
  public HttpServer create(HttpServerConfiguration serverConfiguration) throws ConnectionException {
    ServerAddress serverAddress;
    String host = serverConfiguration.getHost();
    try {
      serverAddress = createServerAddress(host, serverConfiguration.getPort());
    } catch (UnknownHostException e) {
      throw new ConnectionException(String.format("Cannot resolve host %s", host), e);
    }

    TlsContextFactory tlsContextFactory = serverConfiguration.getTlsContextFactory();
    HttpServer httpServer;
    if (tlsContextFactory == null) {
      httpServer = createServer(serverAddress,
                                serverConfiguration.isUsePersistentConnections(), serverConfiguration.getConnectionIdleTimeout());
    } else {
      httpServer = createSslServer(serverAddress, tlsContextFactory,
                                   serverConfiguration.isUsePersistentConnections(),
                                   serverConfiguration.getConnectionIdleTimeout());
    }

    return httpServer;
  }

  public HttpServer createServer(ServerAddress serverAddress,
                                 boolean usePersistentConnections,
                                 int connectionIdleTimeout) {
    if (!containsServerFor(serverAddress)) {
      try {
        return httpServerManager.createServerFor(serverAddress, usePersistentConnections,
                                                 connectionIdleTimeout);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage(String.format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp())));
    }
  }

  public boolean containsServerFor(ServerAddress serverAddress) {
    return httpServerManager.containsServerFor(serverAddress);
  }

  public HttpServer createSslServer(ServerAddress serverAddress, TlsContextFactory tlsContext,
                                    boolean usePersistentConnections, int connectionIdleTimeout) {
    if (!containsServerFor(serverAddress)) {
      try {
        return httpServerManager.createSslServerFor(tlsContext, serverAddress, usePersistentConnections,
                                                    connectionIdleTimeout);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage(String.format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp())));
    }
  }

  /**
   * Creates the server address object with the IP and port that a server should bind to.
   */
  private ServerAddress createServerAddress(String host, int port) throws UnknownHostException {
    return new DefaultServerAddress(NetworkUtils.getLocalHostIp(host), port);
  }

  private class DefaultTcpServerSocketProperties implements TcpServerSocketProperties {

    @Override
    public Integer getSendBufferSize() {
      return null;
    }

    @Override
    public Integer getReceiveBufferSize() {
      return null;
    }

    @Override
    public Integer getClientTimeout() {
      return null;
    }

    @Override
    public Boolean getSendTcpNoDelay() {
      return true;
    }

    @Override
    public Integer getLinger() {
      return null;
    }

    @Override
    public Boolean getKeepAlive() {
      return false;
    }

    @Override
    public Boolean getReuseAddress() {
      return true;
    }

    @Override
    public Integer getReceiveBacklog() {
      return 50;
    }

    @Override
    public Integer getServerTimeout() {
      return null;
    }
  }

}
