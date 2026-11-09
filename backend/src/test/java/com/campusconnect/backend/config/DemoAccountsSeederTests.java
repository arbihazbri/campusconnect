package com.campusconnect.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.support.AbstractApiTest;

/** Comptes de démonstration (README) : création, contenu, connexion et absence de doublon. */
class DemoAccountsSeederTests extends AbstractApiTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private DemoAccountsSeeder seeder() {
        return new DemoAccountsSeeder(true, userRepository, subjectRepository, helpRequestRepository,
                helpOfferRepository, ratingRepository, passwordEncoder);
    }

    private void runSeeder() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seeder().run(null));
    }

    private String loginDemo(String email) throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + DemoAccountsSeeder.PASSWORD + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        Matcher matcher = TOKEN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void demoAccountsShowTheWholeJourneyAndAreCreatedOnlyOnce() throws Exception {
        boolean alreadyPresent = userRepository.existsByEmail(DemoAccountsSeeder.AMIRA);
        if (!alreadyPresent) {
            // Créés par ce test : supprimés à la fin comme les autres comptes de test
            createdEmails.add(DemoAccountsSeeder.AMIRA);
            createdEmails.add(DemoAccountsSeeder.YOUSSEF);
            createdEmails.add(DemoAccountsSeeder.SARAH);
        }
        runSeeder();
        long users = userRepository.count();
        long requests = helpRequestRepository.count();
        runSeeder();
        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(helpRequestRepository.count()).isEqualTo(requests);

        String amira = loginDemo(DemoAccountsSeeder.AMIRA);
        String sarah = loginDemo(DemoAccountsSeeder.SARAH);
        loginDemo(DemoAccountsSeeder.YOUSSEF);

        String dashboard = send("GET", "/api/dashboard", amira, null).body();
        assertThat(dashboard).contains("Jointures SQL").contains("Héritage et interfaces en Java").contains("TERMINEE");

        assertThat(send("GET", "/api/recommendations?limit=20", amira, null).body())
                .contains("\"hasSkills\":true").contains("Complexité des algorithmes de tri");

        assertThat(send("GET", "/api/dashboard", sarah, null).body())
                .contains("\"reputation\":{\"averageRating\":5.0,\"ratingsCount\":1,\"sessionsHelped\":1,\"badge\":\"CONTRIBUTEUR\"}");
    }
}
