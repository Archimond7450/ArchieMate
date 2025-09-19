#!/bin/sh

printf "%s\n" "==================     Help for psql   ========================="
printf "%s\n" "\\dt		: Describe the current database"
printf "%s\n" "\\t [table]	: Describe a table"
printf "%s\n" "\\c		: Connect to a database"
printf "%s\n" "\\h		: help with SQL commands"
printf "%s\n" "\\?		: help with psql commands"
printf "%s\n" "\\q		: quit"
printf "%s\n" "=================================================================="

docker exec -it postgres psql -U archiemate -d archiemate_db
