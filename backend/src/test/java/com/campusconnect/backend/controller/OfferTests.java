package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.support.AbstractApiTest;

class OfferTests extends AbstractApiTest {

    private String offerJson(String message) {
        return "{\"message\":\"" + message + "\"}";
    }

    private Long openRequestOf(String authorToken) throws Exception {
        List<Long> subjects = subjectIds(authorToken);
        return createRequest(authorToken, newMarker(), subjects.get(0), "L2");
    }

    @Test
    void anotherStudentCanOfferHelp() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String helperToken = registerAndLogin();

        HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                offerJson("Je peux t aider en soiree"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("EN_ATTENTE")
                .contains("Je peux t aider en soiree")
                .contains("Etudiant Test")
                .contains("\"requestId\":" + requestId);
    }

    @Test
    void authorCannotOfferHelpOnTheirOwnRequest() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);

        HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", authorToken,
                offerJson("Je me propose moi meme"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("propre demande");
    }

    @Test
    void sameStudentCannotOfferTwiceOnTheSameRequest() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String helperToken = registerAndLogin();

        HttpResponse<String> first = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                offerJson("Premiere proposition"));
        HttpResponse<String> second = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                offerJson("Deuxieme proposition"));

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("déjà proposé");
    }

    @Test
    void requestThatIsNotOpenReceivesNoMoreOffers() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String helperToken = registerAndLogin();

        for (RequestStatus status : List.of(RequestStatus.AIDE_ACCEPTEE, RequestStatus.TERMINEE)) {
            helpRequestRepository.findById(requestId).ifPresent(request -> {
                request.setStatus(status);
                helpRequestRepository.save(request);
            });

            HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                    offerJson("Proposition tardive"));

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("plus de propositions");
        }
    }

    @Test
    void unknownOrHiddenRequestReturns404() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String helperToken = registerAndLogin();

        HttpResponse<String> unknown = send("POST", "/api/requests/999999999/offers", helperToken,
                offerJson("Proposition valide"));
        assertThat(unknown.statusCode()).isEqualTo(404);

        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });
        HttpResponse<String> hidden = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                offerJson("Proposition valide"));
        assertThat(hidden.statusCode()).isEqualTo(404);
    }

    @Test
    void invalidMessageReturns400() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String helperToken = registerAndLogin();

        HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                offerJson("abc"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("fieldErrors").contains("message");
    }

    @Test
    void offerRequiresAuthentication() throws Exception {
        HttpResponse<String> response = send("POST", "/api/requests/1/offers", null,
                offerJson("Proposition valide"));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void adminCannotOfferHelp() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String adminToken = adminLogin();

        HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", adminToken,
                offerJson("Proposition valide"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("Accès refusé");
    }

    @Test
    void authorCanListTheOffersInTheOrderTheyArrived() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String firstMarker = "Premiere " + newMarker();
        String secondMarker = "Seconde " + newMarker();

        String firstHelper = registerAndLogin();
        String secondHelper = registerAndLogin();
        send("POST", "/api/requests/" + requestId + "/offers", firstHelper, offerJson(firstMarker));
        send("POST", "/api/requests/" + requestId + "/offers", secondHelper, offerJson(secondMarker));

        HttpResponse<String> response = send("GET", "/api/requests/" + requestId + "/offers", authorToken, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(firstMarker).contains(secondMarker);
        assertThat(response.body().indexOf(firstMarker)).isLessThan(response.body().indexOf(secondMarker));
    }

    @Test
    void onlyTheAuthorCanListTheOffers() throws Exception {
        String authorToken = registerAndLogin();
        Long requestId = openRequestOf(authorToken);
        String helperToken = registerAndLogin();
        send("POST", "/api/requests/" + requestId + "/offers", helperToken, offerJson("Ma proposition"));

        HttpResponse<String> byHelper = send("GET", "/api/requests/" + requestId + "/offers", helperToken, null);
        HttpResponse<String> byOther = send("GET", "/api/requests/" + requestId + "/offers", registerAndLogin(), null);

        assertThat(byHelper.statusCode()).isEqualTo(403);
        assertThat(byOther.statusCode()).isEqualTo(403);
        assertThat(byOther.body()).contains("auteur");
    }
}