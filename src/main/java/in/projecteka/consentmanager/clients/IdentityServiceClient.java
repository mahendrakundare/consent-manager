package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

public class IdentityServiceClient {

    private final WebClient.Builder webClientBuilder;

    public IdentityServiceClient(WebClient.Builder webClientBuilder,
                                 IdentityServiceProperties identityServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(identityServiceProperties.getBaseUrl());
    }

    public Mono<Void> createUser(Session session, KeycloakUser request) {
        String accessToken = format("Bearer %s", session.getAccessToken());
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/admin/realms/consent-manager/users").build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), KeycloakUser.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Session> getToken(MultiValueMap<String, String> formData) {
        System.out.println("Body inserters: " + BodyInserters.fromFormData(formData));
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/token").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Session.class);
    }

    public Mono<KeyCloakUserRepresentation> getUser(String userName, String accessToken) {
        String uri = format("/admin/realms/consent-manager/users?username=%s", userName);
        System.out.println("URI after formatting: " + uri);
        return webClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder.path(format("/admin/realms/consent-manager/users?username=%s", userName)).build())
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .retrieve()
//                .onStatus(HttpStatus::isError, clientResponse -> {
//                    System.out.println("get user: " + clientResponse.statusCode());
//                    System.out.println("get user: " + clientResponse.toString());
//                    return Mono.error(ClientError.userNotFound());
//                })
                .bodyToMono(KeyCloakUserRepresentation.class)
                .cast(KeyCloakUserRepresentation.class)
                .doOnError(e -> {
                    e.printStackTrace();
                    System.out.println("GET USER: " + e.getMessage());
                });
    }

    public Mono<Void> logout(MultiValueMap<String, String> formData) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/logout").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Void.class);
    }

    //TODO: Veena: Update credentials on keycloak
    public Mono<Void> updateUser(Session session, String keyCloakUserId, String userPassword) {
        String accessToken = format("Bearer %s", session.getAccessToken());
        String uri = format("/admin/realms/consent-manager/users/%s/reset-password", keyCloakUserId);
        KeyCloakUserPasswordChangeRequest keyCloakUserPasswordChangeRequest = KeyCloakUserPasswordChangeRequest
                .builder()
                .temporary(Boolean.toString(false))
                .value(userPassword)
                .type("password")
                .build();

        return webClientBuilder.build()
                .put()//TODO: Veena: should it be put/patch?
                .uri(uriBuilder ->
                        uriBuilder.path(uri).build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(keyCloakUserPasswordChangeRequest), KeyCloakUserPasswordChangeRequest.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }
}