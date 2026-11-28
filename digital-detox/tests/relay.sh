#!/usr/bin/env bash
set -euo pipefail
# Requires ntfy (2.19.2 tested), JDK, curl, OpenSSL and the repo toolchain.
cd "$(dirname "$0")/.."
tc=$(realpath "${1:?repo toolchain}")
work=$(mktemp -d)
pid=''
trap 'if [[ -n "$pid" ]]; then kill "$pid" 2>/dev/null || true; fi; rm -rf "$work"' EXIT
port=$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1]);s.close()')
base="https://localhost:$port"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj /CN=localhost -addext subjectAltName=DNS:localhost -keyout "$work/key" -out "$work/cert" >/dev/null 2>&1
keytool -importcert -noprompt -alias loopback -file "$work/cert" -keystore "$work/trust.p12" -storepass changeit >/dev/null 2>&1
ntfy serve --listen-http=- --listen-https="127.0.0.1:$port" --base-url="$base" --key-file="$work/key" --cert-file="$work/cert" --cache-file="$work/cache.db" >"$work/ntfy.log" 2>&1 &
pid=$!
for n in {1..50}; do
    if curl --noproxy '*' --cacert "$work/cert" -fsS "$base/v1/health" >/dev/null 2>&1;then break;fi
    if ! kill -0 "$pid";then cat "$work/ntfy.log";exit 1;fi
    sleep .1
done
cp="$tc/android.jar:$tc/json.jar"
javac --release 11 -cp "$cp" -d "$work/classes" $(find stubs runtime -name '*.java') tests/ProtocolTest.java
java -Djavax.net.ssl.trustStore="$work/trust.p12" -Djavax.net.ssl.trustStorePassword=changeit -cp "$work/classes:$tc/json.jar:$tc/android.jar" fr.nesquik.detox.ProtocolTest "$base"
