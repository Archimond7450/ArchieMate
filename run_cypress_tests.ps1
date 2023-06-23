docker compose -f docker-compose.cypress.yml up --build --remove-orphans --force-recreate
docker logs -f archiemate_cypress
docker compose -f docker-compose.cypress.yml down
