import type { JSX } from 'react'

import type { DailyReport, LatestReportState } from '../App'

type ReportPanelProps = {
  token: string | null
  reportState: LatestReportState
  formatTimestamp: (report: DailyReport) => string
  renderSummary: (summary: string) => JSX.Element
}

export function ReportPanel({ token, reportState, formatTimestamp, renderSummary }: ReportPanelProps) {
  if (reportState.phase === 'success' && reportState.report) {
    formatTimestamp(reportState.report)
  }

  return (
    <>
      {reportState.phase === 'loading' && <p className="auth-note">Hämtar rapport…</p>}
      {reportState.phase === 'error' && (
        <p className="subscription-error">{reportState.error ?? 'Ett fel inträffade.'}</p>
      )}
      {reportState.phase === 'success' && reportState.report && (
        <article className="report-preview">
          <section className="report-summary">{renderSummary(reportState.report.summary)}</section>
        </article>
      )}
      {!token && <p className="auth-note">Logga in för att kunna läsa rapporten.</p>}
    </>
  )
}
