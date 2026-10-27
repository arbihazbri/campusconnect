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
class AuthLoginTests {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");

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

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String newEmail() {
        String email = "login." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);
        return email;
    }

    private String registerAndLogin(String email) throws Exception {
        HttpResponse<String> registration = post("/api/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\",\"fullName\":\"Etudiant Login\"}");
        assertThat(registration.statusCode()).isEqualTo(201);

        HttpResponse<String> login = post("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\"}");
        assertThat(login.statusCode()).isEqualTo(200);

        Matcher matcher = TOKEN_PATTERN.matcher(login.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void loginReturnsATokenThatGivesAccessToMe() throws Exception {
        String email = newEmail();
        String token = registerAndLogin(email);

        HttpResponse<String> me = get("/api/users/me", token);

        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(email).contains("STUDENT");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSame401() throws Exception {
        String email = newEmail();
        registerAndLogin(email);

        HttpResponse<String> wrongPassword = post("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"MauvaisMotDePasse\"}");
        HttpResponse<String> unknownEmail = post("/api/auth/login",
                "{\"email\":\"inconnu." + UUID.randomUUID() + "@campusconnect.local\",\"password\":\"MotDePasse123\"}");

        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(unknownEmail.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.body()).contains("Email ou mot de passe incorrect");
        assertThat(unknownEmail.body()).isEqualTo(wrongPassword.body());
    }

    @Test
    void protectedEndpointRejectsMissingOrInvalidToken() throws Exception {
        assertThat(get("/api/users/me", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/users/me", "ceci.nest.pas-un-jeton").statusCode()).isEqualTo(401);
    }

    @Test
    void studentCannotAccessTheAdminArea() throws Exception {
        String token = registerAndLogin(newEmail());

        HttpResponse<String> response = get("/api/admin/reports", token);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("Accès refusé");
    }

    @Test
    void healthEndpointStaysPublic() throws Exception {
        assertThat(get("/api/health", null).statusCode()).isEqualTo(200);
    }
}