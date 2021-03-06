/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TcpJmsResponseTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");


  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/tcp-jms-response-flow.xml";
  }

  @Test
  public void testSyncResponse() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message =
        client.send("tcp://localhost:" + httpPort.getValue(), InternalMessage.builder().payload("request").build()).getRight();
    assertNotNull(message);
    assertEquals("Received: request", getPayloadAsString(message));
  }
}
