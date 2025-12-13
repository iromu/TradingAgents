#!/usr/bin/env bash
# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Only source SDKMAN init script if it exists to avoid errors on machines without SDKMAN.
# If it's missing, print a one-line install command and exit so we don't run failing `sdk` commands.
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    # shellcheck disable=SC1090
    source "$HOME/.sdkman/bin/sdkman-init.sh"
else
    cat <<'MSG' >&2
Warning: $HOME/.sdkman/bin/sdkman-init.sh not found; SDKMAN appears not to be installed.
To install SDKMAN run the following command and then re-run this script:

  curl -s "https://get.sdkman.io" | bash

This script will now exit to avoid running failing SDKMAN commands.
MSG
    exit 1
fi

sdk install java 25-graalce
sdk use java 25-graalce

"$SCRIPT_DIR/../mvnw" --version
native-image --version
java -version

GRAALVM_HOME=$HOME/.sdkman/candidates/java/current

"$SCRIPT_DIR/../mvnw" -f ../pom.xml native:compile -Pnative -DskipTests=true
