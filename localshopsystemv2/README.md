# Web y panel · Kiosco Botánico

React 19 + TypeScript 5 + Vite. Web pública en `/` y panel de administración en `/admin`
(se descarga aparte: los visitantes no cargan el panel ni las gráficas).

```bash
npm install
PORT=5173 npm run dev      # /kiosco se reenvía a la API (BACKEND_URL, por defecto http://localhost:56784)
npm test                   # Vitest + Testing Library
npm run typecheck
npm run build              # genera build/ (lo sirve nginx en la imagen Docker)
```

- **API**: todas las llamadas pasan por `src/utils/api.ts`. El interceptor de
  `src/utils/authInterceptor.ts` añade el token y lo renueva con el refresh token cuando caduca.
  Los repositorios (`src/repositories`) no tocan cabeceras.
- **URL de la API**: `VITE_API_URL`. En producción se fija en `.env.production`; en desarrollo es
  `/kiosco/` a través del proxy de Vite.
- **Informes** (`src/screens/GraphScreen.tsx`): colores por función, validados para daltonismo:
  ingresos en azul, beneficio en naranja, efectivo en aqua y tarjeta en violeta.
