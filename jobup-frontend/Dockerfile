# Stage 1: Build the Angular application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source files
COPY . .

# Build the Angular application for production
RUN npm run build:prod

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy the built Angular application to Nginx web root
COPY --from=build /app/dist/jobup-frontend/browser /usr/share/nginx/html

# Copy custom Nginx configuration (optional)
# COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

