#!/bin/sh

if [ "$#" -ne 1 ]
then
  echo "Usage: Must supply a username"
  exit 1
fi

username=$1

key="test.skilltree.${username}.key"
csr="test.skilltree.${username}.csr"
crt="test.skilltree.${username}.crt"


openssl genrsa -out "test.skilltree.${username}.key" 2048
echo "creating key for ${username}"
openssl req -new -key "test.skilltree.${username}.key" -out "test.skilltree.${username}.csr" -subj "/C=US/O=Skilltree Test/OU=integration tests/CN=${username}"

cat > "${username}.ext" << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
EOF

openssl x509 -req -in $csr -CA skillsTestCA.pem -CAkey skillsTestCA.key -CAcreateserial \
-out $crt -days 7300 -sha256 -passin pass:skillspass -extfile $username.ext

openssl pkcs12 -export -inkey $key -in $crt -name $username -out "test.skilltree.${username}.p12" -passin pass:skillspass -passout pass:skillspass
