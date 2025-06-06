import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import CategoryRepository from '../repositories/CategoryRepository';
import ItemSearcher from '../components/ItemSearcher';

type Props = {}

const ItemsScreen = (props: Props) => {
  const { token } = useContext(MainContext);
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
      }}
    >
      <ItemSearcher />

    </div>
  );
};

export default ItemsScreen;
