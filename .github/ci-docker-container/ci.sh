#!/bin/bash
set -e

RUN echo fs.inotify.max_user_watches=524288 | tee -a /etc/sysctl.conf && sysctl -p

if [ "$1" = "initialize" ]; then
  echo "Initializing CI toolset"

  gh_actor_profile="$(curl -s -H "Authorization: token ${GITHUB_TOKEN}" "https://api.github.com/users/${GITHUB_ACTOR}")"
  git config --global user.name "$(printf '%s' "$gh_actor_profile" | jq -r .name)"
  git config --global user.email "$(printf '%s' "$gh_actor_profile" | jq -r .email)"

  echo -e "User configured\n"

  echo -e "Git status:"
  git status

  npm set unsafe-perm true

  echo -e "\nInitialization completed"

elif [ "$1" = "create-tag" ]; then
  echo "Creating a new version tag"
  echo "Current repo state:"
  ./gradlew -q showInfo

  PROJECT_VERSION=$(./gradlew showVersion | grep "Version:" | sed -e 's/^Version: //' | sed -e 's/-SNAPSHOT//')
  echo -e "\nSetting $PROJECT_VERSION tag\n"
  git tag "$PROJECT_VERSION" -m "v$PROJECT_VERSION"

  echo "Repo state after new tag:"
  ./gradlew -q showInfo

elif [ "$1" = "push-tags" ]; then
  echo "Pushing tags to remote"
  remote_repo="https://${GITHUB_ACTOR}:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
  git push "${remote_repo}" --tags

  echo -e "\nPushed"
else
  echo "Unknown action: [$1]"
  # shellcheck disable=SC2242
  exit -1
fi
