import React, { useCallback, useEffect, useState } from 'react'
import { RefreshCw, ShieldAlert, ShieldCheck } from 'lucide-react'
import VerifactuRepository, { VerifactuSummary } from '../repositories/VerifactuRepository'
import { formatDateTime } from '../utils/format'

type Props = { token: string | null }

const repository = new VerifactuRepository();

/** Resumen del envío a la AEAT. No se muestra si VERI*FACTU está desactivado. */
const VerifactuBanner = ({ token }: Props) => {
    const [summary, setSummary] = useState<VerifactuSummary | null>(null);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState<string | null>(null);

    const load = useCallback(async () => {
        try {
            const s = await repository.getStatus(token);
            setSummary(s ?? null);
        } catch {
            setSummary(null);
        }
    }, [token]);

    useEffect(() => {
        load();
        const timer = setInterval(load, 30000);
        return () => clearInterval(timer);
    }, [load]);

    const sendNow = async () => {
        setBusy(true);
        setMessage(null);
        try {
            const r = await repository.sendNow(token);
            setMessage(r && typeof r.sent === 'number' ? `Enviados: ${r.sent}` : null);
        } catch (e: any) {
            setMessage(e?.response?.data?.error ?? 'No se pudo enviar ahora');
        } finally {
            setBusy(false);
            load();
        }
    };

    if (!summary || !summary.enabled) {
        return null;
    }

    const c = summary.counts ?? {};
    const pending = c.PENDING ?? 0;
    const problems = (c.REJECTED ?? 0) + (c.ACCEPTED_WITH_ERRORS ?? 0);
    const attention = problems > 0 || !!summary.lastError;

    return (
        <div className={`verifactu-banner ${attention ? 'is-attention' : ''}`}>
            {attention ? <ShieldAlert size={20} /> : <ShieldCheck size={20} />}
            <div className="verifactu-text">
                <strong>Agencia Tributaria (VERI*FACTU){summary.environment !== 'production' ? ' · entorno de pruebas' : ''}</strong>
                <span>
                    {c.ACCEPTED ?? 0} aceptadas · {pending} pendientes
                    {problems > 0 ? ` · ${problems} con problemas` : ''}
                    {summary.lastSuccess ? ` · último envío ${formatDateTime(summary.lastSuccess)}` : ''}
                </span>
                {summary.lastError && <span className="verifactu-error">Último error: {summary.lastError}</span>}
                {message && <span>{message}</span>}
            </div>
            <button className="btn btn-sm" onClick={sendNow} disabled={busy || pending === 0}>
                <RefreshCw size={14} /> {busy ? 'Enviando…' : 'Enviar ahora'}
            </button>
        </div>
    )
}

export default VerifactuBanner
