-- Seed a multi-role user with venue-scoped manager permissions
-- Ensure required roles are present for assignments
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_MANAGER') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT DO NOTHING;

-- Ensure the Downtown Theater venue exists for manager scoping
INSERT INTO venues (id, name, location, description)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Downtown Theater',
    'New York, NY',
    'Historic theater in downtown Manhattan'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, display_name, created_at)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    'purple.manager@example.com',
    'Go Wildcats!',
    NOW()
)
ON CONFLICT (email) DO NOTHING;

UPDATE users
SET display_name = 'Go Wildcats!'
WHERE email = 'purple.manager@example.com';

-- Attach base user role
INSERT INTO user_roles (user_id, role_id, venue_id)
SELECT u.id, r.id, NULL
FROM users u
JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.email = 'purple.manager@example.com'
ON CONFLICT (user_id, role_id, venue_id) DO NOTHING;

-- Attach admin role for elevated access
INSERT INTO user_roles (user_id, role_id, venue_id)
SELECT u.id, r.id, NULL
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.email = 'purple.manager@example.com'
ON CONFLICT (user_id, role_id, venue_id) DO NOTHING;

-- Attach manager role scoped to the Downtown Theater venue
INSERT INTO user_roles (user_id, role_id, venue_id)
SELECT u.id, r.id, v.id
FROM users u
JOIN roles r ON r.name = 'ROLE_MANAGER'
JOIN venues v ON v.id = '11111111-1111-1111-1111-111111111111'
WHERE u.email = 'purple.manager@example.com'
ON CONFLICT (user_id, role_id, venue_id) DO NOTHING;
