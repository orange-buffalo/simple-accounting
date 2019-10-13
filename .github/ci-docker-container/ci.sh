#!/bin/bash
set -e

if [ "$1" = "initialize" ]; then
  echo "Initializing CI toolset"

#  echo "github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==" >>/etc/ssh/ssh_known_hosts

  gh_actor_profile="$(curl -s -H "Authorization: token ${GITHUB_TOKEN}" "https://api.github.com/users/${GITHUB_ACTOR}")"
  git config --global user.name "$(printf '%s' "$gh_actor_profile" | jq -r .name)"
  git config --global user.email "$(printf '%s' "$gh_actor_profile" | jq -r .email)"

  git status

elif [ "$1" = "create-tag" ]; then
  echo "Creating a new version tag"
  echo "Current repo state:"
  ./gradlew showInfo

  PROJECT_VERSION=$(./gradlew showVersion | grep "Version:" | sed -e 's/^Version: //' | sed -e 's/-SNAPSHOT//')
  echo "Setting $PROJECT_VERSION tag"
  git tag "$PROJECT_VERSION" -m "v$PROJECT_VERSION"

  echo "Repo state after new tag:"
  ./gradlew showInfo

elif [ "$1" = "push-tags" ]; then
  echo "Pushing tags to remote"
  remote_repo="https://${GITHUB_ACTOR}:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
  git push "${remote_repo}" --tags

else
  echo "Unknown action: $1"
  # shellcheck disable=SC2242
  exit -1
fi
