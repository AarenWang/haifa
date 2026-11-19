package org.wrj.haifa.ai.deerflow.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class SandboxConfigurationValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SandboxConfigurationValidator.class);
    private final DeerFlowProperties properties;

    public SandboxConfigurationValidator(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        SandboxBackend backend = SandboxBackend.from(properties.getSandbox().getBackend());
        if (properties.getSandbox().isRunScriptLocalUnsafeAllowed()) {
            log.warn("Deprecated sandbox setting run-script-local-unsafe-allowed=true is active; migrate to allow-host-execution");
        }
        if (backend == SandboxBackend.LOCAL_TRUSTED) {
            if (!properties.getSandbox().isAllowHostExecution()
                    || !properties.getSandbox().getLocalTrusted().isEnabled()) {
                throw new IllegalStateException("local-trusted requires allow-host-execution=true and local-trusted.enabled=true");
            }
            log.warn("Local Trusted executes host processes without strong isolation and is only for trusted single-user development");
        }
    }
}
