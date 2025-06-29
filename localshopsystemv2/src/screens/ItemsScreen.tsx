import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ItemSearcher from '../components/ItemSearcher';

type Props = {}

const ItemsScreen = (props: Props) => {
  const { token, setOpenAddItemModal } = useContext(MainContext);
  const navigate = useNavigate();


  useEffect(() => {



    if (!token) {
      navigate('/login');
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
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          marginBottom: '20px',
        }}
      >
        <button
          onClick={() => setOpenAddItemModal({
            categoryId: 0,
            image: '', 
            name: '',
            price: 0,
            id: -1,
          })}
          style={{
            backgroundColor: '#4CAF50',
            color: 'white',
            padding: '12px 24px',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '18px',
            fontWeight: 'bold',
            boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
            transition: 'background 0.2s',
          }}
          onMouseOver={e => (e.currentTarget.style.backgroundColor = '#388e3c')}
          onMouseOut={e => (e.currentTarget.style.backgroundColor = '#4CAF50')}
        >
          Agregar Producto
        </button>
      </div>
      <ItemSearcher />
    </div>
  );
};

export default ItemsScreen;
