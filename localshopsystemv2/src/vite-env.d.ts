/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base de la API (por defecto /kiosco/, ver .env.production). */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
