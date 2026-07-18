package it.bd.model.domain;

import java.sql.Date;

public class OffertaAccettata {
    private String cliente;
    private String offerta;
    private String utente;
    private Date data;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getOfferta() {
        return offerta;
    }

    public void setOfferta(String offerta) {
        this.offerta = offerta;
    }

    public String getUtente() {
        return utente;
    }

    public void setUtente(String utente) {
        this.utente = utente;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }
}
