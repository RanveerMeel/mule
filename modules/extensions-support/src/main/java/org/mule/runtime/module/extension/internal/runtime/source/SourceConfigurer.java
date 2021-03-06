/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Resolves and injects the values of a {@link Source} that has fields annotated with {@link Parameter} or
 * {@link ParameterGroup}
 *
 * @since 4.0
 */
public final class SourceConfigurer {

  private final SourceModel model;
  private final ResolverSet resolverSet;
  private final MuleContext muleContext;

  /**
   * Create a new instance
   *
   * @param model the {@link SourceModel} which describes the instances that the {@link #configure(Source)} method will accept
   * @param resolverSet the {@link ResolverSet} used to resolve the parameters
   * @param muleContext the current {@link MuleContext}
   */
  public SourceConfigurer(SourceModel model, ResolverSet resolverSet, MuleContext muleContext) {
    this.model = model;
    this.resolverSet = resolverSet;
    this.muleContext = muleContext;
  }

  /**
   * Performs the configuration of the given {@code source} and returns the result
   *
   * @param source a {@link Source}
   * @return the configured instance
   * @throws MuleException
   */
  public Source configure(Source source) throws MuleException {
    ResolverSetBasedObjectBuilder<Source> builder =
        new ResolverSetBasedObjectBuilder<Source>(source.getClass(), model, resolverSet) {

          @Override
          protected Source instantiateObject() {
            return source;
          }
        };

    try {
      return builder.build(getInitialiserEvent(muleContext));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception was found trying to configure source of type "
          + source.getClass().getName()), e);
    }
  }

}
