import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import TradeRepository from '../repositories/TradeRepository';
import TradeComponent from '../components/TradeComponent';
import Trade from '../models/Trade';

type Props = {}

const TodayCash = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const [todayTrades, setTodayTrades] = useState([]);
    const tradeRepository = new TradeRepository();
    const handleDelete = async (tradeId: number) => {
        if (!window.confirm('¿Estás seguro de que deseas eliminar esta venta? Esta acción no se puede deshacer.')) {
            return;
        }
        await tradeRepository.deleteTrade(tradeId, token);
        fetchTodayTrades();
    };
    const fetchTodayTrades = async () => {
        const trades = await tradeRepository.getTodayTrades(token);
        setTodayTrades(trades || []);
    };
    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
        }

        fetchTodayTrades();
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
            <div style={{  padding: '20px', backgroundColor: '#1e1e2f', color: 'white', borderRadius: 8, height: '85vh', overflowY: 'scroll', margin: 10 }}>
                <div style={{ flex: 1, display: 'flex', justifyContent: 'space-around', alignItems: 'center', marginBottom: 10 }}>
                    <h2 style={{ textAlign: 'center' }}>Identificador</h2>
                    <h2 style={{ textAlign: 'center' }}>Fecha</h2>
                    <h2 style={{ textAlign: 'center' }}>Total</h2>
                    <h2 style={{ textAlign: 'center' }}>Acciones</h2>
                </div>
                <div>
                    {todayTrades.map((trade: Trade) => (
                        <TradeComponent key={trade.id} trade={trade} onDelete={handleDelete}/>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default TodayCash