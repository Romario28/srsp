CREATE TABLE employe
(
    id_employe    BIGINT AUTO_INCREMENT NOT NULL,
    matricule     VARCHAR(20)           NOT NULL,
    nom           VARCHAR(100)          NOT NULL,
    prenom        VARCHAR(100)          NOT NULL,
    poste         VARCHAR(100)          NULL,
    departement   VARCHAR(100)          NULL,
    date_embauche date                  NULL,
    CONSTRAINT pk_employe PRIMARY KEY (id_employe)
);

ALTER TABLE employe
    ADD CONSTRAINT uc_employe_matricule UNIQUE (matricule);