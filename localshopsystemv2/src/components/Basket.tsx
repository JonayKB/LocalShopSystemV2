import React, { useContext, useState } from 'react';
import { Minus, Plus, ShoppingBag, X } from 'lucide-react';
import { MainContext } from './MainContextProvider';
import TradeRepository from '../repositories/TradeRepository';
import { eur } from '../utils/format';

const Basket: React.FC = () => {
    const { basketItems, openBasket, setOpenBasket, updateBasket, token, setBasketItems } = useContext(MainContext);
    const tradeRepository = new TradeRepository();
    const [printTicket, setPrintTicket] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    const entries = Array.from(basketItems.entries());
    const count = entries.reduce((n, [, q]) => n + q, 0);
    const total = entries.reduce((sum, [item, q]) => sum + item.price * q, 0);

    const playSound = (src: string) => {
        try {
            new Audio(src).play().catch(() => undefined);
        } catch {
            /* sin audio */
        }
    };

    const handleCreateTrade = async () => {
        setError(null);
        setBusy(true);
        try {
            const result = await tradeRepository.createTrade(basketItems, token, printTicket);
            if (result) {
                playSound('/success_sound.mp3');
                setBasketItems(new Map());
                setPrintTicket(false);
                setOpenBasket(false);
                document.activeElement instanceof HTMLElement && document.activeElement.blur();
            }
        } catch (e: any) {
            const msg = typeof e?.response?.data === 'string' ? e.response.data : null;
            setError(msg ?? 'No se pudo registrar la venta. Inténtalo de nuevo.');
        } finally {
            setBusy(false);
        }
    };

    return (
        <>
            <button
                className={`cart-fab ${openBasket ? 'is-hidden' : ''}`}
                onClick={() => setOpenBasket(true)}
                aria-label="Abrir carrito"
            >
                <ShoppingBag size={20} strokeWidth={1.9} />
                <span>{count > 0 ? eur(total) : 'Carrito'}</span>
                {count > 0 && <span className="cart-count">{count}</span>}
            </button>

            <div className={`admin-scrim ${openBasket ? 'is-open' : ''}`} onClick={() => setOpenBasket(false)} />

            <aside className={`cart ${openBasket ? 'is-open' : ''}`} aria-label="Venta actual">
                <div className="cart-head">
                    <h2>Venta actual</h2>
                    <button className="btn-icon" onClick={() => setOpenBasket(false)} aria-label="Cerrar carrito">
                        <X size={20} strokeWidth={1.9} />
                    </button>
                </div>

                {entries.length === 0 ? (
                    <div className="cart-list" style={{ display: 'grid', placeItems: 'center' }}>
                        <div className="empty" style={{ border: 'none' }}>
                            <ShoppingBag size={36} strokeWidth={1.4} />
                            <strong>El carrito está vacío</strong>
                            <span>Escanea un código o elige un producto.</span>
                        </div>
                    </div>
                ) : (
                    <ul className="cart-list">
                        {entries.map(([item, quantity]) => (
                            <li key={item.id} className="cart-line">
                                <span className="cart-name">{item.name}</span>
                                <span className="cart-sum">{eur(item.price * quantity)}</span>
                                <span className="cart-unit">{eur(item.price)} / ud.</span>
                                <div className="qty">
                                    <button onClick={() => updateBasket(item, quantity - 1)} aria-label="Quitar uno">
                                        <Minus size={14} />
                                    </button>
                                    <input
                                        type="number"
                                        min={1}
                                        value={quantity}
                                        aria-label={`Cantidad de ${item.name}`}
                                        onChange={e => {
                                            const n = parseInt(e.target.value);
                                            updateBasket(item, isNaN(n) ? 0 : n);
                                        }}
                                    />
                                    <button onClick={() => updateBasket(item, quantity + 1)} aria-label="Añadir uno">
                                        <Plus size={14} />
                                    </button>
                                </div>
                            </li>
                        ))}
                    </ul>
                )}

                <div className="cart-foot">
                    <div className="cart-total">
                        <span>Total</span>
                        <strong>{eur(total)}</strong>
                    </div>
                    {error && <div className="cart-error" role="alert">{error}</div>}
                    <label className="check">
                        <input type="checkbox" checked={printTicket} onChange={e => setPrintTicket(e.target.checked)} />
                        Imprimir ticket
                    </label>
                    <button
                        className="btn btn-primary btn-lg btn-block"
                        onClick={handleCreateTrade}
                        disabled={entries.length === 0 || busy}
                        aria-label="Cobrar"
                    >
                        {busy ? 'Registrando…' : `Cobrar ${eur(total)}`}
                    </button>
                </div>
            </aside>
        </>
    );
};

export default Basket;
