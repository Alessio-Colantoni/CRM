CREATE SCHEMA IF NOT EXISTS CRM;
USE CRM;

CREATE TABLE IF NOT EXISTS CRM.Credenziali (
    Username VARCHAR(45) PRIMARY KEY NOT NULL,
    Password VARCHAR(100) NOT NULL
);

ALTER TABLE CRM.Credenziali
MODIFY Password VARCHAR(100) NOT NULL;

CREATE TABLE IF NOT EXISTS CRM.Sede (
    Indirizzo VARCHAR(255) PRIMARY KEY NOT NULL
);

CREATE TABLE IF NOT EXISTS CRM.Cliente (
    CF CHAR(16) PRIMARY KEY NOT NULL,
    DataNascita DATE NOT NULL,
    Nome VARCHAR(45) NOT NULL,
    Cognome VARCHAR(45) NOT NULL,
    DataRegistrazione DATE NOT NULL,
    DataUltimaInterazione DATE,
    IndResidenza VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS CRM.Utente (
    ID VARCHAR(45) PRIMARY KEY NOT NULL,
    Ruolo ENUM('segreteria', 'operatore', 'amministratore') NOT NULL,
    Nome VARCHAR(45) NOT NULL,
    Cognome VARCHAR(45) NOT NULL,
    CONSTRAINT FK_UTENTE_CREDENZIALI
    FOREIGN KEY (ID) REFERENCES CRM.Credenziali(Username)
);

CREATE TABLE IF NOT EXISTS CRM.Offerta (
    Nome VARCHAR(45) PRIMARY KEY NOT NULL,
    Descrizione TEXT,
    Disponibile BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS CRM.Interazione (
    CodInterazione INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    Nota TEXT NOT NULL,
    Data DATE NOT NULL,
    Cliente CHAR(16) NOT NULL,
    CONSTRAINT FK_INTERAZIONE_CLIENTE
    FOREIGN KEY (Cliente) REFERENCES CRM.Cliente(CF)
);

CREATE TABLE IF NOT EXISTS CRM.Appuntamento (
    CodAllegato INT PRIMARY KEY NOT NULL,
    Sede VARCHAR(255) NOT NULL,
    Data DATE NOT NULL,
    Ora TIME NOT NULL,
    CONSTRAINT FK_APPUNTAMENTO_INTERAZIONE
    FOREIGN KEY (CodAllegato) REFERENCES CRM.Interazione(CodInterazione),
    CONSTRAINT FK_APPUNTAMENTO_SEDE
    FOREIGN KEY (Sede) REFERENCES CRM.Sede(Indirizzo)
);

CREATE TABLE IF NOT EXISTS CRM.Email (
    IndirizzoEmail VARCHAR(45) PRIMARY KEY NOT NULL,
    Cliente CHAR(16) NOT NULL,
    CONSTRAINT FK_EMAIL_CLIENTE
    FOREIGN KEY (Cliente) REFERENCES CRM.Cliente(CF)
);

CREATE TABLE IF NOT EXISTS CRM.Telefono (
    Numero VARCHAR(45) PRIMARY KEY NOT NULL,
    Cliente CHAR(16) NOT NULL,
    CONSTRAINT FK_TELEFONO_CLIENTE
    FOREIGN KEY (Cliente) REFERENCES CRM.Cliente(CF)
);

CREATE TABLE IF NOT EXISTS CRM.OffertaAccettata (
    Cliente CHAR(16) NOT NULL,
    Offerta VARCHAR(45) NOT NULL,
    Utente VARCHAR(45) NOT NULL,
    Data DATE NOT NULL,
    PRIMARY KEY (Cliente, Offerta, Data),
    CONSTRAINT FK_OA_CLIENTE
    FOREIGN KEY (Cliente) REFERENCES CRM.Cliente(CF),
    CONSTRAINT FK_OA_UTENTE
    FOREIGN KEY (Utente) REFERENCES CRM.Utente(ID),
    CONSTRAINT FK_OA_OFFERTA
    FOREIGN KEY (Offerta) REFERENCES CRM.Offerta(Nome)
);

DROP TRIGGER IF EXISTS CRM.check_telefonoCliente_interazione;
DELIMITER //
CREATE TRIGGER CRM.check_telefonoCliente_interazione
BEFORE INSERT ON CRM.Interazione
FOR EACH ROW
BEGIN
    DECLARE num_telefoni INT;
    SELECT COUNT(*) INTO num_telefoni FROM CRM.Telefono WHERE Cliente = NEW.Cliente;
    IF num_telefoni = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Impossibile registrare un\'interazione con un cliente che non ha un numero di telefono.';
    END IF;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS CRM.check_telefonoCliente_offerta;
DELIMITER //
CREATE TRIGGER CRM.check_telefonoCliente_offerta
BEFORE INSERT ON CRM.OffertaAccettata
FOR EACH ROW
BEGIN
    DECLARE num_telefoni INT;
    SELECT COUNT(*) INTO num_telefoni FROM CRM.Telefono WHERE Cliente = NEW.Cliente;
    IF num_telefoni = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Impossibile registrare un\'offerta accettata per un cliente che non ha un numero di telefono.';
    END IF;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS CRM.check_dataAppuntamento;
DELIMITER //
CREATE TRIGGER CRM.check_dataAppuntamento
BEFORE INSERT ON CRM.Appuntamento
FOR EACH ROW
BEGIN
    DECLARE data_interazione DATE;
    SELECT Data INTO data_interazione FROM CRM.Interazione WHERE CodInterazione = NEW.CodAllegato;
    IF NEW.Data <= data_interazione THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La data dell\'appuntamento deve essere successiva alla data dell\'interazione associata.';
    END IF;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.S1;

DROP PROCEDURE IF EXISTS CRM.S2;
DROP PROCEDURE IF EXISTS CRM.sp_create_customer;
DELIMITER //
CREATE PROCEDURE sp_create_customer(
    IN c_CF CHAR(16),
    IN c_data_nascita DATE,
    IN c_nome VARCHAR(45),
    IN c_cognome VARCHAR(45),
    IN c_data_registrazione DATE,
    IN c_indirizzo_residenza VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO CRM.Cliente(CF, DataNascita, Nome, Cognome, DataRegistrazione, IndResidenza)
    VALUES (c_CF, c_data_nascita, c_nome, c_cognome, c_data_registrazione, c_indirizzo_residenza);
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.sp_update_customer;
DELIMITER //
CREATE PROCEDURE sp_update_customer(
    IN c_CF CHAR(16),
    IN c_data_nascita DATE,
    IN c_nome VARCHAR(45),
    IN c_cognome VARCHAR(45),
    IN c_indirizzo_residenza VARCHAR(255),
    IN c_numero_telefono VARCHAR(45),
    IN c_indirizzo_email VARCHAR(45)
)
BEGIN
    DECLARE cliente_trovato CHAR(16) DEFAULT NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT CF INTO cliente_trovato
    FROM CRM.Cliente
    WHERE CF = c_CF
    FOR UPDATE;

    IF cliente_trovato IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cliente non trovato.';
    END IF;

    UPDATE CRM.Cliente
    SET DataNascita = c_data_nascita,
        Nome = c_nome,
        Cognome = c_cognome,
        IndResidenza = c_indirizzo_residenza
    WHERE CF = c_CF;

    IF NULLIF(TRIM(c_numero_telefono), '') IS NOT NULL THEN
        INSERT INTO CRM.Telefono(Numero, Cliente)
        VALUES (TRIM(c_numero_telefono), c_CF);
    END IF;

    IF NULLIF(TRIM(c_indirizzo_email), '') IS NOT NULL THEN
        INSERT INTO CRM.Email(IndirizzoEmail, Cliente)
        VALUES (TRIM(c_indirizzo_email), c_CF);
    END IF;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.sp_delete_customer;
DELIMITER //
CREATE PROCEDURE sp_delete_customer(IN c_CF CHAR(16))
BEGIN
    DECLARE cliente_trovato CHAR(16) DEFAULT NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT CF INTO cliente_trovato
    FROM CRM.Cliente
    WHERE CF = c_CF
    FOR UPDATE;

    IF cliente_trovato IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cliente non trovato.';
    END IF;

    DELETE A
    FROM CRM.Appuntamento AS A
    INNER JOIN CRM.Interazione AS I ON I.CodInterazione = A.CodAllegato
    WHERE I.Cliente = c_CF;

    DELETE FROM CRM.OffertaAccettata WHERE Cliente = c_CF;
    DELETE FROM CRM.Email WHERE Cliente = c_CF;
    DELETE FROM CRM.Telefono WHERE Cliente = c_CF;
    DELETE FROM CRM.Interazione WHERE Cliente = c_CF;
    DELETE FROM CRM.Cliente WHERE CF = c_CF;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.S3;
DROP PROCEDURE IF EXISTS CRM.sp_generate_customer_report;
DELIMITER //
CREATE PROCEDURE sp_generate_customer_report(
    IN r_data_inizio DATE,
    IN r_data_fine DATE,
    OUT r_numero_clienti_contattati INT
)
BEGIN
    SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
    START TRANSACTION;

    SELECT COUNT(*) INTO r_numero_clienti_contattati
    FROM (
        SELECT Cliente
        FROM CRM.Interazione
        WHERE Data BETWEEN r_data_inizio AND r_data_fine
        UNION
        SELECT Cliente
        FROM CRM.OffertaAccettata
        WHERE Data BETWEEN r_data_inizio AND r_data_fine
    ) AS ClientiConAttivita;

    SELECT
        C.Nome AS Nome,
        C.Cognome AS Cognome,
        COALESCE(NumInterazioni, 0) AS Interazioni,
        COALESCE(NumOfferteAccettate, 0) AS OfferteAccettate
    FROM CRM.Cliente AS C
    LEFT JOIN
        (SELECT I.Cliente, COUNT(*) AS NumInterazioni
         FROM CRM.Interazione AS I
         WHERE I.Data BETWEEN r_data_inizio AND r_data_fine
         GROUP BY I.Cliente) AS T1 ON T1.Cliente = C.CF
    LEFT JOIN
        (SELECT O.Cliente, COUNT(*) AS NumOfferteAccettate
         FROM CRM.OffertaAccettata AS O
         WHERE O.Data BETWEEN r_data_inizio AND r_data_fine
         GROUP BY O.Cliente) AS T2 ON T2.Cliente = C.CF
    WHERE COALESCE(NumInterazioni, 0) > 0
       OR COALESCE(NumOfferteAccettate, 0) > 0
    ORDER BY C.Cognome, C.Nome;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O1;
DROP PROCEDURE IF EXISTS CRM.sp_list_available_offers;
DELIMITER //
CREATE PROCEDURE sp_list_available_offers()
BEGIN
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT Nome, Descrizione
    FROM CRM.Offerta
    WHERE Disponibile = TRUE;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O2;
DROP PROCEDURE IF EXISTS CRM.sp_list_operator_customers;
DELIMITER //
CREATE PROCEDURE sp_list_operator_customers()
BEGIN
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT Nome, Cognome
    FROM CRM.Cliente
    ORDER BY DataRegistrazione IS NULL, DataRegistrazione ASC;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O3;
DROP PROCEDURE IF EXISTS CRM.sp_list_customer_interactions;
DELIMITER //
CREATE PROCEDURE sp_list_customer_interactions(IN c_CF CHAR(16))
BEGIN
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT I.CodInterazione AS CodInterazione, I.Nota AS Nota, I.Data AS DataInterazione, A.Sede AS IndirizzoAppuntamento,
           A.Data AS DataAppuntamento, A.Ora AS OraAppuntamento
    FROM CRM.Interazione AS I
    LEFT JOIN CRM.Appuntamento AS A ON A.CodAllegato = I.CodInterazione
    WHERE I.Cliente = c_CF
    ORDER BY I.Data DESC, I.CodInterazione DESC;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O7;
DROP PROCEDURE IF EXISTS CRM.sp_list_customer_accepted_offers;
DELIMITER //
CREATE PROCEDURE sp_list_customer_accepted_offers(IN o_cliente CHAR(16))
BEGIN
    SELECT Cliente, Offerta, Utente, Data
    FROM CRM.OffertaAccettata
    WHERE Cliente = o_cliente
    ORDER BY Data DESC, Offerta;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O8;
DROP PROCEDURE IF EXISTS CRM.sp_delete_customer_interaction;
DELIMITER //
CREATE PROCEDURE sp_delete_customer_interaction(
    IN i_codice_interazione INT,
    IN i_cliente CHAR(16)
)
BEGIN
    DECLARE cliente_interazione CHAR(16) DEFAULT NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    SELECT Cliente INTO cliente_interazione
    FROM CRM.Interazione
    WHERE CodInterazione = i_codice_interazione
    FOR UPDATE;

    IF cliente_interazione IS NULL OR cliente_interazione <> i_cliente THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Interazione non trovata per il cliente selezionato.';
    END IF;

    DELETE FROM CRM.Appuntamento
    WHERE CodAllegato = i_codice_interazione;

    DELETE FROM CRM.Interazione
    WHERE CodInterazione = i_codice_interazione;

    UPDATE CRM.Cliente
    SET DataUltimaInterazione = (
        SELECT MAX(Data)
        FROM CRM.Interazione
        WHERE Cliente = i_cliente
    )
    WHERE CF = i_cliente;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O9;
DROP PROCEDURE IF EXISTS CRM.sp_delete_customer_appointment;
DELIMITER //
CREATE PROCEDURE sp_delete_customer_appointment(
    IN i_codice_interazione INT,
    IN i_cliente CHAR(16)
)
BEGIN
    DECLARE cliente_interazione CHAR(16) DEFAULT NULL;
    DECLARE appuntamenti_rimossi INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    SELECT Cliente INTO cliente_interazione
    FROM CRM.Interazione
    WHERE CodInterazione = i_codice_interazione
    FOR UPDATE;

    IF cliente_interazione IS NULL OR cliente_interazione <> i_cliente THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Interazione non trovata per il cliente selezionato.';
    END IF;

    DELETE FROM CRM.Appuntamento
    WHERE CodAllegato = i_codice_interazione;
    SET appuntamenti_rimossi = ROW_COUNT();

    IF appuntamenti_rimossi = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Appuntamento non trovato.';
    END IF;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O10;
DROP PROCEDURE IF EXISTS CRM.sp_delete_customer_accepted_offer;
DELIMITER //
CREATE PROCEDURE sp_delete_customer_accepted_offer(
    IN o_cliente CHAR(16),
    IN o_offerta VARCHAR(45),
    IN o_data DATE
)
BEGIN
    DECLARE offerte_rimosse INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    DELETE FROM CRM.OffertaAccettata
    WHERE Cliente = o_cliente
      AND Offerta = o_offerta
      AND Data = o_data;
    SET offerte_rimosse = ROW_COUNT();

    IF offerte_rimosse = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Offerta accettata non trovata.';
    END IF;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O4;
DROP PROCEDURE IF EXISTS CRM.sp_create_interaction;
DELIMITER //
CREATE PROCEDURE sp_create_interaction(
    IN i_nota TEXT,
    IN i_data DATE,
    IN i_cliente CHAR(16)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO CRM.Interazione(Nota, Data, Cliente) VALUES (i_nota, i_data, i_cliente);
    UPDATE CRM.Cliente
    SET DataUltimaInterazione = CASE
        WHEN DataUltimaInterazione IS NULL OR i_data > DataUltimaInterazione THEN i_data
        ELSE DataUltimaInterazione
    END
    WHERE CF = i_cliente;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O5;
DROP PROCEDURE IF EXISTS CRM.sp_create_interaction_with_appointment;
DELIMITER //
CREATE PROCEDURE sp_create_interaction_with_appointment(
    IN i_nota TEXT,
    IN i_data DATE,
    IN i_cliente CHAR(16),
    IN a_sede VARCHAR(255),
    IN a_data DATE,
    IN a_ora TIME
)
BEGIN
    DECLARE cod_interazione INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO CRM.Interazione(Nota, Data, Cliente) VALUES (i_nota, i_data, i_cliente);
    SET cod_interazione = LAST_INSERT_ID();
    INSERT INTO CRM.Appuntamento(CodAllegato, Sede, Data, Ora) VALUES (cod_interazione, a_sede, a_data, a_ora);
    UPDATE CRM.Cliente
    SET DataUltimaInterazione = CASE
        WHEN DataUltimaInterazione IS NULL OR i_data > DataUltimaInterazione THEN i_data
        ELSE DataUltimaInterazione
    END
    WHERE CF = i_cliente;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.O6;
DROP PROCEDURE IF EXISTS CRM.sp_register_accepted_offer;
DELIMITER //
CREATE PROCEDURE sp_register_accepted_offer(
    IN o_cliente CHAR(16),
    IN o_offerta VARCHAR(45),
    IN o_utente VARCHAR(45),
    IN o_data DATE
)
BEGIN
    DECLARE offerta_disponibile BOOLEAN DEFAULT NULL;
    DECLARE offerta_gia_accettata BOOLEAN DEFAULT FALSE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT Disponibile INTO offerta_disponibile
    FROM CRM.Offerta
    WHERE Nome = o_offerta
    FOR UPDATE;

    IF offerta_disponibile IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Offerta non trovata.';
    END IF;

    IF offerta_disponibile = FALSE THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'L''offerta selezionata non e disponibile.';
    END IF;

    SELECT EXISTS(
        SELECT 1
        FROM CRM.OffertaAccettata
        WHERE Cliente = o_cliente
          AND Offerta = o_offerta
          AND Data = o_data
    ) INTO offerta_gia_accettata;

    IF offerta_gia_accettata THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Esiste gia un''offerta accettata per questo cliente, questa data e questa offerta.';
    END IF;

    INSERT INTO CRM.OffertaAccettata(Cliente, Offerta, Utente, Data) VALUES (o_cliente, o_offerta, o_utente, o_data);
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.CC;
DROP PROCEDURE IF EXISTS CRM.sp_search_customer_by_name;
DELIMITER //
CREATE PROCEDURE sp_search_customer_by_name(
    IN c_nome VARCHAR(45),
    IN c_cognome VARCHAR(45)
)
BEGIN
    SELECT CF, Nome, Cognome, DataNascita, DataRegistrazione, DataUltimaInterazione, IndResidenza
    FROM CRM.Cliente
    WHERE (NULLIF(TRIM(c_nome), '') IS NULL OR Nome LIKE CONCAT('%', TRIM(c_nome), '%'))
      AND (NULLIF(TRIM(c_cognome), '') IS NULL OR Cognome LIKE CONCAT('%', TRIM(c_cognome), '%'))
    ORDER BY Cognome, Nome, CF;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.TC;
DROP PROCEDURE IF EXISTS CRM.sp_list_customer_telephones;
DELIMITER //
CREATE PROCEDURE sp_list_customer_telephones(IN c_cf VARCHAR(45))
BEGIN
    SELECT T.Numero AS Numero
    FROM CRM.Telefono AS T JOIN CRM.Cliente AS C ON T.Cliente = C.CF
    WHERE C.CF = c_cf;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.MS;
DROP PROCEDURE IF EXISTS CRM.sp_list_office_addresses;
DELIMITER //
CREATE PROCEDURE sp_list_office_addresses()
BEGIN
    SELECT Indirizzo FROM CRM.Sede;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.AT;
DROP PROCEDURE IF EXISTS CRM.sp_add_customer_telephone;
DELIMITER //
CREATE PROCEDURE sp_add_customer_telephone(
    IN t_numero VARCHAR(45),
    IN t_cliente CHAR(16)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO CRM.Telefono(Numero, Cliente) VALUES (t_numero, t_cliente);
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.AE;
DROP PROCEDURE IF EXISTS CRM.sp_add_customer_email;
DELIMITER //
CREATE PROCEDURE sp_add_customer_email(
    IN e_email VARCHAR(45),
    IN e_cliente CHAR(16)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO CRM.Email(IndirizzoEmail, Cliente) VALUES (e_email, e_cliente);
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.AM1;
DROP PROCEDURE IF EXISTS CRM.sp_admin_create_offer;
DELIMITER //
CREATE PROCEDURE sp_admin_create_offer(
    IN off_nome VARCHAR(45),
    IN off_descrizione TEXT,
    IN off_disponibile BOOLEAN
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO CRM.Offerta(Nome, Descrizione, Disponibile) VALUES (off_nome, off_descrizione, off_disponibile);
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.AM2;
DROP PROCEDURE IF EXISTS CRM.sp_admin_set_offer_availability;
DELIMITER //
CREATE PROCEDURE sp_admin_set_offer_availability(
    IN off_nome VARCHAR(45),
    IN off_disponibile BOOLEAN
)
BEGIN
    DECLARE offerta_trovata VARCHAR(45) DEFAULT NULL;
    DECLARE offerte_accettate INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT Nome INTO offerta_trovata
    FROM CRM.Offerta
    WHERE Nome = off_nome
    FOR UPDATE;

    IF offerta_trovata IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Offerta non trovata.';
    END IF;

    SELECT COUNT(*) INTO offerte_accettate
    FROM CRM.OffertaAccettata
    WHERE Offerta = off_nome;

    IF offerte_accettate > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Impossibile modificare l''offerta: risulta gia accettata da uno o piu clienti.';
    END IF;

    UPDATE CRM.Offerta
    SET Disponibile = off_disponibile
    WHERE Nome = off_nome;
    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS CRM.AM3;
DROP PROCEDURE IF EXISTS CRM.sp_admin_delete_offer;
DELIMITER //
CREATE PROCEDURE sp_admin_delete_offer(IN off_nome VARCHAR(45))
BEGIN
    DECLARE offerta_trovata VARCHAR(45) DEFAULT NULL;
    DECLARE offerte_accettate INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    SELECT Nome INTO offerta_trovata
    FROM CRM.Offerta
    WHERE Nome = off_nome
    FOR UPDATE;

    IF offerta_trovata IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Offerta non trovata.';
    END IF;

    SELECT COUNT(*) INTO offerte_accettate
    FROM CRM.OffertaAccettata
    WHERE Offerta = off_nome;

    IF offerte_accettate > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Impossibile rimuovere l''offerta: risulta gia accettata da uno o piu clienti.';
    END IF;

    DELETE FROM CRM.Offerta
    WHERE Nome = off_nome;
    COMMIT;
END //
DELIMITER ;

SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'CRM'
      AND TABLE_NAME = 'Cliente'
      AND INDEX_NAME = 'Cliente_DataUltimaInterazione'
);
SET @create_index_sql = IF(
    @index_exists = 0,
    'CREATE INDEX Cliente_DataUltimaInterazione ON CRM.Cliente (DataUltimaInterazione)',
    'SELECT 1'
);
PREPARE create_index_stmt FROM @create_index_sql;
EXECUTE create_index_stmt;
DEALLOCATE PREPARE create_index_stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'CRM'
      AND TABLE_NAME = 'OffertaAccettata'
      AND INDEX_NAME = 'OffertaAccettata_Offerta'
);
SET @create_index_sql = IF(
    @index_exists = 0,
    'CREATE INDEX OffertaAccettata_Offerta ON CRM.OffertaAccettata (Offerta)',
    'SELECT 1'
);
PREPARE create_index_stmt FROM @create_index_sql;
EXECUTE create_index_stmt;
DEALLOCATE PREPARE create_index_stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'CRM'
      AND TABLE_NAME = 'Interazione'
      AND INDEX_NAME = 'Interazione_Cliente_Data'
);
SET @create_index_sql = IF(
    @index_exists = 0,
    'CREATE INDEX Interazione_Cliente_Data ON CRM.Interazione (Cliente, Data)',
    'SELECT 1'
);
PREPARE create_index_stmt FROM @create_index_sql;
EXECUTE create_index_stmt;
DEALLOCATE PREPARE create_index_stmt;

-- Permessi database
-- La webapp usa un unico utente tecnico configurato tramite variabili d'ambiente.
-- Questo script non crea utenti MySQL e non assegna GRANT applicativi.
-- L'utente tecnico deve avere i permessi necessari sullo schema CRM:
-- SELECT, INSERT, UPDATE, DELETE, EXECUTE e, per l'inizializzazione,
-- CREATE, ALTER, DROP, TRIGGER, CREATE ROUTINE e ALTER ROUTINE.
-- I ruoli applicativi sono verificati dal backend Spring.

START TRANSACTION;

-- Il dataset pubblico contiene solo dati CRM dimostrativi e non credenziali.
-- Al primo avvio il backend puo creare l'amministratore iniziale dalle
-- variabili BOOTSTRAP_ADMIN_*; gli altri utenti vengono creati dall'admin.

INSERT IGNORE INTO CRM.Sede (Indirizzo) VALUES
('Via Roma 10, Roma'),
('Corso Milano 25, Milano'),
('Via Toledo 88, Napoli'),
('Piazza Maggiore 4, Bologna');

INSERT IGNORE INTO CRM.Cliente (CF, DataNascita, Nome, Cognome, DataRegistrazione, DataUltimaInterazione, IndResidenza) VALUES
('RSSMRA85M01H501Z', '1985-08-01', 'Mario', 'Rossi', '2024-01-10', '2026-01-18', 'Via Appia 101, Roma'),
('BNCLGU90A41F205X', '1990-01-01', 'Luigi', 'Bianchi', '2024-02-14', '2026-01-20', 'Via Dante 12, Milano'),
('VRDLRA78C55F839Q', '1978-03-15', 'Laura', 'Verdi', '2024-03-05', '2026-02-02', 'Via Chiaia 7, Napoli'),
('NRIGPP92D12A944K', '1992-04-12', 'Giuseppe', 'Neri', '2024-04-22', '2026-02-10', 'Via Indipendenza 31, Bologna'),
('FRRSRA88L64H501B', '1988-07-24', 'Sara', 'Ferraro', '2024-05-18', '2026-02-14', 'Via Tuscolana 77, Roma'),
('CNTPLA95P18F205D', '1995-09-18', 'Paola', 'Conti', '2024-06-03', NULL, 'Viale Monza 140, Milano');

INSERT IGNORE INTO CRM.Offerta (Nome, Descrizione, Disponibile) VALUES
('Fibra Casa Plus', 'Connessione fibra fino a 1Gbps con modem incluso.', TRUE),
('Mobile 100GB', 'SIM mobile con minuti illimitati e 100GB mensili.', TRUE),
('Business CRM', 'Pacchetto servizi business con assistenza prioritaria.', TRUE),
('Assistenza Premium', 'Supporto tecnico prioritario sette giorni su sette.', TRUE),
('Vecchio Piano ADSL', 'Piano non piu commercializzato.', FALSE);

INSERT IGNORE INTO CRM.Telefono (Numero, Cliente) VALUES
('+390612345001', 'RSSMRA85M01H501Z'),
('+393331234501', 'RSSMRA85M01H501Z'),
('+390212345002', 'BNCLGU90A41F205X'),
('+393331234502', 'VRDLRA78C55F839Q'),
('+390512345003', 'NRIGPP92D12A944K'),
('+393331234503', 'FRRSRA88L64H501B'),
('+390212345004', 'CNTPLA95P18F205D');

INSERT IGNORE INTO CRM.Email (IndirizzoEmail, Cliente) VALUES
('mario.rossi@example.com', 'RSSMRA85M01H501Z'),
('luigi.bianchi@example.com', 'BNCLGU90A41F205X'),
('laura.verdi@example.com', 'VRDLRA78C55F839Q'),
('giuseppe.neri@example.com', 'NRIGPP92D12A944K'),
('sara.ferraro@example.com', 'FRRSRA88L64H501B'),
('paola.conti@example.com', 'CNTPLA95P18F205D');

INSERT IGNORE INTO CRM.Interazione (CodInterazione, Nota, Data, Cliente) VALUES
(1001, 'Cliente interessato a passare alla fibra; richiede verifica copertura.', '2026-01-18', 'RSSMRA85M01H501Z'),
(1002, 'Richiamare per proposta Mobile 100GB.', '2026-01-20', 'BNCLGU90A41F205X'),
(1003, 'Cliente business interessato ad assistenza premium.', '2026-02-02', 'VRDLRA78C55F839Q'),
(1004, 'Incontro fissato per valutazione offerta business.', '2026-02-10', 'NRIGPP92D12A944K'),
(1005, 'Cliente ha accettato invio preventivo fibra.', '2026-02-14', 'FRRSRA88L64H501B'),
(1006, 'Primo contatto informativo, nessuna offerta inviata.', '2026-02-18', 'CNTPLA95P18F205D');

INSERT IGNORE INTO CRM.Appuntamento (CodAllegato, Sede, Data, Ora) VALUES
(1001, 'Via Roma 10, Roma', '2026-01-25', '10:30:00'),
(1003, 'Via Toledo 88, Napoli', '2026-02-08', '15:00:00'),
(1004, 'Piazza Maggiore 4, Bologna', '2026-02-18', '11:15:00'),
(1006, 'Corso Milano 25, Milano', '2026-02-25', '09:45:00');

-- Le offerte accettate non vengono inserite nel dataset pubblico perche
-- devono fare riferimento a utenti applicativi creati in modo controllato.

UPDATE CRM.Cliente C
LEFT JOIN (
    SELECT Cliente, MAX(Data) AS UltimaInterazione
    FROM CRM.Interazione
    GROUP BY Cliente
) I ON I.Cliente = C.CF
SET C.DataUltimaInterazione = I.UltimaInterazione
WHERE C.CF IN (
    'RSSMRA85M01H501Z',
    'BNCLGU90A41F205X',
    'VRDLRA78C55F839Q',
    'NRIGPP92D12A944K',
    'FRRSRA88L64H501B',
    'CNTPLA95P18F205D'
);

COMMIT;
