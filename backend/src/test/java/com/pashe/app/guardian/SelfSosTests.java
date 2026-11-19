package com.pashe.app.guardian;

import java.time.*;
import java.util.*;
import com.pashe.app.guardian.sos.*;
import com.pashe.app.guardian.carecircle.CareCircleService;
import com.pashe.app.identity.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="pashe.guardian.scheduling-enabled=false")
@Transactional
class SelfSosTests {
    @Autowired SelfSosService service; @Autowired SelfSosRepository repo;
    @Autowired UserService users; @Autowired CareCircleService circle;
    @MockitoBean Clock clock;
    final Instant now=Instant.parse("2026-11-18T05:00:00Z");
    @BeforeEach void setup(){when(clock.instant()).thenReturn(now);}
    UUID person(){return users.create("Synthetic demo",UUID.randomUUID()+"@example.test","01712345678","not-a-login-hash",true);}
    UUID guardian(UUID owner){UUID g=person();circle.invite(owner,users.byId(g).email(),"GUARDIAN");circle.change(g,circle.list(owner,0).getFirst().id(),"accept");return g;}
    @Test void cancellationBeforeDeadlineNeverActivates(){
        UUID owner=person(),key=UUID.randomUUID();var r=service.start(owner,key,null);
        assertEquals(r.id(),service.start(owner,key,null).id());assertEquals(now.plusSeconds(10),r.activationDeadline());
        when(clock.instant()).thenReturn(now.plusSeconds(9));service.cancel(owner,r.id());
        when(clock.instant()).thenReturn(now.plusSeconds(11));service.activate(r.id());
        assertEquals("CANCELED",service.request(owner,r.id()).status());assertTrue(repo.active(owner).isEmpty());
    }
    @Test void persistedDeadlineActivatesOnceAndMergesAnotherStart(){
        UUID owner=person();var r=service.start(owner,UUID.randomUUID(),"Synthetic gate");
        when(clock.instant()).thenReturn(now.plusSeconds(20));
        assertEquals(r.id(),repo.due(clock.instant()).getFirst().id());
        assertThrows(ResponseStatusException.class,()->service.cancel(owner,r.id()));
        service.activate(r.id());service.activate(r.id());var id=service.request(owner,r.id()).incidentId();
        assertNotNull(id);assertEquals(id,service.start(owner,UUID.randomUUID(),null).incidentId());assertEquals(1,service.list(owner,0).size());
    }
    @Test void guardiansNeedConsentAndLoseAccessAfterRevocation(){
        UUID owner=person(),g=guardian(owner),stranger=person();
        service.location(owner,new SelfSosRepository.Point(23.81,90.41,10,now));
        var r=service.start(owner,UUID.randomUUID(),"Synthetic gate");when(clock.instant()).thenReturn(now.plusSeconds(10));service.activate(r.id());
        UUID id=service.request(owner,r.id()).incidentId();assertNotNull(service.view(g,id).location());assertEquals(1,service.list(g,0).size());
        assertThrows(ResponseStatusException.class,()->service.view(stranger,id));assertThrows(ResponseStatusException.class,()->service.resolve(g,id,"not the owner"));
        circle.change(owner,circle.list(owner,0).getFirst().id(),"revoke");
        assertThrows(ResponseStatusException.class,()->service.view(g,id));assertTrue(service.list(g,0).isEmpty());
    }
    @Test void missingLocationDoesNotPreventSosAndOwnerCanResolve(){
        UUID owner=person(),g=guardian(owner);var r=service.start(owner,UUID.randomUUID(),null);
        when(clock.instant()).thenReturn(now.plusSeconds(10));service.activate(r.id());UUID id=service.request(owner,r.id()).incidentId();
        assertEquals("UNAVAILABLE",service.view(g,id).locationStatus());service.resolve(owner,id,"Demo complete");service.resolve(owner,id,"Retry");
        assertEquals("RESOLVED",service.view(owner,id).state());assertFalse(service.view(owner,id).canResolve());assertThrows(ResponseStatusException.class,()->service.view(g,id));
    }
    @Test void rejectsInvalidReplayedAndStaleLocation(){
        UUID owner=person();var p=new SelfSosRepository.Point(23,90,10,now);service.location(owner,p);
        assertThrows(ResponseStatusException.class,()->service.location(owner,p));
        assertThrows(ResponseStatusException.class,()->service.location(owner,new SelfSosRepository.Point(91,90,10,now.plusSeconds(1))));
        assertThrows(ResponseStatusException.class,()->service.location(owner,new SelfSosRepository.Point(23,90,10,now.minusSeconds(301))));
        service.clearLocation(owner);assertTrue(repo.point(owner).isEmpty());
    }
    @Test void requestOwnershipAndPaginationAreEnforced(){
        UUID owner=person(),other=person();var r=service.start(owner,UUID.randomUUID(),null);
        assertThrows(ResponseStatusException.class,()->service.request(other,r.id()));
        assertThrows(ResponseStatusException.class,()->service.cancel(other,r.id()));
        assertThrows(ResponseStatusException.class,()->service.list(owner,-1));
    }
}
