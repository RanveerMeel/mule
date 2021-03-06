/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.grizzly;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.module.http.internal.listener.ServerAddressMap;
import org.mule.runtime.module.http.internal.listener.ServerAddress;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * {@link org.mule.runtime.module.http.internal.listener.grizzly.ExecutorProvider} implementation that retrieves an
 * {@link java.util.concurrent.Executor} from a {@link org.mule.runtime.core.api.context.WorkManagerSource}
 */
public class WorkManagerSourceExecutorProvider implements ExecutorProvider {

  private ServerAddressMap<WorkManagerSource> executorPerServerAddress =
      new ServerAddressMap<>(new ConcurrentHashMap<ServerAddress, WorkManagerSource>());

  /**
   * Adds an {@link java.util.concurrent.Executor} to be used when a request is made to a
   * {@link org.mule.runtime.module.http.internal.listener.ServerAddress}
   *
   * @param serverAddress address to which the executor should be applied to
   * @param workManagerSource the executor to use when a request is done to the server address
   */
  public void addExecutor(final ServerAddress serverAddress, final WorkManagerSource workManagerSource) {
    executorPerServerAddress.put(serverAddress, workManagerSource);
  }

  @Override
  public Executor getExecutor(ServerAddress serverAddress) {
    try {
      return executorPerServerAddress.get(serverAddress).getWorkManager();
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
