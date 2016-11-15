/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static java.util.Collections.emptyList;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.internal.introspection.describer.XmlBasedDescriber;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConfigurationBuilder} that registers a {@link ExtensionManager}
 *
 * @since 4.0
 */
public class ApplicationExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  private static Logger LOGGER = LoggerFactory.getLogger(ApplicationExtensionsManagerConfigurationBuilder.class);

  private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
  private final List<ArtifactPlugin> artifactPlugins;

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins) {
    this(artifactPlugins, new DefaultExtensionManagerAdapterFactory());
  }

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                          ExtensionManagerAdapterFactory extensionManagerAdapterFactory) {
    this.artifactPlugins = artifactPlugins;
    this.extensionManagerAdapterFactory = extensionManagerAdapterFactory;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

    for (ArtifactPlugin artifactPlugin : artifactPlugins) {
      URL manifestUrl =
          artifactPlugin.getArtifactClassLoader().findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
      if (manifestUrl == null) {
        checkIfSmartExtensionApplies(artifactPlugin, extensionManager);
        continue;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Discovered extension " + artifactPlugin.getArtifactName());
      }
      ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
      extensionManager.registerExtension(extensionManifest, artifactPlugin.getArtifactClassLoader().getClassLoader());
    }
  }

  private void checkIfSmartExtensionApplies(ArtifactPlugin artifactPlugin, ExtensionManagerAdapter extensionManager)
      throws IOException {
    // TODO(fernandezlautaro): MULE-10866 clean this before PR
    String modulePath = "module-bye.xml";


    ArtifactClassLoader artifactClassLoader = artifactPlugin.getArtifactClassLoader();
    ClassLoader classLoader = artifactClassLoader.getClassLoader();

    DescribingContext context = new DefaultDescribingContext(classLoader);
    XmlBasedDescriber describer = new XmlBasedDescriber(modulePath);
    //ExtensionDeclarer extensionDeclarer = describer.describe(context);

    DefaultExtensionFactory defaultExtensionFactory = new DefaultExtensionFactory(emptyList(), emptyList());



    ExtensionModel extensionModel =
        withContextClassLoader(classLoader, () -> defaultExtensionFactory.createFrom(describer.describe(context), context));
    //// TODO(fernandezlautaro): MULE-10866 clean this before PR
    ////ClassUtils.withContextClassLoader(classLoader, )
    //URL resource = artifactClassLoader.findResource("classes/module-bye.xml");
    //resource.getFile();

    extensionManager.registerExtension(extensionModel);

  }

  private ExtensionManagerAdapter createExtensionManager(MuleContext muleContext) throws InitialisationException {
    try {
      return extensionManagerAdapterFactory.createExtensionManager(muleContext);
    } catch (Exception e) {
      throw new InitialisationException(e, muleContext);
    }
  }
}
