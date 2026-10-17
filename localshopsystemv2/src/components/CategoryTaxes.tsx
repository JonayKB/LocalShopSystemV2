import React, { useCallback, useContext, useEffect, useState } from 'react';
import { MainContext } from './MainContextProvider';
import CategoryRepository from '../repositories/CategoryRepository';
import Category from '../models/Category';

/**
 * Tipo de IGIC por categoría. Cada venta congela el tipo aplicado, así que
 * cambiarlo aquí solo afecta a las ventas nuevas.
 */
const CategoryTaxes = () => {
    const { token, categories, setCategories } = useContext(MainContext);
    const [drafts, setDrafts] = useState<Record<number, string>>({});
    const [savingId, setSavingId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const repo = new CategoryRepository();

    const load = useCallback(async () => {
        try {
            const list = await new CategoryRepository().getCategories(token);
            if (list) setCategories(list);
        } catch {
            setError('No se pudieron cargar las categorías.');
        }
    }, [token, setCategories]);

    useEffect(() => {
        if (token) load();
    }, [token, load]);

    const valueOf = (c: Category) => drafts[c.id] ?? (c.igicRate != null ? String(c.igicRate) : '');

    const isDirty = (c: Category) => {
        const current = c.igicRate != null ? String(c.igicRate) : '';
        return drafts[c.id] !== undefined && drafts[c.id] !== current;
    };

    const save = async (c: Category) => {
        const raw = valueOf(c).trim().replace(',', '.');
        const rate = raw === '' ? null : Number(raw);
        if (rate !== null && (isNaN(rate) || rate < 0 || rate > 100)) {
            setError('El tipo de IGIC debe estar entre 0 y 100.');
            return;
        }
        setError(null);
        setSavingId(c.id);
        try {
            await repo.saveCategory({ ...c, igicRate: rate }, token);
            setDrafts(d => {
                const next = { ...d };
                delete next[c.id];
                return next;
            });
            await load();
        } catch {
            setError('No se pudo guardar el tipo de IGIC.');
        } finally {
            setSavingId(null);
        }
    };

    return (
        <section className="card card-pad" style={{ marginTop: 40 }}>
            <h2 className="section-title">IGIC por categoría</h2>
            <p className="page-sub" style={{ marginTop: 0, marginBottom: 14 }}>
                Los precios de venta llevan el IGIC incluido. Si dejas una categoría vacía se usa el tipo por defecto del
                servidor (7 %). Confirma los tipos reales con tu gestoría; los cambios solo afectan a las ventas nuevas.
            </p>
            {error && <div className="cart-error" role="alert">{error}</div>}
            {Array.isArray(categories) && categories.map(c => (
                <div className="tax-row" key={c.id}>
                    <span className="name">{c.name}</span>
                    <div className="input-suffix">
                        <input
                            className="input"
                            inputMode="decimal"
                            placeholder="defecto"
                            value={valueOf(c)}
                            aria-label={`IGIC de ${c.name}`}
                            onChange={e => setDrafts(d => ({ ...d, [c.id]: e.target.value }))}
                        />
                        <span>%</span>
                    </div>
                    <button className="btn btn-sm" disabled={!isDirty(c) || savingId === c.id} onClick={() => save(c)}>
                        {savingId === c.id ? 'Guardando…' : 'Guardar'}
                    </button>
                </div>
            ))}
        </section>
    );
};

export default CategoryTaxes;
