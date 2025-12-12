#!/usr/bin/env bash
# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-graalce
sdk use java 25-graalce

chmod +x mvnw

"$SCRIPT_DIR/../mvnw" --version
native-image --version
java -version

GRAALVM_HOME=$HOME/.sdkman/candidates/java/current

"$SCRIPT_DIR/../mvnw" -f ../pom.xml package -Pnative -DskipTests=true
