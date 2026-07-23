package it.bd.model.domain;

import java.sql.Date;
import java.sql.Time;

public class Appuntamento {
    private int codiceAllegato;
    private Sede sede;
    private Date data;
    private Time ora;

    public int getCodiceAllegato() {
        return codiceAllegato;
    }

    public void setCodiceAllegato(int codiceAllegato) {
        this.codiceAllegato = codiceAllegato;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public Time getOra() {
        return ora;
    }

    public void setOra(Time ora) {
        this.ora = ora;
    }
}
