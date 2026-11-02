package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/**
 * Suivi d'une demande : ouverte → aide acceptée → terminée.
 */
class SessionCompletionTests extends AbstractApiTest {

    private HttpResponse<String> complete(String token, Long requestId) throws Exception {
        return send("POST", "/api/requests/" + requestId + "/complete", token, null);
    }

    private String detail(String token, Long requestId) throws Exception {
        return send("GET", "/api/requests/" + requestId, token, null).body();
    }

    @Test
    void authorCanCompleteARequestOnceHelpIsAccepted() throws Exception {
        String author = registerAndLogin();
        Long requestId = acceptedRequest(author, registerAndLogin());

        HttpResponse<String> response = complete(author, requestId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"TERMINEE\"");
        assertThat(response.body()).doesNotContain("\"completedAt\":null");
        assertThat(detail(author, requestId)).contains("\"status\":\"TERMINEE\"");
    }

    @Test
    void anOpenRequestCannotBeCompleted() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L1");

        HttpResponse<String> response = complete(author, requestId);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("acceptation d'une aide");
        assertThat(detail(author, requestId)).contains("\"status\":\"OUVERTE\"");
    }

    @Test
    void aCompletedRequestCannotBeCompletedAgain() throws Exception {
        String author = registerAndLogin();
        Long requestId = completedRequest(author, registerAndLogin());

        HttpResponse<String> again = complete(author, requestId);

        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.body()).contains("déjà terminée");
    }

    @Test
    void onlyTheAuthorCanComplete() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = acceptedRequest(author, helper);

        HttpResponse<String> byHelper = complete(helper, requestId);
        HttpResponse<String> byOther = complete(registerAndLogin(), requestId);

        assertThat(byHelper.statusCode()).isEqualTo(403);
        assertThat(byOther.statusCode()).isEqualTo(403);
        assertThat(byOther.body()).contains("auteur");
        assertThat(detail(author, requestId)).contains("\"status\":\"AIDE_ACCEPTEE\"");
    }

    @Test
    void aCompletedRequestCannotBeEditedNorReceiveOffers() throws Exception {
        String author = registerAndLogin();
        Long requestId = completedRequest(author, registerAndLogin());

        HttpResponse<String> offer = send("POST", "/api/requests/" + requestId + "/offers", registerAndLogin(),
                "{\"message\":\"Je propose après la fin\"}");
        HttpResponse<String> update = send("PUT", "/api/requests/" + requestId, author,
                "{\"title\":\"Nouveau titre\",\"description\":\"Nouvelle description assez longue\","
                        + "\"availability\":\"Lundi\",\"subjectId\":" + subjectIds(author).get(0)
                        + ",\"level\":\"L2\"}");

        assertThat(offer.statusCode()).isEqualTo(409);
        assertThat(update.statusCode()).isEqualTo(409);
    }

    @Test
    void unknownOrHiddenRequestReturns404AndAuthenticationIsRequired() throws Exception {
        String author = registerAndLogin();
        assertThat(complete(author, 999999999L).statusCode()).isEqualTo(404);

        Long requestId = acceptedRequest(author, registerAndLogin());
        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });
        assertThat(complete(author, requestId).statusCode()).isEqualTo(404);

        assertThat(complete(null, requestId).statusCode()).isEqualTo(401);
    }
}
