FROM node:18-alpine

WORKDIR /app
EXPOSE 3000
RUN apk update && apk add tzdata npm
COPY ./ArchieMate.Frontend/ /app/
RUN chmod +x /app/start.sh
RUN npm install

CMD ["/app/start.sh"]
