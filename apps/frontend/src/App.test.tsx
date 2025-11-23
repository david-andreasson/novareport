import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import { translateStatus, renderInlineMarkdown, renderReportSummary } from './App'

describe('translateStatus', () => {
  it('översätter kända statuser till svenska', () => {
    expect(translateStatus('ACTIVE')).toBe('Aktiv')
    expect(translateStatus('EXPIRED')).toBe('Utgången')
    expect(translateStatus('CANCELLED')).toBe('Avslutad')
  })

  it('lämnar okända statuser oförändrade', () => {
    expect(translateStatus('UNKNOWN')).toBe('UNKNOWN')
  })
})

describe('renderInlineMarkdown', () => {
  it('wrappar **text** i <strong>', () => {
    const { container } = render(<>{renderInlineMarkdown('Hello **World**')}</>)
    const strong = container.querySelector('strong')
    expect(strong).not.toBeNull()
    expect(strong?.textContent).toBe('World')
  })
})

describe('renderReportSummary', () => {
  it('renderar fallback-text när sammanfattningen är tom', () => {
    const { container } = render(<>{renderReportSummary('')}</>)
    const empty = container.querySelector('.report-summary__empty')
    expect(empty).not.toBeNull()
  })

  it('renderar rubriker för markdown-överskrifter', () => {
    const summary = '# Titel\n\n- Punkt 1\n- Punkt 2'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const heading = container.querySelector('.report-summary__heading')
    expect(heading).not.toBeNull()
    expect(heading?.textContent).toContain('Titel')
  })
})
