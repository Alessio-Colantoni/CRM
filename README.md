# CRM

CRM è una web application per la gestione di clienti, recapiti, offerte commerciali, interazioni e appuntamenti. L'applicazione riunisce un frontend Vue, API REST Spring Boot e un database MySQL basato su query, trigger e stored procedure.

Il sistema applica ruoli distinti per operatori, segreteria e amministratori e viene distribuito come un singolo servizio Java, senza un processo Node separato.

## Web application

L'istanza pubblica è disponibile su:

**[https://crm-operativo.onrender.com](https://crm-operativo.onrender.com)**

L'endpoint `GET /api/health` controlla anche la connessione al database.

## Funzionalità

- Gestione dei clienti, dei dati anagrafici, dei telefoni e degli indirizzi email.
- Registrazione di interazioni e appuntamenti.
- Gestione delle offerte disponibili e di quelle accettate dai clienti.
- Generazione di report per intervallo di date.
- Gestione degli utenti e dei ruoli applicativi.

## Architettura

| Livello | Tecnologie | Responsabilità |
| --- | --- | --- |
| Frontend | Vue 3, HTML, CSS | SPA, navigazione per ruolo e chiamate alle API |
| Backend | Java 17, Spring Boot 3.3, Spring MVC | API REST, autenticazione e autorizzazione |
| Accesso dati | JDBC, DAO, HikariCP | Pool di connessioni, query e stored procedure |
| Database | MySQL | Dati, relazioni, trigger e regole applicative |
| Esecuzione | Jar Java o Docker | Avvio su server e piattaforme cloud |

Il frontend è incluso nel jar tramite WebJar e risorse statiche. Le route `/`, `/app` e `/app/**` vengono inoltrate alla SPA.

Le sessioni sono conservate nella memoria della singola istanza applicativa e vengono perse al riavvio; per questo il servizio deve usare una sola replica.

### Sicurezza e credenziali

Le password devono contenere almeno otto caratteri e vengono salvate come hash BCrypt con costo `12`. I token Bearer sono generati con `SecureRandom` usando 32 byte casuali, scadono dopo otto ore e vengono revocati dopo logout, cambio password, modifica del ruolo o eliminazione dell'utente.

La SPA conserva il token in `localStorage` per mantenere la sessione dopo un aggiornamento della pagina; la password non viene mai salvata nel browser. Per ridurre l'esposizione del token, frontend e API sono serviti dalla stessa origine, Vue è incluso nel jar senza script esterni e il servizio deve essere raggiunto esclusivamente tramite HTTPS.

Autorizzazioni e ruoli vengono verificati dal backend prima di ogni operazione protetta. Dopo cinque credenziali errate per la stessa combinazione di username e indirizzo client in quindici minuti, l'accesso viene bloccato per quindici minuti. Le query usano parametri JDBC o stored procedure, mentre `CONNECTION_URL`, `DB_USER`, `DB_PASSWORD` e le credenziali iniziali devono essere configurati nell'ambiente del server e non nel repository. Per connessioni remote l'URL JDBC deve richiedere TLS.

## Ruoli e flussi operativi

### Operatore

L'operatore può:

- consultare offerte e clienti;
- cercare un cliente e aprirne la scheda;
- consultare telefoni, interazioni e offerte accettate;
- registrare o eliminare interazioni e appuntamenti;
- registrare o rimuovere offerte accettate.

### Segreteria

La segreteria può:

- consultare, cercare, creare, aggiornare ed eliminare clienti;
- aggiungere telefoni e indirizzi email;
- consultare gli indirizzi delle sedi;
- generare report su interazioni e offerte accettate.

### Amministratore

L'amministratore può:

- creare, consultare, aggiornare ed eliminare utenti;
- assegnare i ruoli `operatore`, `segreteria` e `amministratore`;
- creare e consultare le offerte;
- modificarne la disponibilità o eliminare quelle non ancora accettate.

## Configurazione

### Requisiti

- JDK 17 o successivo.
- Maven 3.9 o compatibile.
- MySQL 8 o un servizio compatibile con trigger e stored procedure.
- Docker, facoltativo, per eseguire l'applicazione in container.

### Variabili d'ambiente

Il file `.env.example` contiene solo segnaposto, non deve essere compilato con valori reali e l'applicazione non lo carica automaticamente.

| Variabile | Obbligatoria | Default | Uso |
| --- | --- | --- | --- |
| `CONNECTION_URL` | Sì | Nessuno | URL JDBC MySQL completo |
| `DB_USER` | Sì | Nessuno | Utente tecnico del database |
| `DB_PASSWORD` | Sì | Nessuno | Password dell'utente tecnico |
| `PORT` | No | `8080` | Porta HTTP dell'applicazione |
| `BOOTSTRAP_ADMIN_USERNAME` | Per il primo account | Nessuno | Username dell'amministratore iniziale |
| `BOOTSTRAP_ADMIN_PASSWORD` | Per il primo account | Nessuno | Password iniziale di almeno otto caratteri |
| `BOOTSTRAP_ADMIN_NAME` | No | `Initial` | Nome dell'amministratore iniziale |
| `BOOTSTRAP_ADMIN_SURNAME` | No | `Admin` | Cognome dell'amministratore iniziale |

Esempio di URL:

```text
jdbc:mysql://HOST:PORT/CRM?sslMode=REQUIRED
```

### Database e primo amministratore

`db.sql` crea lo schema `CRM`, le tabelle, gli indici, i trigger, le stored procedure e un insieme di dati dimostrativi senza credenziali applicative.

Eseguire lo script con un account MySQL autorizzato a creare lo schema e le relative strutture:

```bash
mysql -h HOST -P PORTA -u UTENTE -p < db.sql
```

Al primo avvio impostare le variabili `BOOTSTRAP_ADMIN_*`. L'account viene creato soltanto se non esiste già; in seguito le variabili di bootstrap possono essere rimosse.

## Avvio locale

Clonare il repository:

```bash
git clone https://github.com/Alessio-Colantoni/CRM.git
cd CRM
```

Inizializzare MySQL con `db.sql`, quindi esportare la configurazione:

```bash
export CONNECTION_URL='jdbc:mysql://HOST:PORT/CRM?sslMode=REQUIRED'
export DB_USER='crm_app'
export DB_PASSWORD='password-database'
export BOOTSTRAP_ADMIN_USERNAME='initial-admin'
export BOOTSTRAP_ADMIN_PASSWORD='password-iniziale-sicura'
export BOOTSTRAP_ADMIN_NAME='Nome'
export BOOTSTRAP_ADMIN_SURNAME='Cognome'
```

Avviare l'applicazione:

```bash
mvn spring-boot:run
```

La web application sarà disponibile su `http://localhost:8080`.

## Installazione su server

Il repository include un `Dockerfile` multi-stage. Il servizio richiede un database MySQL esterno raggiungibile dal container e deve essere esposto tramite HTTPS.

### Esempio Render

1. Inizializzare un database MySQL esterno con `db.sql`.
2. Creare un Blueprint dal repository oppure un Web Service basato sul `Dockerfile`.
3. Configurare `CONNECTION_URL`, `DB_USER` e `DB_PASSWORD`.
4. Per il primo avvio, aggiungere le variabili `BOOTSTRAP_ADMIN_*`.
5. Usare `/api/health` come Health Check Path.
6. Dopo la creazione dell'amministratore, rimuovere le variabili di bootstrap.

Il file `render.yaml` configura il servizio Docker e l'health check, ma non crea il database MySQL.

## Struttura del repository

| Percorso | Contenuto |
| --- | --- |
| `src/it/bd/controller/` | Controller REST e inoltro alla SPA |
| `src/it/bd/model/dao/` | DAO JDBC, query e stored procedure |
| `src/it/bd/model/domain/` | Modelli di dominio |
| `src/it/bd/model/service/` | Sessioni, limitazione degli accessi e bootstrap |
| `src/main/resources/static/` | Frontend Vue, HTML e CSS |
| `src/main/resources/application.properties` | Configurazione Spring Boot |
| `db.sql` | Schema, trigger, procedure e dati dimostrativi |
| `.env.example` | Modello delle variabili d'ambiente |
| `pom.xml` | Dipendenze e configurazione Maven |
| `Dockerfile` | Build e runtime Java |
| `render.yaml` | Configurazione opzionale per Render |
