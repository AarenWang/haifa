import { HelpCircle, CheckCircle2 } from 'lucide-react';
import { renderMarkdown } from '../utils/markdownRenderer';
import type { MessageRecord } from '../types';

interface ClarificationCardProps {
  message: MessageRecord;
  isPending: boolean;
  onAnswer?: (answer: string) => void;
}

export default function ClarificationCard({ message, isPending, onAnswer }: ClarificationCardProps) {
  const options = Array.isArray(message.metadata?.options)
    ? (message.metadata.options as string[])
    : undefined;

  return (
    <div className={`clarification-card ${isPending ? 'pending' : 'resolved'}`}>
      <div className="clarification-card-header">
        <div className="clarification-card-title-group">
          <div className="clarification-card-icon">
            {isPending ? (
              <HelpCircle size={18} className="icon-pending" />
            ) : (
              <CheckCircle2 size={18} className="icon-resolved" />
            )}
          </div>
          <div className="clarification-card-meta">
            <div className="clarification-card-eyebrow">
              {isPending ? 'Action Required' : 'Completed'}
            </div>
            <div className="clarification-card-title">
              {isPending ? '需要补充信息' : '请确认后继续'}
            </div>
          </div>
        </div>
      </div>

      <div className="clarification-card-content-wrapper">
        <div
          className="clarification-card-content"
          dangerouslySetInnerHTML={{ __html: renderMarkdown(message.content) }}
        />
      </div>

      {options && options.length > 0 && (
        <div className="clarification-card-options">
          {options.map((option, idx) => (
            <button
              key={idx}
              type="button"
              className="clarification-option-btn"
              disabled={!isPending}
              onClick={() => isPending && onAnswer && onAnswer(option)}
            >
              {option}
            </button>
          ))}
        </div>
      )}

      {isPending && (
        <div className="clarification-card-footer">
          {options && options.length > 0
            ? '请在下方输入框回答这些问题，或点击上方选项直接提交。提交后 DeerFlow 会继续执行。'
            : '请在下方输入框回答这些问题。提交后 DeerFlow 会继续执行。'}
        </div>
      )}
    </div>
  );
}
