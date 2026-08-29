package io.academicmonitor.integration.idukay.auth;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Component
public class IdukayAuthClient {

    private final RestClient.Builder restClientBuilder;
    private final CryptoJsPasswordEncoder passwordEncoder;
    private final String baseUrl;
    private final String clientVersion;

    public IdukayAuthClient(
        RestClient.Builder restClientBuilder,
        CryptoJsPasswordEncoder passwordEncoder,
        @Value(
            "${app.idukay.base-url:"
                + "https://idukay.net/colegios/api/}")
        String baseUrl,
        @Value("${app.idukay.client-version:12.0.2}")
        String clientVersion) {

        this.restClientBuilder = restClientBuilder;
        this.passwordEncoder = passwordEncoder;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.clientVersion =
            requireText(
                clientVersion,
                "clientVersion");
    }

    public IdukayLoginSession startLogin(String email, char[] password, String subdomainSchool) {

        String normalizedEmail = requireText(email, "email");

        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password is required");
        }

        RestClient sessionClient = createSessionClient();

        char[] workingPassword = password.clone();

        String encodedPassword;

        try {
            encodedPassword = passwordEncoder.encode(workingPassword);
        } finally {
            Arrays.fill(workingPassword, '\0');
        }

        IdukayLoginWebRequest request =
            new IdukayLoginWebRequest(normalizedEmail, encodedPassword, normalizeOptional(subdomainSchool));

        IdukayLoginWebResponse result;

        try {
            result = sessionClient
                .post()
                .uri("login/web")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IdukayLoginWebResponse.class);

        } catch (RestClientResponseException exception) {

            throw new IdukayLoginException(
                "Idukay login request failed with HTTP "
                    + exception.getStatusCode().value(),
                exception);

        } catch (RestClientException exception) {

            throw new IdukayLoginException("Unable to communicate with Idukay login service", exception);
        }

        if (result == null) {
            throw new IdukayLoginException("Idukay returned an empty login response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayLoginException(
                "Idukay rejected the login request: "
                    + summarizeErrors(result.errors()));
        }
        if (result.response() == null
            || result.response().attempt_id() == null
            || result.response().attempt_id().isBlank()) {

            throw new IdukayLoginException("Idukay login response did not contain an attempt id");
        }

        return new IdukayLoginSession(result.response().attempt_id().trim(), sessionClient);
    }

    public IdukayLoginContexts getAvailableContexts(IdukayLoginSession session) {

        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        IdukayLoginContextsRequest request = new IdukayLoginContextsRequest(session.attemptId());

        IdukayLoginContextsResponse result;

        try {
            result = session.restClient()
                .post()
                .uri("login/contexts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IdukayLoginContextsResponse.class);

        } catch (RestClientResponseException exception) {

            throw new IdukayLoginException(
                "Idukay login contexts request failed with HTTP "
                    + exception.getStatusCode().value(),
                exception);

        } catch (RestClientException exception) {

            throw new IdukayLoginException("Unable to communicate with Idukay login contexts service", exception);
        }

        if (result == null) {
            throw new IdukayLoginException("Idukay returned an empty login contexts response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayLoginException("Idukay rejected the login contexts request");
        }

        if (result.response() == null) {
            throw new IdukayLoginException("Idukay login contexts response did not contain context data");
        }

        return result.response();
    }

    public IdukayLoginProfiles getProfilesBySchool(IdukayLoginSession session, String schoolId) {

        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        String normalizedSchoolId = requireText(schoolId, "schoolId");

        IdukayLoginProfilesRequest request = new IdukayLoginProfilesRequest(normalizedSchoolId, session.attemptId());

        IdukayLoginProfilesResponse result;

        try {
            result = session.restClient()
                .post()
                .uri("login/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IdukayLoginProfilesResponse.class);

        } catch (RestClientResponseException exception) {

            throw new IdukayLoginException(
                "Idukay login profiles request failed with HTTP "
                    + exception.getStatusCode().value(),
                exception);

        } catch (RestClientException exception) {

            throw new IdukayLoginException("Unable to communicate with Idukay login profiles service", exception);
        }

        if (result == null) {
            throw new IdukayLoginException("Idukay returned an empty login profiles response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayLoginException("Idukay rejected the login profiles request");
        }

        if (result.response() == null) {
            throw new IdukayLoginException("Idukay login profiles response did not contain profile data");
        }

        if (result.response().user() == null || result.response().user().isBlank()) {

            throw new IdukayLoginException("Idukay login profiles response did not contain a user id");
        }

        return result.response();
    }

    public IdukayAuthenticatedSession completeLogin(
        IdukayLoginSession session, IdukayOauthProfile profile, IdukayFingerprint fingerprint) {

        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }

        if (fingerprint == null) {
            throw new IllegalArgumentException("fingerprint is required");
        }

        IdukayOauthRequest request = new IdukayOauthRequest(profile, fingerprint, session.attemptId());

        IdukayOauthResponse result;

        try {
            result = session.restClient()
                .post()
                .uri("login/oauth")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IdukayOauthResponse.class);

        } catch (RestClientResponseException exception) {

            throw new IdukayLoginException(
                "Idukay OAuth request failed with HTTP "
                    + exception.getStatusCode().value(),
                exception);

        } catch (RestClientException exception) {

            throw new IdukayLoginException("Unable to communicate with Idukay OAuth service", exception);
        }

        if (result == null) {
            throw new IdukayLoginException("Idukay returned an empty OAuth response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayLoginException("Idukay rejected the OAuth request");
        }

        if (result.response() == null) {
            throw new IdukayLoginException("Idukay OAuth response did not contain session data");
        }

        String token = requireText(result.response().token(), "OAuth token");

        IdukayOauthUser user = result.response().user();

        if (user == null || user.preferences() == null) {

            throw new IdukayLoginException("Idukay OAuth response did not contain user preferences");
        }

        IdukaySessionContext context = createSessionContext(user);

        return new IdukayAuthenticatedSession(token, context, session.restClient());
    }

    private static IdukaySessionContext createSessionContext(IdukayOauthUser user) {

        IdukayUserPreferences preferences = user.preferences();

        if (preferences.workingYear() == null
            || isBlank(preferences.workingYear().id())) {

            throw new IdukayLoginException("Idukay session did not contain a working year");
        }

        if (preferences.workingSchool() == null
            || isBlank(preferences.workingSchool().id())) {

            throw new IdukayLoginException("Idukay session did not contain a working school");
        }

        if (preferences.workingProfile() == null
            || isBlank(preferences.workingProfile().id())
            || isBlank(preferences.workingProfile().collectionName())) {

            throw new IdukayLoginException("Idukay session did not contain a working profile");
        }

        return new IdukaySessionContext(
            preferences.workingYear().id(),
            preferences.workingSchool().id(),
            idOf(preferences.workingOrganization()),
            idOf(preferences.selectedStudent()),
            preferences.workingProfile().id(),
            preferences.workingProfile().collectionName(),
            preferences.timeZone(),
            user.acceptedPermissions());
    }

    private void applyLoginHeaders(
        org.springframework.http.HttpHeaders headers) {

        headers.set(
            "ClientVersion",
            clientVersion);
    }

    private static String summarizeErrors(
        JsonNode errors) {

        if (errors == null
            || errors.isNull()
            || errors.isMissingNode()) {

            return "unknown";
        }

        if (errors.isArray()) {

            return errors.valueStream()
                .map(error -> {

                    JsonNode code =
                        error.get("code");

                    JsonNode message =
                        error.get("message");

                    if (code != null
                        && code.isTextual()) {

                        return code.asText();
                    }

                    if (message != null
                        && message.isTextual()) {

                        return message.asText();
                    }

                    return "unknown_error";
                })
                .distinct()
                .toList()
                .toString();
        }

        return "unknown";
    }

    private static String idOf(IdukayPreferenceReference reference) {

        if (reference == null || isBlank(reference.id())) {

            return null;
        }

        return reference.id();
    }

    private static boolean isBlank(String value) {

        return value == null || value.isBlank();
    }

    private RestClient createSessionClient() {

        CookieManager cookieManager =
            new CookieManager();

        cookieManager.setCookiePolicy(
            CookiePolicy.ACCEPT_ALL);

        HttpClient httpClient =
            HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(
                    Duration.ofSeconds(15))
                .followRedirects(
                    HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(
                httpClient);

        return restClientBuilder
            .clone()
            .requestFactory(requestFactory)
            .baseUrl(baseUrl)
            .defaultHeader(
                "ClientVersion",
                clientVersion)
            .build();
    }

    private static boolean hasErrors(JsonNode errors) {

        if (errors == null || errors.isNull() || errors.isMissingNode()) {

            return false;
        }

        if (errors.isArray() || errors.isObject()) {

            return errors.size() > 0;
        }

        if (errors.isTextual()) {
            return !errors.asText().isBlank();
        }

        return true;
    }

    private static String normalizeBaseUrl(String value) {

        String normalized = requireText(value, "baseUrl");

        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static String normalizeOptional(String value) {

        if (value == null || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }
}
