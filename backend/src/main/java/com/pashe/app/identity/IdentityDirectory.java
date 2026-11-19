package com.pashe.app.identity;

import java.util.*;
import org.springframework.stereotype.Service;

/** Public identity boundary for domain modules; never exposes password/session fields. */
@Service
public class IdentityDirectory {
    private final UserService users;
    public IdentityDirectory(UserService users) { this.users=users; }
    public record Person(UUID id,String name,String email) {}
    public Optional<Person> eligible(UUID id) {
        try { var a=users.byId(id); return a.enabled() && a.emailVerified() ? Optional.of(new Person(id,a.fullName(),a.email())) : Optional.empty(); }
        catch (org.springframework.web.server.ResponseStatusException ex) { return Optional.empty(); }
    }
    public Optional<Person> find(String email) { return users.byEmail(email).flatMap(a -> eligible(a.id())); }
}
