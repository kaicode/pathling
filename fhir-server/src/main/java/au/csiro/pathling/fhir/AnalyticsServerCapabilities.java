/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhir;

import au.csiro.pathling.query.ResourceReader;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IServerConformanceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.*;
import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.CapabilityStatement.*;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a customised CapabilityStatement describing the functionality of the
 * analytics server.
 *
 * @author John Grimes
 */
public class AnalyticsServerCapabilities implements
    IServerConformanceProvider<CapabilityStatement> {

  private static final Logger logger = LoggerFactory.getLogger(AnalyticsServerCapabilities.class);

  public static final String URI_BASE = "https://server.pathling.app/fhir";
  public static final String API_VERSION = "1.0.0";
  public static final String API_MAJOR_VERSION = API_VERSION.substring(0, 1);
  public static final String CAPABILITY_URI =
      URI_BASE + "/CapabilityStatement/pathling-fhir-api-" + API_MAJOR_VERSION;
  public static final String SEARCH_URI =
      URI_BASE + "/OperationDefinition/search-" + API_MAJOR_VERSION;
  public static final String AGGREGATE_URI =
      URI_BASE + "/OperationDefinition/aggregate-" + API_MAJOR_VERSION;
  public static final String IMPORT_URI =
      URI_BASE + "/OperationDefinition/import-" + API_MAJOR_VERSION;
  public static final String FHIR_RESOURCE_BASE = "http://hl7.org/fhir/StructureDefinition/";

  private final AnalyticsServerConfiguration configuration;
  private final ResourceReader resourceReader;
  private ServerCapabilityStatementProvider delegate;
  private RestfulServer restfulServer;

  public AnalyticsServerCapabilities(AnalyticsServerConfiguration configuration,
      ResourceReader resourceReader) {
    this.configuration = configuration;
    this.resourceReader = resourceReader;
    delegate = new ServerCapabilityStatementProvider();
  }

  @Override
  @Metadata
  public CapabilityStatement getServerConformance(HttpServletRequest httpServletRequest,
      RequestDetails requestDetails) {
    logger.info("Received request for server capabilities");

    CapabilityStatement capabilityStatement = new CapabilityStatement();
    capabilityStatement
        .setUrl(CAPABILITY_URI);
    capabilityStatement.setVersion(API_VERSION);
    capabilityStatement.setTitle("Pathling FHIR API");
    capabilityStatement.setName("pathling-fhir-api");
    capabilityStatement.setStatus(PublicationStatus.ACTIVE);
    capabilityStatement.setExperimental(true);
    capabilityStatement.setPublisher("Australian e-Health Research Centre, CSIRO");
    capabilityStatement.setCopyright(
        "Dedicated to the public domain via CC0: https://creativecommons.org/publicdomain/zero/"
            + API_MAJOR_VERSION + ".0/");
    capabilityStatement.setKind(CapabilityStatementKind.INSTANCE);

    CapabilityStatementSoftwareComponent software = new CapabilityStatementSoftwareComponent(
        new StringType("Pathling FHIR Server"));
    software.setVersion(configuration.getVersion());
    capabilityStatement.setSoftware(software);

    CapabilityStatementImplementationComponent implementation = new CapabilityStatementImplementationComponent(
        new StringType("Pathling FHIR Server"));
    implementation.setUrl(getServerBase(httpServletRequest));
    capabilityStatement.setImplementation(implementation);

    capabilityStatement.setFhirVersion(FHIRVersion._4_0_0);
    capabilityStatement.setFormat(
        Arrays.asList(new CodeType("application/fhir+json"), new CodeType("application/fhir+xml")));
    capabilityStatement.setRest(buildRestComponent());

    return capabilityStatement;
  }

  @Nonnull
  private List<CapabilityStatementRestComponent> buildRestComponent() {
    List<CapabilityStatementRestComponent> rest = new ArrayList<>();
    CapabilityStatementRestComponent server = new CapabilityStatementRestComponent();
    server.setMode(RestfulCapabilityMode.SERVER);
    server.setResource(buildResources());
    server.setOperation(buildOperations());
    rest.add(server);
    return rest;
  }

  private List<CapabilityStatementRestResourceComponent> buildResources() {
    List<CapabilityStatementRestResourceComponent> resources = new ArrayList<>();
    CapabilityStatementRestResourceComponent opDefResource = null;
    Set<Enumerations.ResourceType> availableResourceTypes = EnumSet
        .copyOf(resourceReader.getAvailableResourceTypes());
    availableResourceTypes.add(Enumerations.ResourceType.OPERATIONDEFINITION);

    for (Enumerations.ResourceType resourceType : availableResourceTypes) {
      // Add the `fhirPath` search parameter to all resources.
      CapabilityStatementRestResourceComponent resource = new CapabilityStatementRestResourceComponent(
          new CodeType(resourceType.toCode()));
      resource.setProfile(FHIR_RESOURCE_BASE + resourceType.toCode());
      ResourceInteractionComponent interaction = new ResourceInteractionComponent();
      interaction.setCode(TypeRestfulInteraction.SEARCHTYPE);
      resource.getInteraction().add(interaction);
      CapabilityStatementRestResourceOperationComponent searchOperation = new CapabilityStatementRestResourceOperationComponent();
      searchOperation.setName("fhirPath");
      searchOperation.setDefinition(SEARCH_URI);
      resource.addOperation(searchOperation);
      resources.add(resource);

      // Save away the OperationDefinition resource, so that we can later add the read operation to
      // it.
      if (resourceType.toCode().equals("OperationDefinition")) {
        opDefResource = resource;
      }
    }

    // Add the read operation to the StructureDefinition and OperationDefinition resources.
    assert opDefResource != null;
    ResourceInteractionComponent readInteraction = new ResourceInteractionComponent();
    readInteraction.setCode(TypeRestfulInteraction.READ);
    opDefResource.addInteraction(readInteraction);

    return resources;
  }

  private List<CapabilityStatementRestResourceOperationComponent> buildOperations() {
    List<CapabilityStatementRestResourceOperationComponent> operations = new ArrayList<>();

    CanonicalType aggregateOperationUri = new CanonicalType(AGGREGATE_URI);
    CapabilityStatementRestResourceOperationComponent aggregateOperation = new CapabilityStatementRestResourceOperationComponent(
        new StringType("aggregate"), aggregateOperationUri);

    CanonicalType importOperationUri = new CanonicalType(IMPORT_URI);
    CapabilityStatementRestResourceOperationComponent importOperation = new CapabilityStatementRestResourceOperationComponent(
        new StringType("import"), importOperationUri);

    operations.add(aggregateOperation);
    operations.add(importOperation);
    return operations;
  }

  @Override
  public void setRestfulServer(RestfulServer restfulServer) {
    this.restfulServer = restfulServer;
  }

  private String getServerBase(HttpServletRequest httpServletRequest) {
    ServletContext servletContext = (ServletContext) (httpServletRequest == null
                                                      ? null
                                                      : httpServletRequest.getAttribute(
                                                          RestfulServer.SERVLET_CONTEXT_ATTRIBUTE));
    return restfulServer.getServerAddressStrategy()
        .determineServerBase(servletContext, httpServletRequest);
  }

}
