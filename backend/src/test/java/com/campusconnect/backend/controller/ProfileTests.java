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

import com.campusconnect.backend.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileTests {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UserRepository userRepository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
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
        String email = "profil." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);

        HttpResponse<String> registration = send("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\",\"fullName\":\"Etudiant Profil\"}");
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
        return ids;
    }

    private int countOccurrences(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    @Test
    void updatedProfileIsSavedAndReturnedByMe() throws Exception {
        String token = registerAndLogin();
        List<Long> ids = subjectIds(token);
        assertThat(ids).hasSizeGreaterThanOrEqualTo(2);

        String body = "{\"fullName\":\"Nouveau Nom\",\"specialty\":\"Génie logiciel\",\"level\":\"L2\","
                + "\"skillIds\":[" + ids.get(0) + "," + ids.get(1) + "]}";
        HttpResponse<String> update = send("PUT", "/api/users/me", token, body);

        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(update.body()).contains("Nouveau Nom").contains("L2");

        HttpResponse<String> me = send("GET", "/api/users/me", token, null);

        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("Nouveau Nom").contains("Génie logiciel").contains("L2");
        assertThat(countOccurrences(me.body(), "\"name\"")).isEqualTo(2);
    }

    @Test
    void unknownSkillIsRejectedWith400() throws Exception {
        String token = registerAndLogin();

        HttpResponse<String> response = send("PUT", "/api/users/me", token,
                "{\"fullName\":\"Nom Valide\",\"skillIds\":[999999999]}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("skillIds");
    }

    @Test
    void blankNameIsRejectedWith400() throws Exception {
        String token = registerAndLogin();

        HttpResponse<String> response = send("PUT", "/api/users/me", token, "{\"fullName\":\"\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("fieldErrors").contains("fullName");
    }

    @Test
    void updateWithoutTokenReturns401() throws Exception {
        HttpResponse<String> response = send("PUT", "/api/users/me", null, "{\"fullName\":\"Nom Valide\"}");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void updatingOneProfileDoesNotAffectAnotherUser() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        HttpResponse<String> update = send("PUT", "/api/users/me", tokenA,
                "{\"fullName\":\"Etudiant A\",\"specialty\":\"SpecialiteDeA\",\"level\":\"M1\"}");
        assertThat(update.statusCode()).isEqualTo(200);

        HttpResponse<String> meB = send("GET", "/api/users/me", tokenB, null);

        assertThat(meB.statusCode()).isEqualTo(200);
        assertThat(meB.body()).doesNotContain("SpecialiteDeA").doesNotContain("Etudiant A");
    }

    @Test
    void subjectsListRequiresAuthenticationAndReturnsTheSubjects() throws Exception {
        assertThat(send("GET", "/api/subjects", null, null).statusCode()).isEqualTo(401);

        String token = registerAndLogin();
        HttpResponse<String> response = send("GET", "/api/subjects", token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Mathématiques");
    }
}