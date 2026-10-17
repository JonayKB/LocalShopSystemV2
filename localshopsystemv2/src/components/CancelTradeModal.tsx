import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import Trade from '../models/Trade';
import { eur, formatDateTime, tradeLabel, tradeTotal } from '../utils/format';

type Props = {
    trade: Trade;
    onClose: () => void;
    onConfirm: (reason: string) => Promise<void>;
};

const CancelTradeModal = ({ trade, onClose, onConfirm }: Props) => {
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const submit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setBusy(true);
        setError(null);

        try {
            await onConfirm(reason);
        } catch {
            setError('No se pudo anular la venta. Inténtalo de nuevo.');
            setBusy(false);
        }
    };

    return createPortal(
        <div
            className="modal-backdrop"
            role="dialog"
            aria-modal="true"
            aria-label="Anular venta"
            onClick={busy ? undefined : onClose}
        >
            <form
                className="modal-card"
                onClick={e => e.stopPropagation()}
                onSubmit={submit}
            >
                <button
                    className="btn-icon modal-close"
                    type="button"
                    onClick={onClose}
                    disabled={busy}
                    aria-label="Cerrar"
                >
                    <X size={18} strokeWidth={2} />
                </button>

                <h2>Anular venta {tradeLabel(trade)}</h2>

                <p style={{ color: 'var(--muted)', marginBottom: 18 }}>
                    {formatDateTime(trade.date)} ·{' '}
                    <strong style={{ color: 'var(--text)' }}>
                        {eur(tradeTotal(trade))}
                    </strong>
                </p>

                <div className="trade-note" style={{ marginBottom: 18 }}>
                    <span>
                        La venta <strong>no se borra</strong>: queda registrada
                        como anulada y se devuelve el stock de sus productos.
                        Esta acción no se puede deshacer.
                    </span>
                </div>

                <div className="field">
                    <label htmlFor="cancel-reason">
                        Motivo (opcional)
                    </label>
                    <textarea
                        id="cancel-reason"
                        className="textarea"
                        value={reason}
                        maxLength={255}
                        placeholder="Ej.: error al cobrar, devolución del cliente…"
                        onChange={e => setReason(e.target.value)}
                        autoFocus
                    />
                </div>

                {error && (
                    <div
                        className="cart-error"
                        style={{ marginTop: 14 }}
                        role="alert"
                    >
                        {error}
                    </div>
                )}

                <div className="modal-actions">
                    <button
                        type="button"
                        className="btn"
                        onClick={onClose}
                        disabled={busy}
                    >
                        Conservar venta
                    </button>

                    <button
                        type="submit"
                        className="btn btn-danger-solid"
                        disabled={busy}
                    >
                        {busy ? 'Anulando…' : 'Anular venta'}
                    </button>
                </div>
            </form>
        </div>,
        document.body
    );
};

export default CancelTradeModal;