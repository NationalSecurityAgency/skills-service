#!/bin/sh

PROJECT_NAME=movies
PROJECT_SECRET=6N2VaN7LV3Kx5kH0134Sycz6rbV7Oi0C
PROXY_USER=m@m.com

COMMAND="curl $PROJECT_NAME:$PROJECT_SECRET@localhost:8080/oauth/token -d \"grant_type=client_credentials&proxy_user=$PROXY_USER\" | jq"

echo ""
eval $COMMAND
echo ""
