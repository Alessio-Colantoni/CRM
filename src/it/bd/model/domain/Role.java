package it.bd.model.domain;

public enum Role {

    SEGRETERIA(1),

    OPERATORE(2),

    AMMINISTRATORE(3);

    private final int id;

    private Role(int id) {
        this.id = id;
    }

    public static Role fromInt(int id) {
        for (Role type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }
}
