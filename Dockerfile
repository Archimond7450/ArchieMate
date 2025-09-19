FROM archlinux:latest AS base
WORKDIR /app
RUN groupadd -o -g 1000 archi \
    && useradd -o -u 1000 -g 1000 --create-home archi \
    && chmod u+rwx /app \
    && chown -R archi /app
USER archi

FROM base AS build
USER root
RUN mkdir -p /etc/sudoers.d \
    && echo "archi ALL=(ALL:ALL) NOPASSWD:ALL" > /etc/sudoers.d/archi
RUN pacman -Syu --noconfirm git base-devel jdk-openjdk sbt npm
USER archi
RUN git clone https://aur.archlinux.org/yay.git && cd yay && makepkg -si --noconfirm && cd - && rm -rf .cache yay
RUN yay -Syu --noconfirm scala3
COPY --chown=archi . .
RUN cd js && npm install --include=dev && cd -
RUN sbt clean
RUN cd js && npm run build && cd - && cp -r /app/js/dist/* /app/jvm/src/main/resources/public/
RUN sbt "crossJVM / assembly"

FROM base AS final
USER root
RUN pacman -Syu --noconfirm jre-openjdk-headless
USER archi
COPY --from=build /app/jvm/target/scala-*/*.jar /app/ScalaJSTest.jar
EXPOSE 8080
CMD [ "java", "-jar", "/app/ScalaJSTest.jar" ]