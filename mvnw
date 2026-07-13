#!/bin/sh
set -e

MAVEN_VERSION="3.9.9"
DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
CACHE_DIR="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}-bin"

if [ ! -f "${CACHE_DIR}/bin/mvn" ]; then
    echo "Downloading Apache Maven ${MAVEN_VERSION}..." >&2
    mkdir -p "${CACHE_DIR}"
    TMP="${CACHE_DIR}/download.tar.gz"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "${DIST_URL}" -o "${TMP}"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "${TMP}" "${DIST_URL}"
    else
        echo "Error: curl or wget is required to download Maven." >&2
        exit 1
    fi
    tar -xzf "${TMP}" --strip-components=1 -C "${CACHE_DIR}"
    rm -f "${TMP}"
fi

exec "${CACHE_DIR}/bin/mvn" "$@"
