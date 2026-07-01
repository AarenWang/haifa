package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.wrj.haifa.ai.deerflow.memory.AgentPersonaRecord;
import org.wrj.haifa.ai.deerflow.memory.MemoryCandidateRecord;
import org.wrj.haifa.ai.deerflow.memory.MemoryFactRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentPersonaStore;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryCandidateStore;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryFactStore;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemoryControllerTest {

    private AgentPersonaStore personaStore;
    private MemoryFactStore factStore;
    private MemoryCandidateStore candidateStore;
    private MemoryController controller;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        personaStore = mock(AgentPersonaStore.class);
        factStore = mock(MemoryFactStore.class);
        candidateStore = mock(MemoryCandidateStore.class);
        controller = new MemoryController(personaStore, factStore, candidateStore);

        exchange = mock(ServerWebExchange.class);
        ServerHttpRequest req = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "alice");
        when(exchange.getRequest()).thenReturn(req);
        when(req.getHeaders()).thenReturn(headers);
    }

    @Test
    void testGetPersonasResolvesUserId() {
        when(personaStore.findByUserId("alice")).thenReturn(List.of());
        
        StepVerifier.create(controller.getPersonas(exchange))
                .expectNext(List.of())
                .verifyComplete();

        verify(personaStore, times(1)).findByUserId("alice");
    }

    @Test
    void testSavePersonaChecksOwnership() {
        AgentPersonaRecord personaOfBob = new AgentPersonaRecord(
                "p1", "bob", "agent1", "PersonaName", "Desc", "SoulRules", true, Instant.now(), Instant.now()
        );
        when(personaStore.findById("p1")).thenReturn(Optional.of(personaOfBob));

        AgentPersonaRecord request = new AgentPersonaRecord(
                "p1", "alice", "agent1", "NewName", "NewDesc", "NewSoul", true, Instant.now(), Instant.now()
        );

        // Save should fail because p1 belongs to bob, but request is alice
        try {
            controller.savePersona(request, exchange).block();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void testApproveCandidateWithAddAction() {
        MemoryCandidateRecord candidate = new MemoryCandidateRecord(
                "c1", "alice", "agent1", "preference", "PrefContent", "reflection", "t1", "r1", 0.9, "pending", "ADD", null, Instant.now(), Instant.now()
        );
        when(candidateStore.findById("c1")).thenReturn(Optional.of(candidate));
        when(factStore.save(any(MemoryFactRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(controller.approveCandidate("c1", exchange))
                .assertNext(fact -> {
                    assertThat(fact.userId()).isEqualTo("alice");
                    assertThat(fact.content()).isEqualTo("PrefContent");
                    assertThat(fact.status()).isEqualTo("active");
                })
                .verifyComplete();

        verify(candidateStore, times(1)).save(argThat(c -> "approved".equals(c.status())));
    }

    @Test
    void testApproveCandidateWithUpdateAction() {
        MemoryCandidateRecord candidate = new MemoryCandidateRecord(
                "c2", "alice", "agent1", "preference", "NewPrefContent", "reflection", "t1", "r1", 0.9, "pending", "UPDATE", "f1", Instant.now(), Instant.now()
        );
        MemoryFactRecord existingFact = new MemoryFactRecord(
                "f1", "alice", "agent1", "preference", "OldPrefContent", "manual", "t0", "r0", 0.8, "active", Instant.now(), Instant.now(), Instant.now()
        );

        when(candidateStore.findById("c2")).thenReturn(Optional.of(candidate));
        when(factStore.findById("f1")).thenReturn(Optional.of(existingFact));
        when(factStore.save(any(MemoryFactRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(controller.approveCandidate("c2", exchange))
                .assertNext(fact -> {
                    assertThat(fact.id()).isEqualTo("f1");
                    assertThat(fact.userId()).isEqualTo("alice");
                    assertThat(fact.content()).isEqualTo("NewPrefContent");
                    assertThat(fact.status()).isEqualTo("active");
                })
                .verifyComplete();

        verify(candidateStore, times(1)).save(argThat(c -> "approved".equals(c.status())));
    }

    @Test
    void testApproveCandidateWithArchiveAction() {
        MemoryCandidateRecord candidate = new MemoryCandidateRecord(
                "c3", "alice", "agent1", "preference", "PrefContent", "reflection", "t1", "r1", 0.9, "pending", "ARCHIVE", "f2", Instant.now(), Instant.now()
        );
        MemoryFactRecord existingFact = new MemoryFactRecord(
                "f2", "alice", "agent1", "preference", "OldPrefContent", "manual", "t0", "r0", 0.8, "active", Instant.now(), Instant.now(), Instant.now()
        );

        when(candidateStore.findById("c3")).thenReturn(Optional.of(candidate));
        when(factStore.findById("f2")).thenReturn(Optional.of(existingFact));
        when(factStore.save(any(MemoryFactRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(controller.approveCandidate("c3", exchange))
                .assertNext(fact -> {
                    assertThat(fact.id()).isEqualTo("f2");
                    assertThat(fact.userId()).isEqualTo("alice");
                    assertThat(fact.status()).isEqualTo("archived");
                })
                .verifyComplete();

        verify(candidateStore, times(1)).save(argThat(c -> "approved".equals(c.status())));
    }

    @Test
    void testApproveCandidateRejectsBobResourceForAlice() {
        MemoryCandidateRecord candidateOfBob = new MemoryCandidateRecord(
                "c4", "bob", "agent1", "preference", "PrefContent", "reflection", "t1", "r1", 0.9, "pending", "ADD", null, Instant.now(), Instant.now()
        );
        when(candidateStore.findById("c4")).thenReturn(Optional.of(candidateOfBob));

        // Alice tries to approve Bob's candidate -> FORBIDDEN
        StepVerifier.create(controller.approveCandidate("c4", exchange))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException &&
                        ((ResponseStatusException) ex).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }
}
