package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelpRequestListTests {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HelpRequestRepository helpRequestRepository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                helpRequestRepository.deleteAll(helpRequestRepository.findByAuthorId(user.getId()));
                userRepository.delete(user);
            });
        }
        createdEmails.clear();
    }

    private HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (json != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String registerAndLogin() throws Exception {
        String email = "liste." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);

        HttpResponse<String> registration = send("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\",\"fullName\":\"Etudiant Liste\"}");
        assertThat(registration.statusCode()).isEqualTo(201);

        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        Matcher matcher = TOKEN_PATTERN.matcher(login.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private List<Long> subjectIds(String token) throws Exception {
        HttpResponse<String> response = send("GET", "/api/subjects", token, null);
        assertThat(response.statusCode()).isEqualTo(200);
        List<Long> ids = new ArrayList<>();
        Matcher matcher = ID_PATTERN.matcher(response.body());
        while (matcher.find()) {
            ids.add(Long.valueOf(matcher.group(1)));
        }
        assertThat(ids).hasSizeGreaterThanOrEqualTo(2);
        return ids;
    }

    private String newMarker() {
        return "Marqueur-" + UUID.randomUUID();
    }

    /** Crée une demande dont le titre contient le marqueur, et renvoie son identifiant. */
    private Long createRequest(String token, String marker, Long subjectId, String level) throws Exception {
        HttpResponse<String> response = send("POST", "/api/requests", token,
                "{\"title\":\"Demande " + marker + "\","
                        + "\"description\":\"Description de test suffisamment longue.\","
                        + "\"availability\":\"Tous les jours\","
                        + "\"subjectId\":" + subjectId + ",\"level\":\"" + level + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        Matcher matcher = ID_PATTERN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    private int countOccurrences(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    @Test
    void listReturnsNewestFirstWithPaginationInformation() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String older = newMarker();
        String newer = newMarker();
        createRequest(token, older, subjects.get(0), "L2");
        createRequest(token, newer, subjects.get(1), "L3");

        HttpResponse<String> response = send("GET", "/api/requests?size=50", token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body).contains("\"content\"").contains("\"totalElements\"").contains("\"totalPages\"");
        assertThat(body.indexOf(older)).isGreaterThanOrEqualTo(0);
        assertThat(body.indexOf(newer)).isGreaterThanOrEqualTo(0);
        assertThat(body.indexOf(newer)).isLessThan(body.indexOf(older));
    }

    @Test
    void filterBySubject() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String first = newMarker();
        String second = newMarker();
        createRequest(token, first, subjects.get(0), "L2");
        createRequest(token, second, subjects.get(1), "L2");

        HttpResponse<String> response = send("GET",
                "/api/requests?size=50&subjectId=" + subjects.get(0), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(first).doesNotContain(second);
    }

    @Test
    void filterByLevel() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String level2 = newMarker();
        String level3 = newMarker();
        createRequest(token, level2, subjects.get(0), "L2");
        createRequest(token, level3, subjects.get(0), "L3");

        HttpResponse<String> response = send("GET", "/api/requests?size=50&level=L3", token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(level3).doesNotContain(level2);
    }

    @Test
    void filterByStatus() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String accepted = newMarker();
        String open = newMarker();
        Long acceptedId = createRequest(token, accepted, subjects.get(0), "L2");
        createRequest(token, open, subjects.get(0), "L2");

        helpRequestRepository.findById(acceptedId).ifPresent(request -> {
            request.setStatus(RequestStatus.AIDE_ACCEPTEE);
            helpRequestRepository.save(request);
        });

        HttpResponse<String> acceptedOnly = send("GET", "/api/requests?size=50&status=AIDE_ACCEPTEE", token, null);
        HttpResponse<String> openOnly = send("GET", "/api/requests?size=50&status=OUVERTE", token, null);

        assertThat(acceptedOnly.body()).contains(accepted).doesNotContain(open);
        assertThat(openOnly.body()).contains(open).doesNotContain(accepted);
    }

    @Test
    void combinedFiltersAreAllApplied() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String target = newMarker();
        createRequest(token, target, subjects.get(1), "L3");

        HttpResponse<String> matching = send("GET",
                "/api/requests?size=50&subjectId=" + subjects.get(1) + "&level=L3", token, null);
        HttpResponse<String> wrongSubject = send("GET",
                "/api/requests?size=50&subjectId=" + subjects.get(0) + "&level=L3", token, null);
        HttpResponse<String> wrongLevel = send("GET",
                "/api/requests?size=50&subjectId=" + subjects.get(1) + "&level=L2", token, null);

        assertThat(matching.body()).contains(target);
        assertThat(wrongSubject.body()).doesNotContain(target);
        assertThat(wrongLevel.body()).doesNotContain(target);
    }

    @Test
    void hiddenRequestsAreNotListed() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String hidden = newMarker();
        String visible = newMarker();
        Long hiddenId = createRequest(token, hidden, subjects.get(0), "L2");
        createRequest(token, visible, subjects.get(0), "L2");

        helpRequestRepository.findById(hiddenId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });

        HttpResponse<String> response = send("GET", "/api/requests?size=50", token, null);

        assertThat(response.body()).contains(visible).doesNotContain(hidden);
    }

    @Test
    void paginationSplitsTheResultsAndCapsTheSize() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String first = newMarker();
        String second = newMarker();
        String third = newMarker();
        createRequest(token, first, subjects.get(0), "L2");
        createRequest(token, second, subjects.get(0), "L2");
        createRequest(token, third, subjects.get(0), "L2");

        HttpResponse<String> pageZero = send("GET", "/api/requests?size=1&page=0", token, null);
        HttpResponse<String> pageOne = send("GET", "/api/requests?size=1&page=1", token, null);

        assertThat(countOccurrences(pageZero.body(), "\"title\"")).isEqualTo(1);
        assertThat(countOccurrences(pageOne.body(), "\"title\"")).isEqualTo(1);
        assertThat(pageZero.body()).contains(third).contains("\"page\":0").contains("\"size\":1");
        assertThat(pageOne.body()).contains(second).contains("\"page\":1");

        HttpResponse<String> capped = send("GET", "/api/requests?size=1000&page=-5", token, null);

        assertThat(capped.statusCode()).isEqualTo(200);
        assertThat(capped.body()).contains("\"size\":50").contains("\"page\":0");
    }

    @Test
    void invalidFilterValuesReturn400() throws Exception {
        String token = registerAndLogin();

        HttpResponse<String> badLevel = send("GET", "/api/requests?level=L9", token, null);
        HttpResponse<String> badStatus = send("GET", "/api/requests?status=INCONNU", token, null);

        assertThat(badLevel.statusCode()).isEqualTo(400);
        assertThat(badLevel.body()).contains("level");
        assertThat(badStatus.statusCode()).isEqualTo(400);
        assertThat(badStatus.body()).contains("status");
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        assertThat(send("GET", "/api/requests", null, null).statusCode()).isEqualTo(401);
    }
}