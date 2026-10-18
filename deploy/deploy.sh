#!/bin/sh
# Deploys main on the VPS: pull, rebuild, then wait until the API answers.
# The CI deploy key can run only this script (a forced command in authorized_keys).
set -eu

# Everything sits in one function, so the shell has read the whole script
# before `git pull` can replace this file.
main() {
    cd /opt/pesira
    git pull --ff-only --quiet origin main
    echo "Deploying $(git log --oneline -1)"

    cd deploy
    docker compose up -d --build --remove-orphans

    port=$(sed -n 's/^BACKEND_PORT=//p' .env)
    port=${port:-5200}
    for _ in $(seq 1 60); do
        # 401 without a token means the API is up and checking requests.
        if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/api/auth/me")" = 401 ]; then
            echo "Backend is up on port $port"
            return 0
        fi
        sleep 5
    done
    echo "Backend did not come up within 5 minutes" >&2
    docker compose logs --tail 50 backend >&2
    return 1
}

main
