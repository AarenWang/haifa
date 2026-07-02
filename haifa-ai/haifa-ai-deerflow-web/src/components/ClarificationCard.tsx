import { CheckCircle2, HelpCircle } from 'lucide-react';
import { useMemo, useState } from 'react';
import { renderMarkdown } from '../utils/markdownRenderer';
import type { ClarificationAnswer, ClarificationQuestion, MessageRecord } from '../types';

interface ClarificationCardProps {
  message: MessageRecord;
  isPending: boolean;
  onAnswer?: (answer: string, answers?: ClarificationAnswer[]) => void;
}

type AnswerDraft = {
  selectedChoiceIds: string[];
  customAnswer: string;
};

export default function ClarificationCard({ message, isPending, onAnswer }: ClarificationCardProps) {
  const questions = useMemo(() => parseQuestions(message.metadata?.questions), [message.metadata]);
  const [drafts, setDrafts] = useState<Record<string, AnswerDraft>>({});
  const [submitted, setSubmitted] = useState(false);

  const hasStructuredQuestions = questions.length > 0;
  const isSubmittable = isPending && hasStructuredQuestions && !submitted;

  const updateDraft = (questionId: string, patch: Partial<AnswerDraft>) => {
    setDrafts((current) => {
      const previous = current[questionId] || { selectedChoiceIds: [], customAnswer: '' };
      return {
        ...current,
        [questionId]: {
          ...previous,
          ...patch,
        },
      };
    });
  };

  const toggleChoice = (question: ClarificationQuestion, choiceId: string) => {
    if (!isSubmittable) return;
    const draft = drafts[question.id] || { selectedChoiceIds: [], customAnswer: '' };
    const isMulti = question.answerType === 'MULTI_CHOICE_WITH_CUSTOM';
    const selectedChoiceIds = isMulti
      ? draft.selectedChoiceIds.includes(choiceId)
        ? draft.selectedChoiceIds.filter((id) => id !== choiceId)
        : [...draft.selectedChoiceIds, choiceId]
      : draft.selectedChoiceIds.includes(choiceId)
        ? []
        : [choiceId];
    updateDraft(question.id, { selectedChoiceIds });
  };

  const handleSubmit = () => {
    if (!isSubmittable || !onAnswer) return;
    const answers = questions.map((question) => buildAnswer(question, drafts[question.id]));
    const missingRequired = questions.some((question, index) => question.required && !answers[index].answer.trim());
    if (missingRequired) {
      return;
    }
    const summary = answers
      .map((answer, index) => `${index + 1}. ${questions[index].title}: ${answer.answer}`)
      .join('\n');
    setSubmitted(true);
    onAnswer(summary, answers);
  };

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

      {hasStructuredQuestions ? (
        <div className="clarification-form">
          {questions.map((question, index) => {
            const draft = drafts[question.id] || { selectedChoiceIds: [], customAnswer: '' };
            return (
              <section className="clarification-question" key={question.id}>
                <div className="clarification-question-heading">
                  <span className="clarification-question-index">{index + 1}</span>
                  <div>
                    <h4>{question.title}</h4>
                    {question.prompt && question.prompt !== question.title && (
                      <p>{question.prompt}</p>
                    )}
                  </div>
                </div>

                {question.choices.length > 0 && (
                  <div className="clarification-choice-grid">
                    {question.choices.map((choice) => {
                      const selected = draft.selectedChoiceIds.includes(choice.id);
                      return (
                        <button
                          key={choice.id}
                          type="button"
                          className={`clarification-choice ${selected ? 'selected' : ''}`}
                          disabled={!isSubmittable}
                          onClick={() => toggleChoice(question, choice.id)}
                        >
                          <span>{choice.label}</span>
                          <strong>{choice.text}</strong>
                        </button>
                      );
                    })}
                  </div>
                )}

                {question.allowCustom && (
                  <textarea
                    className="clarification-custom-input"
                    placeholder={question.choices.length > 0 ? '其他或补充说明...' : '请输入你的回答...'}
                    value={draft.customAnswer}
                    onChange={(event) => updateDraft(question.id, { customAnswer: event.target.value })}
                    disabled={!isSubmittable}
                    rows={2}
                  />
                )}
              </section>
            );
          })}

          {isPending && (
            <div className="clarification-card-footer">
              <span>一次性完成这些澄清项，DeerFlow 会继续执行原任务。</span>
              <button
                type="button"
                className="clarification-submit-btn"
                onClick={handleSubmit}
                disabled={!canSubmit(questions, drafts) || submitted}
              >
                提交澄清
              </button>
            </div>
          )}
        </div>
      ) : (
        <>
          <div className="clarification-card-content-wrapper">
            <div
              className="clarification-card-content"
              dangerouslySetInnerHTML={{ __html: renderMarkdown(message.content) }}
            />
          </div>

          {isPending && (
            <div className="clarification-card-footer">
              请在下方输入框回答这些问题。提交后 DeerFlow 会继续执行。
            </div>
          )}
        </>
      )}
    </div>
  );
}

function parseQuestions(value: unknown): ClarificationQuestion[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item, index) => {
      if (!item || typeof item !== 'object') {
        return undefined;
      }
      const record = item as Record<string, unknown>;
      const id = stringValue(record.id) || `q${index + 1}`;
      const title = stringValue(record.title) || stringValue(record.prompt) || `问题 ${index + 1}`;
      const prompt = stringValue(record.prompt) || title;
      const choices = Array.isArray(record.choices)
        ? record.choices
          .map((choice, choiceIndex) => {
            if (!choice || typeof choice !== 'object') return undefined;
            const choiceRecord = choice as Record<string, unknown>;
            return {
              id: stringValue(choiceRecord.id) || String.fromCharCode(97 + choiceIndex),
              label: stringValue(choiceRecord.label) || String.fromCharCode(65 + choiceIndex),
              text: stringValue(choiceRecord.text) || stringValue(choiceRecord.label) || '',
            };
          })
          .filter((choice): choice is { id: string; label: string; text: string } => !!choice && !!choice.text)
        : [];
      return {
        id,
        title,
        prompt,
        answerType: stringValue(record.answerType) || stringValue(record.answer_type) || (choices.length ? 'SINGLE_CHOICE_WITH_CUSTOM' : 'TEXT'),
        choices,
        allowCustom: booleanValue(record.allowCustom, booleanValue(record.allow_custom, true)),
        required: booleanValue(record.required, true),
      };
    })
    .filter((question): question is ClarificationQuestion => !!question);
}

function buildAnswer(question: ClarificationQuestion, draft?: AnswerDraft): ClarificationAnswer {
  const selectedChoiceIds = draft?.selectedChoiceIds || [];
  const selectedTexts = question.choices
    .filter((choice) => selectedChoiceIds.includes(choice.id))
    .map((choice) => choice.text);
  const customAnswer = draft?.customAnswer.trim() || '';
  const parts = [...selectedTexts, customAnswer].filter(Boolean);
  return {
    questionId: question.id,
    selectedChoiceIds,
    customAnswer,
    answer: parts.join('；'),
  };
}

function canSubmit(questions: ClarificationQuestion[], drafts: Record<string, AnswerDraft>) {
  return questions.every((question) => {
    if (!question.required) return true;
    return buildAnswer(question, drafts[question.id]).answer.trim().length > 0;
  });
}

function stringValue(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function booleanValue(value: unknown, fallback: boolean) {
  return typeof value === 'boolean' ? value : fallback;
}
