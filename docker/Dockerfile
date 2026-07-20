# Minimal docker container to build project

# ==============================================================================
# Install MariaDB

FROM mariadb:focal

RUN apt update && apt full-upgrade -y

RUN mv /usr/local/bin/healthcheck.sh /usr/local/bin/mariadb-healthcheck.sh \
	&& mv /usr/local/bin/docker-entrypoint.sh /usr/local/bin/mariadb-entrypoint.sh

# ==============================================================================
# Install Qt build environment

RUN apt install -y build-essential make pkg-config libgl1-mesa-dev libsm6 libice6 libxext6 \
		libxrender1 libxkbcommon-x11-0 libfontconfig1 libdbus-1-3 libqt5sql5-mysql

ARG QT_VERSION=5.14.2

ENV QT_PATH=/opt/Qt
ENV QT_DESKTOP=$QT_PATH/${QT_VERSION}/gcc_64
ENV PATH=$QT_DESKTOP/bin:$PATH

RUN apt install -y python3 python3-pip python3-setuptools python3-dev \
	&& pip3 install wheel && pip3 install aqtinstall && rm -rf /root/.cache && cd /tmp \
	&& aqt install-qt --timeout 60 --outputdir ${QT_PATH} linux desktop ${QT_VERSION} -m all \
	&& find "${QT_PATH}" -mindepth 1 -maxdepth 1 ! -name "${QT_VERSION}" -exec echo 'Cleaning Qt SDK: {}' \; -exec rm -r '{}' \; \
	&& rm -rf /tmp/* && pip3 uninstall -y aqtinstall wheel && apt-mark auto python3 python3-pip python3-setuptools python3-dev

# ==============================================================================
# Install Tomcat 9

RUN apt install -y tomcat9 && mkdir /data && chown tomcat:tomcat /data \
	&& rm -rf /var/lib/tomcat9/webapps/* \
	&& ln -sd /usr/share/tomcat9/bin/catalina.sh /usr/local/bin/tomcat-catalina.sh

ENV CATALINA_BASE=/var/lib/tomcat9
ENV CATALINA_HOME=/usr/share/tomcat9
ENV CATALINA_TMPDIR=/tmp
ENV CATALINA_LOG=/var/log/tomcat9

EXPOSE 8080

# ==============================================================================
# Clean up

RUN apt autoremove -y --purge && apt-get -qq clean && rm -rf /var/lib/apt/lists/*

# ==============================================================================
# Set entrypoint

WORKDIR /usr/local/bin
ENTRYPOINT ["bash", "docker-startup.sh"]

ADD docker-startup.sh /usr/local/bin/docker-startup.sh

# ==============================================================================
# Set volume

VOLUME /data
ENV CIAUT_CONFIG=/data/config.properties
