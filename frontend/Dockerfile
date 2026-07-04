# Stage 1: Build Vite React application
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ARG VITE_STRIPE_CHECKOUT_ENABLED
ENV VITE_STRIPE_CHECKOUT_ENABLED=${VITE_STRIPE_CHECKOUT_ENABLED}
RUN npm run build


# Stage 2: Serve application statically via Nginx
FROM nginx:1.25-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
