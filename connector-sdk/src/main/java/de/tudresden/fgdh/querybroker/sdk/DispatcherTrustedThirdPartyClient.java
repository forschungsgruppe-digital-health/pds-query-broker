package de.tudresden.fgdh.querybroker.sdk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Optional;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TrustedThirdPartyClient} backed by an fTTP FHIR dispatcher — the
 * project's re-implementation of the THS Greifswald TTP-FHIR Gateway (gPAS
 * module). It resolves a site pseudonym back to the local original value by
 * invoking the gPAS {@code $dePseudonymize} operation.
 *
 * <p>Generic against the TTP-FHIR gateway contract: the same client works
 * against the real THS Greifswald gateway. The gPAS endpoint is
 * {@code {baseUrl}/ttp-fhir/fhir/gpas}; {@code targetDomain} is the gPAS
 * domain name the pseudonym belongs to.
 */
public final class DispatcherTrustedThirdPartyClient implements TrustedThirdPartyClient {

  private static final Logger log =
      LoggerFactory.getLogger(DispatcherTrustedThirdPartyClient.class);

  /** gPAS module path under the TTP-FHIR gateway base. */
  public static final String GPAS_PATH = "/ttp-fhir/fhir/gpas";

  private final IGenericClient gpas;
  private final String targetDomain;

  /**
   * @param dispatcherBaseUrl the TTP-FHIR gateway base (e.g.
   *     {@code http://ttp-dispatcher:8080})
   * @param targetDomain the gPAS domain name the pseudonyms belong to
   */
  public DispatcherTrustedThirdPartyClient(
      FhirContext fhirContext, String dispatcherBaseUrl, String targetDomain) {
    String base = dispatcherBaseUrl.endsWith("/")
        ? dispatcherBaseUrl.substring(0, dispatcherBaseUrl.length() - 1)
        : dispatcherBaseUrl;
    this.gpas = fhirContext.newRestfulGenericClient(base + GPAS_PATH);
    this.targetDomain = targetDomain;
  }

  @Override
  public Optional<String> resolveToInternalId(String pseudonym) {
    Parameters in = new Parameters();
    in.addParameter().setName("target").setValue(new StringType(targetDomain));
    in.addParameter().setName("pseudonym").setValue(new StringType(pseudonym));

    Parameters out;
    try {
      out =
          gpas.operation()
              .onServer()
              .named("dePseudonymize")
              .withParameters(in)
              .returnResourceType(Parameters.class)
              .execute();
    } catch (Exception e) {
      // Network/gateway failure is a resolution failure (surfaced by the
      // connector as a pds-error fatal response), not a crash.
      log.warn("gPAS $dePseudonymize failed for domain {}: {}", targetDomain, e.toString());
      return Optional.empty();
    }

    // Success part: name="original" with a sub-part name="original" whose
    // valueIdentifier.value is the resolved original. An unknown pseudonym
    // comes back as an "error" part instead.
    for (ParametersParameterComponent part : out.getParameter()) {
      if ("original".equals(part.getName())) {
        for (ParametersParameterComponent sub : part.getPart()) {
          if ("original".equals(sub.getName()) && sub.getValue() instanceof Identifier id) {
            return Optional.ofNullable(id.getValue());
          }
        }
      }
    }
    return Optional.empty();
  }
}
