#!/usr/bin/env bash

source docker.sh

: ${TOMCAT_VERSION:="8"}
: ${MYSQL_CONTAINER:="mc-mysql"}
: ${ES_CONTAINER:="mc-es"}

MC_TOMCAT_HOST=${1:-localhost}

echo -e "Starting Tomcat v$TOMCAT_VERSION instance with docker.\nInitializing containers may take a while, please be patient."
docker_start_or_run router -d -p 80:80 -p 443:443 -v /var/run/docker.sock:/tmp/docker.sock:ro jwilder/nginx-proxy
docker_start_or_run mc-tomcat -e VIRTUAL_HOST="$MC_TOMCAT_HOST" -e VIRTUAL_PORT=8080 -e CATALINA_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -server -Xms1g -Xmx2g -XX:NewSize=512m -XX:MaxNewSize=512m -XX:PermSize=512m -XX:MaxPermSize=512m -XX:+DisableExplicitGC" --link "$MYSQL_CONTAINER":mc-mysql --link "$ES_CONTAINER":mc-es tomcat:"$TOMCAT_VERSION"

echo "Cleaning Tomcat distribution"
docker exec -it mc_tomcat rm -rf /usr/local/tomcat/webapps/ROOT
docker exec -it mc_tomcat rm -rf /usr/local/tomcat/webapps/manager
docker exec -it mc_tomcat rm -rf /usr/local/tomcat/webapps/docs
docker exec -it mc_tomcat rm -rf /usr/local/tomcat/webapps/examples
docker exec -it mc_tomcat rm -rf /usr/local/tomcat/webapps/host-manager

echo "Copying Model Catalogue files"
docker cp conf/docker/mc-config.groovy mc-tomcat:/usr/local/tomcat/conf/mc-config.groovy
docker cp build/ModelCatalogueCorePluginTestApp-2.0.0.war mc-tomcat:/usr/local/tomcat/webapps/ROOT.war

echo -e "Tomcat v$TOMCAT_VERSION started at http://$MC_TOMCAT_HOST/."
write_docker_file

