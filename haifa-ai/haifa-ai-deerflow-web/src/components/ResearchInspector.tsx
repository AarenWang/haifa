import { useState, useMemo } from 'react';
import {
  Activity,
  Layers,
  Database,
  FileText,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  AlertCircle,
  CheckCircle2,
  Network
} from 'lucide-react';
import type {
  Source,
  EvidenceItem,
  WorkItem,
  Claim,
  Citation,
  BudgetLedger,
  QualityAssessment,
  SkillActivation
} from '../types';

interface ResearchInspectorProps {
  sources: Source[];
  evidenceItems: EvidenceItem[];
  workItems: WorkItem[];
  claims: Claim[];
  citations: Citation[];
  budgetLedger: BudgetLedger | null;
  qualityAssessment: QualityAssessment | null;
  skillActivations: SkillActivation[];
}

type TabType = 'overview' | 'workitems' | 'evidence' | 'sources';

export default function ResearchInspector({
  sources = [],
  evidenceItems = [],
  workItems = [],
  claims = [],
  citations = [],
  budgetLedger,
  qualityAssessment,
  skillActivations = []
}: ResearchInspectorProps) {
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [expandedClaimId, setExpandedClaimId] = useState<string | null>(null);
  const [isCollapsed, setIsCollapsed] = useState(false);

  // Map of source ID to Source
  const sourceMap = useMemo(() => {
    const map = new Map<string, Source>();
    sources.forEach((s) => map.set(s.sourceId, s));
    return map;
  }, [sources]);

  // Map of claim ID to Citations
  const citationMap = useMemo(() => {
    const map = new Map<string, Citation[]>();
    citations.forEach((c) => {
      const list = map.get(c.claimId) || [];
      list.push(c);
      map.set(c.claimId, list);
    });
    return map;
  }, [citations]);

  // Map of evidence ID to EvidenceItem
  const evidenceMap = useMemo(() => {
    const map = new Map<string, EvidenceItem>();
    evidenceItems.forEach((e) => map.set(e.evidenceId, e));
    return map;
  }, [evidenceItems]);

  const toggleClaim = (claimId: string) => {
    setExpandedClaimId(expandedClaimId === claimId ? null : claimId);
  };

  const qualityScore = qualityAssessment
    ? (qualityAssessment.score > 1 ? qualityAssessment.score : qualityAssessment.score * 100)
    : 0;

  // Helper to render budget progress bar
  const renderProgressBar = (label: string, used: number, max: number, colorClass = '') => {
    const percentage = max > 0 ? Math.min(100, (used / max) * 100) : 0;
    return (
      <div className="budget-bar-container" key={label}>
        <div className="budget-bar-labels">
          <span className="budget-bar-name">{label}</span>
          <span className="budget-bar-value">{used} / {max} ({percentage.toFixed(0)}%)</span>
        </div>
        <div className="budget-bar-track">
          <div
            className={`budget-bar-fill ${colorClass}`}
            style={{ width: `${percentage}%` }}
          />
        </div>
      </div>
    );
  };

  return (
    <section className={`research-panel-v2 ${isCollapsed ? 'collapsed' : ''}`}>
      <div className="research-panel-header">
        <div className="panel-brand">
          <Activity className="icon-pulse" size={18} />
          <h2>Deep Research Monitor</h2>
        </div>
        <div className="research-panel-actions">
          {!isCollapsed && (
            <div className="tab-navigation">
          <button
            type="button"
            className={activeTab === 'overview' ? 'active' : ''}
            onClick={() => setActiveTab('overview')}
          >
            <Layers size={14} />
            Overview & Budget
          </button>
          <button
            type="button"
            className={activeTab === 'workitems' ? 'active' : ''}
            onClick={() => setActiveTab('workitems')}
          >
            <Network size={14} />
            Research Path ({workItems.length})
          </button>
          <button
            type="button"
            className={activeTab === 'evidence' ? 'active' : ''}
            onClick={() => setActiveTab('evidence')}
          >
            <FileText size={14} />
            Claims & Evidence ({claims.length})
          </button>
          <button
            type="button"
            className={activeTab === 'sources' ? 'active' : ''}
            onClick={() => setActiveTab('sources')}
          >
            <Database size={14} />
            Sources ({sources.length})
          </button>
            </div>
          )}
          <button
            type="button"
            className="research-collapse-button"
            onClick={() => setIsCollapsed((collapsed) => !collapsed)}
            aria-expanded={!isCollapsed}
            aria-label={isCollapsed ? 'Expand Deep Research Monitor' : 'Collapse Deep Research Monitor'}
            title={isCollapsed ? 'Expand' : 'Collapse'}
          >
            {isCollapsed ? <ChevronDown size={16} /> : <ChevronUp size={16} />}
          </button>
        </div>
      </div>

      {!isCollapsed && (
      <div className="panel-content">
        {/* TAB 1: OVERVIEW & BUDGET */}
        {activeTab === 'overview' && (
          <div className="tab-pane overview-pane">
            <div className="metrics-grid">
              <div className="metric-card ledger-card">
                <h3>Budget & Resources</h3>
                {budgetLedger ? (
                  <div className="ledger-details">
                    {renderProgressBar('Model Calls', budgetLedger.usedModelCalls, budgetLedger.maxModelCalls)}
                    {renderProgressBar('Tool Calls', budgetLedger.usedToolCalls, budgetLedger.maxToolCalls)}
                    {renderProgressBar('Search Queries', budgetLedger.usedSearchQueries, budgetLedger.maxSearchQueries)}
                    {renderProgressBar('Fetched Sources', budgetLedger.usedFetchedSources, budgetLedger.maxFetchedSources)}
                    {budgetLedger.stopReason && (
                      <div className="alert-message warning">
                        <AlertCircle size={14} />
                        <span><strong>Halted:</strong> {budgetLedger.stopReason}</span>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="research-empty-mini">No budget ledger registered.</div>
                )}
              </div>

              <div className="metric-card quality-card">
                <h3>Quality Gate & Skills</h3>
                {qualityAssessment ? (
                  <div className="quality-details">
                    <div className="score-ring-container">
                      <div className={`score-badge ${qualityAssessment.passed ? 'passed' : 'failed'}`}>
                        {qualityAssessment.passed ? (
                          <CheckCircle2 size={24} className="icon-success" />
                        ) : (
                          <AlertCircle size={24} className="icon-warning" />
                        )}
                        <div className="score-text">
                          <span className="score-num">{qualityScore.toFixed(0)}</span>
                          <span className="score-label">Score</span>
                        </div>
                      </div>
                    </div>
                    <div className="assessment-meta">
                      <div className="meta-row">
                        <strong>Action:</strong> <span>{qualityAssessment.nextAction}</span>
                      </div>
                      {qualityAssessment.limitations && (
                        <div className="meta-row desc">
                          <strong>Limitations:</strong> <p>{qualityAssessment.limitations}</p>
                        </div>
                      )}
                    </div>
                    {qualityAssessment.gaps.length > 0 && (
                      <div className="gaps-list">
                        <h4>Identified Gaps:</h4>
                        <ul>
                          {qualityAssessment.gaps.map((gap, idx) => (
                            <li key={idx}><AlertCircle size={12} /> {gap}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="research-empty-mini">No quality assessment made yet.</div>
                )}
              </div>
            </div>

            <div className="skills-card">
              <h3>Activated Skills</h3>
              {skillActivations.length > 0 ? (
                <div className="skills-list">
                  {skillActivations.map((skill) => (
                    <div key={skill.activationId} className="skill-item">
                      <span className="skill-name">{skill.skillName}</span>
                      <span className={`skill-status ${skill.status.toLowerCase()}`}>{skill.status}</span>
                      <span className="skill-reason">{skill.activationReason}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="research-empty-mini">No activated skills found.</div>
              )}
            </div>
          </div>
        )}

        {/* TAB 2: RESEARCH PATH (WORKITEMS) */}
        {activeTab === 'workitems' && (
          <div className="tab-pane workitems-pane">
            {workItems.length > 0 ? (
              <div className="work-items-list">
                {workItems.map((item) => (
                  <div key={item.workItemId} className={`work-item-card status-${item.status}`}>
                    <div className="work-item-header">
                      <span className="work-item-type">{item.kind}</span>
                      <span className={`work-item-badge status-${item.status}`}>{item.status}</span>
                    </div>
                    <h4 className="work-item-title">{item.title}</h4>
                    <p className="work-item-goal">{item.goal}</p>
                    <div className="work-item-footer">
                      <span className="work-item-priority">Priority: {item.priority}</span>
                      <span className="work-item-by">Created by: {item.owner}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="research-empty">
                <Network size={24} />
                <p>No work path tasks discovered yet.</p>
              </div>
            )}
          </div>
        )}

        {/* TAB 3: EVIDENCE & CLAIMS */}
        {activeTab === 'evidence' && (
          <div className="tab-pane evidence-pane">
            {claims.length > 0 ? (
              <div className="claims-list">
                {claims.map((claim) => {
                  const isExpanded = expandedClaimId === claim.claimId;
                  const claimCitations = citationMap.get(claim.claimId) || [];
                  return (
                    <div key={claim.claimId} className="claim-accordion-item">
                      <button
                        type="button"
                        className="claim-trigger"
                        onClick={() => toggleClaim(claim.claimId)}
                      >
                        <div className="claim-headline">
                          <span className={`claim-status-badge ${claim.status}`}>{claim.status}</span>
                          <span className="claim-text">{claim.text}</span>
                        </div>
                        <div className="claim-actions">
                          <span className="claim-count">{claimCitations.length} evidence</span>
                          {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                        </div>
                      </button>
                      
                      {isExpanded && (
                        <div className="claim-body">
                          {claimCitations.length > 0 ? (
                            <div className="citations-list">
                              {claimCitations.map((cit) => {
                                const ev = evidenceMap.get(cit.evidenceId);
                                const src = sourceMap.get(cit.sourceId);
                                return (
                                  <div key={cit.citationId} className="citation-item">
                                    <div className="citation-meta">
                                      <span className="citation-confidence">Confidence: {(claim.confidence * 100).toFixed(0)}%</span>
                                      {src && (
                                        <a href={src.url} target="_blank" rel="noreferrer" className="source-link">
                                          {src.title || src.url}
                                          <ExternalLink size={10} />
                                        </a>
                                      )}
                                    </div>
                                    {ev && (
                                      <>
                                        <p className="citation-summary">{ev.summary}</p>
                                        <blockquote className="citation-quote">
                                          "{ev.claimSupportText}"
                                        </blockquote>
                                      </>
                                    )}
                                    {cit.locator && (
                                      <span className="citation-locator">Locator: {cit.locator}</span>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          ) : (
                            <div className="research-empty-mini">No direct citation links verified yet.</div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="research-empty">
                <FileText size={24} />
                <p>No verified claims or assertions submitted yet.</p>
              </div>
            )}
          </div>
        )}

        {/* TAB 4: DISCOVERED SOURCES */}
        {activeTab === 'sources' && (
          <div className="tab-pane sources-pane">
            {sources.length > 0 ? (
              <div className="sources-table-container">
                <table className="sources-table">
                  <thead>
                    <tr>
                      <th>Title / URL</th>
                      <th>Type</th>
                      <th>Status</th>
                      <th>Tier</th>
                      <th>Domain</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sources.map((src) => (
                      <tr key={src.sourceId}>
                        <td className="source-title-cell">
                          <a href={src.url} target="_blank" rel="noreferrer">
                            {src.title || src.url}
                            <ExternalLink size={12} />
                          </a>
                          {src.contentHash && (
                            <span className="hash-tag">Hash: {src.contentHash.substring(0, 8)}</span>
                          )}
                        </td>
                        <td><span className="source-type-tag">{src.sourceType}</span></td>
                        <td><span className={`status-pill ${src.lifecycleStatus}`}>{src.lifecycleStatus}</span></td>
                        <td><span className="tier-tag">{src.qualityTier}</span></td>
                        <td>{src.domain || 'N/A'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="research-empty">
                <Database size={24} />
                <p>No sources discovered in this thread yet.</p>
              </div>
            )}
          </div>
        )}
      </div>
      )}
    </section>
  );
}
