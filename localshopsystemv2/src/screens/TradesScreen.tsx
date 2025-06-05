import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import TradePagination from '../components/TradePagination';

type Props = {}

const TradesScreen = (props: Props) => {
  const { token } = useContext(MainContext);
  const navitigation = useNavigate();
  useEffect(() => {
    if (!token) {
      navitigation('/login');
    }
  }, [token, navitigation]);
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
      <TradePagination token={token} />
    </div>
  );
};

export default TradesScreen;
