INSERT INTO test_department (name) VALUES ('Engineering');
INSERT INTO test_department (name) VALUES ('Finance');

INSERT INTO test_user (username, is_enabled, status) VALUES ('user_a', TRUE, 1);
INSERT INTO test_user (username, is_enabled, status) VALUES ('user_b', TRUE, 1);
INSERT INTO test_user (username, is_enabled, status) VALUES ('user_c', FALSE, 2);
INSERT INTO test_user (username, is_enabled, status) VALUES ('user_d', FALSE, 1);

INSERT INTO test_role (role_name, parent_role_id) VALUES ('ADMIN', NULL);
INSERT INTO test_role (role_name, parent_role_id) VALUES ('USER', 1);
INSERT INTO test_role (role_name, parent_role_id) VALUES ('VIEWER', 2);

INSERT INTO test_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO test_user_role (user_id, role_id) VALUES (1, 2);
INSERT INTO test_user_role (user_id, role_id) VALUES (2, 2);
INSERT INTO test_user_role (user_id, role_id) VALUES (3, 3);

INSERT INTO test_user_department (user_id, dept_id) VALUES (1, 1);
INSERT INTO test_user_department (user_id, dept_id) VALUES (1, 2);
INSERT INTO test_user_department (user_id, dept_id) VALUES (2, 2);
INSERT INTO test_user_department (user_id, dept_id) VALUES (3, 1);
INSERT INTO test_user_department (user_id, dept_id) VALUES (4, 2);
