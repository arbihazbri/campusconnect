package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** Fonctionnalité originale : réputation de l'aidant visible par l'auteur et dans le tableau de bord. */
class ReputationTests extends AbstractApiTest {

    @Test
    void newHelperHasNoRatingAndNewcomerBadge() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");
        createOffer(helper, requestId);

        String offers = send("GET", "/api/requests/" + requestId + "/offers", author, null).body();
        assertThat(offers).contains("\"helperReputation\":{\"averageRating\":null,\"ratingsCount\":0,"
                + "\"sessionsHelped\":0,\"badge\":\"NOUVEAU\"}");
    }

    @Test
    void completedAndRatedHelpBuildsReputation() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long done = completedRequest(author, helper);
        HttpResponse<String> rating = send("POST", "/api/requests/" + done + "/ratings", author,
                "{\"score\":5,\"comment\":\"Très clair\"}");
        assertThat(rating.statusCode()).isEqualTo(201);

        Long next = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");
        createOffer(helper, next);
        String offers = send("GET", "/api/requests/" + next + "/offers", author, null).body();
        assertThat(offers).contains("\"helperReputation\":{\"averageRating\":5.0,\"ratingsCount\":1,"
                + "\"sessionsHelped\":1,\"badge\":\"CONTRIBUTEUR\"}");

        String dashboard = send("GET", "/api/dashboard", helper, null).body();
        assertThat(dashboard).contains("\"reputation\":{\"averageRating\":5.0,\"ratingsCount\":1,"
                + "\"sessionsHelped\":1,\"badge\":\"CONTRIBUTEUR\"}");
    }
}
