import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from './MainContextProvider';
import Category from '../models/Category';
import Item from '../models/Item';
import ItemRepository from '../repositories/ItemRepository';
import ImageRespository from '../repositories/ImageRepository';
import Cropper, { Area } from 'react-easy-crop';
import getCroppedImg from '../utils/CropImage';



type Props = {
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
                .catch(() => setImageUrl('https://static.vecteezy.com/system/resources/previews/008/695/917/non_2x/no-image-available-icon-simple-two-colors-template-for-no-image-or-picture-coming-soon-and-placeholder-illustration-isolated-on-white-background-vector.jpg'));
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
        setCroppedImage(new File([croppedImg], `${item?.id + '_' + new Date()}.png`, { type: 'image/png' }));
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
        let imageName = code + '.png';
        if (croppedImage) {
            await imageRepository.uploadImage(croppedImage, token)
                .then((name) => {
                    imageName = name;
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
        };
        if (newItem.id < 0 || newItem.name === '' || newItem.price <= 0 || newItem.categoryId < 0) {
            alert('Rellena los datos correctamente');
            return;

        }
        itemRepository.addItem(newItem, token)
            .then(() => {
                alert('Producto agregado correctamente');
                setOpenAddItemModal(null);
            })
            .catch((error) => {
                console.error('Error adding item:', error);
                alert('Error al agregar el producto. Por favor, inténtalo de nuevo.');
            });


    }
    return (
        <div
            style={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100vw',
                height: '100vh',
                background: 'rgba(42,45,58,0.85)',
                zIndex: 1000,
                display: item !== null ? 'flex' : 'none',
                alignItems: 'center',
                justifyContent: 'center',
            }}
            className={`modal ${item !== null ? 'open' : ''}`}
        >

            <div
                style={{
                    background: '#23263a',
                    color: 'white',
                    padding: 32,
                    borderRadius: 12,
                    width: 420,
                    boxShadow: '0 8px 32px rgba(0,0,0,0.25)',
                    position: 'relative',
                    fontSize: 18,
                }}
            >
                <button
                    onClick={() => { setOpenAddItemModal(null) }}
                    style={{
                        position: 'absolute',
                        top: 16,
                        right: 16,
                        width: 32,
                        height: 32,
                        border: 'none',
                        borderRadius: '50%',
                        background: '#444',
                        color: '#fff',
                        fontSize: 20,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'background 0.2s',
                    }}
                    aria-label="Cerrar"
                    onMouseOver={e => (e.currentTarget.style.background = '#666')}
                    onMouseOut={e => (e.currentTarget.style.background = '#444')}
                >
                    ✖
                </button>
                <form onSubmit={onSubmit}>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Código:
                        </label>
                        <input
                            type="text"
                            value={
                                item?.id === -1
                                    ? ''
                                    : item?.id.toString()
                            }
                            disabled={!!item?.id && item.id !== -1}
                            placeholder="Introduce el código del producto"
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: !!item?.id && item.id !== -1 ? '#444' : '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            name="code"
                            id="code"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Nombre:
                        </label>
                        <input
                            type="text"
                            value={item?.name ?? ''}
                            placeholder="Introduce el nombre del producto"
                            onChange={(e) => setItem(prev => prev ? { ...prev, name: e.target.value } : null)}
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            name="name"
                            id="name"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Categoría:
                        </label>
                        <select
                            style={{
                                width: '100%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                                textTransform: 'capitalize',
                            }}
                            name="category"
                            id="category"
                        >
                            {Array.isArray(categories) &&
                                categories.map((cat: Category) => (
                                    <option
                                        key={cat.id}
                                        value={cat.id}
                                        selected={item?.categoryId === cat.id}
                                    >
                                        {cat.name.toUpperCase()}
                                    </option>
                                ))}
                        </select>
                    </div>
                    <div style={{ marginBottom: 24 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Precio:
                        </label>
                        <input
                            type="number"
                            min="0"
                            step="0.01"
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            value={item?.price ?? 0}
                            onChange={(e) => {
                                const value = parseFloat(e.target.value);
                                if (!isNaN(value) && value >= 0) {
                                    setItem(prev => prev ? { ...prev, price: value } : null);
                                } else {
                                    console.error('Invalid price input:', e.target.value);
                                    alert('Please enter a valid price.');
                                    e.target.value = '0';
                                    setItem(prev => prev ? { ...prev, price: 0 } : null);
                                }
                            }
                            }
                            name="price"
                            id="price"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Imagen:
                        </label>
                        <input
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
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            name="image"
                            id="image"
                        />
                        {croppingImage ? (
                            <div style={{ marginTop: 12, textAlign: 'center' }}>
                                <div style={{ marginTop: 12, color: '#fff', position: 'relative', zIndex: 1001, height: 300, width: '100%' }}>
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
                                            containerStyle: {
                                                width: '100%',
                                                height: 'auto',
                                                borderRadius: 6,
                                                overflow: 'hidden',
                                            },
                                            cropAreaStyle: {
                                                border: '2px dashed #4f8cff',
                                                borderRadius: 6,
                                            },
                                        }}
                                    />

                                </div>

                                <button
                                    type="button"
                                    onClick={handleCropImage}
                                    style={{
                                        marginTop: 12,
                                        padding: '8px 16px',
                                        borderRadius: 6,
                                        border: 'none',
                                        background: '#4f8cff',
                                        color: '#fff',
                                        fontWeight: 600,
                                        fontSize: 16,
                                        cursor: 'pointer',
                                        transition: 'background 0.2s',
                                        zIndex: 1002
                                    }}
                                >
                                    Aplicar Recorte
                                </button>
                            </div>

                        ) :
                            <div style={{ marginTop: 12 }}>
                                <img
                                    src={imageUrl}
                                    alt="Vista previa"
                                    style={{
                                        width: '100%',
                                        height: 'auto',
                                        borderRadius: 6,
                                        objectFit: 'cover',
                                    }}
                                />
                            </div>}
                    </div>
                    <button
                        type="submit"
                        style={{
                            width: '100%',
                            padding: '12px 0',
                            borderRadius: 6,
                            border: 'none',
                            background: '#4f8cff',
                            color: '#fff',
                            fontWeight: 600,
                            fontSize: 18,
                            cursor: 'pointer',
                            transition: 'background 0.2s',
                        }}
                    >
                        Guardar
                    </button>
                </form>
            </div>
        </div>
    )
}

export default AddItemComponent