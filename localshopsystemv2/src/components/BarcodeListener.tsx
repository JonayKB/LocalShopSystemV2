import { useContext, useEffect, useRef } from 'react';
import { MainContext } from './MainContextProvider';

interface BarcodeListenerProps {
    onScan: (code: string) => void;
}

const BarcodeListener: React.FC<BarcodeListenerProps> = ({ onScan }) => {
    const buffer = useRef('');
    const timer = useRef<NodeJS.Timeout | null>(null);
    const { token } = useContext(MainContext);

    useEffect(() => {
        if (!token) {
            console.warn('No token available, barcode scanning is disabled.');
            return;
        }

        const handleKeyPress = (e: KeyboardEvent) => {
            console.log('Key pressed:', e.key);
            if (timer.current) clearTimeout(timer.current);

            if (e.key === 'Enter') {
                const code = buffer.current;
                if (/^\d{5,8}$/.test(code)) {
                    onScan(code);
                }
                buffer.current = '';
                return;
            }

            if (/^\d$/.test(e.key)) {
                buffer.current += e.key;

                timer.current = setTimeout(() => {
                    buffer.current = '';
                }, 10000);
            } else {
                buffer.current = '';
            }
        };

        window.addEventListener('keydown', handleKeyPress);
        return () => window.removeEventListener('keydown', handleKeyPress);
    }, [onScan, token]);

    return null;
};

export default BarcodeListener;
