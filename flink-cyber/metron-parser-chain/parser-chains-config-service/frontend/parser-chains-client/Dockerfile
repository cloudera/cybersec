FROM node:12-alpine AS builder
WORKDIR /usr/app

COPY package*.json ./

RUN apk --no-cache add --virtual native-deps \
  g++ gcc libgcc libstdc++ linux-headers make python

RUN npm ci

COPY parser-chains-client .
RUN npx ng build --prod

FROM node:12-alpine
WORKDIR /usr/app

COPY package*.json ./
COPY --from=builder /usr/app/dist ./dist
COPY --from=builder /usr/app/scripts ./scripts
RUN npm ci --production

# ENV Vars
ENV REST_URL=localhost:3000
ENV PORT=4200

# UI listener port
EXPOSE 4200

CMD node scripts/config-app-server.js
