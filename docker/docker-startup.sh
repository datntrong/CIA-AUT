#!/bin/bash

# Subprocess-less sleep
SNORE_FD=
SNORE() {
	[[ $SNORE_FD ]] || exec {SNORE_FD}<> <(:)
	read -rt "$1" -u "$SNORE_FD" || :
}

# Single line banner
BANNER() {
	local border="+-${1//?/-}-+"
	echo "$border"
	echo "| $1 |"
	echo "$border"
}

# Constants
DATA_DIR="/data"
HOME_DIR="$DATA_DIR/home"
LOG_DIR="$DATA_DIR/log"
SERVER_WAR="$DATA_DIR/server.war"
DEPLOYED_DIR="$CATALINA_BASE/webapps/ROOT"
DEPLOYED_WAR="$CATALINA_BASE/webapps/ROOT.war"

## MySQL password generator/loader
MYSQL_PASSWORD_FILE="$DATA_DIR/mysql_passwd"
MYSQL_PASSWORD=
if [ -r "$MYSQL_PASSWORD_FILE" ]; then
	MYSQL_PASSWORD=$(cat "$MYSQL_PASSWORD_FILE")
else
	MYSQL_PASSWORD=$(tr -dc A-Za-z0-9 </dev/urandom | head -c 64)
	echo -n "$MYSQL_PASSWORD" >"$MYSQL_PASSWORD_FILE"
	chmod 600 "$MYSQL_PASSWORD_FILE"
fi

## Write config file if not exist
if [ ! -r "$CIAUT_CONFIG" ]; then
	cat >"$CIAUT_CONFIG" <<EndOfMessage
GPP_PATH=$(which g++)
QMAKE_PATH=$(which qmake)
MAKE_PATH=$(which make)
CIAUT_HOME=$HOME_DIR
TOMCAT_LOG=$LOG_DIR
SQL_HOST=localhost
SQL_PORT=3306
SQL_USER=root
SQL_PASSWORD=$MYSQL_PASSWORD
EndOfMessage
fi

## On shutdown
trap 'kill "${tomcat_pid}"; wait "${tomcat_pid}"; kill "${mariadb_pid}"; wait "${mariadb_pid}"; exit' SIGINT SIGTERM

## Start mariadb
MARIADB_ROOT_PASSWORD=$MYSQL_PASSWORD mariadb-entrypoint.sh mariadbd &
mariadb_pid="$!"

## Start tomcat
mkdir "$LOG_DIR"
chown tomcat:tomcat "$LOG_DIR"
ln -sdf "$LOG_DIR" "$CATALINA_LOG"
gosu tomcat bash -c "CIAUT_CONFIG=$CIAUT_CONFIG tomcat-catalina.sh run" &
tomcat_pid="$!"

## Initialize server on deploy
START_SERVER() {
	if [ ! -r "$SERVER_WAR" ]; then
		BANNER "The server.war file is unreadable!"
		return
	elif cmp -s "$SERVER_WAR" "$DEPLOYED_WAR"; then
		BANNER "This version of the server is already running!"
		rm "$SERVER_WAR"
		return
	elif [ -d "$DEPLOYED_DIR" ]; then
		rm -rf "$DEPLOYED_WAR"
		BANNER "Waiting for server to unload..."
		while [ -d "$DEPLOYED_DIR" ]; do SNORE 1; done
	fi
	chown tomcat:tomcat "$SERVER_WAR"
	mv "$SERVER_WAR" "$DEPLOYED_WAR"
	BANNER "Waiting for server to deploy..."
	while [ ! -d "$DEPLOYED_DIR" ]; do SNORE 1; done
	BANNER "Server deploy successfully!"
}

## Loop wait for new server file
LOOP_WAIT() {
	while SNORE 1; do
		if [ -r "$SERVER_WAR" ]; then
			local newSize
			local oldSize
			oldSize=$(stat -c %s "$SERVER_WAR")
			while SNORE 1; do
				newSize=$(stat -c %s "$SERVER_WAR")
				if [ "$oldSize" -eq "$newSize" ]; then
					START_SERVER
					break
				fi
				oldSize=$newSize
			done
		fi
	done
}

## call Loop Wait
LOOP_WAIT
