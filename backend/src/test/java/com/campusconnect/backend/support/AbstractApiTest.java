package com.campusconnect.backend.support;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Outils communs aux tests qui appellent l'API par HTTP.
 * Tout ce qui est créé par les tests est supprimé après chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractApiTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected HelpRequestRepository helpRequestRepository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedData() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                helpRequestRepository.deleteAll(helpRequestRepository.findByAuthorId(user.getId()));
                userRepository.delete(user);
            });
        }
        createdEmails.clear();
    }

    protected HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
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

    /** Crée un étudiant, le connecte et renvoie son jeton. */
    protected String registerAndLogin() throws Exception {
        String email = "test." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);

        HttpResponse<String> registration = send("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\",\"fullName\":\"Etudiant Test\"}");
        assertThat(registration.statusCode()).isEqualTo(201);

        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        Matcher matcher = TOKEN_PATTERN.matcher(login.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    protected Long firstId(String body) {
        Matcher matcher = ID_PATTERN.matcher(body);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    protected List<Long> subjectIds(String token) throws Exception {
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

    protected String newMarker() {
        return "Marqueur-" + UUID.randomUUID();
    }

    /** Crée une demande dont le titre contient le marqueur et renvoie son identifiant. */
    protected Long createRequest(String token, String marker, Long subjectId, String level) throws Exception {
        HttpResponse<String> response = send("POST", "/api/requests", token,
                "{\"title\":\"Demande " + marker + "\","
                        + "\"description\":\"Description de test suffisamment longue.\","
                        + "\"availability\":\"Tous les jours\","
                        + "\"subjectId\":" + subjectId + ",\"level\":\"" + level + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return firstId(response.body());
    }
}