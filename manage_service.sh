#!/bin/sh

SERVICENAME=$(basename $(pwd))
SYSTEMDPATH="/etc/systemd/system"

SERVICEFILENAME="${SERVICENAME}.service"
SERVICEPATH="${SYSTEMDPATH}/${SERVICEFILENAME}"

TIMERSERVICEFILENAME="${SERVICENAME}Timer.service"
TIMERSERVICEPATH="${SYSTEMDPATH}/${TIMERSERVICEFILENAME}"

TIMERSERVICETIMERFILENAME="${SERVICENAME}Timer.timer"
TIMERSERVICETIMERPATH="${SYSTEMDPATH}/${TIMERSERVICETIMERFILENAME}"

DOCKERCOMPOSE="$(which docker) compose"

SERVICESHELLDESCRIPTION="systemd service for ${SERVICENAME} on ${SERVICEPATH}"
DIVIDER="===================================================="

MANAGECREATE="create"
MANAGEDELETE="delete"
MANAGEOPTION=$MANAGECREATE

if [ $# -gt 0 ]; then
  if [ "$1" = "$MANAGECREATE" -o "$1" = "$MANAGEDELETE" ]; then
    MANAGEOPTION=$1
  fi
fi

if [ "$MANAGEOPTION" = "$MANAGEDELETE" ]; then
  echo "INFO: Deleting $SERVICESHELLDESCRIPTION"
  echo $DIVIDER
  sudo systemctl stop "$TIMERSERVICETIMERFILENAME}"
  sudo systemctl disable "$TIMERSERVICETIMERFILENAME}"
  sudo systemctl stop "${TIMERSERVICEFILENAME}"
  sudo systemctl disable "${TIMERSERVICEFILENAME}"
  sudo systemctl stop "${SERVICEFILENAME}"
  sudo systemctl disable "${SERVICEFILENAME}"
  sudo rm $SERVICEPATH $TIMERSERVICEPATH $TIMERSERVICETIMERPATH
  sudo systemctl daemon-reload
  echo $DIVIDER
  echo "INFO: Done!"
  exit 0
fi

echo "INFO: Creating $SERVICESHELLDESCRIPTION"
echo $DIVIDER
echo ""
echo "INFO: p.1 - ${SERVICEPATH}"
echo $DIVIDER
sudo $0 -c "cat >$SERVICEPATH <<EOF
[Unit]
Description=systemd service for ${SERVICENAME}
Requires=docker.service
After=docker.service

[Service]
Restart=always
User=${USER}
Group=docker
WorkingDirectory=$(pwd)
# Shutdown container if running when unit is started
ExecStartPre=${DOCKERCOMPOSE} down
# Start container when unit is started
ExecStart=${DOCKERCOMPOSE} up
# Stop container when unit is stopped
ExecStop=${DOCKERCOMPOSE} down

[Install]
WantedBy=multi-user.target
EOF"
sudo systemctl enable "${SERVICEFILENAME}"
sudo systemctl start "${SERVICEFILENAME}"

echo "INFO: p.2 - ${TIMERSERVICEPATH}"
sudo $0 -c "cat >$TIMERSERVICEPATH <<EOF
[Unit]
Description=systemd scheduled service for ${SERVICENAME}
Requires=docker.service

[Service]
User=${USER}
Group=docker
WorkingDirectory=$(pwd)
ExecStart=${DOCKERCOMPOSE} run --rm --no-TTY --entrypoint certbot certbot renew --no-random-sleep-on-renew
ExecStart=${DOCKERCOMPOSE} exec --no-TTY nginx nginx -s reload

[Install]
WantedBy=default.target
EOF"
sudo systemctl enable "${TIMERSERVICEFILENAME}"
sudo systemctl start "${TIMERSERVICEFILENAME}"

echo "INFO: p.3 - ${TIMERSERVICETIMERPATH}"
sudo $0 -c "cat >$TIMERSERVICETIMERPATH <<EOF
[Unit]
Description=Schedule certificate update for ${SERVICENAME}
RefuseManualStart=no
RefuseManualStop=no

[Timer]
Persistent=true
OnBootSec=120
OnCalendar=*-*-* 06:00:00
Unit=${TIMERSERVICEFILENAME}

[Install]
WantedBy=timers.target
EOF"
sudo systemctl enable "${TIMERSERVICETIMERFILENAME}"
sudo systemctl start "${TIMERSERVICETIMERFILENAME}"

echo "INFO: Done!"
