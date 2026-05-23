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

INSERT INTO project (project_name, project_code, owner_id) VALUES ('Alpha Project', 'ALPHA', 1);
INSERT INTO project (project_name, project_code, owner_id) VALUES ('Beta Project', 'BETA', 2);

INSERT INTO task (task_name, project_id, created_by, created_date, updated_by, updated_date) VALUES ('Task 1', 1, 'system', '2026-05-23 00:00:00','system', '2026-05-23 00:00:00');
INSERT INTO task (task_name, project_id) VALUES ('Task 2', 1);
INSERT INTO task (task_name, project_id) VALUES ('Task 3', 2);
