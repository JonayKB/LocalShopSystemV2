import React, { useContext, useEffect } from 'react';
import { Plus } from 'lucide-react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ItemSearcher from '../components/ItemSearcher';
import PageHeader from '../components/PageHeader';
import CategoryTaxes from '../components/CategoryTaxes';
import CategoryRepository from '../repositories/CategoryRepository';

type Props = {}

const ItemsScreen = (props: Props) => {
  const { token, setOpenAddItemModal, setCategories } = useContext(MainContext);
  const navigate = useNavigate();

  useEffect(() => {
    if (!token) {
      navigate('/admin/login');
      return;
    }
    // La ficha de producto necesita las categorías cargadas
    new CategoryRepository().getCategories(token).then(list => list && setCategories(list)).catch(() => undefined);
  }, [token, navigate, setCategories]);

  return (
    <>
      <PageHeader
        eyebrow="Gestión"
        title="Productos"
        sub="Busca un producto y pulsa sobre él para editar su precio, stock o imagen."
        actions={
          <button
            className="btn btn-primary"
            onClick={() => setOpenAddItemModal({
              categoryId: 0,
              image: '',
              name: '',
              price: 0,
              id: Math.floor(Math.random() * Number.MAX_SAFE_INTEGER),
              stock: 0,
              bareMinimun: 0,
              net: 0,
              ignoreStock: false,
            })}
          >
            <Plus size={17} /> Añadir producto
          </button>
        }
      />
      <ItemSearcher showAll onItemClickProp={(item) => setOpenAddItemModal(item)} onItemMenu={(item) => setOpenAddItemModal(item)} />
      <CategoryTaxes />
    </>
  );
};

export default ItemsScreen;
