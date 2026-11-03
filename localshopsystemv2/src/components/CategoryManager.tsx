import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Pencil, Plus, X } from 'lucide-react';
import Cropper, { Area } from 'react-easy-crop';
import { MainContext } from './MainContextProvider';
import CategoryRepository from '../repositories/CategoryRepository';
import ImageRespository from '../repositories/ImageRepository';
import Category from '../models/Category';
import getCroppedImg from '../utils/CropImage';
import { PLACEHOLDER_IMG } from '../utils/placeholder';

/** Mensaje de error del servidor (texto plano) o uno genérico. */
const serverMessage = (e: any, fallback: string): string =>
    typeof e?.response?.data === 'string' && e.response.data ? e.response.data : fallback;

type Draft = { id?: number; name: string; igicRate: string; image: string };

/**
 * Alta y edición de categorías: nombre, imagen e IGIC. Cada venta congela el
 * tipo de IGIC aplicado, así que cambiarlo solo afecta a las ventas nuevas.
 */
const CategoryManager = () => {
    const { token, categories, setCategories } = useContext(MainContext);
    const [editing, setEditing] = useState<Draft | null>(null);
    const [error, setError] = useState<string | null>(null);

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

    const openEdit = (c: Category) =>
        setEditing({ id: c.id, name: c.name ?? '', igicRate: c.igicRate != null ? String(c.igicRate) : '', image: c.image ?? '' });

    return (
        <section className="card card-pad" style={{ marginTop: 40 }}>
            <div className="category-head">
                <h2 className="section-title">Categorías</h2>
                <button className="btn btn-sm" onClick={() => setEditing({ name: '', igicRate: '', image: '' })}>
                    <Plus size={15} /> Nueva categoría
                </button>
            </div>
            <p className="page-sub" style={{ marginTop: 0, marginBottom: 14 }}>
                Cambia el nombre, la imagen o el IGIC de cada categoría. Los precios de venta llevan el IGIC incluido; si
                lo dejas vacío se usa el tipo por defecto del servidor (7 %). Confirma los tipos reales con tu gestoría;
                los cambios solo afectan a las ventas nuevas.
            </p>
            {error && <div className="cart-error" role="alert">{error}</div>}
            {Array.isArray(categories) && categories.map(c => (
                <div className="category-row" key={c.id}>
                    <img src={ImageRespository.url(c.image, 160)} alt="" loading="lazy"
                        onError={(e) => { if (e.currentTarget.src !== PLACEHOLDER_IMG) e.currentTarget.src = PLACEHOLDER_IMG; }} />
                    <span className="name">{c.name}</span>
                    <span className="rate">{c.igicRate != null ? `IGIC ${c.igicRate} %` : 'IGIC por defecto'}</span>
                    <button className="btn btn-sm" onClick={() => openEdit(c)} aria-label={`Editar categoría ${c.name}`}>
                        <Pencil size={14} /> Editar
                    </button>
                </div>
            ))}
            {editing && (
                <CategoryModal
                    draft={editing}
                    token={token}
                    onClose={() => setEditing(null)}
                    onSaved={async () => {
                        setEditing(null);
                        await load();
                    }}
                />
            )}
        </section>
    );
};

type ModalProps = {
    draft: Draft;
    token: string | null;
    onClose: () => void;
    onSaved: () => void;
};

const CategoryModal = ({ draft, token, onClose, onSaved }: ModalProps) => {
    const [name, setName] = useState(draft.name);
    const [igicRate, setIgicRate] = useState(draft.igicRate);
    const [previewUrl, setPreviewUrl] = useState<string | undefined>(
        draft.image ? ImageRespository.url(draft.image, 640) : undefined
    );
    const [croppingSrc, setCroppingSrc] = useState<string | null>(null);
    const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area>();
    const [croppedImage, setCroppedImage] = useState<File | null>(null);
    const [crop, setCrop] = useState({ x: 0, y: 0 });
    const [zoom, setZoom] = useState(1);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const isNew = draft.id === undefined;

    const applyCrop = async () => {
        if (!croppingSrc || !croppedAreaPixels) return;
        const blob = await getCroppedImg(croppingSrc, croppedAreaPixels);
        setCroppingSrc(null);
        setPreviewUrl(URL.createObjectURL(blob));
        setCroppedImage(new File([blob], `categoria_${draft.id ?? 'nueva'}_${Date.now()}.png`, { type: 'image/png' }));
    };

    const onSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
            setError('El nombre es obligatorio.');
            return;
        }
        const raw = igicRate.trim().replace(',', '.');
        const rate = raw === '' ? null : Number(raw);
        if (rate !== null && (isNaN(rate) || rate < 0 || rate > 100)) {
            setError('El tipo de IGIC debe estar entre 0 y 100.');
            return;
        }
        if (croppingSrc) {
            setError('Aplica el recorte de la imagen antes de guardar.');
            return;
        }
        setError(null);
        setSaving(true);
        try {
            let image = draft.image;
            if (croppedImage) {
                image = await new ImageRespository().uploadImage(croppedImage, token);
            }
            await new CategoryRepository().saveCategory(
                { id: draft.id, name: trimmed, image, igicRate: rate } as Category,
                token
            );
            onSaved();
        } catch (err: any) {
            setError(serverMessage(err, 'No se pudo guardar la categoría. Inténtalo de nuevo.'));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label={isNew ? 'Nueva categoría' : 'Editar categoría'}>
            <div className="modal-card" style={{ maxWidth: 520 }}>
                <button className="btn-icon modal-close" onClick={onClose} aria-label="Cerrar" type="button">
                    <X size={18} strokeWidth={2} />
                </button>
                <h2>{isNew ? 'Nueva categoría' : 'Editar categoría'}</h2>
                <form onSubmit={onSubmit}>
                    <div className="form-grid">
                        <div className="field">
                            <label htmlFor="cat-name">Nombre</label>
                            <input className="input" id="cat-name" value={name} required autoFocus
                                onChange={e => setName(e.target.value)} placeholder="Nombre de la categoría" />
                        </div>
                        <div className="field">
                            <label htmlFor="cat-igic">IGIC</label>
                            <div className="input-suffix">
                                <input className="input" id="cat-igic" inputMode="decimal" placeholder="defecto"
                                    value={igicRate} onChange={e => setIgicRate(e.target.value)} />
                                <span>%</span>
                            </div>
                        </div>
                        <div className="field span-2">
                            <label htmlFor="cat-image">Imagen</label>
                            <input
                                className="input"
                                type="file"
                                accept="image/*"
                                id="cat-image"
                                onChange={e => {
                                    const file = e.target.files?.[0];
                                    if (file) {
                                        setCroppingSrc(URL.createObjectURL(file));
                                        setCrop({ x: 0, y: 0 });
                                        setZoom(1);
                                    }
                                }}
                            />
                            <span className="field-hint">Debe ser una foto propia: no puede ser la de un producto ni la de otra categoría.</span>
                            {croppingSrc ? (
                                <div>
                                    <div className="crop-box" style={{ marginTop: 12 }}>
                                        <Cropper
                                            image={croppingSrc}
                                            crop={crop}
                                            zoom={zoom}
                                            aspect={1}
                                            onCropChange={setCrop}
                                            onZoomChange={setZoom}
                                            onCropComplete={(_, pixels) => setCroppedAreaPixels(pixels)}
                                        />
                                    </div>
                                    <button type="button" className="btn btn-sm" style={{ marginTop: 12 }} onClick={applyCrop}>
                                        Aplicar recorte
                                    </button>
                                </div>
                            ) : (
                                previewUrl && <img className="preview" src={previewUrl} alt="Vista previa" style={{ marginTop: 12 }} />
                            )}
                        </div>
                    </div>
                    {error && <div className="cart-error" role="alert" style={{ marginTop: 12 }}>{error}</div>}
                    <div className="modal-actions">
                        <button type="button" className="btn" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Guardando…' : 'Guardar categoría'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CategoryManager;
