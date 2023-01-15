FROM mcr.microsoft.com/dotnet/sdk:7.0-alpine AS base

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apk update && apk add tzdata npm git openssh

RUN dotnet tool install --global dotnet-ef

RUN echo 'export PATH=$PATH:/root/.dotnet/tools/' >/root/.profile