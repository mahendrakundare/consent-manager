package in.projecteka.consentmanager.clients.model;

import in.projecteka.consentmanager.user.model.UserCredential;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class KeycloakUser {
    private String id;
    private String firstName;
    private String username;
    private List<UserCredential> credentials;
    private String enabled;

    public <T> KeycloakUser(String name, String username, List<UserCredential> credentials, String toString) {
        this.firstName = name;
        this.username = username;
        this.credentials = credentials;
        this.enabled = toString;
    }
}
