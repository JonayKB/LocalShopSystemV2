import { useContext, useEffect, useRef } from 'react';
import { MainContext } from './MainContextProvider';
import ItemRepository from '../repositories/ItemRepository';
import Item from '../models/Item';

interface BarcodeListenerProps {
}

const BarcodeListener: React.FC<BarcodeListenerProps> = () => {
    const buffer = useRef('');
    const timer = useRef<NodeJS.Timeout | null>(null);
    const { updateBasket, setOpenBasket, token, setOpenAddItemModal } = useContext(MainContext);


    useEffect(() => {

        const itemRepository = new ItemRepository();
        const handleBarcodeScan = async (code: string) => {
            if (!token) {
                return;
            }
            const item = await itemRepository.getItemById(code, token);
            if (item) {
                updateBasket(item, 1);
                setOpenBasket(true);
            } else {
                alert('Este item no esta registrado en el sistema. Por favor, registralo antes de agregarlo al carrito.');
                setOpenAddItemModal({ id: Number(code), name: '', price: 0, categoryId: 0, image: '' } as Item);
            }
        };


        const handleKeyPress = (e: KeyboardEvent) => {
            if (timer.current) clearTimeout(timer.current);
            if (!token) return;

            if (e.key === 'Enter') {
                const code = buffer.current;
                if (/^\d{5,20}$/.test(code)) {
                    handleBarcodeScan(code);
                }
                buffer.current = '';
                return;
            }

            if (/^\d$/.test(e.key)) {
                buffer.current += e.key;

                timer.current = setTimeout(() => {
                    buffer.current = '';
                }, 1000);
            } else {
                buffer.current = '';
            }
        };

        window.addEventListener('keydown', handleKeyPress);
        return () => window.removeEventListener('keydown', handleKeyPress);
    }, [token]);

    return null;
};

export default BarcodeListener;
