package com.pashe.app.guardian;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import com.pashe.app.guardian.sos.*;
import com.pashe.app.identity.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="pashe.guardian.scheduling-enabled=false")
class SelfSosConcurrencyTests {
    @Autowired SelfSosService service;@Autowired UserService users;@Autowired JdbcTemplate db;
    @Autowired PlatformTransactionManager transactions;@MockitoBean Clock clock;
    UUID owner;
    @Test void deadlineCancellationAndConcurrentWorkersCreateOneIncident()throws Exception{
        Instant now=Instant.parse("2026-11-18T05:00:00Z");when(clock.instant()).thenReturn(now);
        owner=users.create("Synthetic race",UUID.randomUUID()+"@example.test","01712345678","not-a-login-hash",true);
        var request=service.start(owner,UUID.randomUUID(),null);when(clock.instant()).thenReturn(now.plusSeconds(10));
        CountDownLatch go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(3)){
            Callable<Void> activation=()->{go.await();service.activate(request.id());return null;};
            var one=pool.submit(activation);var two=pool.submit(activation);
            var cancel=pool.submit(()->{go.await();assertEquals(409,assertThrows(ResponseStatusException.class,()->service.cancel(owner,request.id())).getStatusCode().value());return null;});
            go.countDown();one.get(30,TimeUnit.SECONDS);two.get(30,TimeUnit.SECONDS);cancel.get(30,TimeUnit.SECONDS);
        }
        assertEquals("ACTIVATED",service.request(owner,request.id()).status());assertEquals(1,service.list(owner,0).size());
    }
    @AfterEach void cleanup(){
        if(owner==null)return;
        new TransactionTemplate(transactions).executeWithoutResult(status->{
            db.update("DELETE FROM self_sos_requests WHERE recipient_id=?",owner);
            db.update("DELETE FROM self_sos_incidents WHERE recipient_id=?",owner);
            db.update("DELETE FROM users WHERE id=?",owner);
        });
    }
}
