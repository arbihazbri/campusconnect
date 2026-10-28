package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.support.AbstractApiTest;

class HelpRequestUpdateTests extends AbstractApiTest {

    private String updateJson(String title, Long subjectId, String level) {
        return "{\"title\":\"" + title + "\","
                + "\"description\":\"Description modifiee et assez longue.\","
                + "\"availability\":\"Le week-end\","
                + "\"subjectId\":" + subjectId + ",\"level\":\"" + level + "\"}";
    }

    @Test
    void authorCanUpdateAnOpenRequest() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        String originalMarker = newMarker();
        Long requestId = createRequest(token, originalMarker, subjects.get(0), "L2");
        String newTitle = "Titre modifie " + newMarker();

        HttpResponse<String> update = send("PUT", "/api/requests/" + requestId, token,
                updateJson(newTitle, subjects.get(1), "L3"));

        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(update.body())
                .contains(newTitle)
                .contains("L3")
                .contains("OUVERTE")
                .contains("\"ownedByCurrentUser\":true");

        HttpResponse<String> reread = send("GET", "/api/requests/" + requestId, token, null);

        assertThat(reread.body()).contains(newTitle).doesNotContain(originalMarker);
    }

    @Test
    void anotherStudentCannotUpdateTheRequest() throws Exception {
        String tokenAuthor = registerAndLogin();
        List<Long> subjects = subjectIds(tokenAuthor);
        String originalMarker = newMarker();
        Long requestId = createRequest(tokenAuthor, originalMarker, subjects.get(0), "L2");

        String tokenOther = registerAndLogin();
        HttpResponse<String> attempt = send("PUT", "/api/requests/" + requestId, tokenOther,
                updateJson("Titre pirate " + newMarker(), subjects.get(0), "L2"));

        assertThat(attempt.statusCode()).isEqualTo(403);
        assertThat(attempt.body()).contains("auteur");

        HttpResponse<String> reread = send("GET", "/api/requests/" + requestId, tokenAuthor, null);

        assertThat(reread.body()).contains(originalMarker).doesNotContain("pirate");
    }

    @Test
    void requestThatIsNotOpenCannotBeUpdated() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        Long requestId = createRequest(token, newMarker(), subjects.get(0), "L2");

        for (RequestStatus status : List.of(RequestStatus.AIDE_ACCEPTEE, RequestStatus.TERMINEE)) {
            helpRequestRepository.findById(requestId).ifPresent(request -> {
                request.setStatus(status);
                helpRequestRepository.save(request);
            });

            HttpResponse<String> attempt = send("PUT", "/api/requests/" + requestId, token,
                    updateJson("Titre modifie " + newMarker(), subjects.get(0), "L2"));

            assertThat(attempt.statusCode()).isEqualTo(409);
            assertThat(attempt.body()).contains("ouverte");
        }
    }

    @Test
    void invalidDataReturns400WithFieldErrors() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        Long requestId = createRequest(token, newMarker(), subjects.get(0), "L2");

        HttpResponse<String> response = send("PUT", "/api/requests/" + requestId, token,
                updateJson("abc", subjects.get(0), "L2"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("fieldErrors").contains("title");
    }

    @Test
    void unknownSubjectReturns400() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        Long requestId = createRequest(token, newMarker(), subjects.get(0), "L2");

        HttpResponse<String> response = send("PUT", "/api/requests/" + requestId, token,
                updateJson("Titre modifie valide", 999999999L, "L2"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("subjectId");
    }

    @Test
    void unknownRequestReturns404() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);

        HttpResponse<String> response = send("PUT", "/api/requests/999999999", token,
                updateJson("Titre modifie valide", subjects.get(0), "L2"));

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void hiddenRequestCannotBeUpdated() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        Long requestId = createRequest(token, newMarker(), subjects.get(0), "L2");

        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });

        HttpResponse<String> response = send("PUT", "/api/requests/" + requestId, token,
                updateJson("Titre modifie valide", subjects.get(0), "L2"));

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void updateWithoutTokenReturns401() throws Exception {
        HttpResponse<String> response = send("PUT", "/api/requests/1", null,
                updateJson("Titre modifie valide", 1L, "L2"));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void clientCannotForceTheStatusOrTheAuthor() throws Exception {
        String token = registerAndLogin();
        List<Long> subjects = subjectIds(token);
        Long requestId = createRequest(token, newMarker(), subjects.get(0), "L2");

        String body = "{\"title\":\"Titre modifie valide\","
                + "\"description\":\"Description modifiee et assez longue.\","
                + "\"availability\":\"Le week-end\","
                + "\"subjectId\":" + subjects.get(0) + ",\"level\":\"L2\","
                + "\"status\":\"TERMINEE\",\"hidden\":true,\"authorId\":1}";
        HttpResponse<String> response = send("PUT", "/api/requests/" + requestId, token, body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("OUVERTE").contains("\"ownedByCurrentUser\":true");

        HttpResponse<String> reread = send("GET", "/api/requests/" + requestId, token, null);

        assertThat(reread.statusCode()).isEqualTo(200);
    }
}