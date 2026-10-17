import React, { useContext, useEffect, useState } from 'react';
import { BarChart3 } from 'lucide-react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import GraphRepository from '../repositories/GraphRepository';
import GraphData from '../models/Graph';
import { BarChart } from '@mui/x-charts';
import PageHeader from '../components/PageHeader';

type Props = {}

const GraphScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const graphRepository = new GraphRepository();
    const [selectedCategory, setSelectedCategory] = useState<string>('Ventas');
    const [selectedGraph, setSelectedGraph] = useState<string | null>(null);
    const [graphData, setGraphData] = useState<GraphData | null>(null);
    const [loading, setLoading] = useState(false);

    const GRAPHS: Record<string, { label: string; fn: Function }[]> = {
        Ventas: [
            { label: 'Compras por día', fn: graphRepository.getTradeCountByDay },
            { label: 'Productos más vendidos', fn: graphRepository.getTopSoldItems },
            { label: 'Media de valor de compra', fn: graphRepository.getAverageTradeValue },
            { label: 'Ingresos por categoría', fn: graphRepository.getIncomeByCategory },
        ],
        Stock: [
            { label: 'Stock por categoría', fn: graphRepository.getStockByCategory },
            { label: 'Productos que ignoran stock', fn: graphRepository.getItemsIgnoringStock },
        ],
        Economía: [
            { label: 'Productos más rentables', fn: graphRepository.getTopProfitItems },
            { label: 'Beneficio acumulado', fn: graphRepository.getAccumulatedProfitOvertime },
        ],
    };

    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
        }
    }, [token, navitigation]);

    async function onGraphSelect(label: string, type: Function) {
        if (!token) return;
        setSelectedGraph(label);
        setLoading(true);
        try {
            const data = await type(token);
            if (data) {
                setGraphData(data);
            } else {
                alert('Error al obtener los datos');
            }
        } catch {
            alert('Error al obtener los datos');
        } finally {
            setLoading(false);
        }
    }

    const axisStyle = { tickLabelStyle: { fill: '#5b665f', fontSize: 12 } };

    return (
        <>
            <PageHeader
                eyebrow="Análisis"
                title="Gráficos"
                sub="Elige un área y una gráfica para ver cómo va el negocio. Las ventas anuladas no cuentan."
            />

            <div className="toolbar">
                <div className="seg" role="tablist">
                    {Object.keys(GRAPHS).map(cat => (
                        <button
                            key={cat}
                            role="tab"
                            aria-selected={selectedCategory === cat}
                            className={selectedCategory === cat ? 'is-active' : ''}
                            onClick={() => setSelectedCategory(cat)}
                        >
                            {cat}
                        </button>
                    ))}
                </div>
                <div className="chips">
                    {GRAPHS[selectedCategory].map(g => (
                        <button
                            key={g.label}
                            className={`chip ${selectedGraph === g.label ? 'is-active' : ''}`}
                            onClick={() => onGraphSelect(g.label, g.fn)}
                        >
                            {g.label}
                        </button>
                    ))}
                </div>
            </div>

            <div className="card chart-card">
                {loading ? (
                    <div className="skeleton" style={{ height: 380 }} aria-busy="true" />
                ) : graphData ? (
                    <>
                        {selectedGraph && <h2 className="chart-title">{selectedGraph}</h2>}
                        <BarChart
                            xAxis={[{ scaleType: 'band', data: graphData.labels, ...axisStyle }]}
                            yAxis={[axisStyle]}
                            series={[{ data: graphData.data, color: '#1f5e46' }]}
                            height={400}
                            sx={{
                                '& .MuiChartsAxis-line, & .MuiChartsAxis-tick': { stroke: '#cbc7bb' },
                                '& .MuiChartsGrid-line': { stroke: '#e2dfd6' },
                            }}
                        />
                    </>
                ) : (
                    <div className="empty" style={{ border: 'none' }}>
                        <BarChart3 size={36} strokeWidth={1.4} />
                        <strong>Elige una gráfica</strong>
                        <span>Selecciona una de las opciones de arriba para cargarla.</span>
                    </div>
                )}
            </div>
        </>
    );
};

export default GraphScreen;
