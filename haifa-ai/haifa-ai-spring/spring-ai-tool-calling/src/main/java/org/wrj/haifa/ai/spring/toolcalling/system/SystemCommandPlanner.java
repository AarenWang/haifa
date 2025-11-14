package org.wrj.haifa.ai.spring.toolcalling.system;

/**
 * Strategy interface that determines which system command should be executed for a
 * given inspection section. Implementations typically consult a large language
 * model to synthesize a safe shell command.
 */
public interface SystemCommandPlanner {

    PlannedCommand plan(String instruction, CommandDirective directive);
}
