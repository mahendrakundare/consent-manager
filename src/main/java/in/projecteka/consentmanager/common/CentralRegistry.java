package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@AllArgsConstructor
public class CentralRegistry {
    private final ClientRegistryClient clientRegistryClient;
    private final ClientRegistryProperties properties;

    public Mono<Provider> providerWith(String providerId) {
        return authenticate()
                .flatMap(token -> clientRegistryClient.providerWith(providerId, token));
    }

    public Flux<Provider> providersOf(String providerName) {
        return authenticate()
                .flatMapMany(token -> clientRegistryClient.providersOf(providerName, token));
    }

    public Mono<String> authenticate() {
        return clientRegistryClient
                .getTokenFor(properties.getClientId(), properties.getXAuthToken())
                .map(session -> format("%s %s", session.getTokenType(), session.getAccessToken()));
    }
}
