package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** Fonctionnalité originale : demandes recommandées selon les compétences du profil. */
class RecommendationTests extends AbstractApiTest {

    private void setProfile(String token, String level, Long... skillIds) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (Long id : skillIds) {
            ids.append(ids.length() == 0 ? "" : ",").append(id);
        }
        HttpResponse<String> response = send("PUT", "/api/users/me", token,
                "{\"fullName\":\"Aidant Test\",\"specialty\":\"Génie logiciel\",\"level\":\"" + level + "\","
                        + "\"skillIds\":[" + ids + "]}");
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private String recommendations(String token) throws Exception {
        HttpResponse<String> response = send("GET", "/api/recommendations?limit=20", token, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    @Test
    void profileWithoutSkillsGetsInvitationInsteadOfResults() throws Exception {
        String body = recommendations(registerAndLogin());
        assertThat(body).contains("\"hasSkills\":false").contains("\"items\":[]");
    }

    @Test
    void recommendsOpenRequestsInSkillsOnlyAndPrefersSameLevel() throws Exception {
        String helper = registerAndLogin();
        String author = registerAndLogin();
        List<Long> subjects = subjectIds(author);
        setProfile(helper, "M2", subjects.get(0));

        String sameLevel = newMarker();
        String otherLevel = newMarker();
        String otherSubject = newMarker();
        String own = newMarker();
        createRequest(author, otherLevel, subjects.get(0), "L1");
        createRequest(author, sameLevel, subjects.get(0), "M2");
        createRequest(author, otherSubject, subjects.get(1), "M2");
        createRequest(helper, own, subjects.get(0), "M2");

        String body = recommendations(helper);

        assertThat(body).contains("\"hasSkills\":true").contains(sameLevel).contains(otherLevel);
        assertThat(body).doesNotContain(otherSubject).doesNotContain(own);
        assertThat(body.indexOf(sameLevel)).isLessThan(body.indexOf(otherLevel));
        int start = body.indexOf(sameLevel);
        assertThat(body.substring(start, body.indexOf("sameLevel", start) + 16)).contains("\"sameLevel\":true");
    }

    @Test
    void requestDisappearsOnceHelpOfferedOrAccepted() throws Exception {
        String helper = registerAndLogin();
        String author = registerAndLogin();
        Long subject = subjectIds(author).get(0);
        setProfile(helper, "M2", subject);

        String offered = newMarker();
        String accepted = newMarker();
        Long offeredId = createRequest(author, offered, subject, "M2");
        Long acceptedId = createRequest(author, accepted, subject, "M2");
        assertThat(recommendations(helper)).contains(offered).contains(accepted);

        createOffer(helper, offeredId);
        Long otherOffer = createOffer(registerAndLogin(), acceptedId);
        assertThat(send("POST", "/api/offers/" + otherOffer + "/accept", author, null).statusCode()).isEqualTo(200);

        assertThat(recommendations(helper)).doesNotContain(offered).doesNotContain(accepted);
    }

    @Test
    void limitIsCappedAndAuthenticationRequired() throws Exception {
        String token = registerAndLogin();
        setProfile(token, "L2", subjectIds(token).get(0));
        HttpResponse<String> response = send("GET", "/api/recommendations?limit=500", token, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().split("\"sameLevel\"", -1).length - 1).isLessThanOrEqualTo(20);
        assertThat(send("GET", "/api/recommendations", null, null).statusCode()).isEqualTo(401);
    }
}
