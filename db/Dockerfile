FROM postgres:14

# Allow logging in with sha-256 authentication
RUN echo "" >>/var/lib/postgresql/data/pg_hba.conf && echo "host all all all scram-sha-256" >>/var/lib/postgresql/data/pg_hba.conf
