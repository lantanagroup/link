### STAGE 1: Build ###
FROM node:16.20-alpine3.17 AS build
WORKDIR /usr/src/app
COPY web/package.json ./
COPY web/package-lock.json ./
RUN apk add python3 alpine-sdk
RUN npm ci
RUN npm install -g @angular/cli
COPY web/ .
RUN ng build --configuration production

### STAGE 2: Run ###
FROM nginx:1.23.1
COPY web/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /usr/src/app/dist /usr/share/nginx/html
CMD ["nginx","-g","daemon off;"]
