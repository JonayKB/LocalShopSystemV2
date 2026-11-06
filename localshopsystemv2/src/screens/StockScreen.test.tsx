import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import StockScreen from './StockScreen';
import { MainContext } from '../components/MainContextProvider';
import StockRepository from '../repositories/StockRepository';
import Item from '../models/Item';

const agua: Item = { id: 1001, name: 'Agua', price: 1.2, categoryId: 1, image: '', stock: 10, bareMinimun: 2, net: 0.5, ignoreStock: false };

// El buscador real llama a la API: aquí basta con un botón que "elige" el producto.
jest.mock('../components/ItemSearcher', () => (props: any) => (
  <button onClick={() => props.onItemClickProp(agua)}>elegir agua</button>
));

function renderScreen() {
  return render(
    <MemoryRouter>
      <MainContext.Provider value={{ token: 't' } as any}>
        <StockScreen />
      </MainContext.Provider>
    </MemoryRouter>
  );
}

describe('StockScreen', () => {
  let register: jest.SpyInstance;
  let movements: jest.SpyInstance;

  beforeEach(() => {
    register = jest.spyOn(StockRepository.prototype, 'registerMovements').mockResolvedValue([{ id: 1 } as any]);
    movements = jest.spyOn(StockRepository.prototype, 'getMovements').mockResolvedValue({
      content: [{ id: 9, itemId: 1001, itemName: 'Agua', type: 'SALE', typeLabel: 'Venta', quantity: -2, stockAfter: 8,
        unitCost: 0.5, tradeId: 3, note: 'Venta T-000003', createdBy: 'admin', createdAt: '2025-06-02T10:00:00Z' }],
      totalElements: 1, totalPages: 1, number: 0, size: 20,
    });
  });
  afterEach(() => jest.restoreAllMocks());

  test('entrada de mercancía con coste del albarán y nota', async () => {
    renderScreen();
    fireEvent.click(screen.getByText('elegir agua'));
    fireEvent.click(screen.getByText('elegir agua')); // dos unidades
    fireEvent.change(screen.getByLabelText('Coste unitario de Agua'), { target: { value: '0.45' } });
    fireEvent.change(screen.getByLabelText(/Nota/), { target: { value: 'Albarán 77' } });
    fireEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }));

    await waitFor(() => expect(register).toHaveBeenCalled());
    expect(register).toHaveBeenCalledWith('RESTOCK', [{ itemId: 1001, quantity: 2, unitCost: 0.45 }], 'Albarán 77', 't');
    expect(await screen.findByText(/Entrada registrada/)).toBeInTheDocument();
  });

  test('recuento: se envía lo que hay de verdad y se ve la diferencia', async () => {
    renderScreen();
    fireEvent.click(screen.getByRole('tab', { name: 'Recuento' }));
    fireEvent.click(screen.getByText('elegir agua'));
    fireEvent.change(screen.getByLabelText('Unidades reales de Agua'), { target: { value: '7' } });
    expect(screen.getByText('-3')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Guardar recuento' }));

    await waitFor(() => expect(register).toHaveBeenCalledWith('ADJUSTMENT', [{ itemId: 1001, countedStock: 7 }], '', 't'));
  });

  test('muestra el error del servidor', async () => {
    register.mockRejectedValue({ response: { data: 'La cantidad de Agua debe ser mayor que 0' } });
    renderScreen();
    fireEvent.click(screen.getByRole('tab', { name: 'Merma' }));
    fireEvent.click(screen.getByText('elegir agua'));
    fireEvent.click(screen.getByRole('button', { name: 'Registrar merma' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('debe ser mayor que 0');
  });

  test('libro de movimientos con filtro', async () => {
    renderScreen();
    expect(await screen.findByText('Venta T-000003')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Filtrar movimientos'), { target: { value: 'LOSS' } });
    await waitFor(() => expect(movements).toHaveBeenLastCalledWith({ type: 'LOSS', page: 0, size: 20 }, 't'));
  });
});
