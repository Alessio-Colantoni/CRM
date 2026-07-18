package it.bd.model.domain;

import java.sql.Date;

public class Interazione {
    private int codiceInterazione;
    private String nota;
    private Date data;
    private String cliente;
    private Appuntamento appuntamento;

    public Appuntamento getAppuntamento() {
        return appuntamento;
    }

    public void setAppuntamento(Appuntamento appuntamento) {
        this.appuntamento = appuntamento;
    }

    public int getCodiceInterazione() {
        return codiceInterazione;
    }

    public void setCodiceInterazione(int codiceInterazione) {
        this.codiceInterazione = codiceInterazione;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }
}
