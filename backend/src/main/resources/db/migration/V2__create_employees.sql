CREATE TABLE employees (
    id         BIGSERIAL     PRIMARY KEY,
    first_name VARCHAR(100)  NOT NULL,
    last_name  VARCHAR(100)  NOT NULL,
    email      VARCHAR(255)  NOT NULL,
    department VARCHAR(100)  NOT NULL,
    job_title  VARCHAR(100)  NOT NULL,
    salary     NUMERIC(12,2) NOT NULL CHECK (salary > 0),
    hire_date  DATE          NOT NULL,
    status     VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED')),
    version    BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_employees_email ON employees (lower(email));
CREATE INDEX ix_employees_last_name ON employees (last_name);
CREATE INDEX ix_employees_department ON employees (department);
CREATE INDEX ix_employees_hire_date ON employees (hire_date);
