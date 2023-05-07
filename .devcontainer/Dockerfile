FROM mcr.microsoft.com/dotnet/sdk:7.0-alpine AS base

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apk update && apk add tzdata npm yarn git openssh bash

RUN dotnet tool restore
#RUN echo 'export PATH=$PATH:~/.nuget/packages' >/root/.profile

RUN mkdir -p /root/.ssh
COPY .ssh /root/.ssh
RUN chmod -R 600 /root/.ssh