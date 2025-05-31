import React, { useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MainContext } from '../components/MainContextProvider';

type Props = {}

const LoginScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navigate = useNavigate();
    useEffect(() => {
        const tokenStorage = localStorage.getItem('token');
        if (token || tokenStorage) {
            navigate('/home');
        }
    }, [token, navigate]);

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const username = formData.get('username') as string;
        const password = formData.get('password') as string;
        console.log('Username:', username);
        console.log('Password:', password);
    }

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: '#2a2d3a',
                width: '100vw',
                height: '100vh',
                fontFamily: `'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif`,
            }}
        >
            <div
                style={{
                    background: '#1e1f2a',
                    padding: '40px',
                    borderRadius: '12px',
                    boxShadow: '0 0 20px rgba(0, 0, 0, 0.5)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    width: '320px',
                    color: 'white',
                }}
            >
                <h1 style={{ marginBottom: '25px', fontSize: '26px', fontWeight: '600' }}>Iniciar sesión</h1>
                <form
                    onSubmit={handleSubmit}
                    style={{ display: 'flex', flexDirection: 'column', width: '100%' }}
                >
                    <div style={{ marginBottom: '18px' }}>
                        <label
                            htmlFor="username"
                            style={{
                                display: 'block',
                                marginBottom: '6px',
                                fontSize: '14px',
                                color: '#bbb',
                                fontWeight: '500',
                            }}
                        >
                            Correo electrónico
                        </label>
                        <input
                            type="text"
                            id="username"
                            name="username"
                            style={{
                                width: '93%',
                                padding: '10px',
                                borderRadius: '6px',
                                border: '1px solid #555',
                                backgroundColor: '#2a2d3a',
                                color: 'white',
                                fontSize: '15px',
                                outline: 'none',
                            }}
                        />
                    </div>
                    <div style={{ marginBottom: '25px' }}>
                        <label
                            htmlFor="password"
                            style={{
                                display: 'block',
                                marginBottom: '6px',
                                fontSize: '14px',
                                color: '#bbb',
                                fontWeight: '500',
                            }}
                        >
                            Contraseña
                        </label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            style={{
                                width: '93%',
                                padding: '10px',
                                borderRadius: '6px',
                                border: '1px solid #555',
                                backgroundColor: '#2a2d3a',
                                color: 'white',
                                fontSize: '15px',
                                outline: 'none',
                            }}
                        />
                    </div>
                    <button
                        type="submit"
                        style={{
                            padding: '10px',
                            backgroundColor: '#4c5cdb',
                            border: 'none',
                            borderRadius: '6px',
                            color: 'white',
                            fontSize: '16px',
                            fontWeight: '600',
                            cursor: 'pointer',
                            transition: 'background-color 0.2s ease-in-out',
                        }}
                        onMouseOver={e => (e.currentTarget.style.backgroundColor = '#3b4bc1')}
                        onMouseOut={e => (e.currentTarget.style.backgroundColor = '#4c5cdb')}
                    >
                        Entrar
                    </button>
                </form>
            </div>
        </div>
    );
};

export default LoginScreen;
