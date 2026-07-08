import { useState } from 'react';
import { ChevronDown, ChevronUp, Download, FileText, Maximize2 } from 'lucide-react';
import type { ArtifactRecord } from '../types';
import { artifactDownloadUrl } from '../api/deerflowClient';
import { formatArtifactTime, formatBytes, getArtifactKind } from '../utils/artifactViewer';

interface ArtifactPanelProps {
  artifacts: ArtifactRecord[];
  onOpenCanvas?: (artifactId?: string) => void;
}

export default function ArtifactPanel({ artifacts, onOpenCanvas }: ArtifactPanelProps) {
  const [expanded, setExpanded] = useState(true);

  if (artifacts.length === 0) {
    return null;
  }

  return (
    <section className="artifact-panel">
      <div className="artifact-panel-header">
        <div>
          <div className="artifact-panel-title">Artifacts</div>
          <div className="artifact-panel-subtitle">{artifacts.length} deliverable files</div>
        </div>
        <div className="artifact-panel-actions">
          <button type="button" className="artifact-open-canvas" onClick={() => onOpenCanvas?.()}>
            <Maximize2 size={15} />
            Open Canvas
          </button>
          <button
            type="button"
            className="panel-collapse-button"
            onClick={() => setExpanded((value) => !value)}
            aria-label={expanded ? 'Collapse artifacts panel' : 'Expand artifacts panel'}
            title={expanded ? 'Collapse' : 'Expand'}
          >
            {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </button>
        </div>
      </div>

      {expanded && (
        <div className="artifact-compact-list">
          {artifacts.map((artifact) => (
            <div key={artifact.artifactId} className="artifact-compact-row">
              <button type="button" className="artifact-compact-main" onClick={() => onOpenCanvas?.(artifact.artifactId)}>
                <FileText size={16} />
                <span className="artifact-card-main">
                  <span className="artifact-name">{artifact.filename}</span>
                  <span className="artifact-meta">
                    {getArtifactKind(artifact)} · {formatBytes(artifact.size)} · {formatArtifactTime(artifact.createdAt)}
                  </span>
                </span>
              </button>
              <div className="artifact-compact-actions">
                <button
                  type="button"
                  className="artifact-icon-action"
                  onClick={() => onOpenCanvas?.(artifact.artifactId)}
                  title="Open in canvas"
                  aria-label={`Open ${artifact.filename} in canvas`}
                >
                  <Maximize2 size={15} />
                </button>
                <a
                  className="artifact-icon-action"
                  href={artifact.downloadUrl || artifactDownloadUrl(artifact.artifactId)}
                  download={artifact.filename}
                  title="Download"
                  aria-label={`Download ${artifact.filename}`}
                >
                  <Download size={15} />
                </a>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}