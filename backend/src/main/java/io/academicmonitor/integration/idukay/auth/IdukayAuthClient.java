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

@Component
public class IdukayAuthClient {

    private final RestClient.Builder restClientBuilder;
    private final CryptoJsPasswordEncoder passwordEncoder;
    private final String baseUrl;

    public IdukayAuthClient(
            RestClient.Builder restClientBuilder,
            CryptoJsPasswordEncoder passwordEncoder,
            @Value("${app.idukay.base-url:" + "https://idukay.net/colegios/api/}") String baseUrl) {

        this.restClientBuilder = restClientBuilder;
        this.passwordEncoder = passwordEncoder;
        this.baseUrl = normalizeBaseUrl(baseUrl);
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

        if (!result.errors().isEmpty()) {
            throw new IdukayLoginException("Idukay rejected the login request");
        }

        if (result.response() == null
                || result.response().attempt_id() == null
                || result.response().attempt_id().isBlank()) {

            throw new IdukayLoginException("Idukay login response did not contain an attempt id");
        }

        return new IdukayLoginSession(result.response().attempt_id().trim(), sessionClient);
    }

    private RestClient createSessionClient() {

        CookieManager cookieManager = new CookieManager();

        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return restClientBuilder
                .clone()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
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
