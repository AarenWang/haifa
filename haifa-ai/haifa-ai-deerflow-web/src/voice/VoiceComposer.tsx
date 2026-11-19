import { useState, useEffect } from 'react';
import type { CSSProperties } from 'react';
import { Mic, Power, Square, Send, ArrowDown, RefreshCw, X, Radio, Volume2, Sparkles, Trash2 } from 'lucide-react';
import { useVoiceConversation, type VoiceState } from './useVoiceConversation';

interface VoiceComposerProps {
  threadId?: string;
  onThreadReady?: (threadId: string) => void;
  onTurnComplete?: (threadId?: string, runId?: string) => void;
  onSendTranscript?: (text: string) => void;
  onFillInput?: (text: string) => void;
  onClose?: () => void;
}

export function VoiceComposer({
  threadId,
  onThreadReady,
  onTurnComplete,
  onSendTranscript,
  onFillInput,
  onClose,
}: VoiceComposerProps) {
  const [autoContinuous, setAutoContinuous] = useState(false);

  const {
    state,
    transcript,
    agentText,
    turnsHistory,
    errorMsg,
    connect,
    startListening,
    commitTurn,
    cancelTurn,
    clearHistory,
  } = useVoiceConversation({
    threadId,
    autoContinuous,
    onThreadReady,
    onAgentComplete: (_text, completedThreadId, runId) => onTurnComplete?.(completedThreadId, runId),
  });

  // Sync live ASR text to parent input box as user speaks
  useEffect(() => {
    if (transcript && onFillInput) {
      onFillInput(transcript);
    }
  }, [transcript, onFillInput]);

  const handlePrimaryAction = () => {
    if (state === 'IDLE' || state === 'ERROR') {
      void connect();
    } else if (state === 'READY') {
      void startListening();
    } else if (state === 'LISTENING') {
      commitTurn();
    } else if (state === 'SPEAKING' || state === 'WAITING_AGENT' || state === 'TRANSCRIBING') {
      cancelTurn();
    }
  };

  const statusText: Record<VoiceState, string> = {
    IDLE: '点击启用语音对话',
    CONNECTING: '正在建立云端语音连接…',
    READY: '麦克风已就绪，点击开始说话',
    LISTENING: '🎙️ 正在录音中… (再次点击完成/提交)',
    TRANSCRIBING: '正在识别语音…',
    WAITING_AGENT: 'DeerFlow 正在思考生成回答…',
    SPEAKING: '🔊 DeerFlow 正在朗读回答… (点击可打断)',
    ERROR: errorMsg || '语音会话发生错误',
  };

  const primaryDisabled = state === 'CONNECTING';

  return (
    <section style={styles.container} aria-label="语音问答舱">
      {/* 头部：标题、连续对话开关、关闭按钮 */}
      <div style={styles.headerRow}>
        <div style={styles.titleGroup}>
          <Radio size={16} style={{ color: '#1677ff' }} />
          <span style={styles.titleText}>语音对话模式</span>
        </div>

        <div style={styles.headerActions}>
          <label style={styles.toggleLabel} title="回答结束后自动开启下一轮麦克风录音">
            <input
              type="checkbox"
              checked={autoContinuous}
              onChange={(e) => setAutoContinuous(e.target.checked)}
              style={styles.toggleCheckbox}
            />
            <Sparkles size={12} style={{ color: autoContinuous ? '#52c41a' : '#8c8c8c' }} />
            <span>连续对话</span>
          </label>

          {turnsHistory.length > 0 && (
            <button
              type="button"
              onClick={clearHistory}
              style={styles.iconBtn}
              title="清空本轮历史记录"
            >
              <Trash2 size={14} />
            </button>
          )}

          {onClose && (
            <button
              type="button"
              onClick={onClose}
              style={styles.iconBtn}
              title="关闭语音卡片"
            >
              <X size={16} />
            </button>
          )}
        </div>
      </div>

      {/* 状态通知条 */}
      <div style={styles.statusBadge} aria-live="polite">
        <span
          className={state === 'LISTENING' ? 'voice-recording-dot' : ''}
          style={statusIndicator(state)}
        />
        <span style={styles.statusLabel}>{statusText[state]}</span>
      </div>

      {/* 多轮问答历史记录与当前实时预览 */}
      <div style={styles.historyContainer}>
        {turnsHistory.map((item, idx) => (
          <div key={item.turnId || idx} style={styles.historyCard}>
            <div style={styles.userText}>你：{item.userText}</div>
            <div style={styles.agentText}>DeerFlow：{item.agentText}</div>
          </div>
        ))}

        {/* 当前正在进行的 Turn 实时展示 */}
        {(transcript || agentText) && (
          <div style={styles.activeCard}>
            {transcript && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <div style={styles.userText}>你：{transcript}</div>
                <div style={styles.actionRow}>
                  {onFillInput && (
                    <button
                      type="button"
                      onClick={() => onFillInput(transcript)}
                      style={styles.actionBtnSecondary}
                      title="填入下方输入框"
                    >
                      <ArrowDown size={12} />
                      填入输入框
                    </button>
                  )}
                  {onSendTranscript && (
                    <button
                      type="button"
                      onClick={() => onSendTranscript(transcript)}
                      style={styles.actionBtnPrimary}
                      title="发送给 AI"
                    >
                      <Send size={12} />
                      发送消息
                    </button>
                  )}
                </div>
              </div>
            )}
            {agentText && (
              <div style={styles.agentText}>
                <Volume2 size={13} style={{ display: 'inline', marginRight: 4, verticalAlign: 'middle' }} />
                DeerFlow：{agentText}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 核心多轮控制按钮区 */}
      <div style={styles.controls}>
        <button
          type="button"
          onClick={handlePrimaryAction}
          disabled={primaryDisabled}
          className={state === 'LISTENING' ? 'voice-recording-btn' : ''}
          style={{
            ...styles.primaryButton,
            ...(state === 'LISTENING' ? styles.recordingButton : {}),
            ...(state === 'SPEAKING' || state === 'WAITING_AGENT' ? styles.speakingButton : {}),
            ...(state === 'READY' ? styles.readyButton : {}),
            ...(state === 'ERROR' ? styles.errorButton : {}),
          }}
        >
          {state === 'IDLE' ? <Power size={16} />
            : state === 'ERROR' ? <RefreshCw size={16} />
            : state === 'LISTENING' ? <Square size={16} />
            : state === 'SPEAKING' || state === 'WAITING_AGENT' ? <Square size={16} />
            : <Mic size={16} />}

          {state === 'IDLE' ? '连接语音'
            : state === 'ERROR' ? '重试连接'
            : state === 'LISTENING' ? '完成说话'
            : state === 'SPEAKING' ? '打断朗读'
            : state === 'WAITING_AGENT' ? '停止思考'
            : '开始说话'}
        </button>

        {(state === 'LISTENING' || state === 'TRANSCRIBING' || state === 'SPEAKING' || state === 'WAITING_AGENT') && (
          <button type="button" onClick={cancelTurn} style={styles.stopButton}>
            <Square size={14} />
            取消
          </button>
        )}
      </div>
    </section>
  );
}

const styles: Record<string, CSSProperties> = {
  container: {
    display: 'flex', flexDirection: 'column', padding: '14px 18px',
    background: 'rgba(26, 27, 35, 0.95)', borderRadius: '16px',
    border: '1px solid rgba(255, 255, 255, 0.12)', color: '#fff',
    backdropFilter: 'blur(12px)', boxShadow: '0 8px 32px rgba(0,0,0,0.36)',
    gap: '12px', width: '100%', maxWidth: '640px', margin: '8px auto',
  },
  headerRow: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    borderBottom: '1px solid rgba(255, 255, 255, 0.08)', paddingBottom: '8px',
  },
  titleGroup: { display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600, fontSize: '14px' },
  titleText: { color: '#e6f4ff', letterSpacing: '0.3px' },
  headerActions: { display: 'flex', alignItems: 'center', gap: '12px' },
  toggleLabel: {
    display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#bfbfbf',
    cursor: 'pointer', userSelect: 'none', background: 'rgba(255,255,255,0.06)',
    padding: '3px 8px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)',
  },
  toggleCheckbox: { cursor: 'pointer', accentColor: '#52c41a' },
  iconBtn: {
    background: 'none', border: 'none', color: '#8c8c8c', cursor: 'pointer',
    display: 'flex', alignItems: 'center', padding: '4px', borderRadius: '4px',
    transition: 'all 0.2s',
  },
  statusBadge: { display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' },
  statusLabel: { color: '#d9d9d9', fontWeight: 500 },
  historyContainer: {
    display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '200px',
    overflowY: 'auto', paddingRight: '4px',
  },
  historyCard: {
    padding: '8px 12px', background: 'rgba(255, 255, 255, 0.04)', borderRadius: '8px',
    border: '1px solid rgba(255, 255, 255, 0.06)', fontSize: '13px', lineHeight: 1.5,
  },
  activeCard: {
    padding: '10px 14px', background: 'rgba(22, 119, 255, 0.1)', borderRadius: '10px',
    border: '1px solid rgba(22, 119, 255, 0.3)', fontSize: '13px', lineHeight: 1.5,
  },
  userText: { color: '#69b1ff', fontWeight: 500, marginBottom: '4px' },
  agentText: { color: '#95de64', fontWeight: 400 },
  actionRow: { display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '6px' },
  actionBtnPrimary: {
    display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '3px 10px',
    borderRadius: '12px', border: 'none', background: '#1677ff', color: '#fff',
    fontSize: '12px', fontWeight: 500, cursor: 'pointer',
  },
  actionBtnSecondary: {
    display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '3px 10px',
    borderRadius: '12px', border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(255,255,255,0.08)',
    color: '#d9d9d9', fontSize: '12px', fontWeight: 500, cursor: 'pointer',
  },
  controls: { display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '4px' },
  primaryButton: {
    display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 24px', borderRadius: '24px',
    border: 'none', background: '#1677ff', color: '#fff', fontWeight: 600, fontSize: '14px',
    cursor: 'pointer', transition: 'all 0.25s ease', boxShadow: '0 4px 12px rgba(22, 119, 255, 0.3)',
  },
  readyButton: { background: '#1677ff' },
  recordingButton: { background: '#ff4d4f', boxShadow: '0 4px 14px rgba(255, 77, 79, 0.4)' },
  speakingButton: { background: '#fa8c16', boxShadow: '0 4px 14px rgba(250, 140, 22, 0.4)' },
  errorButton: { background: '#d9363e' },
  stopButton: {
    display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '24px',
    border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(255, 255, 255, 0.08)', color: '#d9d9d9',
    cursor: 'pointer', fontSize: '13px',
  },
};

function statusIndicator(state: VoiceState): CSSProperties {
  const color = state === 'LISTENING' ? '#ff4d4f'
    : state === 'SPEAKING' ? '#52c41a'
      : state === 'TRANSCRIBING' || state === 'WAITING_AGENT' || state === 'CONNECTING' ? '#faad14'
        : state === 'READY' ? '#1677ff' : state === 'ERROR' ? '#ff7875' : '#8c8c8c';
  return { width: '10px', height: '10px', borderRadius: '50%', backgroundColor: color, transition: 'all 0.3s ease' };
}
