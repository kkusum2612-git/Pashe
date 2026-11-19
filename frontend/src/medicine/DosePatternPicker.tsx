
const slots = ['Morning', 'Noon', 'Night', 'Bedtime'];
const amounts = ['0', '0.25', '0.5', '1', '1.5', '2', '3'];

export default function DosePatternPicker({ value, onChange }: {
  value: string;
  onChange: (value: string) => void;
}) {
  const parts = value.split('+').map(part =>
    part.trim().replace(/[০-৯]/g, digit => String(digit.charCodeAt(0) - 0x09e6)).replace('½', '0.5')
  );

  function choose(index: number, amount: string) {
    const next = Array.from({ length: index === 3 || parts.length === 4 ? 4 : 3 },
      (_, position) => position === index ? amount : parts[position] || '0');
    onChange(next.join('+'));
  }

  return (
    <details>
      <summary>Pick doses by time of day</summary>
      {slots.map((slot, index) => (
        <div className="medicine-picker" role="group" aria-label={slot} key={slot}>
          <span>{slot}</span>
          {amounts.map(amount => (
            <button key={amount} type="button" aria-pressed={(parts[index] || '0') === amount}
              aria-label={slot + ' ' + (amount === '0' ? 'skip' : amount)}
              onClick={() => choose(index, amount)}>
              {amount === '0' ? 'Skip' : amount}
            </button>
          ))}
        </div>
      ))}
    </details>
  );
}
