package com.carrental.config;

import com.carrental.model.*;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PickupRecordRepository pickupRecordRepository;

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        // 1. Seed Users
        User admin = new User(null, "Admin Manager", "admin@carrental.lk", "admin123", "+94 77 123 4567", "DL-AD-9921", Role.ADMIN);
        User fleetMgr = new User(null, "Fleet Supervisor", "fleet@carrental.lk", "fleet123", "+94 71 234 5678", "DL-FM-8843", Role.FLEET_MANAGER);
        User staff = new User(null, "Rental Desk Officer", "staff@carrental.lk", "staff123", "+94 76 345 6789", "DL-ST-7732", Role.STAFF);
        User driver = new User(null, "Kamal Fernando (Driver)", "driver@carrental.lk", "driver123", "+94 70 456 7890", "DL-DR-6612", Role.DRIVER);
        User customer = new User(null, "Venuka Samadith", "customer@carrental.lk", "customer123", "+94 78 567 8901", "DL-CU-5590", Role.CUSTOMER);

        userRepository.save(admin);
        userRepository.save(fleetMgr);
        userRepository.save(staff);
        userRepository.save(driver);
        userRepository.save(customer);

        // 2. Seed the four vehicles with the supplied image URLs.
        Vehicle v1 = new Vehicle(null, "Toyota", "Prius Hybrid", 2022, "Sedan",
                "WP CAR-4521", new BigDecimal("14500.00"), 5, 3, "Hybrid", "Automatic",
                32400, "Full", VehicleStatus.AVAILABLE,
                "https://hips.hearstapps.com/hmg-prod/amv-prod-cad-assets/images/10q2/339152/toyota-prius-2012-toyota-prius-plug-in-hybrid-review-car-and-driver-photo-343306-s-original.jpg",
                "Push Start, Dual Climate Control, Lane Assist, Apple CarPlay, Reverse Camera");

        Vehicle v2 = new Vehicle(null, "Honda", "Vezel RS", 2023, "SUV",
                "WP CAY-8832", new BigDecimal("18500.00"), 5, 4, "Hybrid", "Automatic",
                21050, "Full", VehicleStatus.AVAILABLE,
                "https://motorguide-store.s3.ap-southeast-1.amazonaws.com/ikman/631764441_1495229385935907_333522725063162050_n_c4604b2ebc.jpg",
                "Panoramic Sunroof, Cruise Control, Leather Seats, LED Headlights, Honda Sensing");

        Vehicle v4 = new Vehicle(null, "Toyota", "HiAce KDH Super G", 2021, "Van",
                "WP PB-5520", new BigDecimal("22000.00"), 10, 8, "Diesel", "Automatic",
                68000, "3/4", VehicleStatus.RENTED,
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRV9afD9DSlEm1PVuY8XOQmrKxLtpeHuCuqIxL1_CB_8VPKGvzpFg2TIs8&s=10",
                "Dual A/C, Adjustable Reclining Seats, High Roof, Bluetooth Audio, Extra Luggage Space");
        v4.setAssignedDriver(driver);

        Vehicle v5 = new Vehicle(null, "Suzuki", "Wagon R FX", 2021, "Economy",
                "WP CAH-6712", new BigDecimal("7500.00"), 4, 2, "Hybrid", "Automatic",
                41200, "Full", VehicleStatus.AVAILABLE,
                "https://www.indratraders.lk/wp-content/uploads/2025/12/WhatsApp-Image-2026-06-08-at-10.28.04-1.webp",
                "Exceptional Fuel Efficiency, Keyless Entry, Power Shutters, Touchscreen Display");

        vehicleRepository.save(v1);
        vehicleRepository.save(v2);
        vehicleRepository.save(v4);
        vehicleRepository.save(v5);

        // 3. Seed Initial Reservations
        Reservation r1 = new Reservation(null, "RES-804125", customer, v4,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2),
                "Colombo Fort", "Katunayake Airport (BIA)", true, 3,
                v4.getRentalPricePerDay(), new BigDecimal("73500.00"),
                ReservationStatus.PICKED_UP, "Airport VIP Drop & 3-day Tour");
        reservationRepository.save(r1);

        Reservation r2 = new Reservation(null, "RES-792134", customer, v1,
                LocalDate.now().minusDays(7), LocalDate.now().minusDays(4),
                "Kandy Branch", "Kandy Branch", true, 3,
                v1.getRentalPricePerDay(), new BigDecimal("51000.00"),
                ReservationStatus.COMPLETED, "Weekend Hill Country Trip");
        reservationRepository.save(r2);

        Reservation r3 = new Reservation(null, "RES-910452", customer, v2,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(6),
                "Colombo Hub", "Galle Fort", true, 3,
                v2.getRentalPricePerDay(), new BigDecimal("63000.00"),
                ReservationStatus.CONFIRMED, "Southern Coast Holiday");
        reservationRepository.save(r3);

        // 4. Seed Pickup for r1
        PickupRecord p1 = new PickupRecord(null, r1, staff, driver,
                LocalDateTime.now().minusDays(1), 67900, "Full",
                "Vehicle in pristine condition. All documents verified.", "DL-CU-5590", true);
        pickupRecordRepository.save(p1);

        // 5. Seed Return for r2
        ReturnRecord ret1 = new ReturnRecord(null, r2, staff,
                LocalDateTime.now().minusDays(4), 32400, "Full",
                "None. Returned in spotless condition.",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("43500.00"), "Smooth rental, customer returned on time.");
        returnRecordRepository.save(ret1);

    }
}
