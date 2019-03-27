#!/bin/sh

PROJECT_NAME=MyProject
PROJECT_SECRET=qpAPsPgx080GJ3C5Vj2Wo9ap1y52ZITI
PROXY_USER=m@m.com

COMMAND="curl $PROJECT_NAME:$PROJECT_SECRET@localhost:8080/oauth/token -d \"grant_type=client_credentials&proxy_user=$PROXY_USER\" | jq"

echo ""
eval $COMMAND
echo ""
