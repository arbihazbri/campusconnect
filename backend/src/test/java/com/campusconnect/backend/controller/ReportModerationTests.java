package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/**
 * Signalement des publications et espace administrateur de modération.
 */
class ReportModerationTests extends AbstractApiTest {

    private HttpResponse<String> report(String token, Long requestId, String reason) throws Exception {
        return send("POST", "/api/requests/" + requestId + "/reports", token,
                "{\"reason\":\"" + reason + "\"}");
    }

    private HttpResponse<String> admin(String method, String path, String token) throws Exception {
        return send(method, "/api/admin" + path, token, null);
    }

    private Long publishedRequest(String token, String marker) throws Exception {
        return createRequest(token, marker, subjectIds(token).get(0), "L2");
    }

    @Test
    void aStudentCanReportAnotherStudentsRequest() throws Exception {
        Long requestId = publishedRequest(registerAndLogin(), newMarker());

        HttpResponse<String> response = report(registerAndLogin(), requestId, "Contenu hors sujet");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"status\":\"EN_ATTENTE\"").contains("Contenu hors sujet");
    }

    @Test
    void reportingRulesAreEnforced() throws Exception {
        String author = registerAndLogin();
        Long requestId = publishedRequest(author, newMarker());
        String reporter = registerAndLogin();

        HttpResponse<String> own = report(author, requestId, "Je me signale");
        assertThat(own.statusCode()).isEqualTo(403);

        assertThat(report(reporter, requestId, "Premier signalement").statusCode()).isEqualTo(201);
        HttpResponse<String> twice = report(reporter, requestId, "Second signalement");
        assertThat(twice.statusCode()).isEqualTo(409);
        assertThat(twice.body()).contains("déjà signalé");

        HttpResponse<String> tooShort = report(registerAndLogin(), requestId, "abc");
        assertThat(tooShort.statusCode()).isEqualTo(400);
        assertThat(tooShort.body()).contains("\"reason\"");

        assertThat(report(registerAndLogin(), 999999999L, "Demande inconnue").statusCode()).isEqualTo(404);
        assertThat(report(adminLogin(), requestId, "Admin ne signale pas").statusCode()).isEqualTo(403);
        assertThat(report(null, requestId, "Sans jeton").statusCode()).isEqualTo(401);
    }

    @Test
    void adminSeesReportedRequestsGroupedWithTheirReports() throws Exception {
        String marker = newMarker();
        Long requestId = publishedRequest(registerAndLogin(), marker);
        report(registerAndLogin(), requestId, "Premier motif");
        report(registerAndLogin(), requestId, "Second motif");

        HttpResponse<String> response = admin("GET", "/reports", adminLogin());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains(marker)
                .contains("\"requestId\":" + requestId + ",")
                .contains("Premier motif")
                .contains("Second motif");
    }

    @Test
    void hidingARequestRemovesItForStudentsAndResolvesItsReports() throws Exception {
        String author = registerAndLogin();
        String marker = newMarker();
        Long requestId = publishedRequest(author, marker);
        report(registerAndLogin(), requestId, "Publication inappropriée");
        String adminToken = adminLogin();

        HttpResponse<String> hidden = admin("POST", "/requests/" + requestId + "/hide", adminToken);

        assertThat(hidden.statusCode()).isEqualTo(200);
        assertThat(hidden.body()).contains("\"hidden\":true").contains("\"status\":\"TRAITE\"")
                .contains("\"pendingReports\":0");
        assertThat(send("GET", "/api/requests/" + requestId, author, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/api/requests?size=50", author, null).body()).doesNotContain(marker);
        assertThat(admin("GET", "/reports", adminToken).body()).doesNotContain(marker);
        assertThat(admin("GET", "/reports?pendingOnly=false", adminToken).body()).contains(marker);

        HttpResponse<String> hideAgain = admin("POST", "/requests/" + requestId + "/hide", adminToken);
        assertThat(hideAgain.statusCode()).isEqualTo(409);
    }

    @Test
    void restoringAHiddenRequestMakesItVisibleAgain() throws Exception {
        String author = registerAndLogin();
        Long requestId = publishedRequest(author, newMarker());
        String adminToken = adminLogin();
        assertThat(admin("POST", "/requests/" + requestId + "/hide", adminToken).statusCode()).isEqualTo(200);

        HttpResponse<String> restored = admin("POST", "/requests/" + requestId + "/restore", adminToken);

        assertThat(restored.statusCode()).isEqualTo(200);
        assertThat(restored.body()).contains("\"hidden\":false");
        assertThat(send("GET", "/api/requests/" + requestId, author, null).statusCode()).isEqualTo(200);
        assertThat(admin("POST", "/requests/" + requestId + "/restore", adminToken).statusCode()).isEqualTo(409);
    }

    @Test
    void dismissingReportsKeepsTheRequestVisible() throws Exception {
        String author = registerAndLogin();
        String marker = newMarker();
        Long requestId = publishedRequest(author, marker);
        report(registerAndLogin(), requestId, "Signalement abusif");
        String adminToken = adminLogin();

        HttpResponse<String> dismissed = admin("POST", "/requests/" + requestId + "/dismiss", adminToken);

        assertThat(dismissed.statusCode()).isEqualTo(200);
        assertThat(dismissed.body()).contains("\"hidden\":false").contains("\"status\":\"REJETE\"");
        assertThat(send("GET", "/api/requests/" + requestId, author, null).statusCode()).isEqualTo(200);
        assertThat(admin("GET", "/reports", adminToken).body()).doesNotContain(marker);

        HttpResponse<String> nothingLeft = admin("POST", "/requests/" + requestId + "/dismiss", adminToken);
        assertThat(nothingLeft.statusCode()).isEqualTo(409);
        assertThat(nothingLeft.body()).contains("Aucun signalement en attente");
    }

    @Test
    void adminActionsOnUnknownRequestReturn404() throws Exception {
        String adminToken = adminLogin();

        assertThat(admin("POST", "/requests/999999999/hide", adminToken).statusCode()).isEqualTo(404);
        assertThat(admin("POST", "/requests/999999999/restore", adminToken).statusCode()).isEqualTo(404);
        assertThat(admin("POST", "/requests/999999999/dismiss", adminToken).statusCode()).isEqualTo(404);
    }

    @Test
    void theAdminAreaIsForbiddenToStudents() throws Exception {
        String student = registerAndLogin();
        Long requestId = publishedRequest(student, newMarker());

        assertThat(admin("GET", "/reports", student).statusCode()).isEqualTo(403);
        assertThat(admin("POST", "/requests/" + requestId + "/hide", student).statusCode()).isEqualTo(403);
        assertThat(admin("POST", "/requests/" + requestId + "/restore", student).statusCode()).isEqualTo(403);
        assertThat(admin("POST", "/requests/" + requestId + "/dismiss", student).statusCode()).isEqualTo(403);
        assertThat(admin("GET", "/reports", null).statusCode()).isEqualTo(401);
    }
}
