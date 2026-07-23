package it.bd.model.domain;

public class Offerta {
    private String nome;
    private String descrizione;
    private boolean disponibile;
    private boolean modificabile = true;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public boolean isDisponibile() {
        return disponibile;
    }

    public void setDisponibile(boolean disponibile) {
        this.disponibile = disponibile;
    }

    public boolean isModificabile() {
        return modificabile;
    }

    public void setModificabile(boolean modificabile) {
        this.modificabile = modificabile;
    }
}
