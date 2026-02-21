# Etap 1: Budowanie
FROM node:20-alpine AS build
WORKDIR /app

# Kopiujemy tylko pliki definicji paczek
COPY package.json ./
COPY package-lock.json ./ 

# Instalujemy zależności wewnątrz kontenera (teraz będą poprawne dla Linuxa)
RUN npm install

# Dopiero teraz kopiujemy kod źródłowy (pamiętaj o .dockerignore!)
COPY . .

# Budujemy aplikację
RUN npm run build

# Etap 2: Nginx (zostaje tak jak wcześniej)
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]