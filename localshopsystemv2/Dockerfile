# -------------------------
# Build Stage
# -------------------------
FROM node:18-alpine AS build
WORKDIR /app

# Copiamos package.json y package-lock.json
COPY package*.json ./
RUN npm install

# Copiamos el resto del código y hacemos build
COPY . .
RUN npm run build

# -------------------------
# Production Stage
# -------------------------
FROM nginx:stable-alpine AS production
WORKDIR /usr/share/nginx/html

# Copiamos el build de React
COPY --from=build /app/build /usr/share/nginx/html

# Copiamos un archivo de configuración Nginx (lo vamos a crear en host)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
