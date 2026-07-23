package it.bd.model.domain;

import java.sql.Date;

public class Cliente {
    private String codiceFiscale;
    private Date dataNascita;
    private String nome;
    private String cognome;
    private Date dataRegistrazione;
    private Date dataUltimaInterazione;
    private String indirizzoResidenza;
    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public Date getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(Date dataNascita) {
        this.dataNascita = dataNascita;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public Date getDataRegistrazione() {
        return dataRegistrazione;
    }

    public void setDataRegistrazione(Date dataRegistrazione) {
        this.dataRegistrazione = dataRegistrazione;
    }

    public Date getDataUltimaInterazione() {
        return dataUltimaInterazione;
    }

    public void setDataUltimaInterazione(Date dataUltimaInterazione) {
        this.dataUltimaInterazione = dataUltimaInterazione;
    }

    public String getIndirizzoResidenza() {
        return indirizzoResidenza;
    }

    public void setIndirizzoResidenza(String indirizzoResidenza) {
        this.indirizzoResidenza = indirizzoResidenza;
    }
}