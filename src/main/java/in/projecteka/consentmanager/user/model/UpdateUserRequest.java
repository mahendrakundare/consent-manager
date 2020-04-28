package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class UpdateUserRequest {
    private final String password;
    private final String username;
}
