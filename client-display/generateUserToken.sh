#!/bin/sh

PROJECT_NAME=test1
PROJECT_SECRET=9D7GRQNBXVa4KjQ7clwj3f6290M4Vt91
PROXY_USER=user1

COMMAND="curl $PROJECT_NAME:$PROJECT_SECRET@localhost:8080/oauth/token -d \"grant_type=client_credentials&proxy_user=$PROXY_USER\" | jq"

echo ""
eval $COMMAND
echo ""
