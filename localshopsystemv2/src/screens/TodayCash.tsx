import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ExportRepository from '../repositories/ExportRepository';
import PageHeader from '../components/PageHeader';
import { eur } from '../utils/format';

type Props = {}

const TodayCash = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const [incomeByCategory, setIncomeByCategory] = useState<Record<string, number>>();
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
            return;
        }
        new ExportRepository().getIncomeByCategory(token)
            .then(setIncomeByCategory)
            .catch(() => setError('No se pudo cargar la caja del día.'));
    }, [token, navitigation]);

    const entries = incomeByCategory ? Object.entries(incomeByCategory) : [];
    const total = entries.reduce((sum, [, v]) => sum + v, 0);

    return (
        <>
            <PageHeader
                eyebrow="Día a día"
                title="Caja del día"
                sub="Ingresos de hoy por categoría. Las ventas anuladas no se incluyen."
            />
            {error ? (
                <div className="cart-error" role="alert">{error}</div>
            ) : !incomeByCategory ? (
                <div className="skeleton" style={{ height: 200 }} aria-busy="true" />
            ) : (
                <div className="stat-grid">
                    <div className="stat is-total">
                        <small>Total de hoy</small>
                        <strong>{eur(total)}</strong>
                    </div>
                    {entries.length === 0 && (
                        <div className="empty" style={{ gridColumn: '1 / -1' }}>
                            <strong>Todavía no hay ventas hoy</strong>
                        </div>
                    )}
                    {entries.map(([name, value]) => (
                        <div className="stat" key={name}>
                            <small>{name}</small>
                            <strong>{eur(value)}</strong>
                        </div>
                    ))}
                </div>
            )}
        </>
    );
}

export default TodayCash
