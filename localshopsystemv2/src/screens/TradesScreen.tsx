import React, { useContext, useEffect } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import TradePagination from '../components/TradePagination';
import PageHeader from '../components/PageHeader';
import VerifactuBanner from '../components/VerifactuBanner';

type Props = {}

const TradesScreen = (props: Props) => {
  const { token } = useContext(MainContext);
  const navitigation = useNavigate();
  useEffect(() => {
    if (!token) {
      navitigation('/admin/login');
    }
  }, [token, navitigation]);

  return (
    <>
      <PageHeader
        eyebrow="Día a día"
        title="Ventas"
        sub="Cada venta tiene un número correlativo. Una venta no se borra: si te equivocas, anúlala y quedará registrada."
      />
      <VerifactuBanner token={token} />
      <TradePagination token={token} />
    </>
  );
};

export default TradesScreen;
