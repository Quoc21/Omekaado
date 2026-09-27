CREATE USER omekaado_migrator WITH PASSWORD 'mig!Q2w3e4r';

CREATE USER omekaado_app WITH PASSWORD 'app!Q2w3e4r';

CREATE DATABASE omekaado OWNER omekaado_migrator;

\c omekaado

GRANT CONNECT ON DATABASE omekaado TO omekaado_app;
GRANT USAGE ON SCHEMA public TO omekaado_app;

ALTER DEFAULT PRIVILEGES FOR ROLE omekaado_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO omekaado_app;

ALTER DEFAULT PRIVILEGES FOR ROLE omekaado_migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO omekaado_app;