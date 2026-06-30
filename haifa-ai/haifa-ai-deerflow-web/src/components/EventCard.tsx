import { useState } from 'react';
import type { DeerFlowEvent } from '../types';
import {
  Play,
  Wrench,
  CheckCircle2,
  Brain,
  MessageSquare,
  Flag,
  AlertTriangle,
  Ban,
  ChevronDown,
  ChevronUp,
  Code2,
  Copy,
  Check,
  FileSearch,
  Layers,
  Link,
  Download,
  Highlighter,
  Shield,
  XCircle,
  FileText,
  Package,
  Users,
} from 'lucide-react';

const eventTitles: Record<string, string> = {
  RUN_STARTED: 'Run started',
  TOOL_STARTED: 'Tool started',
  TOOL_COMPLETED: 'Tool completed',
  MODEL_STARTED: 'Model started',
  MODEL_COMPLETED: 'Model answered',
  RUN_COMPLETED: 'Run completed',
  RUN_FAILED: 'Run failed',
  RUN_CANCELLED: 'Run cancelled',
  // Research events
  RESEARCH_PLAN_CREATED: 'Research plan created',
  RESEARCH_DIMENSION_STARTED: 'Research dimension started',
  RESEARCH_DIMENSION_COMPLETED: 'Research dimension completed',
  SOURCE_FOUND: 'Source found',
  SOURCE_FETCHED: 'Source fetched',
  EVIDENCE_EXTRACTED: 'Evidence extracted',
  QUALITY_GATE_STARTED: 'Quality gate started',
  QUALITY_GATE_PASSED: 'Quality gate passed',
  QUALITY_GATE_FAILED: 'Quality gate failed',
  REPORT_STARTED: 'Report started',
  REPORT_COMPLETED: 'Report completed',
  ARTIFACT_CREATED: 'Artifact created',
  SUBAGENT_STARTED: 'Subagent started',
  SUBAGENT_COMPLETED: 'Subagent completed',
  MODEL_DELTA: 'Model delta',
  TOOL_CALL_REQUESTED: 'Tool call requested',
  RESEARCH_STEP_COMPLETED: 'Research step completed',
};

const eventIcons: Record<string, React.ReactNode> = {
  RUN_STARTED: <Play size={16} />,
  TOOL_STARTED: <Wrench size={16} />,
  TOOL_COMPLETED: <CheckCircle2 size={16} />,
  MODEL_STARTED: <Brain size={16} />,
  MODEL_COMPLETED: <MessageSquare size={16} />,
  RUN_COMPLETED: <Flag size={16} />,
  RUN_FAILED: <AlertTriangle size={16} />,
  RUN_CANCELLED: <Ban size={16} />,
  // Research events
  RESEARCH_PLAN_CREATED: <FileSearch size={16} />,
  RESEARCH_DIMENSION_STARTED: <Layers size={16} />,
  RESEARCH_DIMENSION_COMPLETED: <Layers size={16} />,
  SOURCE_FOUND: <Link size={16} />,
  SOURCE_FETCHED: <Download size={16} />,
  EVIDENCE_EXTRACTED: <Highlighter size={16} />,
  QUALITY_GATE_STARTED: <Shield size={16} />,
  QUALITY_GATE_PASSED: <CheckCircle2 size={16} />,
  QUALITY_GATE_FAILED: <XCircle size={16} />,
  REPORT_STARTED: <FileText size={16} />,
  REPORT_COMPLETED: <FileText size={16} />,
  ARTIFACT_CREATED: <Package size={16} />,
  SUBAGENT_STARTED: <Users size={16} />,
  SUBAGENT_COMPLETED: <Users size={16} />,
  MODEL_DELTA: <Brain size={16} />,
  TOOL_CALL_REQUESTED: <Wrench size={16} />,
  RESEARCH_STEP_COMPLETED: <CheckCircle2 size={16} />,
};

const eventIconColors: Record<string, string> = {
  RUN_STARTED: 'blue',
  TOOL_STARTED: 'amber',
  TOOL_COMPLETED: 'amber',
  MODEL_STARTED: 'green',
  MODEL_COMPLETED: 'green',
  RUN_COMPLETED: 'blue',
  RUN_FAILED: 'red',
  RUN_CANCELLED: 'gray',
  // Research events
  RESEARCH_PLAN_CREATED: 'purple',
  RESEARCH_DIMENSION_STARTED: 'teal',
  RESEARCH_DIMENSION_COMPLETED: 'teal',
  SOURCE_FOUND: 'cyan',
  SOURCE_FETCHED: 'cyan',
  EVIDENCE_EXTRACTED: 'indigo',
  QUALITY_GATE_STARTED: 'orange',
  QUALITY_GATE_PASSED: 'green',
  QUALITY_GATE_FAILED: 'red',
  REPORT_STARTED: 'violet',
  REPORT_COMPLETED: 'violet',
  ARTIFACT_CREATED: 'pink',
  SUBAGENT_STARTED: 'teal',
  SUBAGENT_COMPLETED: 'teal',
  MODEL_DELTA: 'green',
  TOOL_CALL_REQUESTED: 'amber',
  RESEARCH_STEP_COMPLETED: 'blue',
};

interface EventCardProps {
  event: DeerFlowEvent;
}

export default function EventCard({ event }: EventCardProps) {
  const [expanded, setExpanded] = useState(false);
  const [inspectorOpen, setInspectorOpen] = useState(false);
  const title = eventTitles[event.type] || event.type;
  const icon = eventIcons[event.type] || <Flag size={16} />;
  const colorClass = eventIconColors[event.type] || 'gray';
  const hasContent = event.content && event.content.length > 0;

  return (
    <div className="event-card">
      <div className="event-card-header">
        <span className={`event-card-icon ${colorClass}`}>{icon}</span>
        <span className="event-card-title" title={title}>
          {title}
        </span>
        <span className="event-card-id">#{event.eventId}</span>
      </div>
      {hasContent && (
        <div className={`event-card-content ${expanded ? 'expanded' : ''}`}>
          {event.content}
        </div>
      )}
      <div className="event-card-actions">
        {hasContent && event.content.length > 120 && (
          <button
            type="button"
            className="event-card-btn"
            onClick={() => setExpanded((v) => !v)}
          >
            {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
            {expanded ? 'Collapse' : 'Expand'}
          </button>
        )}
        <button
          type="button"
          className="event-card-btn"
          onClick={() => setInspectorOpen((v) => !v)}
        >
          <Code2 size={12} />
          {inspectorOpen ? 'Hide JSON' : 'View JSON'}
        </button>
      </div>
      {inspectorOpen && (
        <div className="inline-inspector">
          <RawJsonInspector event={event} />
        </div>
      )}
    </div>
  );
}

function RawJsonInspector({ event }: { event: DeerFlowEvent }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(event, null, 2));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8,
        }}
      >
        <span
          style={{
            fontSize: 12,
            fontWeight: 700,
            color: '#adb5bd',
            fontFamily: 'var(--font-mono)',
          }}
        >
          Raw Event
        </span>
        <button
          type="button"
          className="event-card-btn"
          onClick={handleCopy}
          style={{ color: '#adb5bd' }}
        >
          {copied ? <Check size={12} /> : <Copy size={12} />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      <pre className="json-block">
        {JSON.stringify(event, null, 2)}
      </pre>
    </div>
  );
}

export { RawJsonInspector };
