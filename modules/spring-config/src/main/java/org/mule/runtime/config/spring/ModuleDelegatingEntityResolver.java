/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.spring.dsl.model.extension.loader.ModuleExtensionStore;
import org.mule.runtime.config.spring.dsl.model.extension.schema.ModuleSchemaGenerator;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.xml.dsl.api.resources.spi.DslResourceFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Custom implementation of resolver for schemas where it will delegate in the default {@link DelegatingEntityResolver}
 * implementation for the XSDs.
 *
 * <p>If not found, it will go over the {@link ModuleExtensionStore} and see if there is any <module>s that map to
 * it, and if it does, it will generate an XSD on the fly through {@link ModuleSchemaGenerator}.
 */
public class ModuleDelegatingEntityResolver implements EntityResolver {

  private final Optional<ExtensionManager> extensionManager;
  private final EntityResolver entityResolver;

  public ModuleDelegatingEntityResolver(Optional<ExtensionManager> extensionManager) {
    this.entityResolver = new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader());
    this.extensionManager = extensionManager;
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    InputSource inputSource = entityResolver.resolveEntity(publicId, systemId);
    if (inputSource == null) {
      inputSource = generateModuleXsd(publicId, systemId);
    }
    return inputSource;
  }

  private InputSource generateModuleXsd(String publicId, String systemId) {
    InputSource inputSource = null;
    if (extensionManager.isPresent()) {
      Optional<ExtensionModel> extensionModel = extensionManager.get().getExtensions().stream()
          .filter(em -> systemId.startsWith(em.getXmlDslModel().getNamespaceUri()))
          .findAny();

      if (extensionModel.isPresent()) {
        InputStream schema = getSchema(extensionModel.get());
        inputSource = new InputSource(schema);
        inputSource.setPublicId(publicId);
        inputSource.setSystemId(systemId);
      }
    }
    return inputSource;
  }

  /**
   * Given an {@link ExtensionModel} it will generate the XSD for it.
   *
   * @param extensionModel extension to generate the schema for
   * @return the bytes that represent the schema for the {@code extensionModel}
   */
  private InputStream getSchema(ExtensionModel extensionModel) {
    // TODO(fernandezlautaro): MULE-10866 clean this before PR ask for feedback on this one (MG, PLG and Ale). It's messy to use SPI to execute all classes to then check by the extension of the file (ideally we will just call org.mule.runtime.module.extension.internal.capability.xml.schema.SchemaResourceFactory

    SpiServiceRegistry spiServiceRegistry = new SpiServiceRegistry();
    Collection<DslResourceFactory> dslResourceFactories =
        spiServiceRegistry.lookupProviders(DslResourceFactory.class, getClass().getClassLoader());

    Iterator<DslResourceFactory> iterator = dslResourceFactories.iterator();
    while (iterator.hasNext()) {
      DslResourceFactory next = iterator.next();
      Optional<GeneratedResource> generatedResource =
          next.generateResource(extensionModel, s -> extensionManager.get().getExtension(s));
      // there are several generators, we need the one that generates the XSD
      if (generatedResource.isPresent() && generatedResource.get().getPath().endsWith(".xsd")) {

        byte[] schema = generatedResource.get().getContent();
        return new ByteArrayInputStream(schema);

      }
    }
    throw new IllegalStateException(format("There were no schema generators available when trying to work with the extension '%s'",
                                           extensionModel.getName()));

  }
}
