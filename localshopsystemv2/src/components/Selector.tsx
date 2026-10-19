import React, { useContext, useState } from 'react'
import { ArrowLeft, ChevronRight } from 'lucide-react'
import Category from '../models/Category'
import ImageRespository from '../repositories/ImageRepository'
import Item from '../models/Item'
import ItemPagination from './ItemPagination'
import { MainContext } from './MainContextProvider'
import { PLACEHOLDER_IMG } from '../utils/placeholder'

type Props = {
    categories: Category[],
    token: string | null,
}

const Selector = (props: Props) => {
    const { setOpenBasket, addItemToBasket } = useContext(MainContext);
    const [selectedCategory, setSelectedCategory] = useState<number | null>(null);

    function onItemClick(item: Item) {
        addItemToBasket(item);
        setOpenBasket(true);
    }

    if (selectedCategory !== null && selectedCategory >= 0) {
        return (
            <div>
                <div className="crumbs">
                    <button className="btn btn-sm" onClick={() => setSelectedCategory(null)} type="button">
                        <ArrowLeft size={16} /> Categorías
                    </button>
                    <h2>{props.categories.find(cat => cat.id === selectedCategory)?.name}</h2>
                </div>
                <ItemPagination categoryId={selectedCategory} token={props.token} onItemClick={onItemClick} />
            </div>
        );
    }

    return (
        <div className="cat-grid">
            {props.categories.map((category) => (
                <button
                    key={category.id}
                    className="cat-card"
                    onClick={() => setSelectedCategory(category.id)}
                    aria-label={`Categoría ${category.name}`}
                    type="button"
                >
                    <div className="cat-img">
                        <img
                            src={ImageRespository.url(category.image, 320)}
                            alt=""
                            loading="lazy"
                            decoding="async"
                            onError={(e) => { if (e.currentTarget.src !== PLACEHOLDER_IMG) e.currentTarget.src = PLACEHOLDER_IMG; }}
                        />
                    </div>
                    <div className="cat-name">
                        {category.name}
                        <ChevronRight size={18} />
                    </div>
                </button>
            ))}
        </div>
    )
}

export default Selector