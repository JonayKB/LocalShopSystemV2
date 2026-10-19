import axios from 'axios';

/**
 * Si el backend responde 401 a una petición que llevaba token (caducado, secreto JWT
 * cambiado, usuario retirado...), se borra la sesión y se manda al login.
 * Sin esto, la pantalla se queda abierta pero vacía.
 */
let registered = false;

export function registerAuthInterceptor(): void {
  if (registered) {
    return;
  }
  registered = true;

  axios.interceptors.response.use(
    (response) => response,
    (error) => {
      const status = error?.response?.status;
      const sentToken = Boolean(error?.config?.headers?.Authorization);
      const onLogin = window.location.pathname.toLowerCase().endsWith('/admin/login');

      if (status === 401 && sentToken && !onLogin) {
        try {
          localStorage.removeItem('token');
        } catch {
          /* sin almacenamiento disponible */
        }
        // Recarga completa: también limpia el token que vive en el contexto de React.
        window.location.replace('/admin/login');
      }
      return Promise.reject(error);
    }
  );
}