import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import CategoryRepository from '../repositories/CategoryRepository';
import Selector from '../components/Selector';
import ItemSearcher from '../components/ItemSearcher';
import BarcodeListener from '../components/BarcodeListener';
import PageHeader from '../components/PageHeader';
import Item from '../models/Item';
import ItemRepository from '../repositories/ItemRepository';

type Props = {}

const AdminHomeScreen = (props: Props) => {
  const { token, categories, setCategories, setOpenBasket, addItemToBasket, setOpenAddItemModal } = useContext(MainContext);
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
      navigate('/admin/login');
    } else {
      fetchCategories();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, navigate]);

  const handleBarcodeScan = async (code: string) => {
    if (!token) {
      return;
    }

    const item = await itemRepository.getItemById(code, token);
    if (item) {
      addItemToBasket(item);
      setOpenBasket(true);
    } else {
      alert('Este item no esta registrado en el sistema. Por favor, registralo antes de agregarlo al carrito.');
      setOpenAddItemModal({ id: Number(code), name: '', price: 0, categoryId: 0, image: '' } as Item);
    }
  };

  function onItemClick(item: Item) {
    addItemToBasket(item);
  }

  return (
    <>
      <PageHeader
        title="Nueva venta"
        sub="Escanea un código de barras, busca un producto o navega por categorías."
      />
      <BarcodeListener handleBarcodeScan={handleBarcodeScan} />
      <ItemSearcher onItemClickProp={onItemClick}>
        <Selector categories={categories} token={token} />
      </ItemSearcher>
    </>
  );
};

export default AdminHomeScreen;
