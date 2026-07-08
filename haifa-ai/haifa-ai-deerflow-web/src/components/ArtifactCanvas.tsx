import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Check,
  Copy,
  Download,
  FileCode,
  FileText,
  Maximize2,
  Minus,
  Plus,
  RotateCcw,
  WrapText,
  X,
} from 'lucide-react';
import type { ArtifactRecord } from '../types';
import { artifactDownloadUrl, fetchArtifact } from '../api/deerflowClient';
import ArtifactRenderViewer from './ArtifactRenderViewer';
import ArtifactSourceViewer from './ArtifactSourceViewer';
import {
  canRenderArtifact,
  canViewSource,
  formatArtifactTime,
  formatBytes,
  getArtifactKind,
  getDefaultCanvasMode,
  type CanvasMode,
} from '../utils/artifactViewer';

interface ArtifactCanvasProps {
  artifacts: ArtifactRecord[];
  selectedArtifactId: string;
  onSelectArtifact: (artifactId: string) => void;
  onClose: () => void;
}

export default function ArtifactCanvas({
  artifacts,
  selectedArtifactId,
  onSelectArtifact,
  onClose,
}: ArtifactCanvasProps) {
  const selectedArtifact = useMemo(
    () => artifacts.find((artifact) => artifact.artifactId === selectedArtifactId),
    [artifacts, selectedArtifactId]
  );
  const [detailById, setDetailById] = useState<Record<string, ArtifactRecord>>({});
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useState<CanvasMode>('render');
  const [wrapSource, setWrapSource] = useState(false);
  const [zoom, setZoom] = useState<'fit' | number>('fit');
  const [copied, setCopied] = useState(false);

  const detail = selectedArtifact ? detailById[selectedArtifact.artifactId] || selectedArtifact : undefined;
  const renderAvailable = selectedArtifact ? canRenderArtifact(selectedArtifact) : false;
  const sourceAvailable = selectedArtifact ? canViewSource(selectedArtifact) : false;
  const kind = selectedArtifact ? getArtifactKind(selectedArtifact) : 'unsupported';
  const downloadUrl = selectedArtifact
    ? selectedArtifact.downloadUrl || artifactDownloadUrl(selectedArtifact.artifactId)
    : '#';

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  useEffect(() => {
    if (!selectedArtifact) return;
    setMode(getDefaultCanvasMode(selectedArtifact));
    setZoom('fit');
    setCopied(false);
    setError(null);
  }, [selectedArtifact?.artifactId]);

  useEffect(() => {
    if (!selectedArtifact || !sourceAvailable) return;
    if (detailById[selectedArtifact.artifactId]) return;

    let cancelled = false;
    setLoadingId(selectedArtifact.artifactId);
    setError(null);
    fetchArtifact(selectedArtifact.artifactId)
      .then((artifact) => {
        if (cancelled) return;
        setDetailById((current) => ({ ...current, [artifact.artifactId]: artifact }));
      })
      .catch((err) => {
        if (cancelled) return;
        setError((err as Error).message || 'Failed to load artifact preview.');
      })
      .finally(() => {
        if (!cancelled) setLoadingId(null);
      });

    return () => {
      cancelled = true;
    };
  }, [detailById, selectedArtifact, sourceAvailable]);

  const copySource = useCallback(async () => {
    if (!detail?.preview) return;
    await navigator.clipboard.writeText(detail.preview);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1400);
  }, [detail?.preview]);

  const decreaseZoom = () => {
    setZoom((current) => {
      const value = current === 'fit' ? 100 : current;
      return Math.max(25, value - 25);
    });
  };

  const increaseZoom = () => {
    setZoom((current) => {
      const value = current === 'fit' ? 100 : current;
      return Math.min(300, value + 25);
    });
  };

  if (!selectedArtifact) {
    return null;
  }

  const showSegmented = renderAvailable && sourceAvailable;
  const effectiveMode = mode === 'source' && !sourceAvailable ? 'render' : mode;
  const isImage = kind === 'image';

  return (
    <div className="artifact-canvas-overlay" role="dialog" aria-modal="true" aria-label="Artifact canvas">
      <div className="artifact-canvas">
        <aside className="artifact-canvas-sidebar" aria-label="Artifacts">
          <div className="artifact-canvas-sidebar-head">
            <div className="artifact-canvas-sidebar-title">Canvas</div>
            <div className="artifact-canvas-sidebar-subtitle">{artifacts.length} artifacts</div>
          </div>
          <div className="artifact-canvas-file-list">
            {artifacts.map((artifact) => {
              const active = artifact.artifactId === selectedArtifact.artifactId;
              return (
                <button
                  key={artifact.artifactId}
                  type="button"
                  className={`artifact-canvas-file ${active ? 'active' : ''}`}
                  onClick={() => onSelectArtifact(artifact.artifactId)}
                >
                  <FileText size={16} />
                  <span className="artifact-canvas-file-main">
                    <span className="artifact-canvas-file-name">{artifact.filename}</span>
                    <span className="artifact-canvas-file-meta">
                      {formatBytes(artifact.size)} · {formatArtifactTime(artifact.createdAt)}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>
        </aside>

        <main className="artifact-canvas-main">
          <header className="artifact-canvas-header">
            <div className="artifact-canvas-title-block">
              <div className="artifact-canvas-title">{selectedArtifact.filename}</div>
              <div className="artifact-canvas-meta">
                {selectedArtifact.mimeType || 'application/octet-stream'} · {formatBytes(selectedArtifact.size)}
              </div>
            </div>
            <button type="button" className="artifact-canvas-icon-button" onClick={onClose} title="Close canvas" aria-label="Close canvas">
              <X size={18} />
            </button>
          </header>

          <div className="artifact-canvas-toolbar">
            {showSegmented && (
              <div className="artifact-canvas-segmented" role="tablist" aria-label="Canvas mode">
                <button
                  type="button"
                  className={effectiveMode === 'render' ? 'active' : ''}
                  onClick={() => setMode('render')}
                >
                  Render
                </button>
                <button
                  type="button"
                  className={effectiveMode === 'source' ? 'active' : ''}
                  onClick={() => setMode('source')}
                >
                  Source
                </button>
              </div>
            )}
            {!showSegmented && sourceAvailable && !renderAvailable && (
              <div className="artifact-canvas-mode-label"><FileCode size={15} /> Source</div>
            )}
            {!showSegmented && renderAvailable && !sourceAvailable && (
              <div className="artifact-canvas-mode-label"><Maximize2 size={15} /> Render</div>
            )}

            <div className="artifact-canvas-toolbar-spacer" />

            {effectiveMode === 'source' && sourceAvailable && (
              <>
                <button type="button" className="artifact-canvas-tool-button" onClick={copySource} disabled={!detail?.preview}>
                  {copied ? <Check size={15} /> : <Copy size={15} />}
                  {copied ? 'Copied' : 'Copy'}
                </button>
                <button
                  type="button"
                  className={`artifact-canvas-tool-button ${wrapSource ? 'active' : ''}`}
                  onClick={() => setWrapSource((value) => !value)}
                >
                  <WrapText size={15} />
                  Wrap
                </button>
              </>
            )}

            {effectiveMode === 'render' && isImage && (
              <>
                <button type="button" className="artifact-canvas-tool-button" onClick={() => setZoom('fit')}>
                  <Maximize2 size={15} />
                  Fit
                </button>
                <button type="button" className="artifact-canvas-tool-button" onClick={() => setZoom(100)}>
                  <RotateCcw size={15} />
                  100%
                </button>
                <button type="button" className="artifact-canvas-icon-button" onClick={decreaseZoom} title="Zoom out" aria-label="Zoom out">
                  <Minus size={15} />
                </button>
                <button type="button" className="artifact-canvas-icon-button" onClick={increaseZoom} title="Zoom in" aria-label="Zoom in">
                  <Plus size={15} />
                </button>
              </>
            )}

            <a className="artifact-canvas-tool-button" href={downloadUrl} download={selectedArtifact.filename}>
              <Download size={15} />
              Download
            </a>
          </div>

          <section className="artifact-canvas-body">
            {error && <div className="artifact-canvas-error">{error}</div>}
            {loadingId === selectedArtifact.artifactId && sourceAvailable && (
              <div className="artifact-canvas-loading">Loading artifact source...</div>
            )}
            {effectiveMode === 'source' && sourceAvailable ? (
              <ArtifactSourceViewer
                content={detail?.preview || ''}
                filename={selectedArtifact.filename}
                mimeType={selectedArtifact.mimeType}
                wrap={wrapSource}
                truncated={detail?.previewTruncated}
              />
            ) : (
              <ArtifactRenderViewer artifact={selectedArtifact} detail={detail} zoom={zoom} />
            )}
          </section>
        </main>
      </div>
    </div>
  );
}