import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ExportRepository from '../repositories/ExportRepository';

type Props = {}

const ExportScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navigate = useNavigate();
    const exportRepository = new ExportRepository();
    const [month, setMonth] = useState<number>(new Date().getMonth() + 1);
    useEffect(() => {
        if (!token) {
            navigate('/admin/login');
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
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center'
            }}
        >
            <select name="month" id="month"  value={month} style={{ marginBottom: '20px', padding: '10px', fontSize: '18px', borderRadius: '5px', border: '1px solid #ccc', background: '#2a2d3a', color: '#fff' }} onChange={(e) => setMonth(Number(e.target.value))}>
                <option value="1">Enero</option>
                <option value="2">Febrero</option>
                <option value="3">Marzo</option>
                <option value="4">Abril</option>
                <option value="5">Mayo</option>
                <option value="6">Junio</option>
                <option value="7">Julio</option>
                <option value="8">Agosto</option>
                <option value="9">Septiembre</option>
                <option value="10">Octubre</option>
                <option value="11">Noviembre</option>
                <option value="12">Diciembre</option>
            </select>
            <button
                onClick={async () => {
                    try {
                        const blob = await exportRepository.getExcel(token, month);
                        if (blob) {
                            const url = URL.createObjectURL(blob);
                            const link = document.createElement('a');
                            link.href = url;
                            link.download = 'INFORMACiÓN-' + month + '.xlsx';
                            document.body.appendChild(link);
                            link.click();
                            document.body.removeChild(link);
                            URL.revokeObjectURL(url);
                        } else {
                            throw new Error('El archivo no fue generado');
                        }
                    } catch (error) {
                        alert('Error al exportar datos');
                    }
                }}

                style={{
                    padding: '10px 20px',
                    backgroundColor: '#4CAF50',
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: 'pointer',
                    fontSize: '24px',
                    transition: 'transform 0.2s ease',
                    boxShadow: '0 4px 8px rgba(0, 0, 0, 0.2)',
                }}
                onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'scale(1.05)';
                }
                }
                onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'scale(1)';
                }
                }
                tabIndex={0}
                aria-label="Export Data"
            >
                Descargar Informacion
            </button>

        </div>
    )
}

export default ExportScreen