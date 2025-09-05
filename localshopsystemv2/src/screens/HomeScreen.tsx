import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import CategoryRepository from '../repositories/CategoryRepository';
import Selector from '../components/Selector';
import ItemSearcher from '../components/ItemSearcher';
import BarcodeListener from '../components/BarcodeListener';
import Item from '../models/Item';
import ItemRepository from '../repositories/ItemRepository';

type Props = {}

const HomeScreen = (props: Props) => {
  const { token, categories, setCategories, updateBasket, setOpenBasket, setOpenAddItemModal,basketItems } = useContext(MainContext);
  const navigate = useNavigate();
  const categoryRepository = new CategoryRepository();
  const itemRepository = new ItemRepository();

  useEffect(() => {
    async function fetchCategories() {
      const categoriesLocal = await categoryRepository.getCategories(token)
      if (categoriesLocal) {
        setCategories(categoriesLocal);
      }
    }
    if (!token) {
      navigate('/login');
    } else {
      fetchCategories();
    }
  }, [token, navigate]);

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

  


  return (
    <div
      style={{
        flex: 1,
        background: '#2a2d3a',
        width: '100%',
        color: 'white',
        padding: '20px',
        fontSize: '20px',
        overflowY: 'hidden',
      }}
    >
      <BarcodeListener handleBarcodeScan={handleBarcodeScan} />
      <ItemSearcher />
      <Selector categories={categories} token={token}  />
    </div>
  );
};

export default HomeScreen;
