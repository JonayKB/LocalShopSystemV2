import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import CategoryRepository from '../repositories/CategoryRepository';
import Selector from '../components/Selector';
import ItemSearcher from '../components/ItemSearcher';

type Props = {}

const HomeScreen = (props: Props) => {
  const { token, categories, setCategories } = useContext(MainContext);
  const navigate = useNavigate();
  const categoryRepository = new CategoryRepository();

  useEffect(() => {
    async function fetchCategories() {
      const categoriesLocal = await categoryRepository.getCategories(token)
      if (categoriesLocal) {
        setCategories(categoriesLocal);
        console.log('Categorías cargadas:', categoriesLocal);
      } else {
        console.error('No se pudieron cargar las categorías');
      }
    }
    if (!token) {
      navigate('/login');
    } else {
      fetchCategories();
    }
  }, [token, navigate]);
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
      <ItemSearcher />
      <Selector categories={categories} token={token} />
    </div>
  );
};

export default HomeScreen;
