CREATE TABLE IF NOT EXISTS user(
    id uuid,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT user_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS degree(
    id uuid,
    name VARCHAR(128) NOT NULL,
    CONSTRAINT degree_pk PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS user_degree(
    user_id uuid REFERENCES user(id),
    degree_id uuid REFERENCES degree(id),
    CONSTRAINT user_degree_pk PRIMARY KEY(user_id, degree_id)
);

CREATE TABLE IF NOT EXISTS address (
    id       UUID NOT NULL,
    street   VARCHAR(255),
    city     VARCHAR(255),
    province VARCHAR(255),
    CONSTRAINT pk_address PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS employee (
    id         UUID NOT NULL,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    salary     DOUBLE,
    start      TIMESTAMP,
    birthday   TIMESTAMP,
    address_id UUID,
    manager_id UUID,
    CONSTRAINT pk_employee PRIMARY KEY (id)
);

ALTER TABLE employee
    ADD CONSTRAINT IF NOT EXISTS FK_EMPLOYEE_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES address (id);

ALTER TABLE employee
    ADD CONSTRAINT IF NOT EXISTS FK_EMPLOYEE_ON_MANAGER FOREIGN KEY (manager_id) REFERENCES employee (id);

CREATE TABLE IF NOT EXISTS phone (
    id        UUID NOT NULL,
    type      VARCHAR(255),
    number    VARCHAR(255),
    area_code VARCHAR(255),
    owner_id  UUID,
    CONSTRAINT pk_phone PRIMARY KEY (id)
);

ALTER TABLE phone
    ADD CONSTRAINT IF NOT EXISTS FK_PHONE_ON_OWNER FOREIGN KEY (owner_id) REFERENCES employee (id);

CREATE TABLE project (
    id     UUID NOT NULL,
    name   VARCHAR(255),
    budget DOUBLE,
    dtype VARCHAR(255) NOT NULL,
    CONSTRAINT pk_project PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS employee_project (
    employee_id uuid REFERENCES employee(id),
    project_id uuid REFERENCES project(id),
    CONSTRAINT employee_project_pk PRIMARY KEY(employee_id, project_id)
);