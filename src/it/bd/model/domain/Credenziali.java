package it.bd.model.domain;

public class Credenziali {

    private final String username;
    private final Role role;

    public Credenziali(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }
}
