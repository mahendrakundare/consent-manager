package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.user.model.CreatePinRequest;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.consentmanager.user.model.ValidatePinRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_REQUESTER;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
public class PatientsController {
    private final ProfileService profileService;
    private final TransactionPinService transactionPinService;
    private final SignUpService signupService;
    private final UserService userService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/pin")
    public Mono<Void> pin(@RequestBody CreatePinRequest createPinRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(userName -> transactionPinService.createPinFor(userName, createPinRequest.getPin()));
    }

    @GetMapping("/me")
    public Mono<Profile> profileFor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(profileService::profileFor);
    }

    @PostMapping("/verify-pin")
    public Mono<Token> validatePin(@RequestBody ValidatePinRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(userName -> transactionPinService.validatePinFor(userName, request.getPin()));
    }

    @PostMapping("/profile")
    public Mono<Session> create(@RequestBody SignUpRequest request,
                                @RequestHeader(name = "Authorization") String token) {
        var signUpRequests = SignUpRequestValidator.validate(request, userService.getUserIdSuffix());
        return signUpRequests.isValid()
                ? Mono.justOrEmpty(signupService.sessionFrom(token))
                .flatMap(sessionId -> userService.create(signUpRequests.get(), sessionId)
                        .zipWith(Mono.just(sessionId))
                        .flatMap(tuple -> signupService.removeOf(tuple.getT2()).thenReturn(tuple.getT1())))
                : Mono.error(new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_REQUESTER,
                        signUpRequests.getError().reduce((left, right) -> format("%s, %s", left, right))))));
    }

    @PostMapping("/generateotp")
    @ResponseStatus(CREATED)
    public Mono<SignUpSession> generateOtp(@RequestBody GenerateOtpRequest request) {
        System.out.println("Inside generate otp");
        return profileService.profileFor(request.getUsername())
                .switchIfEmpty(Mono.error(ClientError.userNotFound()))
                .flatMap(this::getVerfiedIdentifier)
                .flatMap(userService::sendOtp);
    }

    private Mono<UserSignUpEnquiry> getVerfiedIdentifier(Profile profile) {
        if (profile.getVerifiedIdentifiers() != null && !profile.getVerifiedIdentifiers().isEmpty()) {
            Identifier identifier = profile.getVerifiedIdentifiers().get(0);
            return Mono.just(UserSignUpEnquiry.builder()
                    .identifier(identifier.getValue())
                    .identifierType(identifier.getType().toString())
                    .build());
        }

        return Mono.error(ClientError.unAuthorized());
    }

    @PostMapping("/verifyotp")
    public Mono<Token> verifyOtp(@RequestBody OtpVerification request) {
        return userService.permitOtp(request);
    }

    //TODO: Veena: PUT method to update user profile password
    @PutMapping("/profile/update")
    public Mono<Session> update(@RequestBody UpdateUserRequest request,
                                @RequestHeader(name = "Authorization") String token) {
        var updateUserRequests = UpdateUserRequestValidator.validate(request, userService.getUserIdSuffix());
        return updateUserRequests.isValid()
                ? Mono.justOrEmpty(signupService.sessionFrom(token))
                .flatMap(sessionId -> userService.update(updateUserRequests.get(), sessionId)
                        .zipWith(Mono.just(sessionId))
                        .flatMap(tuple -> signupService.removeOf(tuple.getT2()).thenReturn(tuple.getT1())))
                : Mono.error(new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_REQUESTER,
                        updateUserRequests.getError().reduce((left, right) -> format("%s, %s", left, right))))));
    }
}
