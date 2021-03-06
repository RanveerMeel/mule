/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationReturnTypeModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  private OperationReturnTypeModelValidator validator = new OperationReturnTypeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(String.class), false, emptySet()));
    when(operationModel.getName()).thenReturn("operation");
  }

  @Test
  public void valid() {
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void muleEventReturnType() {
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(MuleEvent.class), false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void muleMessageReturnType() {
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(Message.class), false, emptySet()));
    validator.validate(extensionModel);
  }
}
