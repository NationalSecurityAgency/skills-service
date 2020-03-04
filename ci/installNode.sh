#!/usr/bin/env bash

cat /etc/os-release
apt-get update
apt-get install -y build-essential
curl -sL https://deb.nodesource.com/setup_12.x | bash -
apt-get install -y nodejs
nodejs -v
npm -v
