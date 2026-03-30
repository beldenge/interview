package com.interview.controller;

import com.interview.entity.FuelType;
import com.interview.entity.Vehicle;
import com.interview.repository.VehicleRepository;
import com.interview.support.EnabledIfDockerAvailable;
import com.interview.support.PostgresContainerConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfDockerAvailable
@SpringBootTest(properties = "spring.h2.console.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresContainerConfiguration.class)
@Transactional
class VehicleApiTest {
    private static final String PASSWORD = "password";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String OWNER_ONE_EMAIL = "owner1@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void loginReturnsAccessTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson(OWNER_ONE_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.role").value("VEHICLE_OWNER"));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson(OWNER_ONE_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void getVehiclesReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentication is required"));
    }

    @Test
    void ownerListReturnsOnlyOwnedVehicles() throws Exception {
        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL))
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void adminListReturnsAllVehicles() throws Exception {
        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(ADMIN_EMAIL))
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[2].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void ownerCanGetOwnVehicle() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.vin").value("JTDB4MEE9L1234566"));
    }

    @Test
    void ownerCannotGetAnotherOwnersVehicle() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Resource not found"));
    }

    @Test
    void createVehiclePersistsVehicleForAuthenticatedOwner() throws Exception {
        String responseBody = mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequestJson(2024, "Subaru", "Outback", "Green", "ABC124", "JTDB4MEE9L1234568", "GASOLINE", 4, 12000)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.modelYear").value(2024))
                .andExpect(jsonPath("$.make").value("Subaru"))
                .andExpect(jsonPath("$.model").value("Outback"))
                .andExpect(jsonPath("$.vin").value("JTDB4MEE9L1234568"))
                .andExpect(jsonPath("$.fuelType").value("GASOLINE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long createdId = JsonPath.parse(responseBody).read("$.id", Long.class);
        Vehicle createdVehicle = vehicleRepository.findById(createdId).orElseThrow();

        assertThat(createdVehicle.getOwner().getId()).isEqualTo(2L);
        assertThat(createdVehicle.getMake()).isEqualTo("Subaru");
        assertThat(createdVehicle.getModel()).isEqualTo("Outback");
        assertThat(createdVehicle.getModelYear()).isEqualTo(2024);
        assertThat(createdVehicle.getFuelType()).isEqualTo(FuelType.GASOLINE);
    }

    @Test
    void createVehicleReturnsValidationErrorsForInvalidRequest() throws Exception {
        String invalidVehicleRequestJson = """
            {
              "modelYear": 2024,
              "make": " ",
              "model": "Outback",
              "color": "Green",
              "licensePlate": "ABC124",
              "vin": "BADVIN",
              "doors": 4,
              "mileage": -1
            }
            """;

        mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidVehicleRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("make", "vin", "fuelType", "mileage")));
    }

    @Test
    void createVehicleReturnsConflictWhenVinAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequestJson(2024, "Subaru", "Outback", "Green", "ABC124", "JTDB4MEE9L1234566", "GASOLINE", 4, 12000)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"));
    }

    @Test
    void ownerCanUpdateOwnedVehicle() throws Exception {
        mockMvc.perform(put("/api/vehicles/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequestJson(2025, "Tesla", "Model Y", "Blue", "EV2025", "5YJ3E1EA0MF123457", "ELECTRIC", 5, 2500)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.modelYear").value(2025))
                .andExpect(jsonPath("$.make").value("Tesla"))
                .andExpect(jsonPath("$.model").value("Model Y"))
                .andExpect(jsonPath("$.vin").value("5YJ3E1EA0MF123457"))
                .andExpect(jsonPath("$.fuelType").value("ELECTRIC"));

        Vehicle updatedVehicle = vehicleRepository.findById(1L).orElseThrow();
        assertThat(updatedVehicle.getOwner().getId()).isEqualTo(2L);
        assertThat(updatedVehicle.getModelYear()).isEqualTo(2025);
        assertThat(updatedVehicle.getMake()).isEqualTo("Tesla");
        assertThat(updatedVehicle.getModel()).isEqualTo("Model Y");
        assertThat(updatedVehicle.getColor()).isEqualTo("Blue");
        assertThat(updatedVehicle.getLicensePlate()).isEqualTo("EV2025");
        assertThat(updatedVehicle.getVin()).isEqualTo("5YJ3E1EA0MF123457");
        assertThat(updatedVehicle.getFuelType()).isEqualTo(FuelType.ELECTRIC);
        assertThat(updatedVehicle.getDoors()).isEqualTo(5);
        assertThat(updatedVehicle.getMileage()).isEqualTo(2500);
    }

    @Test
    void ownerCannotUpdateAnotherOwnersVehicle() throws Exception {
        mockMvc.perform(put("/api/vehicles/{id}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequestJson(2025, "Tesla", "Model Y", "Blue", "EV2025", "5YJ3E1EA0MF123457", "ELECTRIC", 5, 2500)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Resource not found"));

        Vehicle unchangedVehicle = vehicleRepository.findById(2L).orElseThrow();
        assertThat(unchangedVehicle.getModelYear()).isEqualTo(2018);
        assertThat(unchangedVehicle.getMake()).isEqualTo("Honda");
        assertThat(unchangedVehicle.getModel()).isEqualTo("Civic");
        assertThat(unchangedVehicle.getVin()).isEqualTo("2HGFC2F69JH123456");
    }

    @Test
    void ownerCannotDeleteAnotherOwnersVehicle() throws Exception {
        mockMvc.perform(delete("/api/vehicles/{id}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Resource not found"));

        assertThat(vehicleRepository.findById(2L)).isPresent();
    }

    @Test
    void ownerCanDeleteOwnedVehicle() throws Exception {
        mockMvc.perform(delete("/api/vehicles/{id}", 3L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(OWNER_ONE_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(vehicleRepository.findById(3L)).isEmpty();
    }

    private String bearerTokenFor(String email) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.parse(responseBody).read("$.accessToken", String.class);
        return "Bearer " + accessToken;
    }

    private static String loginRequestJson(String email, String password) {
        return """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);
    }

    private static String vehicleRequestJson(
            int modelYear,
            String make,
            String model,
            String color,
            String licensePlate,
            String vin,
            String fuelType,
            int doors,
            int mileage
    ) {
        return """
            {
              "modelYear": %d,
              "make": "%s",
              "model": "%s",
              "color": "%s",
              "licensePlate": "%s",
              "vin": "%s",
              "fuelType": "%s",
              "doors": %d,
              "mileage": %d
            }
            """.formatted(modelYear, make, model, color, licensePlate, vin, fuelType, doors, mileage);
    }
}
