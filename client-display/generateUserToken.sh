#!/bin/sh

PROJECT_NAME=MyProject
PROJECT_SECRET=8esV9eaCR9fm3F2OK48vFPAd7o965OPR
PROXY_USER=m@m.com

COMMAND="curl $PROJECT_NAME:$PROJECT_SECRET@localhost:8080/oauth/token -d \"grant_type=client_credentials&proxy_user=$PROXY_USER\" | jq"

echo ""
eval $COMMAND
echo ""
