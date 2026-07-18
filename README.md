# CRM

CRM è una web application per la gestione di clienti, contatti, offerte commerciali, interazioni e appuntamenti. Il progetto rappresenta l'evoluzione di una precedente applicazione Java/JDBC più semplice, utilizzata tramite client desktop, verso un sistema web centralizzato e distribuibile.

La versione attuale mantiene il livello DAO e le stored procedure del progetto originario, aggiungendo API REST Spring Boot, una single-page application Vue, autenticazione applicativa con ruoli, pooling delle connessioni JDBC e distribuzione tramite JVM o container Docker.

## Web application

L'istanza pubblica di riferimento è disponibile all'indirizzo:

**[https://crm-operativo.onrender.com](https://crm-operativo.onrender.com)**

Il primo accesso richiede credenziali applicative provisionate tramite il bootstrap iniziale oppure create da un amministratore già autenticato. L'endpoint di controllo dello stato è `GET /api/health`.

## Funzionalità

Il sistema offre le seguenti funzioni principali:

- autenticazione applicativa e sessioni bearer con durata massima di otto ore;
- autorizzazione lato backend per i ruoli `OPERATORE`, `SEGRETERIA` e `AMMINISTRATORE`;
- cambio password con revoca delle sessioni attive;
- consultazione, ricerca, creazione, aggiornamento ed eliminazione dei clienti secondo il ruolo assegnato;
- gestione di telefoni e indirizzi email dei clienti;
- registrazione e consultazione delle interazioni;
- creazione ed eliminazione di appuntamenti associati alle interazioni;
- registrazione e rimozione delle offerte accettate;
- gestione amministrativa del catalogo offerte;
- gestione amministrativa degli utenti e dei relativi ruoli;
- generazione di report sulle attività svolte in un intervallo di date;
- controlli transazionali e vincoli applicativi implementati nel database;
- health check applicativo con verifica della disponibilità del database.

## Architettura

L'applicazione è composta da un unico servizio Spring Boot organizzato secondo il pattern MVC: i controller espongono le API e inoltrano le route, il model contiene dominio, servizi, sicurezza e DAO, mentre la View è la SPA Vue distribuita dalle risorse statiche.

| Livello | Tecnologia | Responsabilità |
| --- | --- | --- |
| Frontend | Vue 3, HTML e CSS | Interfaccia SPA, navigazione per ruolo, chiamate alle API e gestione dello stato locale |
| API | Spring Boot Web | Endpoint REST, validazione dei flussi, autorizzazione e traduzione degli errori |
| Autenticazione | Token bearer in memoria | Creazione, verifica, scadenza e revoca delle sessioni applicative |
| Accesso dati | JDBC, DAO e HikariCP | Esecuzione delle query e delle stored procedure mediante un pool di connessioni |
| Database | MySQL | Persistenza, relazioni, trigger, transazioni e procedure applicative |
| Distribuzione | JVM 17 o Docker | Esecuzione portabile su workstation, server o piattaforme cloud |

Il frontend Vue è incluso nel jar tramite WebJar e viene servito da Spring Boot. Non esiste un processo Node separato e il browser non dipende da una CDN esterna. Le route `/`, `/app` e `/app/**` vengono inoltrate alla SPA.

### Accesso al database

La web application usa un solo utente tecnico MySQL, configurato tramite variabili d'ambiente. I permessi degli utenti applicativi non dipendono dall'account MySQL: vengono verificati dal backend prima di eseguire ciascuna operazione protetta.

`ConnectionFactory` crea un pool HikariCP condiviso. Ogni DAO prende una connessione dal pool e la restituisce automaticamente al termine dell'operazione. Le funzioni di dominio restano concentrate nei DAO e nelle stored procedure `sp_*` definite in `db.sql`.

Le operazioni che coinvolgono più scritture, come la creazione di utenti, il cambio password, il bootstrap amministratore e diversi flussi SQL, sono eseguite in transazione con rollback in caso di errore.

### Autenticazione e sessioni

Un login riuscito restituisce:

```json
{
  "token": "token-di-sessione",
  "username": "utente",
  "role": "OPERATORE"
}
```

Le richieste protette devono inviare il token nel seguente header:

```http
Authorization: Bearer token-di-sessione
```

Le password sono memorizzate come hash BCrypt. Le sessioni sono conservate in memoria per un massimo di otto ore e vengono revocate dopo cambio password, modifica del ruolo o eliminazione dell'utente. Cinque login falliti, per la stessa combinazione di utente e indirizzo client nell'arco di quindici minuti, causano una risposta HTTP `429` e un blocco temporaneo di quindici minuti.

La configurazione nativa degli header inoltrati consente a Tomcat di ricostruire l'indirizzo client quando l'applicazione è dietro un reverse proxy riconosciuto.

Le sessioni in memoria rendono la configurazione adatta a una singola istanza applicativa. Un'installazione con più repliche richiede uno store di sessione condiviso oppure un meccanismo di autenticazione stateless.

## Ruoli e flussi operativi

### Operatore

L'operatore può:

- visualizzare le offerte disponibili;
- consultare l'elenco clienti e ricercarli per nome e cognome;
- aprire la scheda dettagliata di un cliente;
- consultare telefoni, interazioni e offerte accettate;
- registrare un'interazione;
- registrare un'interazione con appuntamento;
- eliminare un appuntamento o un'interazione;
- registrare e rimuovere un'offerta accettata.

Il database richiede che il cliente abbia almeno un numero di telefono prima di registrare un'interazione o un'offerta accettata. La data di un appuntamento deve essere successiva alla data dell'interazione associata.

### Segreteria

La segreteria può:

- consultare e ricercare i clienti;
- creare un nuovo cliente;
- aggiornare i dati anagrafici e l'indirizzo;
- aggiungere un numero di telefono o un indirizzo email;
- eliminare un cliente e i dati collegati gestiti dalla relativa procedura;
- generare report su interazioni e offerte accettate per intervallo di date.

### Amministratore

L'amministratore può:

- creare utenti scegliendo username, ruolo e password iniziale;
- consultare e aggiornare gli utenti;
- modificare i ruoli, con revoca delle sessioni dell'utente interessato;
- eliminare utenti che non risultino associati a offerte accettate;
- creare e consultare le offerte;
- modificare la disponibilità o eliminare offerte non ancora accettate.

Per evitare di perdere l'accesso amministrativo durante una sessione, il backend impedisce all'amministratore autenticato di eliminare il proprio account o modificarne il ruolo.

## Configurazione

### Requisiti

Per compilare ed eseguire il progetto senza Docker sono necessari:

- Java Development Kit 17 o successivo;
- Maven 3.9 o compatibile;
- un database MySQL 8 o compatibile con trigger e stored procedure;
- un utente MySQL con accesso allo schema `CRM`;
- Git, se il progetto viene clonato dal repository.

Per l'esecuzione tramite container è sufficiente un runtime Docker compatibile con build multi-stage. Node.js non è richiesto perché il frontend è già incluso nelle risorse dell'applicazione Java.

### Variabili d'ambiente

L'applicazione non contiene credenziali del database e non usa un collegamento locale predefinito. La connessione viene letta esclusivamente dalle variabili d'ambiente.

| Variabile | Obbligatoria | Valore predefinito | Descrizione |
| --- | --- | --- | --- |
| `CONNECTION_URL` | Sì | Nessuno | URL JDBC completo, ad esempio `jdbc:mysql://HOST:PORT/CRM?sslMode=REQUIRED` |
| `DB_USER` | Sì | Nessuno | Utente tecnico MySQL usato dal pool JDBC |
| `DB_PASSWORD` | Sì | Nessuno | Password dell'utente tecnico MySQL |
| `PORT` | No | `8080` | Porta HTTP esposta da Spring Boot |
| `BOOTSTRAP_ADMIN_USERNAME` | Solo per il bootstrap | Nessuno | Username dell'amministratore iniziale |
| `BOOTSTRAP_ADMIN_PASSWORD` | Solo per la prima creazione | Nessuno | Password iniziale dell'amministratore; deve contenere almeno otto caratteri |
| `BOOTSTRAP_ADMIN_NAME` | No | `Initial` | Nome dell'amministratore creato dal bootstrap |
| `BOOTSTRAP_ADMIN_SURNAME` | No | `Admin` | Cognome dell'amministratore creato dal bootstrap |

Esempio di configurazione:

```bash
export CONNECTION_URL='jdbc:mysql://mysql.example.com:3306/CRM?sslMode=REQUIRED'
export DB_USER='crm_app'
export DB_PASSWORD='password-database'
export PORT='8080'
```

Il file `.env.example` contiene esclusivamente valori segnaposto. L'applicazione non carica automaticamente file `.env`: le variabili devono essere esportate nella shell o configurate nel servizio di hosting.

### Bootstrap del primo amministratore

Su un database privo di utenti applicativi, configurare al primo avvio almeno:

```bash
export BOOTSTRAP_ADMIN_USERNAME='initial-admin'
export BOOTSTRAP_ADMIN_PASSWORD='password-iniziale-sicura'
export BOOTSTRAP_ADMIN_NAME='Nome'
export BOOTSTRAP_ADMIN_SURNAME='Cognome'
```

Il backend crea l'account soltanto se non esiste e non reimposta la password di un amministratore già presente. Dopo il provisioning è possibile:

- rimuovere tutte le variabili `BOOTSTRAP_ADMIN_*`; oppure
- lasciare `BOOTSTRAP_ADMIN_USERNAME` e rimuovere la password: all'avvio il backend verifica che username, credenziali e ruolo `amministratore` siano coerenti.

Se rimane soltanto la username ma l'amministratore non esiste, oppure se i dati sono incoerenti, l'avvio viene interrotto per evitare una configurazione incompleta.

### Inizializzazione del database

`db.sql` contiene:

- creazione dello schema `CRM` e delle tabelle;
- chiavi primarie, chiavi esterne e indici;
- trigger per i principali vincoli applicativi;
- stored procedure per clienti, interazioni, appuntamenti, offerte e report;
- un dataset dimostrativo privo di credenziali applicative.

Per inizializzare un database:

```bash
mysql -h HOST -P PORTA -u UTENTE -p < db.sql
```

L'utente che esegue l'inizializzazione deve disporre dei permessi necessari per creare e modificare schema, tabelle, trigger, indici e routine. L'utente tecnico usato a runtime deve invece poter eseguire almeno le operazioni `SELECT`, `INSERT`, `UPDATE`, `DELETE` ed `EXECUTE` richieste dall'applicazione.

Il dataset usa `INSERT IGNORE` per non sovrascrivere i record dimostrativi già presenti. `db.sql` è uno script completo di inizializzazione, non un sistema di migrazioni: prima di applicarlo a un database esistente è necessario eseguire un backup e valutare singolarmente le modifiche a schema e procedure.

## Avvio locale

### 1. Clonazione

```bash
git clone https://github.com/Alessio-Colantoni/CRM.git
cd CRM
```

La stessa release è pubblicata anche nel mirror
[`CRM_Operativo`](https://github.com/Alessio-Colantoni/CRM_Operativo).

### 2. Preparazione del database

Creare o scegliere un'istanza MySQL, eseguire `db.sql` e configurare l'utente tecnico. Non è previsto alcun fallback automatico verso `localhost`.

### 3. Configurazione dell'ambiente

Esportare `CONNECTION_URL`, `DB_USER` e `DB_PASSWORD`. Se il database è nuovo, aggiungere anche le variabili del bootstrap amministratore.

### 4. Test e compilazione

```bash
mvn test
mvn package
```

Il jar eseguibile viene generato in:

```text
target/client_java-2.0.0.jar
```

### 5. Avvio

```bash
java -jar target/client_java-2.0.0.jar
```

Aprire quindi:

```text
http://localhost:8080
```

In alternativa è possibile avviare direttamente il progetto con:

```bash
mvn spring-boot:run
```

## Installazione e distribuzione

L'applicazione non dipende dalle API di uno specifico provider. Il contratto di esecuzione è composto da:

- una JVM Java 17 oppure un runtime Docker;
- le variabili d'ambiente documentate;
- una porta HTTP;
- un database MySQL raggiungibile;
- terminazione HTTPS fornita dall'host o da un reverse proxy.

### Distribuzione come jar su qualsiasi host JVM

1. Eseguire `mvn test`.
2. Generare l'artefatto con `mvn package`.
3. Copiare `target/client_java-2.0.0.jar` sul server.
4. Installare una runtime Java 17.
5. Configurare le variabili d'ambiente.
6. Avviare `java -jar client_java-2.0.0.jar` tramite il process manager del sistema.
7. Esporre la porta configurata in `PORT` tramite HTTPS.
8. Configurare `GET /api/health` come health check.

### Distribuzione con Docker

Eseguire prima i test, perché il `Dockerfile` produce l'immagine senza rieseguire la suite:

```bash
mvn test
docker build -t crm-operativo .
```

Avviare il container:

```bash
docker run --rm \
  -p 8080:8080 \
  -e CONNECTION_URL='jdbc:mysql://HOST:PORT/CRM?sslMode=REQUIRED' \
  -e DB_USER='crm_app' \
  -e DB_PASSWORD='password-database' \
  crm-operativo
```

Per il primo provisioning aggiungere allo stesso comando le variabili `BOOTSTRAP_ADMIN_*`.

### Distribuzione su una piattaforma cloud

Su qualunque piattaforma che supporti immagini Docker o applicazioni Java:

1. collegare il repository oppure pubblicare l'immagine in un registry;
2. scegliere il `Dockerfile` o il jar come artefatto di avvio;
3. configurare le variabili del database come secret;
4. impostare le variabili del bootstrap soltanto per la prima creazione dell'amministratore;
5. esporre la porta indicata da `PORT`;
6. configurare `/api/health` come health check;
7. completato il provisioning, rimuovere la password bootstrap;
8. mantenere una singola replica finché le sessioni restano in memoria.

Questi passaggi si applicano, ad esempio, a Railway, Fly.io, Google Cloud Run, AWS, Azure e altri host compatibili con Docker o JVM.

### Esempio Render

Il repository include `render.yaml` come configurazione opzionale per un Web Service Docker. Il file dichiara il `Dockerfile`, l'health check `/api/health` e le tre variabili obbligatorie del database.

Per un nuovo deploy Render:

1. collegare il repository GitHub a un Web Service o creare un Blueprint da `render.yaml`;
2. configurare `CONNECTION_URL`, `DB_USER` e `DB_PASSWORD`;
3. aggiungere temporaneamente le variabili `BOOTSTRAP_ADMIN_*` se il database non contiene ancora un amministratore;
4. avviare il deploy;
5. verificare `/api/health` e il login;
6. rimuovere `BOOTSTRAP_ADMIN_PASSWORD` dopo il provisioning.

Render è soltanto l'host dell'istanza pubblica di riferimento; il codice applicativo e il database non usano API proprietarie Render.

## Test e verifiche

La suite automatica si esegue con:

```bash
mvn test
```

I test attuali verificano:

- hashing e controllo delle password BCrypt;
- rifiuto delle password troppo corte;
- creazione, ricerca e revoca delle sessioni;
- revoca di tutte le sessioni associate a un utente;
- blocco temporaneo dopo ripetuti login falliti;
- separazione dei tentativi per utente e indirizzo client;
- risposta HTTP `429` quando il login è bloccato.

Per verificare anche la creazione del jar eseguire:

```bash
mvn package
```

Il frontend non richiede una build Node. Se Node.js è disponibile, il file JavaScript principale può essere controllato sintatticamente con:

```bash
node --check src/main/resources/static/app.js
```

Le operazioni DAO e le stored procedure richiedono inoltre una verifica di integrazione contro un'istanza MySQL inizializzata con `db.sql`.

## Struttura del repository

| Percorso | Contenuto |
| --- | --- |
| `src/it/bd/CrmWebApplication.java` | Entry point Spring Boot |
| `src/it/bd/controller/` | Controller REST e inoltro delle route alla SPA |
| `src/it/bd/model/dao/` | DAO JDBC, query e chiamate alle stored procedure |
| `src/it/bd/model/domain/` | Modelli di dominio |
| `src/it/bd/model/service/` | Sessioni, limitazione dei login e bootstrap amministratore |
| `src/it/bd/model/security/` | Hashing e verifica BCrypt |
| `src/main/resources/static/` | Frontend Vue, HTML e CSS |
| `src/main/resources/application.properties` | Porta e gestione degli header del reverse proxy |
| `tests/` | Test automatici JUnit |
| `db.sql` | Schema, trigger, procedure, indici e dati dimostrativi |
| `.env.example` | Esempio delle variabili d'ambiente senza credenziali reali |
| `pom.xml` | Dipendenze e configurazione Maven |
| `Dockerfile` | Build multi-stage e immagine runtime Java |
| `render.yaml` | Esempio opzionale di deploy Render |

## Portabilità e limiti operativi

- Le sessioni sono locali alla singola istanza applicativa e vengono perse a ogni riavvio.
- Il repository non include un sistema di migrazioni incrementali del database.
- La suite automatica non sostituisce i test di integrazione con MySQL.
- Il frontend è una SPA statica senza una pipeline Node dedicata.

Questi vincoli mantengono il progetto compatto e coerente con l'evoluzione dell'applicazione originaria, ma devono essere considerati prima di introdurre replica orizzontale, migrazioni automatiche o una pipeline frontend separata.
