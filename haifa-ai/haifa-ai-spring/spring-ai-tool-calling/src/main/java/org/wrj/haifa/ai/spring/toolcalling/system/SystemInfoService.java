package org.wrj.haifa.ai.spring.toolcalling.system;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.spring.toolcalling.model.SystemInfoSummary;
import org.wrj.haifa.ai.spring.toolcalling.tool.ChatTool;

/**
 * Tool implementation that inspects the host by executing a curated set of
 * shell commands.
 */
@Service
public class SystemInfoService {

    private final SystemCommandRunner commandRunner;
    private final SystemCommandPlanner commandPlanner;

    public SystemInfoService(SystemCommandRunner commandRunner, SystemCommandPlanner commandPlanner) {
        this.commandRunner = commandRunner;
        this.commandPlanner = commandPlanner;
    }

    @ChatTool(name = "system_inspect",
            description = "Inspect the local machine for CPU, memory, processes, open ports and disk usage information.")
    public SystemInfoSummary inspect(String instruction) {
        EnumSet<Section> sections = resolveSections(instruction);
        List<SectionExecution> executions = sections.stream()
                .map(section -> runSection(section, instruction))
                .toList();

        String body = executions.stream()
                .map(SectionExecution::body)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()))
                .trim();

        if (!StringUtils.hasText(body)) {
            body = "No system information was collected.";
        }

        List<String> highlights = executions.stream()
                .map(SectionExecution::highlight)
                .toList();

        return new SystemInfoSummary("Local system inspection", body, highlights);
    }

    private SectionExecution runSection(Section section, String instruction) {
        String command = determineCommand(section, instruction);
        String output = execute(section, command);
        return new SectionExecution(section.header() + System.lineSeparator() + output, section.highlight(command));
    }

    private String determineCommand(Section section, String instruction) {
        PlannedCommand planned = this.commandPlanner.plan(instruction, section.directive());
        if (planned != null && planned.hasCommand()) {
            String candidate = planned.command().trim();
            if (CommandSafety.isSafe(candidate)) {
                return candidate;
            }
        }
        return section.command();
    }

    private String execute(Section section, String command) {
        CommandResult result = commandRunner.run(section.description(), List.of("sh", "-c", command));
        if (!result.isSuccess()) {
            String error = StringUtils.hasText(result.stderr()) ? result.stderr().trim() : result.stdout().trim();
            if (StringUtils.hasText(error)) {
                return section.failureMessage() + " (" + error + ")";
            }
            return section.failureMessage();
        }
        String output = StringUtils.hasText(result.stdout()) ? result.stdout().trim() : result.stderr().trim();
        if (!StringUtils.hasText(output)) {
            return section.failureMessage();
        }
        return output;
    }

    private EnumSet<Section> resolveSections(String instruction) {
        if (!StringUtils.hasText(instruction)) {
            return EnumSet.allOf(Section.class);
        }
        String normalized = instruction.toLowerCase(Locale.ROOT);
        EnumSet<Section> sections = EnumSet.noneOf(Section.class);
        if (normalized.contains("cpu") || normalized.contains("processor")) {
            sections.add(Section.CPU);
        }
        if (normalized.contains("memory") || normalized.contains("ram")) {
            sections.add(Section.MEMORY);
        }
        if (normalized.contains("process") || normalized.contains("task")) {
            sections.add(Section.PROCESSES);
        }
        if (normalized.contains("port") || normalized.contains("network")) {
            sections.add(Section.PORTS);
        }
        if (normalized.contains("disk") || normalized.contains("storage") || normalized.contains("filesystem")) {
            sections.add(Section.DISKS);
        }
        if (sections.isEmpty()) {
            return EnumSet.allOf(Section.class);
        }
        return sections;
    }

    private record SectionExecution(String body, String highlight) {
    }

    private enum Section {
        CPU("CPU overview", "CPU inspection", "lscpu | head -n 5", "Unable to determine CPU details."),
        MEMORY("Memory status", "Memory inspection", "free -h", "Unable to determine memory usage."),
        PROCESSES("Top processes", "Process inspection",
                "ps -eo pid,comm,%cpu,%mem --sort=-%cpu | head -n 6",
                "Unable to determine running processes."),
        PORTS("Listening ports", "Network port inspection", "ss -tuln",
                "Unable to determine listening ports."),
        DISKS("Disk usage", "Disk inspection", "df -h", "Unable to determine disk usage.");

        private final String header;
        private final String description;
        private final String command;
        private final String failureMessage;

        Section(String header, String description, String command, String failureMessage) {
            this.header = header;
            this.description = description;
            this.command = command;
            this.failureMessage = failureMessage;
        }

        String header() {
            return header;
        }

        String description() {
            return description;
        }

        String command() {
            return command;
        }

        String failureMessage() {
            return failureMessage;
        }

        CommandDirective directive() {
            return new CommandDirective(name(), description, command, List.of(command));
        }

        String highlight(String executedCommand) {
            return header + " via `" + executedCommand + "`";
        }
    }
}
