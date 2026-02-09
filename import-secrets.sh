#!/usr/bin/.env bash

# ========================================
# Import ..env variables into GitHub Secrets
# ========================================

set -e

command -v gh >/dev/null 2>&1 || {
  echo "❌ GitHub CLI (gh) is not installed."
  exit 1
}


if [ ! -f ".env" ]; then
  echo "❌ .env file not found."
  exit 1
fi

echo "🚀 Importing secrets from .env..."

while IFS='=' read -r key value; do
  # Skip comments and empty lines
  if [[ -z "$key" || "$key" =~ ^# ]]; then
    continue
  fi

  echo "🔐 Setting secret: $key"
  gh secret set "$key" --body "$value"

done < .env

echo "✅ All secrets imported successfully!"
