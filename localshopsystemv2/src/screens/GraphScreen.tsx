import React, { useContext, useEffect, useState } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import GraphRepository from '../repositories/GraphRepository';
import GraphData from '../models/Graph';
import { BarChart } from '@mui/x-charts';


type Props = {}

const GraphScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const graphRepository = new GraphRepository();
    const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
    const [graphData, setGraphData] = useState<GraphData | null>(null);
    const buttonStyle = {

        background: '#00bcd4',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        padding: '8px 20px',
        cursor: 'pointer',
        fontWeight: 'bold',
        fontSize: '16px',

    }
    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
        }
    }, [token, navitigation]);

    async function onGraphSelect(type: Function) {
        if (!token) return;
        const data = await type(token);
        if (data) {
            setGraphData(data);
        } else {
            alert('Error al obtener los datos');
        }
    }





    return (
        <div
            style={{
                flex: 1,
                background: '#2a2d3a',
                width: '100vw',
                color: 'white',
                padding: '32px',
                fontSize: '20px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'flex-start',
                alignItems: 'center',
                minHeight: '100vh',
                boxSizing: 'border-box',
            }}
        >
            <div style={{ display: 'flex', gap: '16px', marginBottom: '32px' }}>
                <button
                    style={{
                        background: selectedCategory === 'Ventas' ? '#4f5b62' : '#3949ab',
                        color: 'white',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '10px 24px',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        fontSize: '18px',
                    }}
                    onClick={() => setSelectedCategory('Ventas')}
                >
                    Ventas
                </button>
                <button
                    style={{
                        background: selectedCategory === 'Stock' ? '#4f5b62' : '#3949ab',
                        color: 'white',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '10px 24px',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        fontSize: '18px',
                    }}
                    onClick={() => setSelectedCategory('Stock')}
                >
                    Stock
                </button>
                <button
                    style={{
                        background: selectedCategory === 'Economia' ? '#4f5b62' : '#3949ab',
                        color: 'white',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '10px 24px',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        fontSize: '18px',
                    }}
                    onClick={() => setSelectedCategory('Economia')}
                >
                    Economia
                </button>
            </div>

            {selectedCategory === 'Ventas' && (
                <div style={{ marginBottom: '24px', display: 'flex', gap: '12px', flexWrap: 'wrap', justifyContent: 'space-between' }}>
                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getTradeCountByDay)}
                    >
                        Compras por día
                    </button>

                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getTopSoldItems)}
                    >
                        Productos mas vendidos
                    </button>
                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getAverageTradeValue)}
                    >
                        Media valor de compra
                    </button>

                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getIncomeByCategory)}
                    >
                        Ingresos por categoría
                    </button>

                </div>
            )}
            {selectedCategory === 'Stock' && (
                <div style={{ marginBottom: '24px', display: 'flex', gap: '12px', flexWrap: 'wrap', justifyContent: 'space-between' }}>
                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getStockByCategory)}
                    >
                        Stock por categoría
                    </button>
                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getItemsIgnoringStock)}
                    >
                        Productos ignorando stock
                    </button>

                </div>
            )}
            {selectedCategory === 'Economia' && (
                <div style={{ marginBottom: '24px', display: 'flex', gap: '12px', flexWrap: 'wrap', justifyContent: 'space-between' }}>
                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getTopProfitItems)}
                    >
                        Productos mas rentables
                    </button>
                    <button
                        style={buttonStyle}
                        onClick={async () => await onGraphSelect(graphRepository.getAccumulatedProfitOvertime)}
                    >
                        Beneficio acumulado
                    </button>

                </div>
            )}

            <div style={{ flex: 1, width: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                {graphData && (
                    <BarChart
                        xAxis={[{ data: graphData.labels, tickLabelStyle: { fill: '#ffffffff' } }]}
                        yAxis={[{ tickLabelStyle: { fill: '#ffffffff' } }]}
                        series={[{ data: graphData.data, color: '#073780ff' }]}
                        height={window.innerHeight - 180}
                        sx={{
                            background: '#23263a',
                            borderRadius: '12px',
                            boxShadow: '0 2px 12px rgba(0,0,0,0.2)',
                            color: '#ffffffff',
                        }}
                    />
                )}
            </div>
        </div>
    );
};

export default GraphScreen;
