import { useState } from 'react';
import { FileSearch, CheckCircle2, Circle, AlertCircle, BarChart3, LayoutList } from 'lucide-react';
import type { ResearchPlan, ResearchProgress, QualityGateResult } from '../types';

interface ResearchPlanViewProps {
  plan?: ResearchPlan;
  progress?: ResearchProgress;
  qualityGate?: QualityGateResult;
}

export default function ResearchPlanView({ plan, progress, qualityGate }: ResearchPlanViewProps) {
  const [activeTab, setActiveTab] = useState<'plan' | 'progress' | 'quality'>('plan');

  if (!plan && !progress && !qualityGate) {
    return null;
  }

  const statusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle2 size={14} className="dim-status-icon completed" />;
      case 'IN_PROGRESS':
        return <Circle size={14} className="dim-status-icon in-progress" />;
      default:
        return <Circle size={14} className="dim-status-icon pending" />;
    }
  };

  return (
    <section className="research-plan-panel">
      <div className="research-plan-header">
        <div className="research-plan-tabs">
          <button
            type="button"
            className={`research-plan-tab ${activeTab === 'plan' ? 'active' : ''}`}
            onClick={() => setActiveTab('plan')}
          >
            <LayoutList size={14} />
            Plan
          </button>
          <button
            type="button"
            className={`research-plan-tab ${activeTab === 'progress' ? 'active' : ''}`}
            onClick={() => setActiveTab('progress')}
          >
            <BarChart3 size={14} />
            Progress
          </button>
          <button
            type="button"
            className={`research-plan-tab ${activeTab === 'quality' ? 'active' : ''}`}
            onClick={() => setActiveTab('quality')}
          >
            <FileSearch size={14} />
            Quality Gate
          </button>
        </div>
      </div>

      <div className="research-plan-body">
        {activeTab === 'plan' && plan && (
          <div className="plan-content">
            <div className="plan-topic">{plan.topic}</div>
            {plan.researchQuestions.length > 0 && (
              <div className="plan-questions">
                <div className="plan-section-title">Research Questions</div>
                <ul>
                  {plan.researchQuestions.map((q, i) => (
                    <li key={i}>{q}</li>
                  ))}
                </ul>
              </div>
            )}
            <div className="plan-dimensions">
              <div className="plan-section-title">Dimensions</div>
              {plan.dimensions.map((dim) => (
                <div key={dim.id} className={`plan-dimension-card ${dim.status.toLowerCase()}`}>
                  <div className="plan-dimension-top">
                    {statusIcon(dim.status)}
                    <span className="plan-dimension-title">{dim.title}</span>
                    <span className="plan-dimension-count">
                      {dim.actualSourceCount}/{dim.expectedSourceCount} sources
                    </span>
                  </div>
                  <div className="plan-dimension-desc">{dim.description}</div>
                  {dim.searchQueries.length > 0 && (
                    <div className="plan-dimension-queries">
                      {dim.searchQueries.slice(0, 3).map((q, i) => (
                        <span key={i} className="plan-query-tag">{q}</span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
            {plan.expectedDeliverable && (
              <div className="plan-deliverable">
                <div className="plan-section-title">Expected Deliverable</div>
                <div className="plan-deliverable-text">{plan.expectedDeliverable}</div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'progress' && progress && (
          <div className="progress-content">
            <div className="progress-summary">
              <div className="progress-stat">
                <div className="progress-stat-value">{progress.completedDimensions}</div>
                <div className="progress-stat-label">Completed</div>
              </div>
              <div className="progress-stat">
                <div className="progress-stat-value">{progress.inProgressDimensions}</div>
                <div className="progress-stat-label">In Progress</div>
              </div>
              <div className="progress-stat">
                <div className="progress-stat-value">{progress.totalSources}</div>
                <div className="progress-stat-label">Sources</div>
              </div>
              <div className="progress-stat">
                <div className="progress-stat-value">{progress.totalEvidence}</div>
                <div className="progress-stat-label">Evidence</div>
              </div>
            </div>
            <div className="progress-bar-container">
              <div className="progress-bar-label">
                Overall Progress: {progress.completionPercentage.toFixed(0)}%
              </div>
              <div className="progress-bar-track">
                <div
                  className="progress-bar-fill"
                  style={{ width: `${progress.completionPercentage}%` }}
                />
              </div>
            </div>
            {plan && (
              <div className="progress-dimensions">
                {plan.dimensions.map((dim) => (
                  <div key={dim.id} className="progress-dimension-row">
                    <span className="progress-dim-name">{dim.title}</span>
                    <span className={`progress-dim-status ${dim.status.toLowerCase()}`}>
                      {dim.status}
                    </span>
                    <span className="progress-dim-count">
                      {dim.actualSourceCount} sources, {dim.actualEvidenceCount} evidence
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'quality' && qualityGate && (
          <div className="quality-content">
            <div className={`quality-badge ${qualityGate.passed ? 'passed' : 'failed'}`}>
              {qualityGate.passed ? (
                <>
                  <CheckCircle2 size={18} />
                  Quality Gate Passed
                </>
              ) : (
                <>
                  <AlertCircle size={18} />
                  Quality Gate Failed
                </>
              )}
            </div>
            <div className="quality-score">Score: {qualityGate.score.toFixed(1)} / 100</div>
            <div className="quality-stats">
              <div className="quality-stat">
                <span className="quality-stat-label">Dimensions</span>
                <span className="quality-stat-value">{qualityGate.dimensionCount}</span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Sources</span>
                <span className="quality-stat-value">{qualityGate.fetchedSourceCount}</span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Facts</span>
                <span className={`quality-stat-value ${qualityGate.hasFacts ? 'ok' : 'missing'}`}>
                  {qualityGate.hasFacts ? 'Yes' : 'No'}
                </span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Data</span>
                <span className={`quality-stat-value ${qualityGate.hasData ? 'ok' : 'missing'}`}>
                  {qualityGate.hasData ? 'Yes' : 'No'}
                </span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Cases</span>
                <span className={`quality-stat-value ${qualityGate.hasCases ? 'ok' : 'missing'}`}>
                  {qualityGate.hasCases ? 'Yes' : 'No'}
                </span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Opinions</span>
                <span className={`quality-stat-value ${qualityGate.hasOpinions ? 'ok' : 'missing'}`}>
                  {qualityGate.hasOpinions ? 'Yes' : 'No'}
                </span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Limitations</span>
                <span className={`quality-stat-value ${qualityGate.hasLimitations ? 'ok' : 'missing'}`}>
                  {qualityGate.hasLimitations ? 'Yes' : 'No'}
                </span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Counter-view</span>
                <span className={`quality-stat-value ${qualityGate.hasCounterView ? 'ok' : 'missing'}`}>
                  {qualityGate.hasCounterView ? 'Yes' : 'No'}
                </span>
              </div>
              <div className="quality-stat">
                <span className="quality-stat-label">Citations</span>
                <span className={`quality-stat-value ${qualityGate.citationComplete ? 'ok' : 'missing'}`}>
                  {qualityGate.citationComplete ? 'Yes' : 'No'}
                </span>
              </div>
            </div>
            {qualityGate.gaps.length > 0 && (
              <div className="quality-gaps">
                <div className="quality-section-title">Gaps</div>
                <ul>
                  {qualityGate.gaps.map((gap, i) => (
                    <li key={i}>{gap}</li>
                  ))}
                </ul>
              </div>
            )}
            {qualityGate.recommendation && (
              <div className="quality-recommendation">
                <div className="quality-section-title">Recommendation</div>
                <div>{qualityGate.recommendation}</div>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
