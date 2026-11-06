import React from 'react';
import { render, screen } from '@testing-library/react';
import { Delta, fmtPct } from './ReportParts';

describe('Delta (variación vs. periodo anterior)', () => {
  test('subida: texto con signo e icono, no solo color', () => {
    const { container } = render(<Delta now={120} before={100} />);
    expect(screen.getByText(/\+20 % vs\. periodo anterior/)).toBeInTheDocument();
    expect(container.querySelector('.is-good')).not.toBeNull();
  });

  test('bajada', () => {
    const { container } = render(<Delta now={75} before={100} />);
    expect(screen.getByText(/−25 % vs\. periodo anterior/)).toBeInTheDocument();
    expect(container.querySelector('.is-bad')).not.toBeNull();
  });

  test('sin periodo anterior no inventa un porcentaje', () => {
    render(<Delta now={50} before={0} />);
    expect(screen.getByText('sin datos del periodo anterior')).toBeInTheDocument();
  });

  test('formato de porcentaje', () => {
    expect(fmtPct(55.4)).toBe('55,4 %');
    expect(fmtPct(null)).toBe('—');
  });
});
