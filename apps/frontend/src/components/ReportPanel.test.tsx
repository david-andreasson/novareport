import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { DailyReport, LatestReportState } from '../App'
import { ReportPanel } from './ReportPanel'

describe('ReportPanel', () => {
  const formatTimestamp = vi.fn((report: DailyReport) => `formatted-${report.reportDate}`)
  const renderSummary = vi.fn((summary: string) => <div data-testid="summary">{summary}</div>)

  const baseState: LatestReportState = {
    phase: 'idle',
    report: null,
  }

  it('shows login hint when token is missing', () => {
    render(
      <ReportPanel
        token={null}
        reportState={baseState}
        formatTimestamp={formatTimestamp}
        renderSummary={renderSummary}
      />,
    )

    expect(screen.getByText('Logga in för att kunna läsa rapporten.')).toBeInTheDocument()
  })

  it('shows loading text when phase is loading', () => {
    const state: LatestReportState = { ...baseState, phase: 'loading' }

    render(
      <ReportPanel
        token="token"
        reportState={state}
        formatTimestamp={formatTimestamp}
        renderSummary={renderSummary}
      />,
    )

    expect(screen.getByText('Hämtar rapport…')).toBeInTheDocument()
  })

  it('shows error message when phase is error', () => {
    const state: LatestReportState = { phase: 'error', report: null, error: 'Något gick fel' }

    render(
      <ReportPanel
        token="token"
        reportState={state}
        formatTimestamp={formatTimestamp}
        renderSummary={renderSummary}
      />,
    )

    expect(screen.getByText('Något gick fel')).toBeInTheDocument()
  })

  it('renders report and calls formatTimestamp and renderSummary when phase is success', () => {
    const report: DailyReport = {
      reportDate: '2024-01-01T00:00:00Z',
      summary: 'Sammanfattning',
    }
    const state: LatestReportState = { phase: 'success', report, error: undefined }

    render(
      <ReportPanel
        token="token"
        reportState={state}
        formatTimestamp={formatTimestamp}
        renderSummary={renderSummary}
      />,
    )

    expect(formatTimestamp).toHaveBeenCalledWith(report)
    expect(renderSummary).toHaveBeenCalledWith('Sammanfattning')
  })
})
