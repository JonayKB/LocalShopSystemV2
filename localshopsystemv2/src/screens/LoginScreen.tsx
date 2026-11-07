import React, { useContext, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { MainContext } from '../components/MainContextProvider';
import AuthRepository from '../repositories/AuthRepository';
import { Lock } from 'lucide-react';
import '../styles/login.css';
import { getAccessToken, setTokens } from '../utils/authStorage';

type Props = {}

const LoginScreen = (props: Props) => {
    const { token, setToken } = useContext(MainContext);
    const navigate = useNavigate();
    const location = useLocation();
    // Tras entrar, vuelve a la pantalla que se quería abrir (si venía redirigido).
    const target = (location.state as { from?: string } | null)?.from ?? '/admin/Home';
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const authRepository = new AuthRepository();
    useEffect(() => {
        const tokenStorage = getAccessToken();
        if (tokenStorage && !token) {
            setToken(tokenStorage);
        }
        if (token) {
            navigate(target, { replace: true });
        }
    }, [token, navigate, setToken, target]);

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const username = formData.get('username') as string;
        const password = formData.get('password') as string;

        try {
            const tokens = await authRepository.login(username, password);
            if (tokens) {
                setTokens(tokens);
                setToken(tokens.accessToken);
                navigate(target, { replace: true });
            }
        } catch (error) {
            if (error && typeof error === 'object' && 'response' in error && error.response && typeof error.response === 'object' && 'data' in error.response) {
                setErrorMessage((error as any).response.data);
            } else {
                setErrorMessage('Ocurrió un error inesperado');
            }
        }


    }

    return (
        <div className="login theme-light">
            <div className="login-card">
                <div className="login-mark" aria-hidden="true">
                    <Lock size={20} strokeWidth={1.9} />
                </div>
                <h1>Iniciar sesión</h1>
                <p className="login-sub">Panel de administración · Kiosco Botanico</p>
                <form onSubmit={handleSubmit}>
                    <div className="login-field">
                        <label htmlFor="username">Correo electrónico</label>
                        <input type="text" id="username" name="username" autoComplete="username" />
                    </div>
                    <div className="login-field">
                        <label htmlFor="password">Contraseña</label>
                        <input type="password" id="password" name="password" autoComplete="current-password" />
                    </div>
                    {errorMessage && <div className="login-error">{errorMessage}</div>}
                    <button type="submit" className="login-submit">
                        Entrar
                    </button>
                </form>
            </div>
        </div>
    );
};

export default LoginScreen;
