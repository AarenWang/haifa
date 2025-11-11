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

    public SystemInfoService(SystemCommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    @ChatTool(name = "system_inspect",
            description = "Inspect the local machine for CPU, memory, processes, open ports and disk usage information.")
    public SystemInfoSummary inspect(String instruction) {
        EnumSet<Section> sections = resolveSections(instruction);
        String body = sections.stream()
                .map(section -> section.header() + System.lineSeparator() + execute(section))
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()))
                .trim();

        if (!StringUtils.hasText(body)) {
            body = "No system information was collected.";
        }

        List<String> highlights = sections.stream()
                .map(Section::highlight)
                .toList();

        return new SystemInfoSummary("Local system inspection", body, highlights);
    }

    private String execute(Section section) {
        CommandResult result = commandRunner.run(section.description(), List.of("sh", "-c", section.command()));
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

        String highlight() {
            return header + " via `" + command + "`";
        }
    }
}
