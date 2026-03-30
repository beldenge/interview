INSERT INTO app_user (id, email, password_hash, role, enabled)
VALUES
    (1, 'admin@example.com', '$2a$10$2WR6y9dvJE4E5jv2oGrhAeQ8PYw0sz4QTgAEqcUYOMwMyOlOm6R0a', 'ADMIN', TRUE),
    (2, 'owner1@example.com', '$2a$10$2WR6y9dvJE4E5jv2oGrhAeQ8PYw0sz4QTgAEqcUYOMwMyOlOm6R0a', 'VEHICLE_OWNER', TRUE),
    (3, 'owner2@example.com', '$2a$10$2WR6y9dvJE4E5jv2oGrhAeQ8PYw0sz4QTgAEqcUYOMwMyOlOm6R0a', 'VEHICLE_OWNER', TRUE);

INSERT INTO vehicle (id, owner_user_id, model_year, make, model, color, license_plate, vin, fuel_type, doors, mileage)
VALUES
    (1, 2, 2020, 'Toyota', 'Corolla', 'Silver', 'ABC123', 'JTDB4MEE9L1234566', 'GASOLINE', 4, 45000),
    (2, 3, 2018, 'Honda', 'Civic', 'Blue', 'XYZ789', '2HGFC2F69JH123456', 'HYBRID', 4, 72000),
    (3, 2, 2023, 'Tesla', 'Model 3', 'Red', 'TKMTRC', '5YJ3E1EA0MF123456', 'ELECTRIC', 4, 15000);
