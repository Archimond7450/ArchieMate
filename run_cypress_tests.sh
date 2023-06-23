#!/bin/sh

docker compose up -f docker-compose.cypress.yml --build --remove-orphans --force-recreate --detach
docker logs -f archiemate_cypress
docker compose down -f docker-compose.cypress.yml
