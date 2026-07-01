import React, { useState, useEffect } from 'react';
import {
  Brain,
  User,
  Settings,
  Archive,
  Check,
  X,
  Plus,
  Loader2,
} from 'lucide-react';
import {
  fetchPersonas,
  savePersona,
  fetchMemoryFacts,
  createMemoryFact,
  deleteMemoryFact,
  fetchMemoryCandidates,
  approveMemoryCandidate,
  rejectMemoryCandidate,
} from '../api/deerflowClient';
import type { AgentPersona, MemoryFact, MemoryCandidate } from '../types';

interface MemorySettingsModalProps {
  onClose: () => void;
}

type TabType = 'persona' | 'facts' | 'candidates';

export default function MemorySettingsModal({ onClose }: MemorySettingsModalProps) {
  const [activeTab, setActiveTab] = useState<TabType>('persona');
  const [loading, setLoading] = useState<boolean>(false);

  // Persona State
  const [persona, setPersona] = useState<AgentPersona>({
    name: '',
    description: '',
    soul: '',
    enabled: false,
  });

  // Facts State
  const [facts, setFacts] = useState<MemoryFact[]>([]);
  const [newFactContent, setNewFactContent] = useState<string>('');
  const [newFactCategory, setNewFactCategory] = useState<string>('preference');

  // Candidates State
  const [candidates, setCandidates] = useState<MemoryCandidate[]>([]);

  // Load Initial Data
  useEffect(() => {
    loadTabContent();
  }, [activeTab]);

  const loadTabContent = async () => {
    setLoading(true);
    try {
      if (activeTab === 'persona') {
        const personas = await fetchPersonas();
        if (personas && personas.length > 0) {
          setPersona(personas[0]);
        }
      } else if (activeTab === 'facts') {
        const data = await fetchMemoryFacts('active');
        setFacts(data);
      } else if (activeTab === 'candidates') {
        const data = await fetchMemoryCandidates('pending');
        setCandidates(data);
      }
    } catch (err) {
      console.error('Failed to fetch data for tab ' + activeTab, err);
    } finally {
      setLoading(false);
    }
  };

  // Persona Handlers
  const handleSavePersona = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const saved = await savePersona(persona);
      setPersona(saved);
      alert('Persona configuration saved successfully.');
    } catch (err) {
      console.error('Failed to save persona', err);
      alert('Failed to save persona.');
    } finally {
      setLoading(false);
    }
  };

  // Facts Handlers
  const handleAddFact = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFactContent.trim()) return;
    setLoading(true);
    try {
      const saved = await createMemoryFact({
        category: newFactCategory,
        content: newFactContent.trim(),
        status: 'active',
      });
      setFacts([saved, ...facts]);
      setNewFactContent('');
    } catch (err) {
      console.error('Failed to create fact', err);
    } finally {
      setLoading(false);
    }
  };

  const handleArchiveFact = async (id?: string) => {
    if (!id) return;
    if (!window.confirm('Are you sure you want to archive this memory fact?')) return;
    setLoading(true);
    try {
      await deleteMemoryFact(id);
      setFacts(facts.filter((f) => f.id !== id));
    } catch (err) {
      console.error('Failed to delete/archive fact', err);
    } finally {
      setLoading(false);
    }
  };

  // Candidates Handlers
  const handleApproveCandidate = async (id?: string) => {
    if (!id) return;
    setLoading(true);
    try {
      await approveMemoryCandidate(id);
      setCandidates(candidates.filter((c) => c.id !== id));
    } catch (err) {
      console.error('Failed to approve candidate', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRejectCandidate = async (id?: string) => {
    if (!id) return;
    setLoading(true);
    try {
      await rejectMemoryCandidate(id);
      setCandidates(candidates.filter((c) => c.id !== id));
    } catch (err) {
      console.error('Failed to reject candidate', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-container memory-modal">
        <div className="modal-header">
          <div className="modal-title-with-icon">
            <Brain className="modal-header-icon" />
            <h2>Agent Persona & Long-Term Memory</h2>
          </div>
          <button type="button" className="btn btn-icon close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="modal-tabs">
          <button
            type="button"
            className={`modal-tab ${activeTab === 'persona' ? 'active' : ''}`}
            onClick={() => setActiveTab('persona')}
          >
            <User size={14} />
            <span>Persona (SOUL)</span>
          </button>
          <button
            type="button"
            className={`modal-tab ${activeTab === 'facts' ? 'active' : ''}`}
            onClick={() => setActiveTab('facts')}
          >
            <Settings size={14} />
            <span>Active Memories</span>
            {facts.length > 0 && <span className="tab-badge">{facts.length}</span>}
          </button>
          <button
            type="button"
            className={`modal-tab ${activeTab === 'candidates' ? 'active' : ''}`}
            onClick={() => setActiveTab('candidates')}
          >
            <Brain size={14} />
            <span>Pending Candidates</span>
            {candidates.length > 0 && (
              <span className="tab-badge warning">{candidates.length}</span>
            )}
          </button>
        </div>

        <div className="modal-body scrollable">
          {loading && (
            <div className="loading-overlay">
              <Loader2 className="animate-spin" />
            </div>
          )}

          {activeTab === 'persona' && (
            <form onSubmit={handleSavePersona} className="persona-form">
              <div className="form-group flex-row align-center justify-between enabled-toggle-group">
                <label htmlFor="persona-enabled">
                  <strong>Enable Custom Persona (SOUL)</strong>
                  <p className="help-text">Inject customized identity and behavior into the Agent loop</p>
                </label>
                <input
                  type="checkbox"
                  id="persona-enabled"
                  className="switch-checkbox"
                  checked={persona.enabled}
                  onChange={(e) => setPersona({ ...persona, enabled: e.target.checked })}
                />
              </div>

              <div className="form-group">
                <label htmlFor="persona-name">Agent Name</label>
                <input
                  type="text"
                  id="persona-name"
                  value={persona.name}
                  onChange={(e) => setPersona({ ...persona, name: e.target.value })}
                  placeholder="e.g. CodeCopilot"
                />
              </div>

              <div className="form-group">
                <label htmlFor="persona-desc">Description</label>
                <input
                  type="text"
                  id="persona-desc"
                  value={persona.description}
                  onChange={(e) => setPersona({ ...persona, description: e.target.value })}
                  placeholder="Short role description"
                />
              </div>

              <div className="form-group">
                <label htmlFor="persona-soul">SOUL (Custom Persona Prompt Rules)</label>
                <textarea
                  id="persona-soul"
                  rows={10}
                  value={persona.soul}
                  onChange={(e) => setPersona({ ...persona, soul: e.target.value })}
                  placeholder="Write personality rules, behavior constraints, or templates here..."
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  Save Persona Configuration
                </button>
              </div>
            </form>
          )}

          {activeTab === 'facts' && (
            <div className="facts-container">
              <form onSubmit={handleAddFact} className="add-fact-form flex-row gap-2">
                <select
                  value={newFactCategory}
                  onChange={(e) => setNewFactCategory(e.target.value)}
                  className="fact-category-select"
                >
                  <option value="preference">Preference</option>
                  <option value="correction">Correction</option>
                  <option value="reinforcement">Reinforcement</option>
                  <option value="goal">Goal</option>
                  <option value="project_context">Project Context</option>
                  <option value="identity">Identity</option>
                  <option value="constraint">Constraint</option>
                </select>
                <input
                  type="text"
                  value={newFactContent}
                  onChange={(e) => setNewFactContent(e.target.value)}
                  placeholder="Type a new persistent memory fact..."
                  className="flex-grow"
                />
                <button type="submit" className="btn btn-primary flex-row gap-1" disabled={loading}>
                  <Plus size={14} />
                  Add
                </button>
              </form>

              <div className="facts-list">
                {facts.length === 0 ? (
                  <div className="empty-state">No long-term memories saved yet.</div>
                ) : (
                  facts.map((fact) => (
                    <div key={fact.id} className="fact-item flex-row justify-between align-center">
                      <div className="fact-content-box">
                        <span className={`fact-category-badge ${fact.category}`}>
                          {fact.category}
                        </span>
                        <p className="fact-text">{fact.content}</p>
                        {fact.lastUsedAt && (
                          <span className="fact-meta">
                            Last used: {new Date(fact.lastUsedAt).toLocaleString()}
                          </span>
                        )}
                      </div>
                      <button
                        type="button"
                        className="btn btn-icon btn-danger-hover"
                        onClick={() => handleArchiveFact(fact.id)}
                        title="Archive fact"
                      >
                        <Archive size={14} />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === 'candidates' && (
            <div className="candidates-container">
              <p className="help-text-banner">
                Memory candidate suggestions generated automatically from conversation history. Approve them to save permanently, or reject to discard.
              </p>

              <div className="candidates-list">
                {candidates.length === 0 ? (
                  <div className="empty-state">No pending memory candidates suggested yet.</div>
                ) : (
                  candidates.map((c) => (
                    <div key={c.id} className="candidate-item flex-row justify-between align-center">
                      <div className="candidate-content-box">
                        <div className="candidate-header flex-row gap-2 align-center">
                          <span className={`fact-category-badge ${c.category}`}>
                            {c.category}
                          </span>
                          <span className="candidate-confidence">
                            Confidence: {Math.round((c.confidence || 0.5) * 100)}%
                          </span>
                        </div>
                        <p className="candidate-text">{c.content}</p>
                      </div>
                      <div className="candidate-actions flex-row gap-2">
                        <button
                          type="button"
                          className="btn btn-icon btn-success"
                          onClick={() => handleApproveCandidate(c.id)}
                          title="Approve & save"
                        >
                          <Check size={14} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-icon btn-danger"
                          onClick={() => handleRejectCandidate(c.id)}
                          title="Reject & discard"
                        >
                          <X size={14} />
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
