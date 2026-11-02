package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/**
 * Évaluation après une séance terminée : réservée aux participants, après la clôture, une seule fois.
 */
class RatingTests extends AbstractApiTest {

    private HttpResponse<String> rate(String token, Long requestId, String json) throws Exception {
        return send("POST", "/api/requests/" + requestId + "/ratings", token, json);
    }

    private HttpResponse<String> ratings(String token, Long requestId) throws Exception {
        return send("GET", "/api/requests/" + requestId + "/ratings", token, null);
    }

    @Test
    void bothParticipantsCanRateEachOtherAfterCompletion() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = completedRequest(author, helper);

        HttpResponse<String> byAuthor = rate(author, requestId, "{\"score\":5,\"comment\":\"  Très clair  \"}");
        HttpResponse<String> byHelper = rate(helper, requestId, "{\"score\":4}");

        assertThat(byAuthor.statusCode()).isEqualTo(201);
        assertThat(byAuthor.body()).contains("\"score\":5").contains("\"comment\":\"Très clair\"");
        assertThat(byHelper.statusCode()).isEqualTo(201);
        assertThat(byHelper.body()).contains("\"score\":4").contains("\"comment\":null");

        HttpResponse<String> list = ratings(author, requestId);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("\"canRate\":false").contains("\"score\":5").contains("\"score\":4");
    }

    @Test
    void ratingIsRefusedBeforeCompletion() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = acceptedRequest(author, helper);

        HttpResponse<String> response = rate(author, requestId, "{\"score\":5}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("après la clôture");
        assertThat(ratings(helper, requestId).body()).contains("\"canRate\":false");
    }

    @Test
    void onlyParticipantsCanRateOrSeeRatings() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = completedRequest(author, helper);
        String outsider = registerAndLogin();

        HttpResponse<String> byOutsider = rate(outsider, requestId, "{\"score\":1}");

        assertThat(byOutsider.statusCode()).isEqualTo(403);
        assertThat(byOutsider.body()).contains("participants");
        assertThat(ratings(outsider, requestId).statusCode()).isEqualTo(403);
        assertThat(rate(adminLogin(), requestId, "{\"score\":1}").statusCode()).isEqualTo(403);
    }

    @Test
    void aRefusedHelperIsNotAParticipant() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L3");
        String accepted = registerAndLogin();
        String refused = registerAndLogin();
        Long acceptedOffer = createOffer(accepted, requestId);
        createOffer(refused, requestId);
        assertThat(send("POST", "/api/offers/" + acceptedOffer + "/accept", author, null).statusCode())
                .isEqualTo(200);
        assertThat(send("POST", "/api/requests/" + requestId + "/complete", author, null).statusCode())
                .isEqualTo(200);

        assertThat(rate(refused, requestId, "{\"score\":3}").statusCode()).isEqualTo(403);
        assertThat(rate(accepted, requestId, "{\"score\":3}").statusCode()).isEqualTo(201);
    }

    @Test
    void aParticipantCanRateOnlyOnce() throws Exception {
        String author = registerAndLogin();
        Long requestId = completedRequest(author, registerAndLogin());

        assertThat(rate(author, requestId, "{\"score\":5}").statusCode()).isEqualTo(201);
        HttpResponse<String> again = rate(author, requestId, "{\"score\":1}");

        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.body()).contains("déjà évalué");
    }

    @Test
    void invalidScoreOrCommentIsRejected() throws Exception {
        String author = registerAndLogin();
        Long requestId = completedRequest(author, registerAndLogin());

        HttpResponse<String> tooHigh = rate(author, requestId, "{\"score\":6}");
        HttpResponse<String> tooLow = rate(author, requestId, "{\"score\":0}");
        HttpResponse<String> missing = rate(author, requestId, "{\"comment\":\"Sans note\"}");
        HttpResponse<String> longComment = rate(author, requestId,
                "{\"score\":3,\"comment\":\"" + "x".repeat(501) + "\"}");

        assertThat(tooHigh.statusCode()).isEqualTo(400);
        assertThat(tooHigh.body()).contains("\"score\"");
        assertThat(tooLow.statusCode()).isEqualTo(400);
        assertThat(missing.statusCode()).isEqualTo(400);
        assertThat(longComment.statusCode()).isEqualTo(400);
        assertThat(longComment.body()).contains("\"comment\"");
        assertThat(ratings(author, requestId).body()).contains("\"canRate\":true");
    }

    @Test
    void unknownRequestReturns404AndAuthenticationIsRequired() throws Exception {
        String token = registerAndLogin();

        assertThat(rate(token, 999999999L, "{\"score\":5}").statusCode()).isEqualTo(404);
        assertThat(ratings(token, 999999999L).statusCode()).isEqualTo(404);
        assertThat(rate(null, 1L, "{\"score\":5}").statusCode()).isEqualTo(401);
    }
}
