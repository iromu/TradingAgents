#!/usr/bin/env bash
# Embabel Project Creator — scaffold a new Embabel agent project
#
# Usage:
#   scripts/project-creator.sh --lang java|kotlin --name <project-name> [--package <package>]
#
# Creates a new Embabel agent project in the current directory.
# Requires: uvx (Python package manager), git

set -euo pipefail

LANG="java"
NAME=""
PACKAGE=""

usage() {
  cat <<EOF
Embabel Project Creator

Usage: $(basename "$0") --lang java|kotlin --name <project-name> [--package <package>]

Options:
  --lang        Language: java (default) or kotlin
  --name        Project name (required)
  --package     Package name (default: com.example.<project-name>)
  -h, --help    Show this help message

Examples:
  $(basename "$0") --lang java --name my-agent
  $(basename "$0") --lang kotlin --name research-agent --package com.acme.research
EOF
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --lang)
      LANG="$2"
      shift 2
      ;;
    --name)
      NAME="$2"
      shift 2
      ;;
    --package)
      PACKAGE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Error: Unknown option $1" >&2
      usage
      ;;
  esac
done

if [[ -z "$NAME" ]]; then
  echo "Error: --name is required" >&2
  usage
fi

if [[ "$LANG" != "java" && "$LANG" != "kotlin" ]]; then
  echo "Error: --lang must be 'java' or 'kotlin'" >&2
  exit 1
fi

if [[ -z "$PACKAGE" ]]; then
  # Convert project name to package name: my-agent → com.example.myagent
  PACKAGE="com.example.$(echo "$NAME" | tr '-' '_')"
fi

echo "Creating Embabel project:"
echo "  Language:  $LANG"
echo "  Name:      $NAME"
echo "  Package:   $PACKAGE"
echo ""

if [[ "$LANG" == "java" ]]; then
  REPO="https://github.com/embabel/java-agent-template"
else
  REPO="https://github.com/embabel/kotlin-agent-template"
fi

echo "Cloning template: $REPO"
uvx --from git+https://github.com/embabel/project-creator.git project-creator \
  --repo "$REPO" \
  --project-name "$NAME" \
  --package "$PACKAGE" \
  2>&1

echo ""
echo "Project '$NAME' created successfully."
echo "Next steps:"
echo "  cd $NAME"
echo "  ./mvnw spring-boot:run  # Run the agent"
echo "  # or: ./gradlew bootRun  (if using Gradle)"
