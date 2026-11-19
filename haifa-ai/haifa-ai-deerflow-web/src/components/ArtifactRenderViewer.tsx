import { Download } from 'lucide-react';
import type { ArtifactRecord } from '../types';
import { artifactDownloadUrl, artifactRawUrl } from '../api/deerflowClient';
import { renderMarkdown } from '../utils/markdownRenderer';
import { formatBytes, getArtifactKind } from '../utils/artifactViewer';

interface ArtifactRenderViewerProps {
  artifact: ArtifactRecord;
  detail?: ArtifactRecord;
  zoom: 'fit' | number;
}

export default function ArtifactRenderViewer({ artifact, detail, zoom }: ArtifactRenderViewerProps) {
  const kind = getArtifactKind(artifact);
  const rawUrl = artifact.rawUrl || artifactRawUrl(artifact.artifactId);
  const downloadUrl = artifact.downloadUrl || artifactDownloadUrl(artifact.artifactId);

  if (kind === 'markdown') {
    return (
      <div
        className="artifact-render-markdown"
        dangerouslySetInnerHTML={{ __html: renderMarkdown(detail?.preview || artifact.preview || '') }}
      />
    );
  }

  if (kind === 'html' || kind === 'svg') {
    return (
      <iframe
        className="artifact-render-frame"
        src={rawUrl}
        sandbox=""
        referrerPolicy="no-referrer"
        title={artifact.filename}
      />
    );
  }

  if (kind === 'image') {
    return (
      <div className="artifact-image-stage">
        <img
          className={zoom === 'fit' ? 'artifact-render-image fit' : 'artifact-render-image'}
          style={zoom === 'fit' ? undefined : { width: `${zoom}%` }}
          src={rawUrl}
          alt={artifact.filename}
        />
      </div>
    );
  }

  if (kind === 'pdf') {
    return <iframe className="artifact-render-frame" src={rawUrl} title={artifact.filename} />;
  }

  return (
    <div className="artifact-unsupported-state">
      <div className="artifact-unsupported-title">Preview unavailable</div>
      <div className="artifact-unsupported-meta">
        {artifact.mimeType || 'application/octet-stream'} · {formatBytes(artifact.size)}
      </div>
      <a className="artifact-canvas-primary-link" href={downloadUrl} download={artifact.filename}>
        <Download size={16} />
        Download file
      </a>
    </div>
  );
}