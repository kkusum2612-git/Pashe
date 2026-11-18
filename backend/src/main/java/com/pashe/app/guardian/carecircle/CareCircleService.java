package com.pashe.app.guardian.carecircle;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import com.pashe.app.identity.*;

@Service
public class CareCircleService {
    private final CareCircleRepository repo;
    private final IdentityDirectory people;
    private final RateLimiter limits;
    private final Clock clock;
    public CareCircleService(CareCircleRepository repo,IdentityDirectory people,RateLimiter limits,Clock clock) { this.repo=repo;this.people=people;this.limits=limits;this.clock=clock; }
    public record View(UUID id,UUID careRecipientId,UUID inviteeId,String recipientName,String inviteeName,String relationshipType,String status,Instant recipientConsentedAt,Instant inviteeConsentedAt) {}
    public List<View> list(UUID actor,int page) {
        requireUser(actor);
        if(page<0 || page>10000) throw new ResponseStatusException(BAD_REQUEST);
        return repo.list(actor,page).stream().map(m -> new View(m.id(),m.careRecipientId(),m.inviteeId(),people.eligible(m.careRecipientId()).map(IdentityDirectory.Person::name).orElse("Unavailable account"),people.eligible(m.inviteeId()).map(IdentityDirectory.Person::name).orElse("Unavailable account"),m.relationshipType(),m.status(),m.recipientConsentedAt(),m.inviteeConsentedAt())).toList();
    }
    public void requireUser(UUID actor) { if(people.eligible(actor).isEmpty()) throw new ResponseStatusException(FORBIDDEN); }
    @Transactional public void invite(UUID actor,String email,String type) {
        requireUser(actor); limits.check("circle:"+actor,20);
        if(!Set.of("GUARDIAN","TRUSTED_NEIGHBOR").contains(type)) throw new ResponseStatusException(BAD_REQUEST);
        var target=people.find(email);
        if(target.isEmpty()) return;
        if(target.get().id().equals(actor)) throw new ResponseStatusException(BAD_REQUEST);
        repo.lock(actor); repo.insert(UUID.randomUUID(),actor,target.get().id(),type,clock.instant());
    }
    @Transactional public void change(UUID actor,UUID id,String action) {
        requireUser(actor);
        var initial=repo.find(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        repo.lock(initial.careRecipientId());
        var m=repo.find(id).orElseThrow();
        boolean invitee=actor.equals(m.inviteeId()), owner=actor.equals(m.careRecipientId());
        if(!invitee && !owner) throw new ResponseStatusException(NOT_FOUND);
        String next=switch(action) { case "accept" -> "ACTIVE"; case "decline" -> "DECLINED"; case "cancel" -> "CANCELED"; case "revoke" -> "REVOKED"; default -> throw new ResponseStatusException(BAD_REQUEST); };
        if(action.equals("revoke")) { if(!m.status().equals("ACTIVE")) throw new ResponseStatusException(CONFLICT); }
        else {
            if(!m.status().equals("PENDING")) throw new ResponseStatusException(CONFLICT);
            if(action.equals("cancel") ? !owner : !invitee) throw new ResponseStatusException(FORBIDDEN);
            requireUser(m.careRecipientId()); requireUser(m.inviteeId());
        }
        repo.transition(id,actor,next,clock.instant());
    }
    public boolean guardian(UUID recipient,UUID actor) { return people.eligible(actor).isPresent() && repo.active(recipient).stream().anyMatch(m -> m.inviteeId().equals(actor) && m.relationshipType().equals("GUARDIAN")); }
    public void lockRecipient(UUID recipient) { repo.lock(recipient); }
}
