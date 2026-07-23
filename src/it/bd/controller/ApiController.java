package it.bd.controller;

import it.bd.exception.DAOException;
import it.bd.model.dao.*;
import it.bd.model.domain.*;
import it.bd.model.service.AuthService;
import it.bd.model.service.LoginThrottleService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private final AuthService authService;
    private final LoginThrottleService loginThrottleService;

    public ApiController(AuthService authService, LoginThrottleService loginThrottleService) {
        this.authService = authService;
        this.loginThrottleService = loginThrottleService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        if (!ConnectionFactory.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "unavailable"));
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(HttpServletRequest httpRequest, @RequestBody LoginRequest request) throws DAOException, SQLException {
        String clientAddress = httpRequest.getRemoteAddr();
        if (loginThrottleService.isBlocked(request.username(), clientAddress)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Troppi tentativi di accesso. Riprova tra qualche minuto");
        }

        Credenziali credentials = execute(() -> new LoginProcedureDAO().execute(request.username(), request.password()));
        if (credentials.getRole() == null) {
            if (loginThrottleService.recordFailure(request.username(), clientAddress)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Troppi tentativi di accesso. Riprova tra qualche minuto");
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }
        loginThrottleService.recordSuccess(request.username(), clientAddress);
        AuthService.AuthSession session = authService.createSession(credentials.getUsername(), credentials.getRole());
        return new AuthResponse(session.token(), session.username(), session.role());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.invalidate(request.getHeader("Authorization"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/password")
    public ResponseEntity<Void> changePassword(HttpServletRequest httpRequest, @RequestBody PasswordRequest request) throws Exception {
        AuthService.AuthSession session = requireRole(httpRequest, Role.OPERATORE, Role.SEGRETERIA, Role.AMMINISTRATORE);
        execute(() -> new ChangePasswordDAO().execute(session.username(), request.oldPassword(), request.newPassword()));
        authService.invalidateUserSessions(session.username());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/operator/offers")
    public Object operatorOffers(HttpServletRequest request) throws Exception {
        requireRole(request, Role.OPERATORE);
        return execute(() -> new OffersAvailableProcedureDAO().execute());
    }

    @GetMapping("/operator/customers")
    public Object operatorCustomers(HttpServletRequest request) throws Exception {
        requireRole(request, Role.OPERATORE);
        return execute(() -> new CustomerListProcedureDAO().execute());
    }

    @GetMapping("/secretariat/customers")
    public Object secretariatCustomers(HttpServletRequest request) throws Exception {
        requireRole(request, Role.SEGRETERIA);
        return execute(() -> new CustomerListProcedureDAO().execute());
    }

    @GetMapping("/customers/search")
    public Object searchCustomers(HttpServletRequest request,
                                  @RequestParam(defaultValue = "") String nome,
                                  @RequestParam(defaultValue = "") String cognome) throws Exception {
        requireRole(request, Role.OPERATORE, Role.SEGRETERIA);
        Cliente customer = new Cliente();
        customer.setNome(nome);
        customer.setCognome(cognome);
        return execute(() -> new SearchCustomerProcedureDAO().execute(customer));
    }

    @GetMapping("/customers/{codiceFiscale}/interactions")
    public Object customerInteractions(HttpServletRequest request, @PathVariable String codiceFiscale) throws Exception {
        requireRole(request, Role.OPERATORE);
        Cliente customer = new Cliente();
        customer.setCodiceFiscale(codiceFiscale);
        return execute(() -> new CustomerInteractionsDAO().execute(customer));
    }

    @GetMapping("/customers/{codiceFiscale}/telephones")
    public Object customerTelephones(HttpServletRequest request, @PathVariable String codiceFiscale) throws Exception {
        requireRole(request, Role.OPERATORE);
        Cliente customer = new Cliente();
        customer.setCodiceFiscale(codiceFiscale);
        return execute(() -> new SearchCustomerTelephoneDAO().execute(customer));
    }

    @GetMapping("/customers/{codiceFiscale}/accepted-offers")
    public Object customerAcceptedOffers(HttpServletRequest request, @PathVariable String codiceFiscale) throws Exception {
        requireRole(request, Role.OPERATORE);
        return execute(() -> new CustomerAcceptedOffersDAO().execute(codiceFiscale));
    }

    @PostMapping("/operator/interactions")
    public ResponseEntity<Void> addInteraction(HttpServletRequest httpRequest, @RequestBody InteractionRequest request) throws Exception {
        requireRole(httpRequest, Role.OPERATORE);
        Interazione interaction = new Interazione();
        interaction.setCliente(request.codiceFiscale());
        interaction.setNota(request.nota());
        interaction.setData(Date.valueOf(request.data()));
        execute(() -> new InsertInteractionDAO().execute(interaction));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/operator/appointments")
    public ResponseEntity<Void> addInteractionWithAppointment(HttpServletRequest httpRequest,
                                                              @RequestBody AppointmentRequest request) throws Exception {
        requireRole(httpRequest, Role.OPERATORE);
        Sede office = new Sede();
        office.setIndirizzo(request.sede());

        Appuntamento appointment = new Appuntamento();
        appointment.setSede(office);
        Date interactionDate = Date.valueOf(request.dataInterazione());
        Date appointmentDate = Date.valueOf(request.dataAppuntamento());
        if (!appointmentDate.after(interactionDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La data dell'appuntamento deve essere successiva alla data dell'interazione");
        }
        appointment.setData(appointmentDate);
        appointment.setOra(parseTime(request.oraAppuntamento()));

        Interazione interaction = new Interazione();
        interaction.setCliente(request.codiceFiscale());
        interaction.setNota(request.nota());
        interaction.setData(interactionDate);
        interaction.setAppuntamento(appointment);

        execute(() -> new InsertInteractionWithAppDAO().execute(interaction));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/operator/accepted-offers")
    public ResponseEntity<Void> addAcceptedOffer(HttpServletRequest httpRequest,
                                                 @RequestBody AcceptedOfferRequest request) throws Exception {
        AuthService.AuthSession session = requireRole(httpRequest, Role.OPERATORE);
        OffertaAccettata acceptedOffer = new OffertaAccettata();
        acceptedOffer.setCliente(request.codiceFiscale());
        acceptedOffer.setOfferta(request.offerta());
        acceptedOffer.setUtente(session.username());
        acceptedOffer.setData(Date.valueOf(request.data()));
        execute(() -> new InsertCustomerOfferDAO().execute(acceptedOffer));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/operator/customers/{codiceFiscale}/interactions/{codiceInterazione}")
    public ResponseEntity<Void> deleteInteraction(HttpServletRequest httpRequest,
                                                  @PathVariable String codiceFiscale,
                                                  @PathVariable int codiceInterazione) throws Exception {
        requireRole(httpRequest, Role.OPERATORE);
        execute(() -> new DeleteInteractionProcedureDAO().execute(codiceInterazione, codiceFiscale));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/operator/customers/{codiceFiscale}/interactions/{codiceInterazione}/appointment")
    public ResponseEntity<Void> deleteAppointment(HttpServletRequest httpRequest,
                                                  @PathVariable String codiceFiscale,
                                                  @PathVariable int codiceInterazione) throws Exception {
        requireRole(httpRequest, Role.OPERATORE);
        execute(() -> new DeleteAppointmentProcedureDAO().execute(codiceInterazione, codiceFiscale));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/operator/accepted-offers")
    public ResponseEntity<Void> deleteAcceptedOffer(HttpServletRequest httpRequest,
                                                    @RequestBody AcceptedOfferDeleteRequest request) throws Exception {
        requireRole(httpRequest, Role.OPERATORE);
        OffertaAccettata acceptedOffer = new OffertaAccettata();
        acceptedOffer.setCliente(request.codiceFiscale());
        acceptedOffer.setOfferta(request.offerta());
        acceptedOffer.setData(Date.valueOf(request.data()));
        execute(() -> new DeleteAcceptedOfferProcedureDAO().execute(acceptedOffer));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/secretariat/addresses")
    public Object addresses(HttpServletRequest request) throws Exception {
        requireRole(request, Role.OPERATORE, Role.SEGRETERIA);
        return execute(() -> new ListAddressProcedureDAO().execute());
    }

    @PostMapping("/secretariat/customers")
    public ResponseEntity<Void> addCustomer(HttpServletRequest httpRequest, @RequestBody CustomerRequest request) throws Exception {
        requireRole(httpRequest, Role.SEGRETERIA);
        Cliente customer = new Cliente();
        customer.setCodiceFiscale(request.codiceFiscale());
        customer.setNome(request.nome());
        customer.setCognome(request.cognome());
        customer.setDataNascita(Date.valueOf(request.dataNascita()));
        customer.setDataRegistrazione(Date.valueOf(request.dataRegistrazione()));
        customer.setIndirizzoResidenza(request.indirizzoResidenza());
        execute(() -> new AddCustomerProcedureDAO().execute(customer));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/secretariat/customers/{codiceFiscale}")
    public ResponseEntity<Void> updateSecretariatCustomer(HttpServletRequest httpRequest,
                                                          @PathVariable String codiceFiscale,
                                                          @RequestBody CustomerUpdateRequest request) throws Exception {
        requireRole(httpRequest, Role.SEGRETERIA);
        Cliente customer = new Cliente();
        customer.setCodiceFiscale(codiceFiscale);
        customer.setNome(request.nome());
        customer.setCognome(request.cognome());
        customer.setDataNascita(Date.valueOf(request.dataNascita()));
        customer.setIndirizzoResidenza(request.indirizzoResidenza());
        execute(() -> new UpdateCustomerProcedureDAO().execute(
                customer, request.numeroTelefono(), request.indirizzoEmail()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/secretariat/customers/{codiceFiscale}")
    public ResponseEntity<Void> deleteSecretariatCustomer(HttpServletRequest httpRequest,
                                                          @PathVariable String codiceFiscale) throws Exception {
        requireRole(httpRequest, Role.SEGRETERIA);
        execute(() -> new DeleteCustomerProcedureDAO().execute(codiceFiscale));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/secretariat/customers/{codiceFiscale}/telephones")
    public ResponseEntity<Void> addTelephone(HttpServletRequest httpRequest,
                                             @PathVariable String codiceFiscale,
                                             @RequestBody TelephoneRequest request) throws Exception {
        requireRole(httpRequest, Role.SEGRETERIA);
        Cliente customer = new Cliente();
        customer.setCodiceFiscale(codiceFiscale);
        Telefono telephone = new Telefono();
        telephone.setNumero(request.numero());
        execute(() -> new AddTelProcedureDAO().execute(customer, telephone));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/secretariat/customers/{codiceFiscale}/emails")
    public ResponseEntity<Void> addEmail(HttpServletRequest httpRequest,
                                         @PathVariable String codiceFiscale,
                                         @RequestBody EmailRequest request) throws Exception {
        requireRole(httpRequest, Role.SEGRETERIA);
        Cliente customer = new Cliente();
        customer.setCodiceFiscale(codiceFiscale);
        Email email = new Email();
        email.setIndirizzoEmail(request.indirizzoEmail());
        execute(() -> new AddEmailProcedureDAO().execute(customer, email));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/secretariat/report")
    public Object report(HttpServletRequest request, @RequestParam String dal, @RequestParam String al) throws Exception {
        requireRole(request, Role.SEGRETERIA);
        Date startDate = Date.valueOf(dal);
        Date endDate = Date.valueOf(al);
        if (startDate.after(endDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La data iniziale deve precedere o coincidere con la data finale");
        }
        return execute(() -> new GenerateReportDAO().execute(startDate, endDate));
    }

    @GetMapping("/admin/offers")
    public Object adminOffers(HttpServletRequest request) throws Exception {
        requireRole(request, Role.AMMINISTRATORE);
        return execute(() -> new ListOffersDAO().execute());
    }

    @PostMapping("/admin/offers")
    public ResponseEntity<Void> addOffer(HttpServletRequest httpRequest, @RequestBody OfferRequest request) throws Exception {
        requireRole(httpRequest, Role.AMMINISTRATORE);
        Offerta offer = new Offerta();
        offer.setNome(request.nome());
        offer.setDescrizione(request.descrizione());
        offer.setDisponibile(request.disponibile());
        execute(() -> new AddOfferProcedureDAO().execute(offer));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/offers/{nome}")
    public ResponseEntity<Void> updateOfferAvailability(HttpServletRequest httpRequest,
                                                        @PathVariable String nome,
                                                        @RequestBody OfferAvailabilityRequest request) throws Exception {
        requireRole(httpRequest, Role.AMMINISTRATORE);
        execute(() -> new UpdateOfferAvailabilityDAO().execute(nome, request.disponibile()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/offers/{nome}")
    public ResponseEntity<Void> deleteOffer(HttpServletRequest httpRequest, @PathVariable String nome) throws Exception {
        requireRole(httpRequest, Role.AMMINISTRATORE);
        execute(() -> new DeleteOfferDAO().execute(nome));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/users")
    public ResponseEntity<Void> addUser(HttpServletRequest httpRequest, @RequestBody UserRequest request) throws Exception {
        requireRole(httpRequest, Role.AMMINISTRATORE);
        Utente user = new Utente();
        user.setId(request.id());
        user.setNome(request.nome());
        user.setCognome(request.cognome());
        user.setRuolo(dbRole(request.ruolo()));
        execute(() -> new AddUserProcedureDAO().execute(user, request.password()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/users")
    public Object users(HttpServletRequest httpRequest) throws Exception {
        requireRole(httpRequest, Role.AMMINISTRATORE);
        return execute(() -> new ListUsersDAO().execute());
    }

    @PutMapping("/admin/users/{id}")
    public ResponseEntity<Void> updateUser(HttpServletRequest httpRequest,
                                           @PathVariable String id,
                                           @RequestBody UserRequest request) throws Exception {
        AuthService.AuthSession session = requireRole(httpRequest, Role.AMMINISTRATORE);
        Role requestedRole = Role.valueOf(request.ruolo().toUpperCase());
        if (session.username().equals(id) && session.role() != requestedRole) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Non puoi modificare il tuo ruolo durante la sessione corrente");
        }
        Utente user = new Utente();
        user.setId(id);
        user.setNome(request.nome());
        user.setCognome(request.cognome());
        user.setRuolo(requestedRole.name().toLowerCase());
        execute(() -> new UpdateUserDAO().execute(user));
        if (!session.username().equals(id)) {
            authService.invalidateUserSessions(id);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Void> deleteUser(HttpServletRequest httpRequest, @PathVariable String id) throws Exception {
        AuthService.AuthSession session = requireRole(httpRequest, Role.AMMINISTRATORE);
        if (session.username().equals(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Non puoi rimuovere l'utente con cui hai effettuato l'accesso");
        }
        execute(() -> new DeleteUserDAO().execute(id));
        authService.invalidateUserSessions(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.status).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DAOException.class)
    public ResponseEntity<Map<String, String>> handleDaoException(DAOException exception) {
        logDatabaseFailure(exception);
        return ResponseEntity.badRequest().body(Map.of("message", databaseMessage(exception.getMessage())));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, String>> handleSqlException(SQLException exception) {
        logger.warn("Database non disponibile [SQLState={}, errorCode={}]", exception.getSQLState(), exception.getErrorCode(), exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Database non disponibile. Riprova tra qualche istante"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "I dati inseriti non sono validi"));
    }

    private <T> T execute(DaoCall<T> call) throws DAOException, SQLException {
        return call.execute();
    }

    private AuthService.AuthSession requireRole(HttpServletRequest request, Role... allowedRoles) {
        AuthService.AuthSession session = authService.findSession(request.getHeader("Authorization"));
        if (session == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sessione non valida o scaduta");
        }

        for (Role allowedRole : allowedRoles) {
            if (session.role() == allowedRole) {
                return session;
            }
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "Operazione non autorizzata per il ruolo " + session.role());
    }

    private String dbRole(String role) {
        return Role.valueOf(role.toUpperCase()).name().toLowerCase();
    }

    private Time parseTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ora non valida");
        }
        return Time.valueOf(value.length() == 5 ? value + ":00" : value);
    }

    private String databaseMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Operazione sul database non riuscita";
        }
        int customMessage = message.indexOf("Impossibile");
        if (customMessage >= 0) {
            return message.substring(customMessage);
        }
        return message;
    }

    private void logDatabaseFailure(DAOException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof SQLException sqlException) {
            logger.warn("Operazione database non riuscita [SQLState={}, errorCode={}]",
                    sqlException.getSQLState(), sqlException.getErrorCode(), sqlException);
            return;
        }
        logger.warn("Operazione database non riuscita", exception);
    }

    private interface DaoCall<T> {
        T execute() throws DAOException, SQLException;
    }

    private static class ApiException extends RuntimeException {
        private final HttpStatus status;

        private ApiException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    public record LoginRequest(String username, String password) {
    }

    public record AuthResponse(String token, String username, Role role) {
    }

    public record PasswordRequest(String oldPassword, String newPassword) {
    }

    public record InteractionRequest(String codiceFiscale, String nota, String data) {
    }

    public record AppointmentRequest(String codiceFiscale, String nota, String dataInterazione,
                                     String sede, String dataAppuntamento, String oraAppuntamento) {
    }

    public record AcceptedOfferRequest(String codiceFiscale, String offerta, String data) {
    }

    public record AcceptedOfferDeleteRequest(String codiceFiscale, String offerta, String data) {
    }

    public record CustomerRequest(String codiceFiscale, String nome, String cognome, String dataNascita,
                                  String dataRegistrazione, String indirizzoResidenza) {
    }

    public record CustomerUpdateRequest(String nome, String cognome, String dataNascita, String indirizzoResidenza,
                                        String numeroTelefono, String indirizzoEmail) {
    }

    public record OfferRequest(String nome, String descrizione, boolean disponibile) {
    }

    public record OfferAvailabilityRequest(boolean disponibile) {
    }

    public record TelephoneRequest(String numero) {
    }

    public record EmailRequest(String indirizzoEmail) {
    }

    public record UserRequest(String id, String ruolo, String nome, String cognome, String password) {
    }
}
