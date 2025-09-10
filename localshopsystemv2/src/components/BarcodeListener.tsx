import { useContext, useEffect, useRef } from 'react';
import { MainContext } from './MainContextProvider';

interface BarcodeListenerProps {
    handleBarcodeScan: (code: string) => void;
}

const BarcodeListener: React.FC<BarcodeListenerProps> = (props) => {
    const buffer = useRef('');
    const timer = useRef<NodeJS.Timeout | null>(null);
    const { token } = useContext(MainContext);



    useEffect(() => {




        const handleKeyPress = (e: KeyboardEvent) => {
            if (timer.current) clearTimeout(timer.current);
            if (!token) return;

            if (e.key === 'Enter') {
                const code = buffer.current;
                if (/^\d{5,20}$/.test(code)) {
                    props.handleBarcodeScan(code);
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
    }, [token]);

    return null;
};

export default BarcodeListener;
