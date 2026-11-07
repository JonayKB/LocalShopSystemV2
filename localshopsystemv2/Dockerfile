# ---------- Compilación ----------
# glibc (no Alpine): el package-lock trae los binarios nativos de rollup/esbuild para glibc
FROM node:20-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
# npm a veces termina "bien" sin instalar nada: se comprueba que vite esté de verdad
RUN npm ci --no-audit --no-fund && test -x node_modules/.bin/vite
COPY . .
RUN npm run build

# ---------- Producción ----------
FROM nginx:stable-alpine AS production
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
