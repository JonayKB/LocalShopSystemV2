import React, { useContext, useEffect } from 'react'
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ExportRepository from '../repositories/ExportRepository';

type Props = {}

const ExportScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navigate = useNavigate();
    const exportRepository = new ExportRepository();
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
            <button
                onClick={async () => {
                    try {
                        const blob = await exportRepository.getExcel(token);
                        if (blob) {
                            const url = URL.createObjectURL(blob);
                            const link = document.createElement('a');
                            link.href = url;
                            link.download = 'DEVOLUCION_' + new Date().toISOString() + '.xlsx';
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