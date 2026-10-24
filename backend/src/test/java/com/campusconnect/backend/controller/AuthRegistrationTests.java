package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthRegistrationTests {

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

    private HttpResponse<String> post(String json) throws Exception {
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String json(String email, String password, String fullName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"fullName\":\"" + fullName + "\"}";
    }

    private String newEmail() {
        String email = "test." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);
        return email;
    }

    @Test
    void validRegistrationReturns201AndStoresAHashedPassword() throws Exception {
        String email = newEmail();

        HttpResponse<String> response = post(json(email, "MotDePasse123", "Etudiant Test"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains(email).contains("STUDENT");
        assertThat(response.body()).doesNotContain("assword");

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("MotDePasse123").startsWith("$2");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void duplicateEmailReturns409EvenWithDifferentCase() throws Exception {
        String email = newEmail();

        assertThat(post(json(email, "MotDePasse123", "Premier")).statusCode()).isEqualTo(201);

        HttpResponse<String> second = post(json(email.toUpperCase(), "MotDePasse123", "Second"));

        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("déjà utilisé");
    }

    @Test
    void invalidDataReturns400WithFieldErrors() throws Exception {
        HttpResponse<String> response = post(json("pas-un-email", "court", ""));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("fieldErrors")
                .contains("email")
                .contains("password")
                .contains("fullName");
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        HttpResponse<String> response = post("{ceci n'est pas du json");

        assertThat(response.statusCode()).isEqualTo(400);
    }
}