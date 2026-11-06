import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import LoginScreen from './LoginScreen';
import MainContextProvider from '../components/MainContextProvider';
import AuthRepository from '../repositories/AuthRepository';
import { getAccessToken, getRefreshToken } from '../utils/authStorage';

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/admin/login']}>
      <MainContextProvider>
        <Routes>
          <Route path="/admin/login" element={<LoginScreen />} />
          <Route path="/admin/Home" element={<div>Panel</div>} />
        </Routes>
      </MainContextProvider>
    </MemoryRouter>
  );
}

function submit(user: string, pass: string) {
  fireEvent.change(screen.getByLabelText(/correo/i), { target: { value: user } });
  fireEvent.change(screen.getByLabelText(/contraseña/i), { target: { value: pass } });
  fireEvent.click(screen.getByRole('button', { name: /entrar/i }));
}

describe('LoginScreen', () => {
  let login: jest.SpyInstance;

  beforeEach(() => {
    localStorage.clear();
    login = jest.spyOn(AuthRepository.prototype, 'login');
  });
  afterEach(() => login.mockRestore());

  test('al entrar guarda los dos tokens y va al panel', async () => {
    login.mockResolvedValue({ accessToken: 'a', refreshToken: 'r', expiresIn: 900 });
    renderLogin();

    submit('admin@test', 'clave');

    expect(await screen.findByText('Panel')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('admin@test', 'clave');
    expect(getAccessToken()).toBe('a');
    expect(getRefreshToken()).toBe('r');
  });

  test('con credenciales malas muestra el error y no guarda nada', async () => {
    login.mockRejectedValue({ response: { status: 401, data: 'Usuario o contraseña incorrectos' } });
    renderLogin();

    submit('admin@test', 'mala');

    expect(await screen.findByText(/incorrectos/i)).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });

  test('si ya hay sesión guardada entra directamente', async () => {
    localStorage.setItem('token', 'guardado');
    renderLogin();

    await waitFor(() => expect(screen.getByText('Panel')).toBeInTheDocument());
    expect(login).not.toHaveBeenCalled();
  });
});
