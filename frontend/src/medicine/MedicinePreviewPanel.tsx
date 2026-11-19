
import type { MedicinePreview } from './types';
import { SafetyWarningMessage } from './MedicineSafetyWarnings';

export const fastingConflictText = 'This noon dose does not fit fasting hours. Ask your doctor how to adjust it.';

export default function MedicinePreviewPanel({ preview, timezone, disabled, onSave, onBack }: {
  preview: MedicinePreview;
  timezone: string;
  disabled: boolean;
  onSave: () => void;
  onBack: () => void;
}) {
  const formatTime = new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone, month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',
  });

  return (
    <section className="medicine-note" aria-labelledby="medicine-preview-heading">
      <h3 id="medicine-preview-heading">Confirm {preview.medicineName}</h3>
      <p>Starting {preview.startDate} · {preview.durationDays === null
        ? 'Ongoing' : preview.durationDays + (preview.durationDays === 1 ? ' day' : ' days')}</p>
      {!preview.doses.length ? (
        <p>Take {preview.doseQuantity} {preview.doseUnit} as needed. No doses are scheduled.</p>
      ) : (
        <>
          <p>Schedule for the first day using your current routine.</p>
          <ul>
            {preview.doses.map(dose => {
              const relation = dose.slot === 'BEDTIME' ? 'at' :
                preview.mealTiming === 'BEFORE_MEAL' ? 'before' :
                  preview.mealTiming === 'AFTER_MEAL' ? 'after' : 'at';
              return (
                <li key={dose.slot}>
                  {dose.slot[0] + dose.slot.slice(1).toLowerCase()} · {dose.quantity} {preview.doseUnit}
                  {dose.scheduledAt
                    ? ' ' + relation + ' ' + dose.meal + ' (' + formatTime.format(new Date(dose.scheduledAt)) + ')'
                    : ' · ' + fastingConflictText}
                </li>
              );
            })}
          </ul>
        </>
      )}
      {!preview.safety.allergyCheckAvailable && <p>Health Passport allergy checking is unavailable.</p>}
      {preview.safety.warnings.length > 0 && (
        <ul className="medicine-warnings">
          {preview.safety.warnings.map(warning => (
            <li key={warning.id}><SafetyWarningMessage warning={warning} /></li>
          ))}
        </ul>
      )}
      <div className="medicine-actions">
        <button type="button" onClick={onBack} disabled={disabled}>Back to details</button>
        <button className="auth-primary" type="button" disabled={disabled} onClick={onSave}>
          Confirm and save
        </button>
      </div>
    </section>
  );
}
