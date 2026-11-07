import { vi, MockInstance } from 'vitest';
import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import GraphScreen from './GraphScreen';
import { MainContext } from '../components/MainContextProvider';
import GraphRepository from '../repositories/GraphRepository';
import { Totals } from '../models/Analytics';

// Los gráficos SVG de MUI necesitan medidas reales del navegador: en jsdom se sustituyen.
vi.mock('@mui/x-charts', () => ({
  LineChart: (p: any) => <div data-testid="line">{p.series.map((s: any) => s.label).join('|')}</div>,
  BarChart: (p: any) => <div data-testid="bar">{p.xAxis[0].data.join('|')}</div>,
}));

const totals = (over: Partial<Totals> = {}): Totals => ({
  revenue: 120, taxBase: 112.15, tax: 7.85, cost: 50, profit: 62.15, marginPct: 55.4, costCoverage: 80,
  trades: 10, units: 25, avgTicket: 12, avgUnitsPerTrade: 2.5, cashRevenue: 70, cardRevenue: 50,
  cancelledTrades: 1, cancelledAmount: 4, ...over,
});

describe('GraphScreen', () => {
  let summary: MockInstance;

  beforeEach(() => {
    const P = GraphRepository.prototype;
    summary = vi.spyOn(P, 'summary').mockResolvedValue({
      from: '2025-06-01', to: '2025-06-30', current: totals(), previousFrom: '2025-05-02', previousTo: '2025-05-31',
      previous: totals({ revenue: 100, profit: 0, trades: 10 }),
    });
    vi.spyOn(P, 'salesOverTime').mockResolvedValue([{ period: '2025-06-01', revenue: 120, profit: 62, trades: 10, units: 25 }]);
    vi.spyOn(P, 'byCategory').mockResolvedValue([{ category: 'Bebidas', revenue: 120, profit: 62.15, units: 25, sharePct: 100 }]);
    vi.spyOn(P, 'byPaymentMethod').mockResolvedValue([
      { method: 'CASH', label: 'Efectivo', revenue: 70, trades: 6, sharePct: 58.3 },
      { method: 'CARD', label: 'Tarjeta', revenue: 50, trades: 4, sharePct: 41.7 },
    ]);
    vi.spyOn(P, 'byHour').mockResolvedValue(
      Array.from({ length: 24 }, (_, h) => ({ bucket: h, label: `${String(h).padStart(2, '0')}:00`, revenue: h === 9 || h === 12 ? 60 : 0, trades: h === 9 || h === 12 ? 5 : 0, avgTicket: 12 }))
    );
    vi.spyOn(P, 'byWeekday').mockResolvedValue([{ bucket: 1, label: 'Lunes', revenue: 120, trades: 10, avgTicket: 12 }]);
    vi.spyOn(P, 'topItems').mockResolvedValue([
      { itemId: 1, name: 'Agua', category: 'Bebidas', units: 20, revenue: 24, profit: 12.4, marginPct: 55.4, trades: 8 },
      { itemId: 2, name: 'Pan', category: 'Panadería', units: 5, revenue: 4, profit: null, marginPct: null, trades: 3 },
    ]);
    vi.spyOn(P, 'inventory').mockResolvedValue({
      items: 3, units: 40, valueAtCost: 20, valueAtPrice: 48, potentialProfit: 22.8, itemsWithoutCost: 1,
      outOfStock: 1, belowMinimum: 0, ignoringStock: 0,
      byCategory: [{ category: 'Bebidas', items: 2, units: 40, valueAtCost: 20, valueAtPrice: 48 }],
      alerts: [{ itemId: 2, name: 'Pan', category: 'Panadería', stock: 0, bareMinimun: 2 }],
    });
  });
  afterEach(() => vi.restoreAllMocks());

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <MainContext.Provider value={{ token: 't' } as any}>
          <GraphScreen />
        </MainContext.Provider>
      </MemoryRouter>
    );

  test('KPIs con comparación, aviso de costes y desglose', async () => {
    renderScreen();

    expect(await screen.findByText('120,00 €', { selector: 'strong' })).toBeInTheDocument();
    expect(screen.getByText(/\+20 % vs\. periodo anterior/)).toBeInTheDocument();
    expect(screen.getByText(/Margen 55,4 %/)).toBeInTheDocument();
    expect(screen.getByRole('note')).toHaveTextContent('solo incluye el 80 %');
    expect(screen.getByTestId('line')).toHaveTextContent('Ingresos|Beneficio bruto');
    // Producto sin coste: beneficio "—", no 0 €.
    const pan = (await screen.findAllByText('Pan'))[0].closest('tr')!;
    expect(pan).toHaveTextContent('—');
    // Efectivo / tarjeta con etiquetas visibles
    expect(screen.getByText('Efectivo')).toBeInTheDocument();
    expect(screen.getByText(/58,3 %/)).toBeInTheDocument();
    // Horas recortadas a la franja con ventas (9h a 12h)
    expect(screen.getAllByTestId('bar')[0]).toHaveTextContent('09h|10h|11h|12h');
    // Inventario
    expect(await screen.findByText('Agotado')).toBeInTheDocument();
  });

  test('cambiar de periodo vuelve a pedir los datos con las fechas nuevas', async () => {
    renderScreen();
    await screen.findByText(/\+20 %/);
    fireEvent.click(screen.getByRole('tab', { name: 'Hoy' }));

    await waitFor(() => {
      const [range] = summary.mock.calls[summary.mock.calls.length - 1];
      expect(range.from).toBe(range.to);
    });
  });

  test('fechas personalizadas al revés: aviso y sin peticiones', async () => {
    renderScreen();
    await screen.findByText(/\+20 %/);
    fireEvent.click(screen.getByRole('tab', { name: 'Fechas…' }));
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2025-06-10' } });
    await waitFor(() => expect(summary.mock.calls[summary.mock.calls.length - 1][0].from).toBe('2025-06-10'));
    const calls = summary.mock.calls.length;

    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2025-06-01' } });

    expect(await screen.findByText(/fecha de inicio anterior/)).toBeInTheDocument();
    expect(summary.mock.calls.length).toBe(calls); // un rango imposible no se pide al servidor
  });
});
