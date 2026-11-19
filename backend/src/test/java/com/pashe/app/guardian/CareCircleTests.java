package com.pashe.app.guardian;
import com.pashe.app.guardian.carecircle.*;
import com.pashe.app.identity.UserService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="pashe.guardian.scheduling-enabled=false")
@Transactional
class CareCircleTests {
 @Autowired CareCircleService circle;
 @Autowired UserService users;
 UUID person(String email) { return users.create("Synthetic user",email,"01712345678","not-a-login-hash",true); }
 @Test void consentAndRevocation() {
   String email=UUID.randomUUID()+"@example.test";
   UUID recipient=person(UUID.randomUUID()+"@example.test"), guardian=person(email), stranger=person(UUID.randomUUID()+"@example.test");
   circle.invite(recipient,email,"GUARDIAN"); var membership=circle.list(recipient,0).getFirst();
   assertFalse(circle.guardian(recipient,guardian));
   assertThrows(ResponseStatusException.class,() -> circle.change(stranger,membership.id(),"accept"));
   assertThrows(ResponseStatusException.class,() -> circle.change(recipient,membership.id(),"accept"));
   circle.change(guardian,membership.id(),"accept"); assertTrue(circle.guardian(recipient,guardian));
   circle.change(recipient,membership.id(),"revoke"); assertFalse(circle.guardian(recipient,guardian));
 }
 @Test void selfInviteAndNeighborHasNoGuardianAccess() {
   String email=UUID.randomUUID()+"@example.test"; UUID recipient=person(email);
   assertThrows(ResponseStatusException.class,() -> circle.invite(recipient,email,"GUARDIAN"));
   String other=UUID.randomUUID()+"@example.test"; UUID neighbor=person(other);
   circle.invite(recipient,other,"TRUSTED_NEIGHBOR"); var membership=circle.list(recipient,0).getFirst();
   circle.change(neighbor,membership.id(),"accept");
   assertFalse(circle.guardian(recipient,neighbor));
 }
}
