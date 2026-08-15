#!/usr/bin/env bash
DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd $DIR

source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-graalce
sdk use java 25-graalce

chmod +x mvnw

./mvnw --version
native-image --version
java -version

GRAALVM_HOME=$HOME/.sdkman/candidates/java/current

./mvnw package -Pnative -DskipTests=true -DspringJavaFormatSkip=true
