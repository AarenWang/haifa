import { useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronUp, Download, FileText, Loader2 } from 'lucide-react';
import type { ArtifactRecord } from '../types';
import { artifactDownloadUrl, fetchArtifact } from '../api/deerflowClient';
import { renderMarkdown } from '../utils/markdownRenderer';

interface ArtifactPanelProps {
  artifacts: ArtifactRecord[];
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatTime(value: string) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

export default function ArtifactPanel({ artifacts }: ArtifactPanelProps) {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [previewById, setPreviewById] = useState<Record<string, string>>({});
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(true);

  const selected = useMemo(
    () => artifacts.find((artifact) => artifact.artifactId === (selectedId || artifacts[0]?.artifactId)),
    [artifacts, selectedId]
  );
  const preview = selected ? previewById[selected.artifactId] || selected.preview || '' : '';

  const loadPreview = async (artifact: ArtifactRecord) => {
    if (previewById[artifact.artifactId] !== undefined || artifact.preview !== undefined) {
      return;
    }
    setLoadingId(artifact.artifactId);
    try {
      const detail = await fetchArtifact(artifact.artifactId);
      setPreviewById((current) => ({
        ...current,
        [artifact.artifactId]: detail.preview || '',
      }));
    } catch {
      setPreviewById((current) => ({
        ...current,
        [artifact.artifactId]: '',
      }));
    } finally {
      setLoadingId(null);
    }
  };

  useEffect(() => {
    if (!selected) {
      return;
    }
    loadPreview(selected);
  }, [selected?.artifactId]);

  if (artifacts.length === 0) {
    return null;
  }

  const selectArtifact = (artifact: ArtifactRecord) => {
    setSelectedId(artifact.artifactId);
    loadPreview(artifact);
  };

  return (
    <section className="artifact-panel">
      <div className="artifact-panel-header">
        <div>
          <div className="artifact-panel-title">Artifacts</div>
          <div className="artifact-panel-subtitle">{artifacts.length} deliverable files</div>
        </div>
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

      {expanded && (
      <div className="artifact-panel-grid">
        <div className="artifact-list">
          {artifacts.map((artifact) => {
            const active = selected?.artifactId === artifact.artifactId;
            return (
              <button
                key={artifact.artifactId}
                type="button"
                className={`artifact-card ${active ? 'active' : ''}`}
                onClick={() => selectArtifact(artifact)}
              >
                <FileText size={16} />
                <span className="artifact-card-main">
                  <span className="artifact-name">{artifact.filename}</span>
                  <span className="artifact-meta">
                    {formatBytes(artifact.size)} - {formatTime(artifact.createdAt)}
                  </span>
                </span>
              </button>
            );
          })}
        </div>

        <div className="artifact-preview">
          {selected && (
            <div className="artifact-preview-toolbar">
              <div className="artifact-preview-title">{selected.filename}</div>
              <a className="artifact-download" href={artifactDownloadUrl(selected.artifactId)}>
                <Download size={14} />
                Download
              </a>
            </div>
          )}
          {selected && loadingId === selected.artifactId ? (
            <div className="artifact-loading">
              <Loader2 size={16} className="spin-icon" />
              Loading preview
            </div>
          ) : (
            <div
              className="artifact-markdown-preview"
              dangerouslySetInnerHTML={{ __html: renderMarkdown(`\n${preview}`) }}
            />
          )}
        </div>
      </div>
      )}
    </section>
  );
}
