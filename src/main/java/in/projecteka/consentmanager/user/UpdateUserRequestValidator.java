package in.projecteka.consentmanager.user;

import com.google.common.base.Strings;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.passay.CharacterRule;
import org.passay.CharacterSequence;
import org.passay.EnglishCharacterData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;
import org.passay.SequenceData;

import java.util.Arrays;

import static java.lang.String.format;

public class UpdateUserRequestValidator {
    public static Validation<Seq<String>, UpdateUserRequest> validate(UpdateUserRequest updateUserRequest, String userIdSuffix) {
        return Validation.combine(
                validateUserName(updateUserRequest.getUsername(), userIdSuffix),
                validatePassword(updateUserRequest.getPassword()))
                .ap((username, password) -> UpdateUserRequest.builder()
                        .username(username)
                        .password(password)
                        .build());
    }

    private static Validation<String, String> validateUserName(String username, String userIdSuffix) {
        final String VALID_USERNAME_CHARS = "[a-zA-Z@0-9.\\-]";
        if (Strings.isNullOrEmpty(username)) {
            return Validation.invalid("username can't be empty");
        }
        return allowed(VALID_USERNAME_CHARS, "username", username)
                .combine(endsWithProvider(username, userIdSuffix))
                .combine(lengthLimitFor(username.replace(userIdSuffix, "")))
                .ap((validCharacters, validEndsWith, validLength) -> username)
                .mapError(errors -> errors.reduce((left, right) -> format("%s, %s", left, right)));
    }

    private static Validation<String, String> allowed(String characters, String fieldName, String value) {
        return CharSeq.of(value)
                .replaceAll(characters, "")
                .transform(seq -> seq.isEmpty()
                        ? Validation.valid(value)
                        : Validation.invalid(format("%s contains invalid characters: ' %s '",
                        fieldName,
                        seq.distinct().sorted())));
    }

    private static Validation<String, String> endsWithProvider(String username, String userIdSuffix) {
        if (!username.endsWith(userIdSuffix)) {
            return Validation.invalid("username does not end with " + userIdSuffix);
        }
        return Validation.valid(username);
    }

    private static Validation<String, String> lengthLimitFor(String username) {
        if (username.length() < 3 || username.length() > 150) {
            return Validation.invalid(format("%s should be between %d and %d characters", "username", 3, 150));
        }
        return Validation.valid(username);
    }

    private static Validation<String, String> validatePassword(String password) {
        if (Strings.isNullOrEmpty(password)) {
            return Validation.invalid("password can't be empty");
        }
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(8, 30),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new IllegalSequenceRule(new SequenceData() {
                    @Override
                    public String getErrorCode() {
                        return "cannot have three or more consecutive numbers";
                    }

                    @Override
                    public CharacterSequence[] getSequences() {
                        return new CharacterSequence[]{
                                new CharacterSequence("`1234567890-=")
                        };
                    }
                }, 3, false)));
        RuleResult result = validator.validate(new PasswordData(password));
        if (result.isValid()) {
            return Validation.valid(password);
        }
        var error = result.getDetails()
                .stream()
                .map(RuleResultDetail::toString)
                .reduce((left, right) -> format("%s, %s", left, right))
                .map(message -> format("password has following issues: %s", message))
                .orElse("password did not meet criteria");
        return Validation.invalid(error);
    }
}
