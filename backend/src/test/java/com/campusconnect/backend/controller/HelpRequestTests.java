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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelpRequestTests {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HelpRequestRepository helpRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private String newEmail() {
        String email = "demande." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);
        return email;
    }

    private String login(String email) throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        Matcher matcher = TOKEN_PATTERN.matcher(login.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String registerAndLogin(String email) throws Exception {
        HttpResponse<String> registration = send("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\",\"fullName\":\"Etudiant Demande\"}");
        assertThat(registration.statusCode()).isEqualTo(201);
        return login(email);
    }

    private Long firstId(String body) {
        Matcher matcher = ID_PATTERN.matcher(body);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    private Long firstSubjectId(String token) throws Exception {
        HttpResponse<String> response = send("GET", "/api/subjects", token, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return firstId(response.body());
    }

    private String validRequestJson(Long subjectId) {
        return "{\"title\":\"Aide en algebre lineaire\","
                + "\"description\":\"Je n'arrive pas a comprendre les espaces vectoriels.\","
                + "\"availability\":\"Soirs en semaine\","
                + "\"subjectId\":" + subjectId + ",\"level\":\"L2\"}";
    }

    @Test
    void studentCanPublishAndReadARequest() throws Exception {
        String token = registerAndLogin(newEmail());
        Long subjectId = firstSubjectId(token);

        HttpResponse<String> created = send("POST", "/api/requests", token, validRequestJson(subjectId));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body())
                .contains("Aide en algebre lineaire")
                .contains("OUVERTE")
                .contains("\"ownedByCurrentUser\":true");

        Long requestId = firstId(created.body());
        HttpResponse<String> detail = send("GET", "/api/requests/" + requestId, token, null);

        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.body()).contains("Aide en algebre lineaire").contains("OUVERTE");
    }

    @Test
    void anotherStudentSeesTheRequestWithoutPrivateData() throws Exception {
        String emailA = newEmail();
        String tokenA = registerAndLogin(emailA);
        Long subjectId = firstSubjectId(tokenA);
        Long requestId = firstId(send("POST", "/api/requests", tokenA, validRequestJson(subjectId)).body());

        String tokenB = registerAndLogin(newEmail());
        HttpResponse<String> detail = send("GET", "/api/requests/" + requestId, tokenB, null);

        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.body())
                .contains("\"ownedByCurrentUser\":false")
                .contains("Etudiant Demande")
                .doesNotContain(emailA);
    }

    @Test
    void invalidDataReturns400WithFieldErrors() throws Exception {
        String token = registerAndLogin(newEmail());

        HttpResponse<String> response = send("POST", "/api/requests", token,
                "{\"title\":\"abc\",\"description\":\"court\",\"availability\":\"\","
                        + "\"subjectId\":null,\"level\":null}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("fieldErrors")
                .contains("title")
                .contains("description")
                .contains("availability")
                .contains("subjectId")
                .contains("level");
    }

    @Test
    void unknownSubjectReturns400() throws Exception {
        String token = registerAndLogin(newEmail());

        HttpResponse<String> response = send("POST", "/api/requests", token, validRequestJson(999999999L));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("subjectId");
    }

    @Test
    void unknownRequestReturns404() throws Exception {
        String token = registerAndLogin(newEmail());

        HttpResponse<String> response = send("GET", "/api/requests/999999999", token, null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("introuvable");
    }

    @Test
    void hiddenRequestBehavesLikeAMissingOne() throws Exception {
        String token = registerAndLogin(newEmail());
        Long subjectId = firstSubjectId(token);
        Long requestId = firstId(send("POST", "/api/requests", token, validRequestJson(subjectId)).body());

        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });

        HttpResponse<String> response = send("GET", "/api/requests/" + requestId, token, null);

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void requestsRequireAuthentication() throws Exception {
        assertThat(send("POST", "/api/requests", null, validRequestJson(1L)).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/requests/1", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void adminCannotPublishARequest() throws Exception {
        String email = newEmail();
        User admin = new User();
        admin.setEmail(email);
        admin.setFullName("Admin Test");
        admin.setPasswordHash(passwordEncoder.encode("MotDePasse123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String token = login(email);
        HttpResponse<String> response = send("POST", "/api/requests", token, validRequestJson(1L));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("Accès refusé");
    }
}