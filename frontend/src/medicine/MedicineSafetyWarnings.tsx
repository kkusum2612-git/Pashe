
import type { MedicineSafetyWarning } from './types';

const warningLabels = {
  DUPLICATE_INGREDIENT: 'Duplicate ingredient',
  EXACT_CLASS: 'Allergy class match',
  RELATED_CLASS: 'Related allergy class',
  POSSIBLE_TEXT: 'Possible allergy match',
  UNCHECKABLE: 'Safety check unavailable',
};

export function SafetyWarningMessage({ warning }: { warning: MedicineSafetyWarning }) {
  return (
    <>
      <strong className="medicine-warning">{warningLabels[warning.kind]}</strong>
      <p>{warning.message}{warning.kind !== 'UNCHECKABLE' && ' Check with your doctor or pharmacist before taking this.'}</p>
    </>
  );
}

export default function MedicineSafetyWarnings({ warnings, disabled, onAcknowledge }: {
  warnings: MedicineSafetyWarning[];
  disabled: boolean;
  onAcknowledge: (id: string, intentional: boolean) => void;
}) {
  if (!warnings.length) return null;

  return (
    <ul className="medicine-warnings">
      {warnings.map(warning => (
        <li key={warning.id}>
          <SafetyWarningMessage warning={warning} />
          <div className="medicine-actions">
            {warning.acknowledgedAt
              ? <span>Acknowledged</span>
              : <button type="button" disabled={disabled}
                  onClick={() => onAcknowledge(warning.id, false)}>Got it</button>}
            {warning.kind === 'DUPLICATE_INGREDIENT' &&
              <button type="button" disabled={disabled} onClick={() => onAcknowledge(warning.id, true)}>
                My doctor prescribed both
              </button>}
          </div>
        </li>
      ))}
    </ul>
  );
}
