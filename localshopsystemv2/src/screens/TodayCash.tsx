import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ExportRepository from '../repositories/ExportRepository';


type Props = {}

const TodayCash = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const exportRepository = new ExportRepository();
    const [incomeByCategory, setIncomeByCategory] = useState<Record<string, number>>();

    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
        }
        const fetchData = async () => {
            if (token) {
                const data = await exportRepository.getIncomeByCategory(token);
                setIncomeByCategory(data);
                console.log(data);
            }

        }
        fetchData();


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
            <h1 style={{ fontSize: '3rem', fontWeight: 'bold', textAlign: 'center' }}>CAJA DEL DÍA</h1>
            <div style={{ padding: '20px', backgroundColor: '#1e1e2f', color: 'white', borderRadius: 8, height: '85vh', overflowY: 'scroll', margin: 10 }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 20 }}>
                    <thead style={{ borderBottom: '2px solid white' }}>
                        <tr>
                            {incomeByCategory && Object.keys(incomeByCategory).map((key) => (
                                <th key={key} style={{ padding: 10, textAlign: 'left' }}>{key.toUpperCase()}</th>
                            ))}
                            <th key="total" style={{ padding: 10, textAlign: 'left' }}>TOTAL</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>

                            {incomeByCategory && Object.keys(incomeByCategory).map((key) => (
                                <td key={key} style={{ padding: 10, textAlign: 'left', border: '1px solid white' }}>{incomeByCategory[key].toFixed(2)} €</td>
                            ))}
                            <td key="total" style={{ padding: 10, textAlign: 'left', fontWeight: 'bold', border: '1px solid white' }}>{incomeByCategory && Object.values(incomeByCategory).reduce((a, b) => a + b, 0).toFixed(2)} €</td>
                        </tr>
                    </tbody>

                </table>

                <div>

                </div>
            </div>
        </div>
    );
}

export default TodayCash