#!/bin/sh

set -e

if [ -z "$DOMAINS" ]; then
  echo "DOMAINS environment variable is not set"
  exit 1;
fi

if [ ! -f /etc/nginx/sites ]; then
  mkdir -p "/etc/nginx/sites"
fi

domains_fixed=$(echo "$DOMAINS" | tr -d \")
for domain in $domains_fixed; do
  echo "Checking configuration for $domain"

  if [ ! -f "/etc/nginx/sites/$domain.conf" ]; then
    echo "Creating Nginx configuration file /etc/nginx/sites/$domain.conf"
    sed "s/\${domain}/$domain/g" /customization/site.conf.debug.tpl > "/etc/nginx/sites/$domain.conf"
  fi
done

exec nginx -g "daemon off;"