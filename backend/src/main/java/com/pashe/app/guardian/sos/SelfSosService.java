package com.pashe.app.guardian.sos;

import java.time.*;
import java.util.*;
import com.pashe.app.guardian.carecircle.CareCircleService;
import com.pashe.app.identity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class SelfSosService {
    private final SelfSosRepository repo;
    private final CareCircleService circle;
    private final IdentityDirectory people;
    private final RateLimiter limits;
    private final Clock clock;
    public SelfSosService(SelfSosRepository repo,CareCircleService circle,IdentityDirectory people,RateLimiter limits,Clock clock) {
        this.repo=repo;this.circle=circle;this.people=people;this.limits=limits;this.clock=clock;
    }
    public record RequestView(UUID id,String status,Instant activationDeadline,UUID incidentId,Instant serverNow) {}
    public record IncidentView(UUID id,UUID careRecipientId,String recipientName,String state,Instant openedAt,SelfSosRepository.Point location,String locationStatus,String pickupAddress,boolean canResolve) {}
    private RequestView projection(SelfSosRepository.Request r) { return new RequestView(r.id(),r.status(),r.deadline(),r.incident(),clock.instant()); }
    public List<RequestView> current(UUID actor) { circle.requireUser(actor);return repo.latest(actor).stream().map(this::projection).toList(); }
    public RequestView request(UUID actor,UUID id) {
        circle.requireUser(actor);var r=repo.request(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND));
        if(!r.recipient().equals(actor))throw new ResponseStatusException(NOT_FOUND);return projection(r);
    }
    @Transactional public RequestView start(UUID actor,UUID key,String address) {
        circle.requireUser(actor);circle.lockRecipient(actor);
        var existing=repo.existing(actor,key);if(existing.isPresent())return projection(existing.get());
        limits.check("self-sos:"+actor,30);
        var r=repo.create(actor,key,address,clock.instant());
        repo.active(actor).ifPresent(i->repo.activate(r.id(),i.id()));
        return projection(repo.request(r.id()).orElseThrow());
    }
    @Transactional public void cancel(UUID actor,UUID id) {
        circle.requireUser(actor);circle.lockRecipient(actor);
        var r=repo.request(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND));
        if(!r.recipient().equals(actor))throw new ResponseStatusException(NOT_FOUND);
        if(!r.status().equals("COUNTDOWN")||!clock.instant().isBefore(r.deadline()))throw new ResponseStatusException(CONFLICT);
        repo.cancel(id,clock.instant());
    }
    @Transactional public void activate(UUID id) {
        var initial=repo.request(id).orElseThrow();circle.lockRecipient(initial.recipient());
        var r=repo.request(id).orElseThrow();
        if(!r.status().equals("COUNTDOWN")||clock.instant().isBefore(r.deadline()))return;
        if(people.eligible(r.recipient()).isEmpty()){repo.cancel(id,clock.instant());return;}
        var incident=repo.active(r.recipient()).orElseGet(()->repo.open(r.recipient(),r.address(),clock.instant()));
        repo.activate(id,incident.id());
    }
    public IncidentView view(UUID actor,UUID id) {
        circle.requireUser(actor);
        var i=repo.incident(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND));
        boolean owner=i.recipient().equals(actor);
        if(!owner&&(i.resolvedAt()!=null||!circle.guardian(i.recipient(),actor)))throw new ResponseStatusException(NOT_FOUND);
        var point=repo.point(i.recipient()).orElse(null);
        String status=point==null?"UNAVAILABLE":point.observedAt().isBefore(clock.instant().minusSeconds(300))?"LAST_KNOWN":"RECENT";
        return new IncidentView(i.id(),i.recipient(),people.eligible(i.recipient()).map(IdentityDirectory.Person::name).orElse("Unavailable account"),i.resolvedAt()==null?"OPEN":"RESOLVED",i.openedAt(),point,status,i.address(),owner&&i.resolvedAt()==null);
    }
    public List<IncidentView> list(UUID actor,int page) {
        circle.requireUser(actor);if(page<0||page>10000)throw new ResponseStatusException(BAD_REQUEST);
        return repo.list(actor,page).stream().map(i->view(actor,i.id())).toList();
    }
    @Transactional public void resolve(UUID actor,UUID id,String reason) {
        circle.requireUser(actor);circle.lockRecipient(actor);
        var i=repo.incident(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND));
        if(!i.recipient().equals(actor))throw new ResponseStatusException(NOT_FOUND);
        if(i.resolvedAt()==null)repo.resolve(id,reason,clock.instant());
    }
    @Transactional public void location(UUID actor,SelfSosRepository.Point p) {
        circle.requireUser(actor);circle.lockRecipient(actor);limits.check("self-sos-location:"+actor,180);
        if(!Double.isFinite(p.latitude())||!Double.isFinite(p.longitude())||!Double.isFinite(p.accuracy())||Math.abs(p.latitude())>90||Math.abs(p.longitude())>180||p.accuracy()<0||p.accuracy()>10000||p.observedAt()==null||p.observedAt().isBefore(clock.instant().minusSeconds(300))||p.observedAt().isAfter(clock.instant().plusSeconds(30)))throw new ResponseStatusException(BAD_REQUEST);
        if(!repo.savePoint(actor,p))throw new ResponseStatusException(CONFLICT);
    }
    @Transactional public void clearLocation(UUID actor) { circle.requireUser(actor);circle.lockRecipient(actor);repo.clearPoint(actor); }
}
