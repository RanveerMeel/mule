/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.introspection.describer;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.model.ComponentModelReader;
import org.mule.runtime.config.spring.dsl.model.extension.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.dsl.model.extension.OperationComponentModelModelProperty;
import org.mule.runtime.config.spring.dsl.model.extension.XmlExtensionModelProperty;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.manifest.DescriberManifest;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

/**
 * Represents a TODO-REMOVE_OR_COMPLETE
 * // TODO(fernandezlautaro): MULE-10866 clean this before PR
 * @since 4.0
 */
public class XmlBasedDescriber implements Describer {

  /**
   * The ID which represents {@code this} {@link Describer} in a {@link DescriberManifest}
   */
  public static final String DESCRIBER_ID = "xml-based";

  public static final String PARAMETER_NAME = "name";
  public static final String PARAMETER_DEFAULT_VALUE = "defaultValue";
  public static final String TYPE_ATTRIBUTE = "type";

  public static final String MODULE_NAME = "name";
  public static final String MODULE_NAMESPACE_ATTRIBUTE = "namespace";

  public static final String MODULE_NAMESPACE_NAME = "module";
  public static final String CONFIG_NAME = "config";

  public static final ComponentIdentifier OPERATION_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("operation").build();
  public static final ComponentIdentifier OPERATION_PROPERTY_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("property").build();
  public static final ComponentIdentifier OPERATION_PARAMETERS_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("parameters").build();
  public static final ComponentIdentifier OPERATION_PARAMETER_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("parameter").build();
  public static final ComponentIdentifier OPERATION_BODY_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("body").build();
  public static final ComponentIdentifier OPERATION_OUTPUT_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("output").build();
  public static final ComponentIdentifier MODULE_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(MODULE_NAMESPACE_NAME).withName(MODULE_NAMESPACE_NAME)
          .build();

  private final String modulePath;

  public XmlBasedDescriber(String modulePath) {
    this.modulePath = modulePath;
  }

  @Override
  public ExtensionDeclarer describe(DescribingContext context) {
    //URL resource = getClass().getClassLoader().getResource(modulePath);

    // We will assume the current class loader will be the one defined for the plugin (which is not filtered and will allow us to access any resource in it
    URL resource = Thread.currentThread().getContextClassLoader().getResource(modulePath);

    // TODO(fernandezlautaro): MULE-10866 clean this before PR
    // TODO(fernandezlautaro): add assertion to guarantee resource is != null

    XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader = new XmlConfigurationDocumentLoader();

    Document moduleDocument = getModuleDocument(xmlConfigurationDocumentLoader, resource);
    XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(new SpiServiceRegistry());
    Optional<ConfigLine> parseModule = xmlApplicationParser.parse(moduleDocument.getDocumentElement());
    if (!parseModule.isPresent()) {
      //this happens in org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser.configLineFromElement()
      throw new IllegalArgumentException(format("There was an issue trying to read the stream of '%s'", resource.getFile()));
    }
    //no support for properties in modules for now.
    Properties properties = new Properties();
    ComponentModelReader componentModelReader = new ComponentModelReader(properties);
    ComponentModel componentModel =
        componentModelReader.extractComponentDefinitionModel(parseModule.get(), resource.getFile());


    ExtensionDeclarer declarer = context.getExtensionDeclarer();
    loadModuleExtension(declarer, componentModel);
    return declarer;
  }

  private Document getModuleDocument(XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader, URL resource) {
    try {
      return xmlConfigurationDocumentLoader.loadDocument(empty(), resource.openStream());
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("There was an issue reading the stream for the resource %s",
                                                                resource.getFile())));
    }
  }

  private void loadModuleExtension(ExtensionDeclarer declarer, ComponentModel moduleModel) {
    if (!moduleModel.getIdentifier().equals(MODULE_IDENTIFIER)) {
      throw new IllegalArgumentException(format("The root element of a module must be '%s', but found '%s'",
                                                MODULE_IDENTIFIER.toString(), moduleModel.getIdentifier().toString()));
    }
    String name = moduleModel.getParameters().get(MODULE_NAME);
    String namespace = moduleModel.getParameters().get(MODULE_NAMESPACE_ATTRIBUTE);

    String version = "4.0"; // TODO(fernandezlautaro): MULE-10866 add from version to smart extensions
    declarer.named(name)
        .describedAs("some description") // TODO(fernandezlautaro): MULE-10866 add description to smart extensions
        .fromVendor("MuleSoft") // TODO(fernandezlautaro): MULE-10866 add vendor to smart extensions
        .onVersion(version)
        .withMinMuleVersion(new MuleVersion("4.0.0")) // TODO(fernandezlautaro): MULE-10866 add minMuleVersion to smart extensions
        .withCategory(Category.COMMUNITY)
        .withXmlDsl(XmlDslModel.builder() // TODO(fernandezlautaro): MULE-10866 talk to Alejandro so that the current builder can be refactored
            .setSchemaVersion(version)
            .setNamespace(name)
            .setNamespaceUri(namespace)
            .setSchemaLocation(namespace.concat("current/").concat(name).concat(".xsd"))
            .setXsdFileName(name.concat(".xsd"))
            .build());
    declarer.withModelProperty(new XmlExtensionModelProperty());
    loadPropertiesFrom(declarer, moduleModel);
    loadOperationsFrom(declarer, moduleModel);
  }

  private List<ComponentModel> extractGlobalElementsFrom(ComponentModel moduleModel) {
    return moduleModel.getInnerComponents().stream()
        .filter(child -> !child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER)
            && !child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .collect(Collectors.toList());
  }

  private void loadPropertiesFrom(ExtensionDeclarer declarer, ComponentModel moduleModel) {

    List<ComponentModel> globalElementsComponentModel = extractGlobalElementsFrom(moduleModel);

    List<ComponentModel> properties = moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER))
        .collect(Collectors.toList());

    if (!properties.isEmpty() || !globalElementsComponentModel.isEmpty()) {
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(CONFIG_NAME);
      configurationDeclarer.withModelProperty(new GlobalElementComponentModelModelProperty(globalElementsComponentModel));

      properties.stream().forEach(param -> extractParameter(configurationDeclarer, param));
    }
  }

  private void loadOperationsFrom(ExtensionDeclarer declarer, ComponentModel moduleModel) {
    moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .forEach(operationModel -> extractOperationExtension(declarer, operationModel));
  }

  private void extractOperationExtension(ExtensionDeclarer declarer, ComponentModel operationModel) {

    String operationName = operationModel.getNameAttribute();
    OperationDeclarer operationDeclarer = declarer.withOperation(operationName);
    ComponentModel bodyComponentModel = operationModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_BODY_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("The operation '%s' is missing the <body> statement",
                                                               operationName)));

    operationDeclarer.withModelProperty(new OperationComponentModelModelProperty(bodyComponentModel));

    extractOperationParameters(operationDeclarer, operationModel);
    extractOutputType(operationDeclarer, operationModel);
  }

  private void extractOperationParameters(OperationDeclarer operationDeclarer, ComponentModel componentModel) {
    Optional<ComponentModel> optionalParametersComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETERS_IDENTIFIER)).findAny();
    if (optionalParametersComponentModel.isPresent()) {
      optionalParametersComponentModel.get().getInnerComponents()
          .stream()
          .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETER_IDENTIFIER))
          .forEach(param -> extractParameter(operationDeclarer, param));
    }
  }

  private void extractParameter(ParameterizedDeclarer parameterizedDeclarer, ComponentModel param) {
    Map<String, String> parameters = param.getParameters();
    String parameterName = parameters.get(PARAMETER_NAME);
    String parameterDefaultValue = parameters.get(PARAMETER_DEFAULT_VALUE);
    MetadataType parameterType = extractParameterType(parameters.get(TYPE_ATTRIBUTE));


    ParameterDeclarer parameterDeclarer =
        parameterDefaultValue == null ? parameterizedDeclarer.withRequiredParameter(parameterName)
            : parameterizedDeclarer.withOptionalParameter(parameterName).defaultingTo(parameterDefaultValue);
    parameterDeclarer.ofType(parameterType);
  }

  private MetadataType extractParameterType(String type) {
    Optional<MetadataType> metadataType = extractType(type);

    if (!metadataType.isPresent()) {
      throw new IllegalArgumentException(String.format(
                                                       "should not have reach here, supported types for <parameter>(simple) are string, boolean, datetime, date, number or time for now. Type obtained [%s]",
                                                       type));
    }
    return metadataType.get();
  }

  private void extractOutputType(OperationDeclarer operationDeclarer, ComponentModel componentModel) {
    ComponentModel outputComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_OUTPUT_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Having an operation without <output> is not supported"));

    String type = outputComponentModel.getParameters().get(TYPE_ATTRIBUTE);
    Optional<MetadataType> metadataType = extractType(type);

    if (!metadataType.isPresent()) {
      if ("void".equals(type)) {
        metadataType = Optional.of(BaseTypeBuilder.create(MetadataFormat.JAVA)
            .voidType().build());
      } else {
        throw new IllegalArgumentException(String.format(
                                                         "should not have reach here, supported types for <parameter>(simple) are string, boolean, datetime, date, number or time for now. Type obtained [%s]",
                                                         type));
      }
    }
    operationDeclarer.withOutput().ofType(metadataType.get());
  }

  private Optional<MetadataType> extractType(String type) {
    BaseTypeBuilder baseTypeBuilder = BaseTypeBuilder.create(MetadataFormat.JAVA);
    switch (type) {
      case "string":
        baseTypeBuilder.stringType();
        break;
      case "boolean":
        baseTypeBuilder.booleanType();
        break;
      case "datetime":
        baseTypeBuilder.dateTimeType();
        break;
      case "date":
        baseTypeBuilder.dateType();
        break;
      case "integer":
        baseTypeBuilder.numberType();
        break;
      case "time":
        baseTypeBuilder.timeType();
        break;
      default:
        return Optional.empty();
    }
    return Optional.of(baseTypeBuilder.build());
  }
}
