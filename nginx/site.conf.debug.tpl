server {
    listen 80;
    server_name ${domain};

    include /etc/nginx/vhosts/${domain}.conf;
}