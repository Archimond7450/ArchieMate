FROM mcr.microsoft.com/dotnet/aspnet:7.0-alpine AS base
WORKDIR /app
EXPOSE 8080
RUN apk update && apk add tzdata

ENV ASPNETCORE_URLS=http://+:8080

FROM mcr.microsoft.com/dotnet/sdk:7.0-alpine AS staging
WORKDIR /src
RUN apk add --update npm
COPY ArchieMate.Backend ./ArchieMate.Backend
COPY ArchieMate.Chatbot ./ArchieMate.Chatbot
COPY ArchieMate.EventSub ./ArchieMate.EventSub
COPY ArchieMate.Frontend ./ArchieMate.Frontend
COPY ArchieMate.Helpers ./ArchieMate.Helpers
COPY ArchieMate.TwitchIRC ./ArchieMate.TwitchIRC
WORKDIR /src

FROM staging AS publish
RUN dotnet publish "ArchieMate.Backend/ArchieMate.Backend.csproj" -c Release -o /app/publish /p:UseAppHost=false --nologo

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "ArchieMate.Backend.dll"]
