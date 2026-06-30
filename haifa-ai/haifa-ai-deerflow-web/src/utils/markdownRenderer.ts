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

export function renderMarkdown(text: string): string {
  if (!text) return '';

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
