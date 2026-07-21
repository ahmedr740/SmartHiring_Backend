package com.smarthiring;

import com.smarthiring.model.Application;
import com.smarthiring.model.ChatMessage;
import com.smarthiring.model.IssueReport;
import com.smarthiring.model.LikedJob;
import com.smarthiring.model.MockPayment;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ChatMessageRepository;
import com.smarthiring.repository.IssueReportRepository;
import com.smarthiring.repository.LikedJobRepository;
import com.smarthiring.repository.MockPaymentRepository;
import com.smarthiring.repository.ShiftRepository;
import com.smarthiring.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class DemoDataInitializer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository,
            LikedJobRepository likedJobRepository,
            IssueReportRepository issueReportRepository,
            ChatMessageRepository chatMessageRepository,
            MockPaymentRepository mockPaymentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-seed.enabled:false}") boolean demoSeedEnabled,
            @Value("${app.demo-seed.password:StaffMatch2026!}") String demoPassword
    ) {
        return args -> {
            if (!demoSeedEnabled) {
                return;
            }

            if (demoPassword == null || demoPassword.length() < 8) {
                throw new IllegalStateException("DEMO_SEED_PASSWORD must be at least 8 characters long");
            }

            seedDemoDataset(
                    userRepository,
                    shiftRepository,
                    applicationRepository,
                    likedJobRepository,
                    issueReportRepository,
                    chatMessageRepository,
                    mockPaymentRepository,
                    passwordEncoder,
                    demoPassword
            );
        };
    }

    @Transactional
    void seedDemoDataset(
            UserRepository userRepository,
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository,
            LikedJobRepository likedJobRepository,
            IssueReportRepository issueReportRepository,
            ChatMessageRepository chatMessageRepository,
            MockPaymentRepository mockPaymentRepository,
            PasswordEncoder passwordEncoder,
            String demoPassword
    ) {
        User admin = upsertUser(
                userRepository,
                passwordEncoder,
                "admin@smarthiring.local",
                "demo.admin@smarthiring.local",
                demoPassword,
                "Avery Admin",
                "ADMIN",
                "ACTIVE",
                null,
                "555-0101",
                "Central Office",
                null,
                null,
                4.9,
                7,
                12
        );

        User manager = upsertUser(
                userRepository,
                passwordEncoder,
                "manager@smarthiring.local",
                "demo.manager@smarthiring.local",
                demoPassword,
                "Mina Manager",
                "MANAGER",
                "ACTIVE",
                "Harbour Kitchen",
                "555-0102",
                "Harbour",
                null,
                null,
                4.8,
                9,
                18
        );

        User worker = upsertUser(
                userRepository,
                passwordEncoder,
                "worker@smarthiring.local",
                "demo.worker@smarthiring.local",
                demoPassword,
                "Wes Worker",
                "WORKER",
                "ACTIVE",
                null,
                "555-0103",
                "Downtown",
                "Weekday evenings, weekends",
                "Server, Barista, Cashier, Kitchen Helper",
                4.7,
                11,
                24
        );

        User applicant = upsertUser(
                userRepository,
                passwordEncoder,
                "applicant@smarthiring.local",
                "demo.applicant@smarthiring.local",
                demoPassword,
                "Priya Applicant",
                "WORKER",
                "ACTIVE",
                null,
                "555-0104",
                "Central",
                "Weekend mornings",
                "Kitchen Helper, Prep Cook, Dishwasher",
                4.5,
                6,
                13
        );

        upsertUser(
                userRepository,
                passwordEncoder,
                "pending.manager@smarthiring.local",
                "demo.pending.manager@smarthiring.local",
                demoPassword,
                "Pending Manager",
                "MANAGER",
                "PENDING",
                "Pending Bistro",
                "555-0105",
                "Westside",
                null,
                null,
                0d,
                0,
                0
        );

        LocalDate today = LocalDate.now();
        Shift serverShift = upsertShift(
                shiftRepository,
                manager,
                "Evening Server Shift",
                "Demo Evening Server Shift",
                today.plusDays(2).format(DATE_FORMAT),
                "17:00",
                "22:00",
                18.50,
                "Server",
                "Downtown",
                "OPEN",
                null,
                false
        );

        Shift kitchenShift = upsertShift(
                shiftRepository,
                manager,
                "Weekend Kitchen Helper",
                "Demo Weekend Kitchen Helper",
                today.plusDays(4).format(DATE_FORMAT),
                "10:00",
                "16:00",
                17.00,
                "Kitchen Helper",
                "Central",
                "OPEN",
                null,
                false
        );

        upsertShift(
                shiftRepository,
                manager,
                "Morning Barista Cover",
                "Demo Morning Barista Cover",
                today.plusDays(6).format(DATE_FORMAT),
                "08:00",
                "13:00",
                19.00,
                "Barista",
                "Harbour",
                "OPEN",
                null,
                false
        );

        Shift completedShift = upsertShift(
                shiftRepository,
                manager,
                "Completed Dinner Service",
                "Demo Completed Dinner Service",
                today.minusDays(2).format(DATE_FORMAT),
                "18:00",
                "23:00",
                20.00,
                "Server",
                "Harbour",
                "COMPLETED",
                worker,
                false
        );
        completedShift.setCompletedAt(LocalDateTime.now().minusDays(1));
        shiftRepository.save(completedShift);

        Application pendingWorkerApplication = upsertApplication(applicationRepository, worker, serverShift, "PENDING", null, null);
        upsertApplication(applicationRepository, applicant, kitchenShift, "PENDING", null, null);
        Application completedApplication = upsertApplication(applicationRepository, worker, completedShift, "ACCEPTED", null, null);
        upsertLikedJob(likedJobRepository, worker, kitchenShift);
        upsertIssue(issueReportRepository, worker, pendingWorkerApplication, serverShift);
        upsertChat(chatMessageRepository, manager, worker, completedApplication, completedShift);
        upsertMockPayment(mockPaymentRepository, completedShift, manager, worker);

        refreshCompletionCounts(userRepository, shiftRepository, manager, worker, applicant, admin);
    }

    private User upsertUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String legacyEmail,
            String rawPassword,
            String name,
            String role,
            String status,
            String restaurantName,
            String phone,
            String location,
            String availability,
            String skills,
            Double rating,
            Integer ratingCount,
            Integer completedShiftsCount
    ) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByEmailIgnoreCase(legacyEmail))
                .orElseGet(User::new);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(status);
        user.setRestaurantName(restaurantName);
        user.setPhone(phone);
        user.setLocation(location);
        user.setAvailability(availability);
        user.setSkills(skills);
        user.setRating(rating);
        user.setRatingCount(ratingCount);
        user.setCompletedShiftsCount(completedShiftsCount);
        return userRepository.save(user);
    }

    private Shift upsertShift(
            ShiftRepository shiftRepository,
            User manager,
            String title,
            String legacyTitle,
            String date,
            String startTime,
            String endTime,
            Double pay,
            String roleNeeded,
            String location,
            String status,
            User assignedWorker,
            Boolean paid
    ) {
        Shift shift = shiftRepository.findAllByManagerId(manager.getId()).stream()
                .filter(candidate -> title.equals(candidate.getTitle()) || legacyTitle.equals(candidate.getTitle()))
                .findFirst()
                .orElseGet(Shift::new);
        shift.setManager(manager);
        shift.setTitle(title);
        shift.setDate(date);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setPay(pay);
        shift.setRoleNeeded(roleNeeded);
        shift.setLocation(location);
        shift.setStatus(status);
        shift.setAssignedWorker(assignedWorker);
        shift.setPaid(paid);
        if (!"COMPLETED".equals(status)) {
            shift.setCompletedAt(null);
        }
        if (!Boolean.TRUE.equals(paid)) {
            shift.setPaidAt(null);
        }
        return shiftRepository.save(shift);
    }

    private Application upsertApplication(
            ApplicationRepository applicationRepository,
            User worker,
            Shift shift,
            String status,
            Integer workerRating,
            Integer managerRating
    ) {
        Application application = applicationRepository.findByWorkerIdAndShiftId(worker.getId(), shift.getId())
                .orElseGet(Application::new);
        application.setWorker(worker);
        application.setShift(shift);
        application.setStatus(status);
        application.setWorkerRating(workerRating);
        application.setManagerRating(managerRating);
        return applicationRepository.save(application);
    }

    private void upsertLikedJob(LikedJobRepository likedJobRepository, User worker, Shift shift) {
        if (likedJobRepository.existsByWorkerIdAndShiftId(worker.getId(), shift.getId())) {
            return;
        }

        LikedJob likedJob = new LikedJob();
        likedJob.setWorker(worker);
        likedJob.setShift(shift);
        likedJobRepository.save(likedJob);
    }

    private void upsertIssue(
            IssueReportRepository issueReportRepository,
            User worker,
            Application application,
            Shift shift
    ) {
        boolean exists = issueReportRepository.findAllByReportedByIdOrderByCreatedAtDesc(worker.getId()).stream()
                .anyMatch(issue -> issue.getShift() != null
                        && issue.getShift().getId().equals(shift.getId())
                        && "PAYMENT".equalsIgnoreCase(issue.getCategory()));
        if (exists) {
            return;
        }

        IssueReport issue = new IssueReport();
        issue.setReportedBy(worker);
        issue.setApplication(application);
        issue.setShift(shift);
        issue.setCategory("PAYMENT");
        issue.setDescription("Worker needs manager clarification before the shift starts.");
        issue.setStatus("OPEN");
        issueReportRepository.save(issue);
    }

    private void upsertChat(
            ChatMessageRepository chatMessageRepository,
            User manager,
            User worker,
            Application application,
            Shift shift
    ) {
        if (!chatMessageRepository.findAllByShiftIdOrderByCreatedAtAsc(shift.getId()).isEmpty()) {
            return;
        }

        ChatMessage managerMessage = new ChatMessage();
        managerMessage.setShift(shift);
        managerMessage.setApplication(application);
        managerMessage.setSender(manager);
        managerMessage.setMessage("Thanks for covering the dinner service. Please arrive 10 minutes early.");
        chatMessageRepository.save(managerMessage);

        ChatMessage workerMessage = new ChatMessage();
        workerMessage.setShift(shift);
        workerMessage.setApplication(application);
        workerMessage.setSender(worker);
        workerMessage.setMessage("Confirmed. I will be there early and bring my black uniform.");
        chatMessageRepository.save(workerMessage);
    }

    private void upsertMockPayment(
            MockPaymentRepository mockPaymentRepository,
            Shift shift,
            User manager,
            User worker
    ) {
        if (mockPaymentRepository.findByShiftId(shift.getId()).isPresent()) {
            return;
        }

        MockPayment payment = new MockPayment();
        payment.setShift(shift);
        payment.setManager(manager);
        payment.setWorker(worker);
        payment.setAmount(100.00);
        payment.setStatus("PENDING");
        payment.setMethodLabel("Card");
        mockPaymentRepository.save(payment);
    }

    private void refreshCompletionCounts(
            UserRepository userRepository,
            ShiftRepository shiftRepository,
            User... users
    ) {
        List.of(users).forEach(user -> {
            if ("WORKER".equalsIgnoreCase(user.getRole())) {
                user.setCompletedShiftsCount((int) shiftRepository.countByAssignedWorkerIdAndStatusIgnoreCase(user.getId(), "COMPLETED"));
            }

            if ("MANAGER".equalsIgnoreCase(user.getRole())) {
                user.setCompletedShiftsCount((int) shiftRepository.countByManagerIdAndStatusIgnoreCase(user.getId(), "COMPLETED"));
            }

            userRepository.save(user);
        });
    }
}
