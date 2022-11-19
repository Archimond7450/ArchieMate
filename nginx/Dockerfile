FROM nginx:alpine

RUN apk add --no-cache openssl

COPY ./nginx/default.conf /etc/nginx/conf.d
COPY ./nginx/gzip.conf ./nginx/options-ssl-nginx.conf ./nginx/hsts.conf /etc/nginx/includes/
COPY ./nginx/site.conf.tpl /customization/
COPY ./nginx/nginx.sh /customization/

RUN chmod +x /customization/nginx.sh

EXPOSE 80

CMD ["/customization/nginx.sh"]
