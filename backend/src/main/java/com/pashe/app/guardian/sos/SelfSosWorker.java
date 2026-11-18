package com.pashe.app.guardian.sos;

import java.time.Clock;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="pashe.guardian.scheduling-enabled",havingValue="true",matchIfMissing=true)
public class SelfSosWorker {
    private final SelfSosRepository repo;private final SelfSosService service;private final Clock clock;
    public SelfSosWorker(SelfSosRepository repo,SelfSosService service,Clock clock){this.repo=repo;this.service=service;this.clock=clock;}
    @Scheduled(fixedDelay=1000) public void activate(){
        for(var request:repo.due(clock.instant()))try{service.activate(request.id());}catch(RuntimeException ex){LoggerFactory.getLogger(SelfSosWorker.class).warn("SOS activation will be retried");}
    }
    @Scheduled(fixedDelay=3600000) public void purge(){repo.purgePoints(clock.instant().minusSeconds(86400));}
}
