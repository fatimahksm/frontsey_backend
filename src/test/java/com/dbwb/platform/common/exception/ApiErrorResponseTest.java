package com.dbwb.platform.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a caller is told when they get the request wrong.
 *
 * Driven over HTTP rather than by calling the handler directly, because
 * routing is the part that quietly breaks: which exception Spring throws for
 * an unknown address or a badly typed path variable is a framework detail that
 * moves between versions, and a handler nothing reaches is a handler that does
 * nothing.
 *
 * Before these existed, every case below answered 500 "An unexpected error
 * occurred. Please try again or contact support." - telling someone who
 * mistyped a URL that the platform is broken and to go and find a human, and
 * filling the error log with routine mistakes so a real crash had somewhere to
 * hide.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiErrorResponseTest {

    @Autowired private TestRestTemplate rest;

    private ResponseEntity<String> post(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    @Test
    void aBodyThatIsNotJsonIsTheCallersMistake() {
        ResponseEntity<String> response = post("/api/auth/login", "{not json");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("not in the expected format");
    }

    /**
     * Under /api/public, where there is no session to fail first.
     *
     * An unknown address anywhere else answers 401 rather than 404, because
     * security runs before routing - which is the right way round: whether an
     * endpoint exists is not something an anonymous caller should be able to
     * map out by reading status codes.
     */
    @Test
    void anAddressNothingServesIsANotFound() {
        ResponseEntity<String> response = rest.getForEntity("/api/public/does/not/exist", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("does not exist");
    }

    @Test
    void theWrongMethodSaysSoRatherThanBlamingTheServer() {
        ResponseEntity<String> response = rest.exchange("/api/auth/login", HttpMethod.DELETE, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).contains("not available on this address");
    }

    @Test
    void aBodySentWithNoContentTypeIsRefusedAsSuch() {
        ResponseEntity<String> response = rest.exchange(
                "/api/auth/login", HttpMethod.POST, new HttpEntity<>("x"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void validationNamesTheFieldThatIsWrong() {
        ResponseEntity<String> response = post("/api/auth/register", "{\"email\":\"someone@example.com\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // The whole point: "must not be blank; must not be blank" told someone
        // filling in a form neither what was blank nor that it was their turn
        // to act.
        assertThat(response.getBody()).contains("Full name").contains("Password");
    }

    @Test
    void noRefusalEverCarriesAClassNameOrAStackTrace() {
        List<ResponseEntity<String>> refusals = List.of(
                post("/api/auth/login", "{not json"),
                rest.getForEntity("/api/public/does/not/exist", String.class),
                post("/api/auth/register", "{}"));

        for (ResponseEntity<String> response : refusals) {
            assertThat(response.getBody())
                    .doesNotContain("Exception")
                    .doesNotContain("java.")
                    .doesNotContain("org.springframework");
        }
    }
}
