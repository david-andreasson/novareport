import type { JSX } from 'react'

import type { DailyReport, LatestReportState } from '../App'

type ReportPanelProps = {
  token: string | null
  reportState: LatestReportState
  formatTimestamp: (report: DailyReport) => string
  renderSummary: (summary: string) => JSX.Element
}

export function ReportPanel({ token, reportState, formatTimestamp, renderSummary }: ReportPanelProps) {
  return (
    <>
      <h2>Senaste rapport</h2>
      <p className="auth-note">
        Visar resultatet från reporter-service. Kräver aktiv prenumeration.
      </p>
      {reportState.phase === 'loading' && <p className="auth-note">Hämtar rapport…</p>}
      {reportState.phase === 'error' && (
        <p className="subscription-error">{reportState.error ?? 'Ett fel inträffade.'}</p>
      )}
      {reportState.phase === 'success' && reportState.report && (
        <article className="report-preview">
          <header className="report-preview__header">
            <div>
              <span className="chip">Senaste rapport</span>
              <h3>{new Date(reportState.report.reportDate).toLocaleDateString('sv-SE')}</h3>
            </div>
            <div className="report-meta">
              <span>Skapad</span>
              <strong>{formatTimestamp(reportState.report)}</strong>
            </div>
          </header>
          <section className="report-summary">{renderSummary(reportState.report.summary)}</section>
        </article>
      )}
      {!token && <p className="auth-note">Logga in för att kunna läsa rapporten.</p>}
    </>
  )
}
