package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class KeyCloakUserPasswordChangeRequest {
    private String type;
    private String value;
    private String temporary;
}
