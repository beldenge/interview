package com.interview.repository;

import com.interview.dto.VehicleSearchCriteria;
import com.interview.entity.AppUser;
import com.interview.entity.FuelType;
import com.interview.entity.UserRole;
import com.interview.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false"
})
class VehicleSpecificationTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser ownerOne;
    private AppUser ownerTwo;

    @BeforeEach
    void setUp() {
        ownerOne = appUserRepository.save(owner("owner1@example.com"));
        ownerTwo = appUserRepository.save(owner("owner2@example.com"));
        appUserRepository.flush();

        vehicleRepository.saveAllAndFlush(List.of(
                vehicle(ownerOne, "Toyota", "Corolla", 2020, "ABC123", "JTDB4MEE9L1234566", FuelType.GASOLINE),
                vehicle(ownerTwo, "Honda", "Civic", 2018, "XYZ789", "2HGFC2F69JH123456", FuelType.HYBRID),
                vehicle(ownerOne, "Tesla", "Model 3", 2023, "TKMTRC", "5YJ3E1EA0MF123456", FuelType.ELECTRIC),
                vehicle(ownerTwo, "Tesla", "Model 3", 2023, "TKMTRC2", "5YJ3E1EA0MF123457", FuelType.ELECTRIC)
        ));
    }

    @Test
    void fromCriteriaReturnsAllVehiclesWhenCriteriaIsNull() {
        List<Vehicle> results = vehicleRepository.findAll(VehicleSpecification.fromCriteria(null));

        assertThat(results)
                .hasSize(4)
                .extracting(Vehicle::getVin)
                .containsExactlyInAnyOrder(
                        "JTDB4MEE9L1234566",
                        "2HGFC2F69JH123456",
                        "5YJ3E1EA0MF123456",
                        "5YJ3E1EA0MF123457"
                );
    }

    @Test
    void fromCriteriaMapsYearToModelYearField() {
        VehicleSearchCriteria criteria = new VehicleSearchCriteria();
        criteria.setYear(2018);

        List<Vehicle> results = vehicleRepository.findAll(VehicleSpecification.fromCriteria(criteria));

        assertThat(results)
                .singleElement()
                .extracting(Vehicle::getVin)
                .isEqualTo("2HGFC2F69JH123456");
    }

    @Test
    void fromCriteriaIgnoresBlankStringFilters() {
        VehicleSearchCriteria criteria = new VehicleSearchCriteria();
        criteria.setMake("Toyota");
        criteria.setLicensePlate("   ");

        List<Vehicle> results = vehicleRepository.findAll(VehicleSpecification.fromCriteria(criteria));

        assertThat(results)
                .singleElement()
                .extracting(Vehicle::getVin)
                .isEqualTo("JTDB4MEE9L1234566");
    }

    @Test
    void fromCriteriaCombinesMultipleFiltersWithAnd() {
        VehicleSearchCriteria criteria = new VehicleSearchCriteria();
        criteria.setMake("Tesla");
        criteria.setModel("Model 3");
        criteria.setLicensePlate("TKMTRC");
        criteria.setYear(2023);

        List<Vehicle> results = vehicleRepository.findAll(VehicleSpecification.fromCriteria(criteria));

        assertThat(results)
                .singleElement()
                .extracting(Vehicle::getVin)
                .isEqualTo("5YJ3E1EA0MF123456");
    }

    @Test
    void ownedByFiltersVehiclesByOwnerId() {
        List<Vehicle> results = vehicleRepository.findAll(VehicleSpecification.ownedBy(ownerOne.getId()));

        assertThat(results)
                .hasSize(2)
                .extracting(Vehicle::getVin)
                .containsExactlyInAnyOrder(
                        "JTDB4MEE9L1234566",
                        "5YJ3E1EA0MF123456"
                );
    }

    private static AppUser owner(String email) {
        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("encoded");
        appUser.setRole(UserRole.VEHICLE_OWNER);
        appUser.setEnabled(true);
        return appUser;
    }

    private static Vehicle vehicle(
            AppUser owner,
            String make,
            String model,
            int modelYear,
            String licensePlate,
            String vin,
            FuelType fuelType
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(owner);
        vehicle.setModelYear(modelYear);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setColor("Test");
        vehicle.setLicensePlate(licensePlate);
        vehicle.setVin(vin);
        vehicle.setFuelType(fuelType);
        vehicle.setDoors(4);
        vehicle.setMileage(10000);
        return vehicle;
    }
}
