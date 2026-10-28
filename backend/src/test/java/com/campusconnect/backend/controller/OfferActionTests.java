package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.support.AbstractApiTest;

class OfferActionTests extends AbstractApiTest {

    private static final String ACCEPTED = "\"status\":\"ACCEPTEE\"";
    private static final String REFUSED = "\"status\":\"REFUSEE\"";
    private static final String PENDING = "\"status\":\"EN_ATTENTE\"";

    private Long openRequest(String authorToken) throws Exception {
        List<Long> subjects = subjectIds(authorToken);
        return createRequest(authorToken, newMarker(), subjects.get(0), "L2");
    }

    private Long offerFrom(String helperToken, Long requestId) throws Exception {
        HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                "{\"message\":\"Je peux aider volontiers\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return firstId(response.body());
    }

    private HttpResponse<String> accept(String token, Long offerId) throws Exception {
        return send("POST", "/api/offers/" + offerId + "/accept", token, null);
    }

    private HttpResponse<String> reject(String token, Long offerId) throws Exception {
        return send("POST", "/api/offers/" + offerId + "/reject", token, null);
    }

    private String offersOf(String authorToken, Long requestId) throws Exception {
        HttpResponse<String> response = send("GET", "/api/requests/" + requestId + "/offers", authorToken, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private String requestDetail(String token, Long requestId) throws Exception {
        return send("GET", "/api/requests/" + requestId, token, null).body();
    }

    private int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    @Test
    void authorCanAcceptAnOfferAndTheRequestBecomesAccepted() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long offerId = offerFrom(registerAndLogin(), requestId);

        HttpResponse<String> response = accept(author, offerId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(ACCEPTED);
        assertThat(requestDetail(author, requestId)).contains("\"status\":\"AIDE_ACCEPTEE\"");
    }

    @Test
    void acceptingAnOfferRefusesTheOtherPendingOnes() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long first = offerFrom(registerAndLogin(), requestId);
        offerFrom(registerAndLogin(), requestId);

        assertThat(accept(author, first).statusCode()).isEqualTo(200);

        String offers = offersOf(author, requestId);
        assertThat(count(offers, ACCEPTED)).isEqualTo(1);
        assertThat(count(offers, REFUSED)).isEqualTo(1);
        assertThat(count(offers, PENDING)).isEqualTo(0);
    }

    @Test
    void rejectingAnOfferKeepsTheRequestOpenAndAnotherOfferCanStillBeAccepted() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long first = offerFrom(registerAndLogin(), requestId);
        Long second = offerFrom(registerAndLogin(), requestId);

        HttpResponse<String> rejection = reject(author, first);

        assertThat(rejection.statusCode()).isEqualTo(200);
        assertThat(rejection.body()).contains(REFUSED);
        assertThat(requestDetail(author, requestId)).contains("\"status\":\"OUVERTE\"");
        assertThat(accept(author, second).statusCode()).isEqualTo(200);
    }

    @Test
    void refusedHelperCannotOfferAgain() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        String helper = registerAndLogin();
        Long offerId = offerFrom(helper, requestId);

        assertThat(reject(author, offerId).statusCode()).isEqualTo(200);

        HttpResponse<String> again = send("POST", "/api/requests/" + requestId + "/offers", helper,
                "{\"message\":\"Je retente ma chance\"}");

        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.body()).contains("déjà proposé");
    }

    @Test
    void onlyTheAuthorCanAcceptOrReject() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        String helper = registerAndLogin();
        Long offerId = offerFrom(helper, requestId);
        String otherStudent = registerAndLogin();

        assertThat(accept(helper, offerId).statusCode()).isEqualTo(403);
        assertThat(reject(helper, offerId).statusCode()).isEqualTo(403);
        HttpResponse<String> byOther = accept(otherStudent, offerId);
        assertThat(byOther.statusCode()).isEqualTo(403);
        assertThat(byOther.body()).contains("auteur");

        String offers = offersOf(author, requestId);
        assertThat(count(offers, PENDING)).isEqualTo(1);
        assertThat(count(offers, ACCEPTED)).isEqualTo(0);
        assertThat(requestDetail(author, requestId)).contains("\"status\":\"OUVERTE\"");
    }

    @Test
    void nothingCanBeProcessedOnceTheRequestIsNotOpen() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long first = offerFrom(registerAndLogin(), requestId);
        Long second = offerFrom(registerAndLogin(), requestId);

        assertThat(accept(author, first).statusCode()).isEqualTo(200);

        HttpResponse<String> secondAccept = accept(author, second);
        assertThat(secondAccept.statusCode()).isEqualTo(409);
        assertThat(secondAccept.body()).contains("plus ouverte");

        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setStatus(RequestStatus.TERMINEE);
            helpRequestRepository.save(request);
        });
        HttpResponse<String> rejectAfterEnd = reject(author, second);
        assertThat(rejectAfterEnd.statusCode()).isEqualTo(409);
        assertThat(rejectAfterEnd.body()).contains("plus ouverte");
    }

        @Test
    void anOfferAlreadyProcessedCannotBeProcessedAgain() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long offerId = offerFrom(registerAndLogin(), requestId);

        assertThat(reject(author, offerId).statusCode()).isEqualTo(200);

        HttpResponse<String> secondReject = reject(author, offerId);
        HttpResponse<String> acceptAfterReject = accept(author, offerId);

        assertThat(secondReject.statusCode()).isEqualTo(409);
        assertThat(secondReject.body()).contains("déjà été traitée");
        assertThat(acceptAfterReject.statusCode()).isEqualTo(409);
        assertThat(acceptAfterReject.body()).contains("déjà été traitée");
    }

    @Test
    void unknownOfferOrHiddenRequestReturns404() throws Exception {
        String author = registerAndLogin();

        assertThat(accept(author, 999999999L).statusCode()).isEqualTo(404);
        assertThat(reject(author, 999999999L).statusCode()).isEqualTo(404);

        Long requestId = openRequest(author);
        Long offerId = offerFrom(registerAndLogin(), requestId);
        helpRequestRepository.findById(requestId).ifPresent(request -> {
            request.setHidden(true);
            helpRequestRepository.save(request);
        });

        assertThat(accept(author, offerId).statusCode()).isEqualTo(404);
    }

    @Test
    void actionsRequireAuthentication() throws Exception {
        assertThat(accept(null, 1L).statusCode()).isEqualTo(401);
        assertThat(reject(null, 1L).statusCode()).isEqualTo(401);
    }

    @Test
    void aNewOfferAfterAnAcceptanceIsRefused() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long offerId = offerFrom(registerAndLogin(), requestId);
        assertThat(accept(author, offerId).statusCode()).isEqualTo(200);

        HttpResponse<String> late = send("POST", "/api/requests/" + requestId + "/offers", registerAndLogin(),
                "{\"message\":\"Je propose trop tard\"}");

        assertThat(late.statusCode()).isEqualTo(409);
        assertThat(late.body()).contains("plus de propositions");
    }

    @Test
    void twoSimultaneousAcceptancesLetOnlyOneWin() throws Exception {
        String author = registerAndLogin();
        Long requestId = openRequest(author);
        Long first = offerFrom(registerAndLogin(), requestId);
        Long second = offerFrom(registerAndLogin(), requestId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> acceptFirst = () -> {
            start.await();
            return accept(author, first).statusCode();
        };
        Callable<Integer> acceptSecond = () -> {
            start.await();
            return accept(author, second).statusCode();
        };
        Future<Integer> firstResult = executor.submit(acceptFirst);
        Future<Integer> secondResult = executor.submit(acceptSecond);
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        statuses.add(firstResult.get(30, TimeUnit.SECONDS));
        statuses.add(secondResult.get(30, TimeUnit.SECONDS));
        executor.shutdown();
        Collections.sort(statuses);

        assertThat(statuses).containsExactly(200, 409);
        String offers = offersOf(author, requestId);
        assertThat(count(offers, ACCEPTED)).isEqualTo(1);
        assertThat(count(offers, REFUSED)).isEqualTo(1);
    }
}