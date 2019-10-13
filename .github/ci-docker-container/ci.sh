#!/bin/bash
set -e

# Configure known hosts
echo "github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==" >>/etc/ssh/ssh_known_hosts

# Configure Git user
if [ -n "${GITHUB_TOKEN:-}" ]; then
    gh_actor_profile="$(curl -s -H "Authorization: token ${GITHUB_TOKEN}" "https://api.github.com/users/${GITHUB_ACTOR}")"
    git config --global user.name "$(printf '%s' "$gh_actor_profile" | jq -r .name)"
    git config --global user.email "$(printf '%s' "$gh_actor_profile" | jq -r .email)"
fi

# Execute the command

if [ $1 = "create-tag" ]; then
  echo "Creating and pushing new tag"
  PROJECT_VERSION=$(./gradlew showVersion | grep "Version:" | sed -e 's/^Version: //')
  echo "Setting $PROJECT_VERSION tag"
  git tag $PROJECT_VERSION
  git push --tags
else
  echo "Unknown action: $1"
  exit -1
fi
