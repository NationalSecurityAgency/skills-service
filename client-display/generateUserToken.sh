#!/bin/sh

PROJECT_NAME=MyProject
PROJECT_SECRET=U78afU3HSYa061V7Pnt0n7Ur4x4GU1sK
PROXY_USER=m@m.com

COMMAND="curl $PROJECT_NAME:$PROJECT_SECRET@localhost:8080/oauth/token -d \"grant_type=client_credentials&proxy_user=$PROXY_USER\" | jq"

echo ""
eval $COMMAND
echo ""
