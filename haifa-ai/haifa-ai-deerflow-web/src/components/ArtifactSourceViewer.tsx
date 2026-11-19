import { useEffect, useRef } from 'react';
import { EditorState, type Extension } from '@codemirror/state';
import { EditorView } from '@codemirror/view';
import { basicSetup } from 'codemirror';
import { html } from '@codemirror/lang-html';
import { markdown } from '@codemirror/lang-markdown';
import { json } from '@codemirror/lang-json';
import { javascript } from '@codemirror/lang-javascript';
import { css } from '@codemirror/lang-css';
import { xml } from '@codemirror/lang-xml';

interface ArtifactSourceViewerProps {
  content: string;
  filename: string;
  mimeType: string;
  wrap: boolean;
  truncated?: boolean;
}

function languageExtension(filename: string, mimeType: string): Extension {
  const lower = filename.toLowerCase();
  const mime = mimeType.toLowerCase();
  if (mime.includes('html') || lower.endsWith('.html') || lower.endsWith('.htm')) return html();
  if (mime.includes('svg') || lower.endsWith('.svg') || lower.endsWith('.xml')) return xml();
  if (mime.includes('markdown') || lower.endsWith('.md') || lower.endsWith('.markdown')) return markdown();
  if (mime.includes('json') || lower.endsWith('.json')) return json();
  if (mime.includes('css') || lower.endsWith('.css')) return css();
  if (lower.endsWith('.ts') || lower.endsWith('.tsx')) {
    return javascript({ typescript: true, jsx: lower.endsWith('.tsx') });
  }
  if (lower.endsWith('.js') || lower.endsWith('.jsx') || mime.includes('javascript')) {
    return javascript({ jsx: lower.endsWith('.jsx') });
  }
  return [];
}

export default function ArtifactSourceViewer({
  content,
  filename,
  mimeType,
  wrap,
  truncated,
}: ArtifactSourceViewerProps) {
  const hostRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!hostRef.current) return;
    hostRef.current.innerHTML = '';

    const view = new EditorView({
      state: EditorState.create({
        doc: content || '',
        extensions: [
          basicSetup,
          languageExtension(filename, mimeType),
          EditorState.readOnly.of(true),
          EditorView.editable.of(false),
          wrap ? EditorView.lineWrapping : [],
          EditorView.theme({
            '&': {
              height: '100%',
              backgroundColor: '#ffffff',
              color: '#111827',
              fontSize: '13px',
            },
            '.cm-scroller': {
              overflow: 'auto',
              fontFamily: 'JetBrains Mono, Consolas, "SFMono-Regular", monospace',
            },
            '.cm-content': {
              minHeight: '100%',
            },
            '.cm-gutters': {
              backgroundColor: '#f8fafc',
              color: '#64748b',
              borderRight: '1px solid #e5e7eb',
            },
            '.cm-activeLine': {
              backgroundColor: '#f8fafc',
            },
            '.cm-activeLineGutter': {
              backgroundColor: '#eef2ff',
            },
          }),
        ],
      }),
      parent: hostRef.current,
    });

    return () => view.destroy();
  }, [content, filename, mimeType, wrap]);

  return (
    <div className="artifact-source-viewer">
      {truncated && (
        <div className="artifact-source-warning">
          Content is truncated. Download the full file for complete source.
        </div>
      )}
      {content ? <div ref={hostRef} className="artifact-source-editor" /> : <div className="artifact-empty-state">No source preview available.</div>}
    </div>
  );
}