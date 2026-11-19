import type { ArtifactRecord } from '../types';

export type ArtifactKind = 'markdown' | 'html' | 'svg' | 'image' | 'pdf' | 'text' | 'unsupported';
export type CanvasMode = 'render' | 'source';

const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'gif', 'webp']);
const TEXT_EXTENSIONS = new Set([
  'txt',
  'log',
  'json',
  'csv',
  'tsv',
  'xml',
  'css',
  'js',
  'jsx',
  'ts',
  'tsx',
  'yaml',
  'yml',
]);

export function getArtifactKind(artifact: ArtifactRecord): ArtifactKind {
  const mime = normalizeMime(artifact.mimeType);
  const ext = extension(artifact.filename);

  if (mime === 'text/markdown' || ext === 'md' || ext === 'markdown') return 'markdown';
  if (mime === 'text/html' || ext === 'html' || ext === 'htm') return 'html';
  if (mime === 'image/svg+xml' || ext === 'svg') return 'svg';
  if (mime.startsWith('image/') || IMAGE_EXTENSIONS.has(ext)) return 'image';
  if (mime === 'application/pdf' || ext === 'pdf') return 'pdf';
  if (isTextLike(mime, ext)) return 'text';
  return 'unsupported';
}

export function canRenderArtifact(artifact: ArtifactRecord): boolean {
  if (artifact.renderable === true) return true;
  return ['markdown', 'html', 'svg', 'image', 'pdf'].includes(getArtifactKind(artifact));
}

export function canViewSource(artifact: ArtifactRecord): boolean {
  if (artifact.sourceViewable === true) return true;
  return ['markdown', 'html', 'svg', 'text'].includes(getArtifactKind(artifact));
}

export function getDefaultCanvasMode(artifact: ArtifactRecord): CanvasMode {
  const kind = getArtifactKind(artifact);
  if (kind === 'text') return 'source';
  if (canRenderArtifact(artifact)) return 'render';
  if (canViewSource(artifact)) return 'source';
  return 'render';
}

export function getCodeMirrorLanguage(artifact: ArtifactRecord): string {
  const mime = normalizeMime(artifact.mimeType);
  const ext = extension(artifact.filename);
  if (mime.includes('html') || ext === 'html' || ext === 'htm') return 'html';
  if (mime.includes('svg') || ext === 'svg' || ext === 'xml') return 'xml';
  if (mime.includes('markdown') || ext === 'md' || ext === 'markdown') return 'markdown';
  if (mime.includes('json') || ext === 'json') return 'json';
  if (mime.includes('css') || ext === 'css') return 'css';
  if (ext === 'ts' || ext === 'tsx') return 'typescript';
  if (ext === 'js' || ext === 'jsx' || mime.includes('javascript')) return 'javascript';
  return 'text';
}

export function formatBytes(size: number): string {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatArtifactTime(value: string): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function isTextLike(mime: string, ext: string): boolean {
  return mime.startsWith('text/')
    || mime === 'application/json'
    || mime === 'application/xml'
    || mime === 'application/javascript'
    || TEXT_EXTENSIONS.has(ext);
}

function extension(filename: string): string {
  const lower = (filename || '').toLowerCase();
  const dot = lower.lastIndexOf('.');
  return dot >= 0 && dot < lower.length - 1 ? lower.slice(dot + 1) : '';
}

function normalizeMime(mimeType: string): string {
  return (mimeType || 'application/octet-stream').split(';')[0].trim().toLowerCase();
}