package com.carrental.service;

import com.carrental.dto.PickupRequest;
import com.carrental.dto.ReturnRequest;
import com.carrental.dto.DriverReturnRequest;
import com.carrental.model.*;
import com.carrental.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:driver-availability-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class DriverAvailabilityTest {
    @Autowired private RentalService rentalService;
    @Autowired private UserRepository users;
    @Autowired private VehicleRepository vehicles;
    @Autowired private ReservationRepository reservations;
    @Autowired private PickupRecordRepository pickups;
    @Autowired private DriverReturnService driverReturns;
    @Autowired private DriverReturnReportRepository reports;
    @Autowired private ReturnRecordRepository returns;
    @Autowired private ReservationService reservationService;
    @Autowired private MockMvc mvc;
    @Autowired private MaintenanceRequestRepository maintenanceRequests;
    @Autowired private MaintenanceService maintenanceService;
    @Autowired private VehicleService vehicleService;

    private User driver;
    private User staff;
    private Reservation first;
    private Reservation second;

    @BeforeEach
    void prepareTrips() {
        driver = users.save(new User(null, "Test driver", UUID.randomUUID() + "@test.lk",
                "unused-test-password", "0700000000", "TEST-LICENCE", Role.DRIVER));
        staff = users.findByRole(Role.STAFF).get(0);
        first = createReservation();
        second = createReservation();
    }

    private Reservation createReservation() {
        Vehicle vehicle = vehicles.save(new Vehicle(null, "Test", "Car", 2026, "Sedan",
                UUID.randomUUID().toString(), new BigDecimal("10000"), 5, 2, "Petrol", "Automatic",
                1000, "Full", VehicleStatus.AVAILABLE, null, null));
        return reservations.save(new Reservation(null, UUID.randomUUID().toString(),
                users.findByRole(Role.CUSTOMER).get(0), vehicle, LocalDate.now(), LocalDate.now(),
                "Colombo", "Colombo", true, 1, vehicle.getRentalPricePerDay(),
                new BigDecimal("12500"), ReservationStatus.CONFIRMED, null));
    }

    private PickupRequest pickupRequest(Reservation reservation) {
        PickupRequest request = new PickupRequest();
        request.setReservationId(reservation.getId());
        request.setStaffId(staff.getId());
        request.setDriverId(driver.getId());
        request.setInitialMileage(1000);
        request.setInitialFuelLevel("Full");
        return request;
    }

    private boolean driverIsAvailable() {
        return rentalService.getAvailableDrivers().stream().anyMatch(user -> user.getId().equals(driver.getId()));
    }

    @Test
    void busyDriverIsRejectedAndBecomesAvailableAfterReturn() {
        assertThat(driverIsAvailable()).isTrue();
        rentalService.processPickup(pickupRequest(first));
        assertThat(driverIsAvailable()).isFalse();

        assertThatThrownBy(() -> rentalService.processPickup(pickupRequest(second)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already on an active trip");
        assertThat(pickups.findByReservationId(second.getId())).isEmpty();
        assertThat(reservations.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(vehicles.findById(second.getVehicle().getId()).orElseThrow().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        ReturnRequest returned = new ReturnRequest();
        returned.setReservationId(first.getId());
        returned.setStaffId(staff.getId());
        returned.setFinalMileage(1050);
        returned.setFinalFuelLevel("Full");
        driverReturns.submit(new DriverReturnRequest(first.getId(), 1050, "Full", "None", "Returned"), driver.getEmail());
        assertThat(driverIsAvailable()).isFalse();
        rentalService.processReturn(returned);
        assertThat(driverIsAvailable()).isTrue();

        rentalService.processPickup(pickupRequest(second));
        assertThat(driverIsAvailable()).isFalse();
        assertThat(pickups.findByReservationId(second.getId()).orElseThrow().getDriver().getId()).isEqualTo(driver.getId());
    }

    @Test
    void simultaneousPickupsCannotAssignTheSameDriverTwice() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> attemptPickup(first, ready, start));
            var b = executor.submit(() -> attemptPickup(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            long activeTrips = pickups.findAll().stream()
                    .filter(record -> record.getDriver() != null && record.getDriver().getId().equals(driver.getId()))
                    .filter(record -> record.getReservation().getStatus() == ReservationStatus.PICKED_UP).count();
            assertThat(activeTrips).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void driverReportRequiresOwnershipValidMileageAndSingleSubmission() {
        rentalService.processPickup(pickupRequest(first));
        var invalid = new DriverReturnRequest(first.getId(), 999, "Full", null, null);
        assertThatThrownBy(() -> driverReturns.submit(invalid, driver.getEmail())).hasMessageContaining("pickup mileage");
        User other = users.save(new User(null, "Other driver", UUID.randomUUID() + "@test.lk", "unused", null, "OTHER", Role.DRIVER));
        var valid = new DriverReturnRequest(first.getId(), 1150, "1/2", "None", "Original driver notes");
        assertThatThrownBy(() -> driverReturns.submit(valid, other.getEmail()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(driverReturns.activeTrips(other.getEmail())).isEmpty();
        driverReturns.submit(valid, driver.getEmail());
        assertThatThrownBy(() -> driverReturns.submit(valid, driver.getEmail())).hasMessageContaining("already been submitted");
        assertThat(driverReturns.list(other.getEmail())).isEmpty();
        assertThat(reservations.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(ReservationStatus.PICKED_UP);
        assertThat(vehicles.findById(first.getVehicle().getId()).orElseThrow().getStatus()).isEqualTo(VehicleStatus.RENTED);
        assertThat(driverIsAvailable()).isFalse();
    }

    @Test
    void staffReviewKeepsOriginalReportAndCompletesOnlyOnce() {
        rentalService.processPickup(pickupRequest(first));
        ReturnRequest request = new ReturnRequest();
        request.setReservationId(first.getId());
        request.setStaffId(staff.getId());
        request.setFinalMileage(1160);
        request.setFinalFuelLevel("Full");
        request.setRemarks("Staff corrected reading");
        assertThatThrownBy(() -> rentalService.processReturn(request)).hasMessageContaining("Wait for the driver");
        driverReturns.submit(new DriverReturnRequest(first.getId(), 1150, "1/2", "None", "Driver notes"), driver.getEmail());
        request.setDamageFee(new BigDecimal("-1"));
        assertThatThrownBy(() -> rentalService.processReturn(request)).hasMessageContaining("cannot be negative");
        assertThat(reports.findByReservationId(first.getId()).orElseThrow().getStatus()).isEqualTo(DriverReturnReport.Status.SUBMITTED);
        request.setDamageFee(BigDecimal.ZERO);
        ReturnRecord record = rentalService.processReturn(request);
        assertThat(record.getGrandTotal()).isEqualByComparingTo("17300");
        DriverReturnReport report = reports.findByReservationId(first.getId()).orElseThrow();
        assertThat(report.getFinalMileage()).isEqualTo(1150);
        assertThat(report.getRemarks()).isEqualTo("Driver notes");
        assertThat(report.getStatus()).isEqualTo(DriverReturnReport.Status.APPROVED);
        assertThat(report.getReviewedBy().getId()).isEqualTo(staff.getId());
        assertThat(report.getReviewedAt()).isNotNull();
        assertThat(record.getReturnTime()).isEqualTo(report.getSubmittedAt());
        assertThat(driverReturns.activeTrips(driver.getEmail())).isEmpty();
        assertThat(driverIsAvailable()).isTrue();
        assertThatThrownBy(() -> rentalService.processReturn(request)).hasMessageContaining("Only active trips");
        reservationService.deleteCompletedReservation(first.getId());
        assertThat(reports.findByReservationId(first.getId())).isEmpty();
        assertThat(returns.findByReservationId(first.getId())).isEmpty();
    }

    @Test
    void httpPermissionsAndAuthenticatedReviewerAreEnforced() throws Exception {
        rentalService.processPickup(pickupRequest(first));
        String body = "{\"reservationId\":" + first.getId() + ",\"finalMileage\":1150,\"finalFuelLevel\":\"Full\"}";
        mvc.perform(post("/api/driver-return-reports").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/driver-return-reports").session(session(staff)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/returns").session(session(driver)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/driver-return-reports").session(session(driver)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(get("/api/driver-return-reports").session(session(users.findByRole(Role.CUSTOMER).get(0)))).andExpect(status().isForbidden());
        // A forged staffId must be ignored in favour of the logged-in reviewer.
        mvc.perform(post("/api/returns").session(session(staff)).contentType("application/json")
                .content(body.replace("}", ",\"staffId\":" + driver.getId() + "}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.staff.id").value(staff.getId()));
    }

    private ReturnRequest maintenanceReturn(boolean required) {
        rentalService.processPickup(pickupRequest(first));
        driverReturns.submit(new DriverReturnRequest(first.getId(), 1050, "Full", "Bumper scratch", "Returned"), driver.getEmail());
        ReturnRequest request = new ReturnRequest();
        request.setReservationId(first.getId());
        request.setStaffId(staff.getId());
        request.setFinalMileage(1050);
        request.setFinalFuelLevel("Full");
        request.setDamagesFound("Bumper scratch");
        request.setDamageFee(new BigDecimal("1500"));
        request.setMaintenanceRequired(required);
        request.setMaintenanceDescription("Repair rear bumper scratch");
        return request;
    }

    @Test
    void maintenanceBlocksRentalsUntilFleetCompletesRepairAndKeepsHistory() {
        ReturnRecord returned = rentalService.processReturn(maintenanceReturn(true));
        var maintenance = maintenanceRequests.findByReturnRecordId(returned.getId()).orElseThrow();
        String manager = users.findByRole(Role.FLEET_MANAGER).get(0).getEmail();
        Long vehicleId = first.getVehicle().getId();
        assertThat(maintenance.getStatus()).isEqualTo(MaintenanceRequest.Status.PENDING);
        assertThat(returned.getGrandTotal()).isEqualByComparingTo("14000");
        assertThat(driverIsAvailable()).isTrue();
        assertThat(vehicles.findById(vehicleId).orElseThrow().getStatus()).isEqualTo(VehicleStatus.UNDER_MAINTENANCE);
        assertThat(vehicleService.isVehicleAvailableForDates(vehicleId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(11))).isFalse();
        assertThatThrownBy(() -> vehicleService.updateStatus(vehicleId, VehicleStatus.AVAILABLE)).hasMessageContaining("Complete the open repair");
        Vehicle edited = vehicles.findById(vehicleId).orElseThrow();
        edited.setStatus(VehicleStatus.AVAILABLE);
        assertThatThrownBy(() -> vehicleService.updateVehicle(vehicleId, edited)).hasMessageContaining("Complete the open repair");
        second.setVehicle(vehicles.findById(vehicleId).orElseThrow());
        reservations.save(second);
        assertThatThrownBy(() -> rentalService.processPickup(pickupRequest(second))).hasMessageContaining("under maintenance");
        assertThatThrownBy(() -> maintenanceService.complete(maintenance.getId(), "Repaired", manager)).hasMessageContaining("Start the repair");
        maintenanceService.start(maintenance.getId(), manager);
        assertThatThrownBy(() -> maintenanceService.start(maintenance.getId(), manager)).hasMessageContaining("Only pending");
        assertThatThrownBy(() -> maintenanceService.complete(maintenance.getId(), " ", manager)).hasMessageContaining("repair notes");
        assertThat(vehicles.findById(vehicleId).orElseThrow().getStatus()).isEqualTo(VehicleStatus.UNDER_MAINTENANCE);
        var completed = maintenanceService.complete(maintenance.getId(), "Bumper repaired and checked", manager);
        assertThat(completed.getStatus()).isEqualTo(MaintenanceRequest.Status.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getRepairNotes()).isEqualTo("Bumper repaired and checked");
        assertThat(vehicles.findById(vehicleId).orElseThrow().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThatThrownBy(() -> maintenanceService.complete(maintenance.getId(), "Again", manager)).hasMessageContaining("cannot be completed again");
        assertThatThrownBy(() -> reservationService.deleteCompletedReservation(first.getId())).hasMessageContaining("maintenance history");
        assertThatThrownBy(() -> rentalService.deleteReturn(returned.getId())).hasMessageContaining("maintenance history");
        assertThat(maintenanceRequests.findById(maintenance.getId())).isPresent();
    }

    @Test
    void staffMustDescribeMaintenanceAndUncheckedReturnDoesNotTriggerKeywordRule() {
        ReturnRequest request = maintenanceReturn(true);
        request.setMaintenanceDescription(" ");
        assertThatThrownBy(() -> rentalService.processReturn(request)).hasMessageContaining("Describe the required maintenance");
        assertThat(returns.findByReservationId(first.getId())).isEmpty();
        assertThat(reports.findByReservationId(first.getId()).orElseThrow().getStatus()).isEqualTo(DriverReturnReport.Status.SUBMITTED);
        request.setMaintenanceRequired(false);
        request.setDamagesFound("No damage");
        request.setDamageFee(new BigDecimal("6000"));
        ReturnRecord returned = rentalService.processReturn(request);
        assertThat(maintenanceRequests.findByReturnRecordId(returned.getId())).isEmpty();
        assertThat(vehicles.findById(first.getVehicle().getId()).orElseThrow().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void maintenanceHttpPermissionsAllowOnlyFleetToRepair() throws Exception {
        ReturnRecord returned = rentalService.processReturn(maintenanceReturn(true));
        var maintenance = maintenanceRequests.findByReturnRecordId(returned.getId()).orElseThrow();
        String path = "/api/maintenance/" + maintenance.getId();
        User manager = users.findByRole(Role.FLEET_MANAGER).get(0);
        mvc.perform(get("/api/maintenance")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/maintenance").session(session(driver))).andExpect(status().isForbidden());
        mvc.perform(get("/api/maintenance").session(session(staff))).andExpect(status().isOk());
        mvc.perform(patch(path + "/start").session(session(staff))).andExpect(status().isForbidden());
        mvc.perform(patch(path + "/start").session(session(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(patch(path + "/complete").session(session(staff)).contentType("application/json").content("{\"repairNotes\":\"Repaired\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(path + "/complete").session(session(manager)).contentType("application/json").content("{\"repairNotes\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(path + "/complete").session(session(manager)).contentType("application/json").content("{\"repairNotes\":\"Bumper repaired\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.vehicle.status").value("AVAILABLE"));
    }

    private MockHttpSession session(User user) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private boolean attemptPickup(Reservation reservation, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out");
        try {
            rentalService.processPickup(pickupRequest(reservation));
            return true;
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("already on an active trip");
            return false;
        }
    }
}
