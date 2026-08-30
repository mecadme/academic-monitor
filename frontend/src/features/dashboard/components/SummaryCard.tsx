type SummaryCardProps = {
  label: string;
  value: number;
  helper: string;
};

export function SummaryCard({
                              label,
                              value,
                              helper,
                            }: SummaryCardProps) {
  return (
    <article className="summary-card">
      <p className="summary-label">
        {label}
      </p>

      <p className="summary-value">
        {value}
      </p>

      <p className="summary-helper">
        {helper}
      </p>
    </article>
  );
}
