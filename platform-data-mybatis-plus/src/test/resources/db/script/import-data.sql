INSERT INTO department (name) VALUES ('Engineering');
INSERT INTO department (name) VALUES ('Finance');

INSERT INTO t_user (username, is_enabled, status) VALUES ('user_a', TRUE, 1);
INSERT INTO t_user (username, is_enabled, status) VALUES ('user_b', TRUE, 1);
INSERT INTO t_user (username, is_enabled, status) VALUES ('user_c', FALSE, 2);
INSERT INTO t_user (username, is_enabled, status) VALUES ('user_d', FALSE, 1);

INSERT INTO role (role_name, parent_role_id) VALUES ('ADMIN', NULL);
INSERT INTO role (role_name, parent_role_id) VALUES ('USER', 1);
INSERT INTO role (role_name, parent_role_id) VALUES ('VIEWER', 2);

INSERT INTO user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO user_role (user_id, role_id) VALUES (1, 2);
INSERT INTO user_role (user_id, role_id) VALUES (2, 2);
INSERT INTO user_role (user_id, role_id) VALUES (3, 3);

INSERT INTO user_department (user_id, dept_id) VALUES (1, 1);
INSERT INTO user_department (user_id, dept_id) VALUES (1, 2);
INSERT INTO user_department (user_id, dept_id) VALUES (2, 2);
INSERT INTO user_department (user_id, dept_id) VALUES (3, 1);
INSERT INTO user_department (user_id, dept_id) VALUES (4, 2);

INSERT INTO menu (name, meta) VALUES ('菜单1', '{"title":"用户管理1","icon":"EyeFilled","show":true,"requiresAuth":true}');
INSERT INTO menu (name, meta) VALUES ('菜单2', '{"title":"用户管理2","icon":"EyeFilled","show":true,"requiresAuth":true}');
INSERT INTO menu (name, meta) VALUES ('菜单3', '{"title":"用户管理3","icon":"EyeFilled","show":true,"requiresAuth":true}');
INSERT INTO menu (name, meta) VALUES ('菜单4', '{"title":"用户管理4","icon":"EyeFilled","show":true,"requiresAuth":true}');
