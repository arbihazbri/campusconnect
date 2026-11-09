package com.campusconnect.backend.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.Rating;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Comptes de démonstration (DEMO_DATA=true) : trois étudiants avec un profil complet et des demandes
 * à tous les stades (ouverte avec proposition, aide acceptée, terminée et évaluée), pour montrer
 * le parcours complet, le tableau de bord et les recommandations sans rien saisir.
 * Rien n'est créé si les comptes existent déjà : le chargement peut être relancé sans doublon.
 */
@Component
@Order(10)
public class DemoAccountsSeeder implements ApplicationRunner {

    public static final String PASSWORD = "Demo12345";
    public static final String AMIRA = "amira@demo.campusconnect.local";
    public static final String YOUSSEF = "youssef@demo.campusconnect.local";
    public static final String SARAH = "sarah@demo.campusconnect.local";

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsSeeder.class);

    private final boolean enabled;
    private final UserRepository users;
    private final SubjectRepository subjects;
    private final HelpRequestRepository requests;
    private final HelpOfferRepository offers;
    private final RatingRepository ratings;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountsSeeder(
            @Value("${app.demo.enabled:false}") boolean enabled,
            UserRepository users,
            SubjectRepository subjects,
            HelpRequestRepository requests,
            HelpOfferRepository offers,
            RatingRepository ratings,
            PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.users = users;
        this.subjects = subjects;
        this.requests = requests;
        this.offers = offers;
        this.ratings = ratings;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (users.existsByEmail(AMIRA)) {
            log.info("Comptes de démonstration déjà présents");
            return;
        }
        Map<String, Subject> byName = subjects.findAll().stream()
                .collect(Collectors.toMap(Subject::getName, subject -> subject));

        User amira = student(AMIRA, "Amira Ben Salah", "Génie logiciel", Level.L2,
                byName.get("Algorithmique"), byName.get("Programmation Java"));
        User youssef = student(YOUSSEF, "Youssef Trabelsi", "Réseaux et télécoms", Level.L3,
                byName.get("Bases de données"), byName.get("Développement web"), byName.get("Réseaux"));
        User sarah = student(SARAH, "Sarah Mansour", "Data science", Level.M1,
                byName.get("Mathématiques"), byName.get("Intelligence artificielle"), byName.get("Algorithmique"));

        // Demandes ouvertes : chacune correspond aux compétences d'un autre compte (recommandations)
        request(youssef, byName.get("Algorithmique"), Level.L2, RequestStatus.OUVERTE,
                "Complexité des algorithmes de tri",
                "Je ne comprends pas pourquoi le tri rapide est en O(n log n) en moyenne mais O(n²) au pire.",
                "Mardi et jeudi après 17 h", 2);
        HelpRequest sql = request(amira, byName.get("Bases de données"), Level.L2, RequestStatus.OUVERTE,
                "Jointures SQL et requêtes imbriquées",
                "Besoin d'aide pour choisir entre une jointure et une sous-requête dans le TP de bases de données.",
                "Mercredi après-midi", 1);
        offer(youssef, sql, OfferStatus.EN_ATTENTE, "J'ai validé ce module l'an dernier, je peux t'expliquer avec des exemples.");
        request(amira, byName.get("Mathématiques"), Level.L2, RequestStatus.OUVERTE,
                "Diagonalisation de matrices",
                "Révision de l'examen d'algèbre : valeurs propres et diagonalisation.",
                "Samedi matin", 0);

        // Aide acceptée, séance en cours
        HelpRequest web = request(sarah, byName.get("Développement web"), Level.M1, RequestStatus.AIDE_ACCEPTEE,
                "Mise en page responsive en CSS Grid",
                "Mon tableau de bord casse sur mobile, je voudrais comprendre grid-template-areas.",
                "Vendredi 14 h", 3);
        offer(youssef, web, OfferStatus.ACCEPTEE, "Je fais du front tous les jours, on regarde ça ensemble.");

        // Séance terminée et évaluée des deux côtés (réputation de Sarah et d'Amira)
        HelpRequest java = request(amira, byName.get("Programmation Java"), Level.L2, RequestStatus.TERMINEE,
                "Héritage et interfaces en Java",
                "Quand utiliser une classe abstraite plutôt qu'une interface ?",
                "Lundi soir", 6);
        offer(sarah, java, OfferStatus.ACCEPTEE, "Je peux te montrer avec un exemple concret.");
        rating(java, amira, sarah, 5, "Explications très claires, merci !");
        rating(java, sarah, amira, 4, "Étudiante motivée et bien préparée.");

        log.info("Comptes de démonstration créés : {}, {}, {} (mot de passe {})", AMIRA, YOUSSEF, SARAH, PASSWORD);
    }

    private User student(String email, String name, String specialty, Level level, Subject... skills) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setSpecialty(specialty);
        user.setLevel(level);
        user.setRole(Role.STUDENT);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        for (Subject skill : skills) {
            if (skill != null) {
                user.getSkills().add(skill);
            }
        }
        return users.save(user);
    }

    private HelpRequest request(User author, Subject subject, Level level, RequestStatus status,
            String title, String description, String availability, int daysAgo) {
        HelpRequest request = new HelpRequest();
        request.setAuthor(author);
        request.setSubject(subject);
        request.setLevel(level);
        request.setStatus(status);
        request.setTitle(title);
        request.setDescription(description);
        request.setAvailability(availability);
        request.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        if (status == RequestStatus.TERMINEE) {
            request.setCompletedAt(Instant.now().minus(Math.max(daysAgo - 2, 0), ChronoUnit.DAYS));
        }
        return requests.save(request);
    }

    private void offer(User helper, HelpRequest request, OfferStatus status, String message) {
        HelpOffer offer = new HelpOffer();
        offer.setHelper(helper);
        offer.setRequest(request);
        offer.setStatus(status);
        offer.setMessage(message);
        offers.save(offer);
    }

    private void rating(HelpRequest request, User rater, User rated, int score, String comment) {
        Rating rating = new Rating();
        rating.setRequest(request);
        rating.setRater(rater);
        rating.setRated(rated);
        rating.setScore(score);
        rating.setComment(comment);
        ratings.save(rating);
    }
}
