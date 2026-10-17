import React, { useContext, useEffect, useState } from 'react'
import { Download } from 'lucide-react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ExportRepository from '../repositories/ExportRepository';
import PageHeader from '../components/PageHeader';

type Props = {}

const MONTHS = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

const ExportScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navigate = useNavigate();
    const exportRepository = new ExportRepository();
    const [month, setMonth] = useState<number>(new Date().getMonth() + 1);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (!token) {
            navigate('/admin/login');
        }
    }, [token, navigate]);

    const download = async () => {
        setBusy(true);
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
        } finally {
            setBusy(false);
        }
    };

    return (
        <>
            <PageHeader
                eyebrow="Análisis"
                title="Exportar"
                sub="Descarga en Excel las ventas e ingresos de un mes."
            />
            <div className="card card-pad export-card">
                <div className="field">
                    <label htmlFor="month">Mes</label>
                    <select className="select" name="month" id="month" value={month} onChange={(e) => setMonth(Number(e.target.value))}>
                        {MONTHS.map((m, i) => (
                            <option key={m} value={i + 1}>{m}</option>
                        ))}
                    </select>
                </div>
                <button className="btn btn-primary btn-lg" onClick={download} disabled={busy} aria-label="Exportar datos">
                    <Download size={18} /> {busy ? 'Generando…' : 'Descargar Excel'}
                </button>
            </div>
        </>
    )
}

export default ExportScreen
