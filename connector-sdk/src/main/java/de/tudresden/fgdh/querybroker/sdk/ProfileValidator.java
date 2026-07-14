package de.tudresden.fgdh.querybroker.sdk;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Port for validating a handler result resource against a FHIR profile before
 * it is sent (see CONTRIBUTING § 2: "the stub validates before sending").
 *
 * <p>Kept deliberately HAPI-validation-free at the API surface (returns plain
 * error strings) so a connector can supply any implementation. The SDK ships
 * {@link CatalogProfileValidator}, backed by the {@code catalog/} mirror of the
 * ImplementationGuide.
 */
@FunctionalInterface
public interface ProfileValidator {

  /**
   * Validates {@code resource} against the profile identified by
   * {@code profileUrl}.
   *
   * @return the validation error/fatal messages; an empty list means valid.
   */
  List<String> validate(IBaseResource resource, String profileUrl);
}
