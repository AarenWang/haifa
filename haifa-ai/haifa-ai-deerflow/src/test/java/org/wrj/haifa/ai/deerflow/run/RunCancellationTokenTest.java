package org.wrj.haifa.ai.deerflow.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RunCancellationTokenTest {

    @Test
    void preservesReasonAndSignalsModelAndToolObservers() {
        RunCancellationToken token = new RunCancellationToken("run-token");
        AtomicBoolean signalled = new AtomicBoolean();
        token.signal().subscribe(ignored -> { }, ignored -> { }, () -> signalled.set(true));

        assertThat(token.cancel("TIMEOUT")).isTrue();
        assertThat(token.cancel("USER_CANCELLED")).isFalse();
        assertThat(token.isCancelled()).isTrue();
        assertThat(token.reason()).isEqualTo("TIMEOUT");
        assertThat(signalled).isTrue();
        assertThatThrownBy(token::throwIfCancelled).isInstanceOf(RunCancelledException.class)
                .hasMessageContaining("TIMEOUT");
    }
}
