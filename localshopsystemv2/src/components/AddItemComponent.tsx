import React, { useContext, useEffect, useState } from 'react'
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { MainContext } from './MainContextProvider';
import Category from '../models/Category';
import Item from '../models/Item';
import ItemRepository from '../repositories/ItemRepository';
import ImageRespository from '../repositories/ImageRepository';
import Cropper, { Area } from 'react-easy-crop';
import getCroppedImg from '../utils/CropImage';
import { PLACEHOLDER_IMG } from '../utils/placeholder';



type Props = {
    onComplete?: () => void;
}

const AddItemComponent = (props: Props) => {
    const { openAddItemModal, categories, setOpenAddItemModal, token } = useContext(MainContext);
    const [item, setItem] = useState<Item | null>(openAddItemModal);
    const [imageUrl, setImageUrl] = useState<string>();
    const [croppingImage, setCroppingImage] = useState<File | null>(null);
    const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area>();
    const [croppedImage, setCroppedImage] = useState<File | null>(null);
    const [crop, setCrop] = useState({ x: 0, y: 0 })
    const [zoom, setZoom] = useState(0.5)
    const itemRepository = new ItemRepository();
    const imageRepository = new ImageRespository();

    useEffect(() => {
        setItem(openAddItemModal);
        setCroppingImage(null);
        setCroppedAreaPixels(undefined);
        setCrop({ x: 0, y: 0 });
        setZoom(0.5);
        if (openAddItemModal?.image) {
            imageRepository.getImageById(openAddItemModal.image, token)
                .then(url => setImageUrl(url))
                .catch(() => setImageUrl(PLACEHOLDER_IMG));
        }
    }, [openAddItemModal]);

    const handleCropImage = async () => {
        if (!croppingImage || !croppedAreaPixels) return;
        const croppedImg = await getCroppedImg(
            URL.createObjectURL(croppingImage),
            croppedAreaPixels
        );
        setCroppingImage(null);
        setImageUrl(URL.createObjectURL(croppedImg));
        setCroppedImage(new File([croppedImg], `${item?.id + '_' + new Date().getTime()}.png`, { type: 'image/png' }));
    };


    const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        let code = formData.get('code') as string;
        if (item?.id !== undefined) {
            code = item?.id.toString() ?? code;
        }
        const category = formData.get('category') as string;
        const price = parseFloat(formData.get('price') as string);
        const name = formData.get('name') as string;
        let imageName = item?.image ?? '';
        if (croppedImage) {
            await imageRepository.uploadImage(croppedImage, token)
                .then((name) => {
                    imageName = name;
                    setCroppedImage(null);

                })
                .catch((error) => {
                    console.error('Error uploading image:', error);
                    alert('Error al subir la imagen. Por favor, inténtalo de nuevo.');
                });


        }

        const newItem: Item = {
            id: Number(code),
            name: name,
            price: price,
            categoryId: Number(category),
            image: imageName,
            stock: item?.stock ?? 0,
            bareMinimun: item?.bareMinimun ?? 0,
            net: item?.net ?? 0,
            ignoreStock: item?.ignoreStock ?? false,
        };
        if (newItem.id < 0 || newItem.name === '' || newItem.price <= 0 || newItem.categoryId < 0) {
            alert('Rellena los datos correctamente');
            return;

        }
        itemRepository.addItem(newItem, token)
            .then(() => {
                alert('Producto agregado correctamente');
                setOpenAddItemModal(null);
                props.onComplete && props.onComplete();
            })
            .catch((error) => {
                console.error('Error adding item:', error);
                alert('Error al agregar el producto. Por favor, inténtalo de nuevo.');
            });


    }
    if (item === null) {
        return null;
    }

    const isNew = !openAddItemModal?.name;
    const lockedCode = !!item.id || item.id === 0;

    return createPortal(
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label={isNew ? 'Nuevo producto' : 'Editar producto'}>
            <div className="modal-card" style={{ maxWidth: 580 }}>
                <button
                    className="btn-icon modal-close"
                    onClick={() => { setOpenAddItemModal(null) }}
                    aria-label="Cerrar"
                    type="button"
                >
                    <X size={18} strokeWidth={2} />
                </button>
                <h2>{isNew ? 'Nuevo producto' : 'Editar producto'}</h2>

                <form onSubmit={onSubmit}>
                    <div className="form-grid">
                        <div className="field">
                            <label htmlFor="code">Código</label>
                            <input
                                className="input"
                                type="text"
                                value={item.id.toString()}
                                readOnly
                                disabled={lockedCode}
                                placeholder="Código del producto"
                                name="code"
                                id="code"
                                required
                            />
                        </div>
                        <div className="field">
                            <label htmlFor="category">Categoría</label>
                            <select
                                className="select"
                                style={{ textTransform: 'capitalize' }}
                                name="category"
                                id="category"
                                value={item.categoryId}
                                onChange={(e) => setItem(prev => prev ? { ...prev, categoryId: Number(e.target.value) } : null)}
                            >
                                {Array.isArray(categories) &&
                                    categories.map((cat: Category) => (
                                        <option key={cat.id} value={cat.id}>
                                            {cat.name}
                                        </option>
                                    ))}
                            </select>
                        </div>

                        <div className="field span-2">
                            <label htmlFor="name">Nombre</label>
                            <input
                                className="input"
                                type="text"
                                value={item.name ?? ''}
                                placeholder="Nombre del producto"
                                onChange={(e) => setItem(prev => prev ? { ...prev, name: e.target.value } : null)}
                                name="name"
                                id="name"
                                required
                            />
                        </div>

                        <div className="field">
                            <label htmlFor="price">Precio de venta (€)</label>
                            <input
                                className="input"
                                type="number"
                                min="0"
                                step="0.01"
                                value={item.price ?? 0}
                                onChange={(e) => {
                                    const value = parseFloat(e.target.value);
                                    setItem(prev => prev ? { ...prev, price: isNaN(value) || value < 0 ? 0 : value } : null);
                                }}
                                name="price"
                                id="price"
                                required
                            />
                            <span className="field-hint">IGIC incluido</span>
                        </div>
                        <div className="field">
                            <label htmlFor="net">Precio neto / coste (€)</label>
                            <input
                                className="input"
                                type="number"
                                min="0"
                                step="0.01"
                                value={item.net ?? 0}
                                onChange={(e) => {
                                    const value = parseFloat(e.target.value);
                                    setItem(prev => prev ? { ...prev, net: isNaN(value) || value < 0 ? 0 : value } : null);
                                }}
                                name="net"
                                id="net"
                                required
                            />
                        </div>

                        <div className="field">
                            <label htmlFor="stock">Stock actual</label>
                            <input
                                className="input"
                                type="number"
                                min="0"
                                step="1"
                                value={item.stock ?? 0}
                                onChange={(e) => {
                                    const value = parseInt(e.target.value);
                                    if (!isNaN(value) && value >= 0) {
                                        setItem(prev => prev ? { ...prev, stock: value } : null);
                                    }
                                }}
                                name="stock"
                                id="stock"
                                required
                            />
                        </div>
                        <div className="field">
                            <label htmlFor="bareMinimun">Stock mínimo</label>
                            <input
                                className="input"
                                type="number"
                                step="1"
                                value={item.bareMinimun ?? 0}
                                onChange={(e) => {
                                    const value = parseInt(e.target.value);
                                    if (!isNaN(value) && value >= 0) {
                                        setItem(prev => prev ? { ...prev, bareMinimun: value } : null);
                                    }
                                }}
                                name="bareMinimun"
                                id="bareMinimun"
                                required
                            />
                        </div>

                        <label className="check span-2">
                            <input
                                type="checkbox"
                                checked={item.ignoreStock ?? false}
                                onChange={(e) => {
                                    const checked = e.target.checked;
                                    setItem(prev => prev ? { ...prev, ignoreStock: checked } : null);
                                }}
                                name="ignoreStock"
                                id="ignoreStock"
                            />
                            Ignorar el control de stock de este producto
                        </label>

                        <div className="field span-2">
                            <label htmlFor="image">Imagen</label>
                            <input
                                className="input"
                                type="file"
                                accept="image/*"
                                onChange={async (e) => {
                                    const file = e.target.files?.[0];
                                    if (file) {
                                        setCroppingImage(file);
                                        setCrop({ x: 0, y: 0 });
                                        setZoom(1);
                                    } else {
                                        setCroppingImage(null);
                                        setImageUrl(undefined);
                                    }
                                }}
                                name="image"
                                id="image"
                            />
                            {croppingImage ? (
                                <div>
                                    <div className="crop-box" style={{ marginTop: 12 }}>
                                        <Cropper
                                            image={URL.createObjectURL(croppingImage)}
                                            crop={crop}
                                            zoom={zoom}
                                            aspect={1}
                                            onCropChange={setCrop}
                                            onZoomChange={setZoom}
                                            onCropComplete={(croppedArea, croppedAreaPixels) => {
                                                setCroppedAreaPixels(croppedAreaPixels);
                                            }}
                                            style={{
                                                cropAreaStyle: {
                                                    border: '2px dashed var(--accent)',
                                                    borderRadius: 6,
                                                },
                                            }}
                                        />
                                    </div>
                                    <button type="button" className="btn btn-sm" style={{ marginTop: 12 }} onClick={handleCropImage}>
                                        Aplicar recorte
                                    </button>
                                </div>
                            ) : (
                                imageUrl && <img className="preview" src={imageUrl} alt="Vista previa" style={{ marginTop: 12 }} />
                            )}
                        </div>
                    </div>

                    <div className="modal-actions">
                        <button type="button" className="btn" onClick={() => setOpenAddItemModal(null)}>
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary">
                            Guardar producto
                        </button>
                    </div>
                </form>
            </div>
        </div>,
        document.body
    )
}

export default AddItemComponent