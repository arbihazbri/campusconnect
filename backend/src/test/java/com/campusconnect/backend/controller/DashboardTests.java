package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/**
 * Tableau de bord personnel : demandes publiées, aides proposées et séances réalisées.
 */
class DashboardTests extends AbstractApiTest {

    private HttpResponse<String> dashboard(String token) throws Exception {
        return send("GET", "/api/dashboard", token, null);
    }

    @Test
    void newStudentHasAnEmptyDashboard() throws Exception {
        HttpResponse<String> response = dashboard(registerAndLogin());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"publishedRequests\":0")
                .contains("\"offersMade\":0")
                .contains("\"completedSessions\":0")
                .contains("\"averageRating\":null")
                .contains("\"myRequests\":[]")
                .contains("\"myOffers\":[]")
                .contains("\"sessions\":[]");
    }

    @Test
    void dashboardShowsRequestsOffersAndSessionsOfBothParticipants() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long completedId = completedRequest(author, helper);
        String openMarker = newMarker();
        Long openId = createRequest(author, openMarker, subjectIds(author).get(0), "M1");
        createOffer(registerAndLogin(), openId);
        assertThat(send("POST", "/api/requests/" + completedId + "/ratings", helper, "{\"score\":4}").statusCode())
                .isEqualTo(201);

        String authorBoard = dashboard(author).body();
        assertThat(authorBoard)
                .contains("\"publishedRequests\":2")
                .contains("\"openRequests\":1")
                .contains("\"completedSessions\":1")
                .contains("\"ratingsReceived\":1")
                .contains("\"averageRating\":4.0")
                .contains(openMarker)
                .contains("\"pendingOffersCount\":1")
                .contains("\"role\":\"AUTEUR\"")
                .contains("\"ratedByMe\":false");

        String helperBoard = dashboard(helper).body();
        assertThat(helperBoard)
                .contains("\"publishedRequests\":0")
                .contains("\"offersMade\":1")
                .contains("\"offersAccepted\":1")
                .contains("\"completedSessions\":1")
                .contains("\"role\":\"AIDANT\"")
                .contains("\"ratedByMe\":true")
                .contains("\"requestStatus\":\"TERMINEE\"");
    }

    @Test
    void hiddenRequestStaysInTheAuthorDashboardWithItsFlag() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L1");
        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });

        assertThat(dashboard(author).body())
                .contains("\"hidden\":true")
                .contains("\"openRequests\":0");
    }

    @Test
    void dashboardIsReservedToAuthenticatedStudents() throws Exception {
        assertThat(dashboard(null).statusCode()).isEqualTo(401);
        assertThat(dashboard(adminLogin()).statusCode()).isEqualTo(403);
    }
}
