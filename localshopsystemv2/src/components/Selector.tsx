
import React, { useEffect, useState } from 'react'
import Category from '../models/Category'
import ImageRespository from '../repositories/ImageRepository'
import Item from '../models/Item'
import Page from '../models/Page'
import ItemRepository from '../repositories/ItemRepository'
import ItemComponent from './ItemComponent'
import ItemPagination from './ItemPagination'

type Props = {
    categories: Category[],
    token: string | null
}

const Selector = (props: Props) => {
    const imageRepository = new ImageRespository();


    function handleCategoryClick(categoryId: number) {
        setSelectedCategory(categoryId);
    }

    const [imageUrls, setImageUrls] = useState<{ [key: string]: string }>({});
    const [selectedCategory, setSelectedCategory] = useState<number | null>(null);


    useEffect(() => {
        const fetchImages = async () => {
            const urls: { [key: string]: string } = {};
            await Promise.all(
                props.categories.map(async (category) => {
                    try {
                        urls[category.id] = await imageRepository.getImageById(category.image, props.token);
                        console.log(`Image for category ${category.id} fetched successfully.`);
                    } catch (error) {
                        console.error(`Error fetching image for category ${category.id}:`, error);
                        urls[category.id] = 'https://static.vecteezy.com/system/resources/previews/008/695/917/non_2x/no-image-available-icon-simple-two-colors-template-for-no-image-or-picture-coming-soon-and-placeholder-illustration-isolated-on-white-background-vector.jpg'; // Fallback image
                    }
                })
            );
            setImageUrls(urls);
        };
        fetchImages();
    }, [props.categories, props.token]);

    return (
        <div
        >
            <div
                style={{
                    width: '100%',
                    fontSize: '16px',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(17%, 2fr))',
                    gap: '20px',
                    height: '94.5vh',
                    justifyItems: 'center',
                    alignItems: 'start',
                    background: '#2a2d3a',
                    color: 'white',
                    fontFamily: `'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif`,
                    overflowY: 'auto',
                    marginTop:25
                }}
            >
                {selectedCategory !== null && selectedCategory >= 0 ? (
                    <div style={{ width: '70%' }}>
                        <button
                            style={{
                                textAlign: 'center',
                                width: '100%',
                                background: 'none',
                                border: 'none',
                                padding: 20,
                                cursor: 'pointer',
                                backgroundColor: '#2a2d3a',
                                color: 'white',
                                borderRadius: '8px',
                                boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                                transition: 'transform 0.2s ease-in-out',
                            }}
                            onClick={() => setSelectedCategory(null)}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.transform = 'scale(1.05)';
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.transform = 'scale(1)';
                            }}
                            tabIndex={0}
                            aria-label="Back to categories"
                            type="button"
                        >
                            Back to Categories
                        </button>
                        <h2 style={{ textAlign: 'center', marginBottom: '20px' }}>
                            Items in Category: {props.categories.find(cat => cat.id === selectedCategory)?.name}
                        </h2>
                        <ItemPagination categoryId={selectedCategory} token={props.token} />

                    </div>
                ) : props.categories.map((category) => (
                    <button
                        key={category.id}
                        style={{
                            textAlign: 'center',
                            width: '100%',
                            background: 'none',
                            border: 'none',
                            padding: 20,
                            cursor: 'pointer',
                            backgroundColor: '#2a2d3a',
                            color: 'white',
                            borderRadius: '8px',
                            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                            transition: 'transform 0.2s ease-in-out',
                            textTransform: 'capitalize'
                        }}
                        onClick={() => handleCategoryClick(category.id)}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'scale(1.05)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'scale(1)';
                        }}
                        tabIndex={0}
                        aria-label={`Select category ${category.name}`}
                        type="button"
                    >
                        <img
                            src={imageUrls[category.id]}
                            alt={category.name}
                            style={{
                                width: '100%',
                                height: 'auto',
                                borderRadius: '8px',
                                marginBottom: '10px'
                            }}
                            onError={(e) => {
                                e.currentTarget.src =
                                    'https://static.vecteezy.com/system/resources/previews/008/695/917/non_2x/no-image-available-icon-simple-two-colors-template-for-no-image-or-picture-coming-soon-and-placeholder-illustration-isolated-on-white-background-vector.jpg';
                            }}
                        />
                        <h2>{category.name}</h2>
                    </button>
                ))}
            </div>
        </div>
    )
}

export default Selector