FROM node:25-alpine

WORKDIR /app

# Copy package files first to leverage Docker layer caching
COPY ./app/package.json ./app/tsconfig.json ./app/tsconfig.node.json ./app/vite.config.ts ./

# Install dependencies
RUN npm install

# Copy the rest of the application source
COPY ./app .

EXPOSE 80

CMD ["/bin/sh", "-c", "npm run dev -- --host"]