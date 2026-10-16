-- Demo logins: admin@example.com / Admin123! and user@example.com / User123! (BCrypt, cost 10)
INSERT INTO app_users (email, password_hash, full_name, role) VALUES
    ('admin@example.com', '$2a$10$PDfcZELcrCn/rsVgLaPqZeTHTpb8i4N8OGBk8jeh0gtJzygsLvTbu', 'Demo Admin', 'ADMIN'),
    ('user@example.com',  '$2a$10$9uNaYdl4s/WA7JeYMtQ0i.3WP8s0pHAvlhwqRHOPCIRzcAW3Wvwq6', 'Demo User',  'USER');
