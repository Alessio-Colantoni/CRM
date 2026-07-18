package it.bd.model.domain;

public class Utente {
    private String id;
    private String ruolo;
    private String nome;
    private String cognome;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
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
}
