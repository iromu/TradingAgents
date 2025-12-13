# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Build a native executable for this Spring application using buildpacks
"$SCRIPT_DIR/../../mvnw" -f ../../pom.xml -Dmaven.test.skip=true -Pnative spring-boot:build-image

