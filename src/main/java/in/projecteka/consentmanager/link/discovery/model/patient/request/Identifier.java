package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class Identifier {
    String type;
    String value;
}
