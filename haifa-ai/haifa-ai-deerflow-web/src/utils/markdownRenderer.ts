import katex from 'katex';
import type { ArtifactRecord } from '../types';

/**
 * Lightweight markdown → HTML renderer. No external dependencies.
 * Escapes HTML first, then applies markdown syntax.
 */

function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function splitMarkdownTableRow(row: string): string[] {
  let normalized = row.trim();
  if (normalized.startsWith('|')) {
    normalized = normalized.slice(1);
  }
  if (normalized.endsWith('|')) {
    normalized = normalized.slice(0, -1);
  }
  return normalized.split('|').map((cell) => cell.trim());
}

function isMarkdownTableSeparator(row: string): boolean {
  const cells = splitMarkdownTableRow(row);
  return cells.length > 1 && cells.every((cell) => /^:?-{3,}:?$/.test(cell));
}

function tableAlignClass(separatorCell: string): string {
  const trimmed = separatorCell.trim();
  if (trimmed.startsWith(':') && trimmed.endsWith(':')) {
    return ' align-center';
  }
  if (trimmed.endsWith(':')) {
    return ' align-right';
  }
  return '';
}

function renderMarkdownTables(input: string): string {
  const lines = input.split('\n');
  const out: string[] = [];

  for (let i = 0; i < lines.length; i += 1) {
    const headerLine = lines[i];
    const separatorLine = lines[i + 1];
    if (
      headerLine?.includes('|')
      && separatorLine?.includes('|')
      && isMarkdownTableSeparator(separatorLine)
    ) {
      const headers = splitMarkdownTableRow(headerLine);
      const separators = splitMarkdownTableRow(separatorLine);
      const rows: string[][] = [];
      i += 2;

      while (i < lines.length && lines[i].trim().includes('|') && lines[i].trim() !== '') {
        rows.push(splitMarkdownTableRow(lines[i]));
        i += 1;
      }
      i -= 1;

      const thead = headers
        .map((cell, index) => `<th class="${tableAlignClass(separators[index] || '')}">${cell}</th>`)
        .join('');
      const tbody = rows
        .map((row) => {
          const cells = headers
            .map((_header, index) => `<td class="${tableAlignClass(separators[index] || '')}">${row[index] || ''}</td>`)
            .join('');
          return `<tr>${cells}</tr>`;
        })
        .join('');

      out.push(
        `<div class="markdown-table-wrapper"><table class="markdown-table"><thead><tr>${thead}</tr></thead><tbody>${tbody}</tbody></table></div>`
      );
    } else {
      out.push(headerLine);
    }
  }

  return out.join('\n');
}

function renderMarkdownProtected(text: string, artifacts?: ArtifactRecord[]): string {
  const mathBlocks: { placeholder: string; formula: string; isDisplay: boolean }[] = [];
  let mathCounter = 0;

  const isLikelyMath = (formula: string): boolean => {
    const trimmed = formula.trim();
    if (!trimmed) return false;
    if (trimmed.includes('\\')) return true;
    if (/[\^_+=\-*/<>~|{}]/.test(trimmed)) return true;
    const hasBoundarySpace = formula.startsWith(' ') || formula.endsWith(' ');
    if (!hasBoundarySpace) return true;
    if (/^[a-zA-Z]$/.test(trimmed)) return true;
    return false;
  };

  // Extract display math: $$...$$
  let processedText = text.replace(/(?<!\\)\$\$([\s\S]+?)(?<!\\)\$\$/g, (match, formula) => {
    if (!isLikelyMath(formula)) return match;
    const placeholder = `MATHBLOCKPLACEHOLDERX${mathCounter++}`;
    mathBlocks.push({ placeholder, formula: formula.trim(), isDisplay: true });
    return placeholder;
  });

  // Extract inline math: $...$
  processedText = processedText.replace(/(?<!\\)\$([^\$\n]+?)(?<!\\)\$/g, (match, formula) => {
    if (!isLikelyMath(formula)) return match;
    const placeholder = `MATHBLOCKPLACEHOLDERX${mathCounter++}`;
    mathBlocks.push({ placeholder, formula: formula.trim(), isDisplay: false });
    return placeholder;
  });

  let html = escapeHtml(processedText);
  const codeBlocks: string[] = [];

  html = html.replace(
    /```([A-Za-z0-9_-]+)?[ \t]*\n?([\s\S]*?)```/g,
    (_m, language, code) => {
      const index = codeBlocks.length;
      const langLabel = language
        ? `<span class="code-block-language">${language}</span>`
        : '<span class="code-block-language"></span>';
      codeBlocks.push(
        `<div class="code-block-wrapper"><div class="code-block-header">${langLabel}<button type="button" class="copy-code-btn" title="Copy code">复制</button></div><pre><code>${code.trim()}</code></pre></div>`
      );
      return `<div data-code-block-placeholder="${index}"></div>`;
    }
  );

  html = html.replace(/`([^`]+)`/g, (_m, code) => `<code>${code}</code>`);
  html = html.replace(/\n---\s*\n/g, '\n<hr>\n');

  html = html.replace(/\n#{6}\s+(.+)/g, '\n<h6>$1</h6>');
  html = html.replace(/\n#{5}\s+(.+)/g, '\n<h5>$1</h5>');
  html = html.replace(/\n#{4}\s+(.+)/g, '\n<h4>$1</h4>');
  html = html.replace(/\n#{3}\s+(.+)/g, '\n<h3>$1</h3>');
  html = html.replace(/\n#{2}\s+(.+)/g, '\n<h2>$1</h2>');
  html = html.replace(/\n#{1}\s+(.+)/g, '\n<h1>$1</h1>');

  html = html.replace(/\n(&gt;|>)\s+(.+)/g, '\n<blockquote>$2</blockquote>');
  html = html.replace(/<\/blockquote>\n<blockquote>/g, '<br>');

  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  html = html.replace(/_([^_]+)_/g, '<em>$1</em>');
  html = html.replace(/~~([^~]+)~~/g, '<del>$1</del>');
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_match, linkText, href) => {
    const artifactMatch = href.match(/\/api\/deerflow\/artifacts\/([a-f0-9-]+)/i);
    if (artifactMatch) {
      const artifactId = artifactMatch[1];
      let isImg = false;
      let rawUrl = '';
      if (artifacts) {
        const artifact = artifacts.find((art) => art.artifactId === artifactId);
        if (artifact) {
          isImg =
            (artifact.mimeType && artifact.mimeType.startsWith('image/')) ||
            /\.(png|jpe?g|gif|webp|svg)$/i.test(artifact.filename || '');
          rawUrl = artifact.rawUrl || `${href}/raw`;
        }
      }
      if (!isImg) {
        isImg = href.endsWith('/raw') || /\.(png|jpe?g|gif|webp|svg)/i.test(href);
        rawUrl = href.endsWith('/raw') ? href : `${href.split('?')[0]}/raw`;
      }
      if (isImg) {
        return `<div class="embedded-artifact-image-container"><img src="${rawUrl}" alt="${linkText}" class="embedded-artifact-image" /><div class="embedded-artifact-image-caption">${linkText}</div></div>`;
      }
    }
    return `<a href="${href}" target="_blank" rel="noopener noreferrer">${linkText}</a>`;
  });

  html = renderMarkdownTables(html);

  html = html.replace(
    /(?:\n(?:-|\*)\s+(.+))+/g,
    (match) => {
      const items = match
        .trim()
        .split('\n')
        .map((line) => `<li>${line.replace(/^[-*]\s+/, '')}</li>`)
        .join('');
      return `<ul>${items}</ul>`;
    }
  );

  html = html.replace(
    /(?:\n\d+\.\s+(.+))+/g,
    (match) => {
      const items = match
        .trim()
        .split('\n')
        .map((line) => `<li>${line.replace(/^\d+\.\s+/, '')}</li>`)
        .join('');
      return `<ol>${items}</ol>`;
    }
  );

  const lines = html.split('\n');
  const out: string[] = [];
  let buffer = '';
  const isBlock = (s: string) =>
    /^<(h[1-6]|pre|ul|ol|blockquote|hr|li|div)/i.test(s.trim());

  for (const line of lines) {
    if (line.trim() === '') {
      if (buffer.trim()) {
        out.push(`<p>${buffer.trim()}</p>`);
        buffer = '';
      }
    } else if (isBlock(line)) {
      if (buffer.trim()) {
        out.push(`<p>${buffer.trim()}</p>`);
        buffer = '';
      }
      out.push(line);
    } else {
      buffer += (buffer ? '<br>' : '') + line;
    }
  }
  if (buffer.trim()) {
    out.push(`<p>${buffer.trim()}</p>`);
  }

  let result = out.join('\n').replace(
    /<div data-code-block-placeholder="(\d+)"><\/div>/g,
    (_m, index) => codeBlocks[Number(index)] || ''
  );

  // Restore and render math blocks using KaTeX
  result = result.replace(/MATHBLOCKPLACEHOLDERX(\d+)/g, (match, indexStr) => {
    const index = parseInt(indexStr, 10);
    const block = mathBlocks[index];
    if (!block) return match;
    try {
      return katex.renderToString(block.formula, {
        displayMode: block.isDisplay,
        throwOnError: false,
      });
    } catch (err) {
      console.error('KaTeX rendering error:', err);
      return block.isDisplay ? `$$${block.formula}$$` : `$${block.formula}$`;
    }
  });

  return result;
}

export function renderMarkdown(text: string, artifacts?: ArtifactRecord[]): string {
  if (!text) return '';
  return renderMarkdownProtected(text, artifacts);

  let html = escapeHtml(text);

  // Code blocks (```) — must run before inline code
  html = html.replace(
    /```([\s\S]*?)```/g,
    (_m, code) => `<div class="code-block-wrapper"><div class="code-block-header"><button type="button" class="copy-code-btn" title="Copy code">复制</button></div><pre><code>${code.trim()}</code></pre></div>`
  );

  // Inline code (`)
  html = html.replace(/`([^`]+)`/g, (_m, code) => `<code>${code}</code>`);

  // Horizontal rule
  html = html.replace(/\n---\s*\n/g, '\n<hr>\n');

  // Headers
  html = html.replace(/\n#{6}\s+(.+)/g, '\n<h6>$1</h6>');
  html = html.replace(/\n#{5}\s+(.+)/g, '\n<h5>$1</h5>');
  html = html.replace(/\n#{4}\s+(.+)/g, '\n<h4>$1</h4>');
  html = html.replace(/\n#{3}\s+(.+)/g, '\n<h3>$1</h3>');
  html = html.replace(/\n#{2}\s+(.+)/g, '\n<h2>$1</h2>');
  html = html.replace(/\n#{1}\s+(.+)/g, '\n<h1>$1</h1>');

  // Blockquote
  html = html.replace(/\n(&gt;|>)\s+(.+)/g, '\n<blockquote>$2</blockquote>');
  // Merge consecutive blockquotes
  html = html.replace(/<\/blockquote>\n<blockquote>/g, '<br>');

  // Bold (must be before italic)
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');

  // Italic
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  html = html.replace(/_([^_]+)_/g, '<em>$1</em>');

  // Strikethrough
  html = html.replace(/~~([^~]+)~~/g, '<del>$1</del>');

  // Links
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

  // Unordered lists
  html = html.replace(
    /(?:\n(?:-|\*)\s+(.+))+/g,
    (match) => {
      const items = match
        .trim()
        .split('\n')
        .map((line) => `<li>${line.replace(/^[-*]\s+/, '')}</li>`)
        .join('');
      return `<ul>${items}</ul>`;
    }
  );

  // Ordered lists
  html = html.replace(
    /(?:\n\d+\.\s+(.+))+/g,
    (match) => {
      const items = match
        .trim()
        .split('\n')
        .map((line) => `<li>${line.replace(/^\d+\.\s+/, '')}</li>`)
        .join('');
      return `<ol>${items}</ol>`;
    }
  );

  // Paragraphs: split by blank lines, wrap non-block elements
  const lines = html.split('\n');
  const out: string[] = [];
  let buffer = '';

  const isBlock = (s: string) =>
    /^<(h[1-6]|pre|ul|ol|blockquote|hr|li)/i.test(s.trim());

  for (const line of lines) {
    if (line.trim() === '') {
      if (buffer.trim()) {
        out.push(`<p>${buffer.trim()}</p>`);
        buffer = '';
      }
    } else if (isBlock(line)) {
      if (buffer.trim()) {
        out.push(`<p>${buffer.trim()}</p>`);
        buffer = '';
      }
      out.push(line);
    } else {
      buffer += (buffer ? '<br>' : '') + line;
    }
  }
  if (buffer.trim()) {
    out.push(`<p>${buffer.trim()}</p>`);
  }

  return out.join('\n');
}
