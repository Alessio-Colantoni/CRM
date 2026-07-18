function formatLocalDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function localDate(offset = 0) {
    const date = new Date();
    date.setDate(date.getDate() + offset);
    return formatLocalDate(date);
}

function dayAfter(value) {
    const date = new Date(`${value}T00:00:00`);
    date.setDate(date.getDate() + 1);
    return formatLocalDate(date);
}

function toInputDate(value) {
    return value ? String(value).slice(0, 10) : '';
}

function loadStoredSession() {
    const storageKey = 'crm-session';
    const storedSession = localStorage.getItem(storageKey);
    if (!storedSession) return null;
    try {
        const session = JSON.parse(storedSession);
        const allowedRoles = ['OPERATORE', 'SEGRETERIA', 'AMMINISTRATORE'];
        if (!session || typeof session.token !== 'string' || !session.token
            || typeof session.username !== 'string' || !session.username
            || !allowedRoles.includes(session.role)) {
            throw new Error('Sessione locale non valida');
        }
        return session;
    } catch (_) {
        localStorage.removeItem(storageKey);
        return null;
    }
}

const today = localDate();
const tomorrow = localDate(1);

const app = Vue.createApp({
    data() {
        return {
            session: loadStoredSession(),
            view: 'dashboard',
            loading: false,
            message: null,
            loginForm: { username: '', password: '' },
            customerSearch: { nome: '', cognome: '' },
            customerDirectory: [],
            customers: [],
            selectedCustomer: null,
            customerDetailsRequestId: 0,
            pendingConfirmation: null,
            interactions: [],
            telephones: [],
            acceptedOffers: [],
            offers: [],
            addresses: [],
            operatorTab: 'interaction',
            interactionForm: {
                nota: '',
                data: today,
                withAppointment: false,
                sede: '',
                dataAppuntamento: tomorrow,
                oraAppuntamento: '09:00'
            },
            acceptedOfferForm: { offerta: '', data: today },
            newCustomer: {
                codiceFiscale: '',
                nome: '',
                cognome: '',
                dataNascita: '',
                dataRegistrazione: today,
                indirizzoResidenza: ''
            },
            newOffer: { nome: '', descrizione: '', disponibile: true },
            managedOffers: [],
            secretariatSearch: { nome: '', cognome: '' },
            secretariatDirectory: [],
            secretariatCustomers: [],
            secretariatSelectedCustomer: null,
            secretariatEditCustomer: {
                nome: '',
                cognome: '',
                dataNascita: '',
                indirizzoResidenza: '',
                numeroTelefono: '',
                indirizzoEmail: ''
            },
            reportRange: { dal: today, al: today },
            report: [],
            reportLoaded: false,
            newUser: { id: '', ruolo: 'OPERATORE', nome: '', cognome: '', password: '' },
            users: [],
            editUserForm: { id: '', ruolo: 'OPERATORE', nome: '', cognome: '' },
            passwordForm: { oldPassword: '', newPassword: '', confirmPassword: '' }
        };
    },
    computed: {
        pageTitle() {
            const titles = {
                dashboard: 'Panoramica',
                operator: 'Area operatore',
                secretariat: 'Area segreteria',
                admin: 'Area amministratore'
            };
            return titles[this.view] || 'CRM';
        },
        appointmentMinimumDate() {
            return dayAfter(this.interactionForm.data);
        }
    },
    watch: {
        view(nextView) {
            if (nextView === 'operator') {
                this.loadOperatorHome();
            } else if (nextView === 'secretariat') {
                this.loadSecretariatHome();
            } else if (nextView === 'admin') {
                this.loadAdminHome();
            }
        }
    },
    mounted() {
        if (this.session?.role === 'OPERATORE') {
            this.view = 'operator';
        } else if (this.session?.role === 'SEGRETERIA') {
            this.view = 'secretariat';
        } else if (this.session?.role === 'AMMINISTRATORE') {
            this.view = 'admin';
        }
    },
    methods: {
        async request(path, options = {}) {
            const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
            if (this.session?.token) {
                headers.Authorization = `Bearer ${this.session.token}`;
            }

            const response = await fetch(path, {
                headers,
                ...options
            });
            if (!response.ok) {
                let error = 'Operazione non riuscita';
                try {
                    const body = await response.json();
                    error = body.message || error;
                } catch (_) {
                    error = response.statusText || error;
                }
                if (response.status === 401) {
                    this.clearSession();
                }
                throw new Error(error);
            }
            if (response.status === 204) {
                return null;
            }
            return response.json();
        },
        notify(text, type = 'ok') {
            this.message = { text, type };
            window.clearTimeout(this.messageTimer);
            this.messageTimer = window.setTimeout(() => this.message = null, 3200);
        },
        openConfirmation(confirmation) {
            this.pendingConfirmation = confirmation;
        },
        cancelConfirmation() {
            this.pendingConfirmation?.onCancel?.();
            this.pendingConfirmation = null;
        },
        async confirmPendingAction() {
            const confirmation = this.pendingConfirmation;
            this.pendingConfirmation = null;
            if (confirmation?.action) await confirmation.action();
        },
        async login() {
            this.loading = true;
            try {
                const credentials = await this.request('/api/auth/login', {
                    method: 'POST',
                    body: JSON.stringify(this.loginForm)
                });
                this.session = {
                    token: credentials.token,
                    username: credentials.username,
                    role: credentials.role
                };
                localStorage.setItem('crm-session', JSON.stringify(this.session));
                this.loginForm = { username: '', password: '' };
                this.view = this.session.role === 'OPERATORE'
                    ? 'operator'
                    : this.session.role === 'SEGRETERIA'
                        ? 'secretariat'
                        : 'admin';
                this.notify('Accesso effettuato');
            } catch (error) {
                this.notify(error.message, 'error');
            } finally {
                this.loginForm.password = '';
                this.loading = false;
            }
        },
        async logout() {
            try {
                await this.request('/api/auth/logout', { method: 'POST' });
            } catch (_) {
                // Local logout still clears the browser session.
            }
            this.clearSession();
        },
        clearSession() {
            this.session = null;
            this.loginForm = { username: '', password: '' };
            localStorage.removeItem('crm-session');
            this.view = 'dashboard';
        },
        async searchCustomers() {
            try {
                const params = new URLSearchParams(this.customerSearch);
                this.customers = await this.request(`/api/customers/search?${params}`);
                this.notify(`${this.customers.length} clienti trovati`);
            } catch (error) {
                this.notify(error.message, 'error');
            }
        },
        async loadOperatorHome() {
            await Promise.all([
                this.loadOperatorCustomers(),
                this.loadOffers(),
                this.loadAddresses()
            ]);
        },
        async loadSecretariatHome() {
            try {
                this.secretariatDirectory = await this.request('/api/secretariat/customers');
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async loadOperatorCustomers() {
            try {
                this.customerDirectory = await this.request('/api/operator/customers');
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async loadOffers() {
            try {
                this.offers = await this.request('/api/operator/offers');
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async loadAddresses() {
            try {
                this.addresses = await this.request('/api/secretariat/addresses');
                if (!this.interactionForm.sede && this.addresses.length) {
                    this.interactionForm.sede = this.addresses[0].indirizzo;
                }
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async refreshOperatorCustomers() {
            if (await this.loadOperatorCustomers()) {
                this.notify('Elenco clienti aggiornato');
            }
        },
        async refreshSecretariatCustomers() {
            if (await this.loadSecretariatHome()) {
                this.notify('Elenco clienti aggiornato');
            }
        },
        async refreshOffers() {
            if (await this.loadOffers()) {
                this.notify('Offerte disponibili aggiornate');
            }
        },
        async selectCustomer(customer) {
            this.selectedCustomer = customer;
            await this.loadCustomerDetails();
        },
        async loadCustomerDetails(showFeedback = false) {
            if (!this.selectedCustomer) return;
            const requestedCustomerCode = this.selectedCustomer.codiceFiscale;
            const requestId = ++this.customerDetailsRequestId;
            try {
                const cf = encodeURIComponent(requestedCustomerCode);
                const [interactions, telephones, acceptedOffers] = await Promise.all([
                    this.request(`/api/customers/${cf}/interactions`),
                    this.request(`/api/customers/${cf}/telephones`),
                    this.request(`/api/customers/${cf}/accepted-offers`)
                ]);
                if (requestId !== this.customerDetailsRequestId
                    || this.selectedCustomer?.codiceFiscale !== requestedCustomerCode) {
                    return false;
                }
                this.interactions = interactions;
                this.telephones = telephones;
                this.acceptedOffers = acceptedOffers;
                if (showFeedback) {
                    this.notify('Storico cliente aggiornato');
                }
                return true;
            } catch (error) {
                if (requestId !== this.customerDetailsRequestId
                    || this.selectedCustomer?.codiceFiscale !== requestedCustomerCode) {
                    return false;
                }
                this.interactions = [];
                this.telephones = [];
                this.acceptedOffers = [];
                this.notify(error.message, 'error');
                return false;
            }
        },
        async saveInteraction() {
            if (!this.selectedCustomer) return;

            const form = this.interactionForm;
            if (form.withAppointment && form.dataAppuntamento <= form.data) {
                this.notify("La data dell'appuntamento deve essere successiva alla data dell'interazione", 'error');
                return;
            }

            const path = form.withAppointment
                ? '/api/operator/appointments'
                : '/api/operator/interactions';
            const payload = form.withAppointment
                ? {
                    codiceFiscale: this.selectedCustomer.codiceFiscale,
                    nota: form.nota,
                    dataInterazione: form.data,
                    sede: form.sede,
                    dataAppuntamento: form.dataAppuntamento,
                    oraAppuntamento: form.oraAppuntamento
                }
                : {
                    codiceFiscale: this.selectedCustomer.codiceFiscale,
                    nota: form.nota,
                    data: form.data
                };
            const success = form.withAppointment
                ? 'Interazione e appuntamento registrati con successo'
                : 'Interazione registrata con successo';
            this.openConfirmation({
                title: form.withAppointment ? 'Registra interazione e appuntamento?' : 'Registra interazione?',
                message: form.withAppointment
                    ? 'L\'interazione e l\'appuntamento verranno registrati per il cliente selezionato.'
                    : 'L\'interazione verra registrata per il cliente selezionato.',
                confirmLabel: 'Registra',
                action: async () => {
                    const saved = await this.saveOperator(path, payload, success);
                    if (saved) {
                        this.interactionForm.nota = '';
                        this.interactionForm.withAppointment = false;
                        await this.loadCustomerDetails();
                    }
                }
            });
        },
        async addAcceptedOffer() {
            this.openConfirmation({
                title: 'Registra offerta accettata?',
                message: 'L\'offerta selezionata verra registrata per il cliente corrente.',
                confirmLabel: 'Registra',
                action: async () => {
                    const saved = await this.saveOperator('/api/operator/accepted-offers', {
                        ...this.acceptedOfferForm,
                        codiceFiscale: this.selectedCustomer.codiceFiscale
                    }, 'Offerta accettata registrata con successo');
                    if (saved) {
                        this.acceptedOfferForm.offerta = '';
                        await Promise.all([this.loadOffers(), this.loadCustomerDetails()]);
                    }
                }
            });
        },
        async deleteInteraction(interaction) {
            this.openConfirmation({
                title: 'Eliminare interazione?',
                message: 'L\'eventuale appuntamento collegato verra eliminato insieme all\'interazione.',
                warning: 'Questa operazione non puo essere annullata.',
                confirmLabel: 'Elimina',
                destructive: true,
                action: () => this.deleteOperatorRecord(
                    `/api/operator/customers/${encodeURIComponent(this.selectedCustomer.codiceFiscale)}/interactions/${interaction.codiceInterazione}`,
                    'Interazione eliminata'
                )
            });
        },
        async deleteAppointment(interaction) {
            this.openConfirmation({
                title: 'Eliminare appuntamento?',
                message: 'L\'appuntamento verra eliminato, mentre l\'interazione restera registrata.',
                warning: 'Questa operazione non puo essere annullata.',
                confirmLabel: 'Elimina',
                destructive: true,
                action: () => this.deleteOperatorRecord(
                    `/api/operator/customers/${encodeURIComponent(this.selectedCustomer.codiceFiscale)}/interactions/${interaction.codiceInterazione}/appointment`,
                    'Appuntamento eliminato'
                )
            });
        },
        async deleteAcceptedOffer(offer) {
            this.openConfirmation({
                title: 'Eliminare offerta accettata?',
                message: 'L\'offerta accettata verra rimossa dallo storico del cliente.',
                warning: 'Questa operazione non puo essere annullata.',
                confirmLabel: 'Elimina',
                destructive: true,
                action: () => this.deleteOperatorRecord('/api/operator/accepted-offers', 'Offerta accettata eliminata', {
                    codiceFiscale: offer.cliente,
                    offerta: offer.offerta,
                    data: offer.data
                })
            });
        },
        async deleteOperatorRecord(path, success, payload) {
            try {
                await this.request(path, {
                    method: 'DELETE',
                    ...(payload ? { body: JSON.stringify(payload) } : {})
                });
                this.notify(success);
                await this.loadCustomerDetails();
            } catch (error) {
                this.notify(error.message, 'error');
            }
        },
        async saveOperator(path, payload, success) {
            if (!this.selectedCustomer) return;
            try {
                await this.request(path, { method: 'POST', body: JSON.stringify(payload) });
                this.notify(success);
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async addCustomer() {
            const customer = { ...this.newCustomer };
            this.openConfirmation({
                title: 'Creare cliente?',
                message: `Verranno registrati i dati del cliente ${customer.nome} ${customer.cognome}.`,
                confirmLabel: 'Crea cliente',
                action: async () => {
                    try {
                        await this.request('/api/secretariat/customers', {
                            method: 'POST',
                            body: JSON.stringify(customer)
                        });
                        this.notify('Cliente creato');
                        this.newCustomer = {
                            codiceFiscale: '',
                            nome: '',
                            cognome: '',
                            dataNascita: '',
                            dataRegistrazione: today,
                            indirizzoResidenza: ''
                        };
                        await this.loadSecretariatHome();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async addOffer() {
            const offer = { ...this.newOffer };
            this.openConfirmation({
                title: 'Creare offerta?',
                message: `L'offerta ${offer.nome} verra aggiunta al catalogo.`,
                confirmLabel: 'Crea offerta',
                action: async () => {
                    try {
                        await this.request('/api/admin/offers', {
                            method: 'POST',
                            body: JSON.stringify(offer)
                        });
                        this.notify('Offerta creata');
                        this.newOffer = { nome: '', descrizione: '', disponibile: true };
                        await this.loadManagedOffers();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async loadAdminHome() {
            await Promise.all([
                this.loadUsers(),
                this.loadManagedOffers()
            ]);
        },
        async loadManagedOffers() {
            try {
                this.managedOffers = await this.request('/api/admin/offers');
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async refreshManagedOffers() {
            if (await this.loadManagedOffers()) {
                this.notify('Elenco offerte aggiornato');
            }
        },
        confirmOfferAvailability(offer) {
            const available = offer.disponibile;
            this.openConfirmation({
                title: 'Aggiornare disponibilita?',
                message: `L'offerta ${offer.nome} verra impostata come ${available ? 'disponibile' : 'non disponibile'}.`,
                confirmLabel: 'Aggiorna',
                onCancel: () => {
                    offer.disponibile = !available;
                },
                action: () => this.updateOfferAvailability(offer, available)
            });
        },
        async updateOfferAvailability(offer, available) {
            try {
                await this.request(`/api/admin/offers/${encodeURIComponent(offer.nome)}`, {
                    method: 'PATCH',
                    body: JSON.stringify({ disponibile: available })
                });
                this.notify('Disponibilita offerta aggiornata');
                await this.loadManagedOffers();
            } catch (error) {
                this.notify(error.message, 'error');
                await this.loadManagedOffers();
            }
        },
        async deleteOffer(offer) {
            this.openConfirmation({
                title: 'Rimuovere offerta?',
                message: `L'offerta ${offer.nome} verra rimossa.`,
                warning: 'Questa operazione non puo essere annullata.',
                confirmLabel: 'Rimuovi',
                destructive: true,
                action: async () => {
                    try {
                        await this.request(`/api/admin/offers/${encodeURIComponent(offer.nome)}`, {
                            method: 'DELETE'
                        });
                        this.notify('Offerta rimossa');
                        await this.loadManagedOffers();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async searchSecretariatCustomers() {
            try {
                const params = new URLSearchParams(this.secretariatSearch);
                this.secretariatCustomers = await this.request(`/api/customers/search?${params}`);
                this.notify(`${this.secretariatCustomers.length} clienti trovati`);
            } catch (error) {
                this.notify(error.message, 'error');
            }
        },
        useSecretariatCustomer(customer) {
            this.secretariatSelectedCustomer = customer;
            this.secretariatEditCustomer = {
                nome: customer.nome,
                cognome: customer.cognome,
                dataNascita: toInputDate(customer.dataNascita),
                indirizzoResidenza: customer.indirizzoResidenza || '',
                numeroTelefono: '',
                indirizzoEmail: ''
            };
            this.notify(`Cliente selezionato: ${customer.nome} ${customer.cognome}`);
        },
        async updateSecretariatCustomer() {
            if (!this.secretariatSelectedCustomer) return;
            const customer = this.secretariatSelectedCustomer;
            const update = { ...this.secretariatEditCustomer };
            this.openConfirmation({
                title: 'Aggiornare cliente?',
                message: `I dati di ${customer.nome} ${customer.cognome} verranno aggiornati.`,
                confirmLabel: 'Aggiorna',
                action: async () => {
                    try {
                        const cf = encodeURIComponent(customer.codiceFiscale);
                        await this.request(`/api/secretariat/customers/${cf}`, {
                            method: 'PATCH',
                            body: JSON.stringify(update)
                        });
                        const customerData = {
                            nome: update.nome,
                            cognome: update.cognome,
                            dataNascita: update.dataNascita,
                            indirizzoResidenza: update.indirizzoResidenza
                        };
                        Object.assign(this.secretariatSelectedCustomer, customerData);
                        this.secretariatCustomers = this.secretariatCustomers.map(item => item.codiceFiscale === customer.codiceFiscale
                            ? { ...item, ...customerData }
                            : item);
                        this.secretariatEditCustomer.indirizzoEmail = '';
                        this.secretariatEditCustomer.numeroTelefono = '';
                        this.notify('Dati cliente aggiornati');
                        await this.loadSecretariatHome();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        openSecretariatCustomerDeletion() {
            if (!this.secretariatSelectedCustomer) return;
            const customer = { ...this.secretariatSelectedCustomer };
            this.openConfirmation({
                title: 'Eliminare cliente?',
                message: `Stai per eliminare definitivamente ${customer.nome} ${customer.cognome}. Verranno eliminati anche telefoni, email, interazioni, appuntamenti e offerte accettate associati.`,
                warning: 'Questa operazione non puo essere annullata.',
                confirmLabel: 'Elimina definitivamente',
                destructive: true,
                action: async () => {
                    try {
                        await this.request(`/api/secretariat/customers/${encodeURIComponent(customer.codiceFiscale)}`, {
                            method: 'DELETE'
                        });
                        this.secretariatSelectedCustomer = null;
                        this.secretariatCustomers = this.secretariatCustomers.filter(item => item.codiceFiscale !== customer.codiceFiscale);
                        this.notify('Cliente e dati collegati eliminati');
                        await this.loadSecretariatHome();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async loadReport() {
            if (this.reportRange.dal > this.reportRange.al) {
                this.notify('La data iniziale deve precedere o coincidere con la data finale', 'error');
                return;
            }
            try {
                const params = new URLSearchParams(this.reportRange);
                this.report = await this.request(`/api/secretariat/report?${params}`);
                this.reportLoaded = true;
            } catch (error) {
                this.notify(error.message, 'error');
            }
        },
        async addUser() {
            const user = { ...this.newUser };
            this.openConfirmation({
                title: 'Creare utente?',
                message: `Viene creato l'utente ${user.id} con ruolo ${user.ruolo}.`,
                confirmLabel: 'Crea utente',
                action: async () => {
                    try {
                        await this.request('/api/admin/users', {
                            method: 'POST',
                            body: JSON.stringify(user)
                        });
                        this.notify('Utente creato');
                        this.newUser = { id: '', ruolo: 'OPERATORE', nome: '', cognome: '', password: '' };
                        await this.loadUsers();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async loadUsers() {
            try {
                this.users = await this.request('/api/admin/users');
                return true;
            } catch (error) {
                this.notify(error.message, 'error');
                return false;
            }
        },
        async refreshUsers() {
            if (await this.loadUsers()) {
                this.notify('Elenco utenti aggiornato');
            }
        },
        editUser(user) {
            this.editUserForm = {
                id: user.id,
                ruolo: String(user.ruolo || '').toUpperCase(),
                nome: user.nome,
                cognome: user.cognome
            };
        },
        async updateUser() {
            if (!this.editUserForm.id) return;
            const user = { ...this.editUserForm };
            this.openConfirmation({
                title: 'Aggiornare utente?',
                message: `I dati e il ruolo di ${user.id} verranno aggiornati.`,
                confirmLabel: 'Aggiorna',
                action: async () => {
                    try {
                        await this.request(`/api/admin/users/${encodeURIComponent(user.id)}`, {
                            method: 'PUT',
                            body: JSON.stringify(user)
                        });
                        this.notify('Utente aggiornato');
                        this.editUserForm = { id: '', ruolo: 'OPERATORE', nome: '', cognome: '' };
                        await this.loadUsers();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async deleteUser(user) {
            this.openConfirmation({
                title: 'Rimuovere utente?',
                message: `L'utente ${user.id} verra rimosso e le sue sessioni attive verranno revocate.`,
                warning: 'Questa operazione non puo essere annullata.',
                confirmLabel: 'Rimuovi',
                destructive: true,
                action: async () => {
                    try {
                        await this.request(`/api/admin/users/${encodeURIComponent(user.id)}`, { method: 'DELETE' });
                        this.notify('Utente rimosso');
                        await this.loadUsers();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        async changePassword() {
            if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
                this.notify('Le nuove password non coincidono', 'error');
                return;
            }
            const password = { ...this.passwordForm };
            this.openConfirmation({
                title: 'Aggiornare password?',
                message: 'La password verra aggiornata e dovrai accedere nuovamente.',
                confirmLabel: 'Aggiorna password',
                action: async () => {
                    try {
                        await this.request('/api/auth/password', {
                            method: 'POST',
                            body: JSON.stringify(password)
                        });
                        this.notify('Password aggiornata. Effettua di nuovo il login.');
                        this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' };
                        this.clearSession();
                    } catch (error) {
                        this.notify(error.message, 'error');
                    }
                }
            });
        },
        reportRows() {
            return this.report.filter(row => !Object.prototype.hasOwnProperty.call(row, 'totaleClientiContattati'));
        },
        reportTotal() {
            const total = this.report.find(row => Object.prototype.hasOwnProperty.call(row, 'totaleClientiContattati'));
            return total?.totaleClientiContattati ?? 0;
        },
        formatDate(value) {
            if (!value) return '-';
            return new Intl.DateTimeFormat('it-IT').format(new Date(value));
        },
        formatTime(value) {
            if (!value) return '';
            return String(value).slice(0, 5);
        },
        appointmentDate(interaction) {
            return interaction?.appuntamento?.data || null;
        },
        appointmentTime(interaction) {
            return interaction?.appuntamento?.ora || null;
        },
        appointmentAddress(interaction) {
            return interaction?.appuntamento?.sede?.indirizzo || 'sede non indicata';
        },
        hasAppointment(interaction) {
            return Boolean(this.appointmentDate(interaction) || this.appointmentTime(interaction) || interaction?.appuntamento?.sede?.indirizzo);
        }
    }
});

app.mount('#app');
