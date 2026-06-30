import { useMemo, useState } from 'react';
import { ChevronRight, ExternalLink, FileSearch, Highlighter, Link2 } from 'lucide-react';
import type { EvidenceItem, ResearchSource } from '../types';

interface ResearchInspectorProps {
  sources: ResearchSource[];
  evidenceItems: EvidenceItem[];
}

export default function ResearchInspector({ sources, evidenceItems }: ResearchInspectorProps) {
  const [selectedSourceId, setSelectedSourceId] = useState<string | null>(null);

  const selectedSource = useMemo(
    () => sources.find((source) => source.sourceId === selectedSourceId) || sources[0],
    [selectedSourceId, sources]
  );

  const visibleEvidence = useMemo(() => {
    if (!selectedSource) return [];
    return evidenceItems.filter((item) => item.sourceId === selectedSource.sourceId);
  }, [evidenceItems, selectedSource]);

  if (sources.length === 0 && evidenceItems.length === 0) {
    return null;
  }

  return (
    <section className="research-panel">
      <div className="research-panel-header">
        <div>
          <div className="research-panel-title">Research Sources</div>
          <div className="research-panel-subtitle">
            {sources.length} sources, {evidenceItems.length} evidence items
          </div>
        </div>
      </div>

      <div className="research-panel-grid">
        <div className="research-source-list">
          {sources.map((source) => {
            const isActive = selectedSource?.sourceId === source.sourceId;
            return (
              <button
                key={source.sourceId}
                type="button"
                className={`research-source-card ${isActive ? 'active' : ''}`}
                onClick={() => setSelectedSourceId(source.sourceId)}
              >
                <div className="research-source-topline">
                  <span className="research-source-status">
                    {source.fetched ? 'Fetched' : 'Candidate'}
                  </span>
                  <span className="research-source-citations">
                    {source.citationCount} citations
                  </span>
                </div>
                <div className="research-source-title">{source.title || source.url}</div>
                <div className="research-source-meta">
                  <Link2 size={12} />
                  {source.domain || 'unknown domain'}
                </div>
                {source.snippet && (
                  <div className="research-source-snippet">{source.snippet}</div>
                )}
              </button>
            );
          })}
        </div>

        <div className="research-evidence-drawer">
          {selectedSource ? (
            <>
              <div className="research-evidence-header">
                <div>
                  <div className="research-evidence-title">
                    {selectedSource.title || selectedSource.url}
                  </div>
                  <a
                    className="research-evidence-link"
                    href={selectedSource.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <ExternalLink size={12} />
                    Open source
                  </a>
                </div>
                <div className="research-evidence-score">
                  Credibility {selectedSource.credibility.toFixed(2)}
                </div>
              </div>

              {visibleEvidence.length === 0 ? (
                <div className="research-empty">
                  <FileSearch size={18} />
                  No extracted evidence yet for this source.
                </div>
              ) : (
                <div className="research-evidence-list">
                  {visibleEvidence.map((evidence) => (
                    <div key={evidence.evidenceId} className="research-evidence-card">
                      <div className="research-evidence-badge">
                        <Highlighter size={12} />
                        {evidence.dimension}
                        <span>{evidence.confidence.toFixed(2)}</span>
                      </div>
                      <div className="research-evidence-claim">{evidence.claim}</div>
                      <details className="research-evidence-details">
                        <summary>
                          <ChevronRight size={12} />
                          View quote / paraphrase
                        </summary>
                        <div>{evidence.quoteOrParaphrase}</div>
                      </details>
                    </div>
                  ))}
                </div>
              )}
            </>
          ) : (
            <div className="research-empty">
              <FileSearch size={18} />
              Select a source to inspect extracted evidence.
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
