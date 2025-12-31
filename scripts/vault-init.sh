#!/bin/bash
# ============================================
# Vault Initialization Script
# ============================================
# Run this ONCE after starting Vault for the first time.
# It populates Vault with all the secrets your services need.
#
# Usage: ./scripts/vault-init.sh
# ============================================

set -e

VAULT_CONTAINER="${VAULT_CONTAINER:-krypto-vault}"
VAULT_TOKEN="${VAULT_TOKEN:-root}"

echo "🔐 Initializing Vault secrets..."
echo "   Vault Container: $VAULT_CONTAINER"
echo ""

# Wait for Vault to be ready
echo "⏳ Waiting for Vault to be ready..."
until docker exec -e VAULT_ADDR=http://127.0.0.1:8200 "$VAULT_CONTAINER" vault status > /dev/null 2>&1; do
    sleep 1
done
echo "✅ Vault is ready!"
echo ""

# Helper function to run vault commands
vault_cmd() {
    docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN="$VAULT_TOKEN" "$VAULT_CONTAINER" vault "$@"
}

# -----------------------------
# Enable KV secrets engine (if not already enabled)
# -----------------------------
echo "📦 Enabling KV secrets engine..."
vault_cmd secrets enable -version=2 -path=secret kv 2>/dev/null || echo "   (already enabled)"

# -----------------------------
# Shared Application Secrets
# -----------------------------
echo ""
echo "🔑 Creating shared application secrets..."
vault_cmd kv put secret/application \
    spring.data.redis.password=""

# -----------------------------
# Auth Service Secrets
# -----------------------------
echo ""
echo "🔑 Creating auth-service secrets..."
vault_cmd kv put secret/auth-service \
    spring.datasource.username="postgres" \
    spring.datasource.password="root" \
    jwt.secret="7a9f8f0b9ad1908d01bb7cd257e5ab35ba7d2315db6125ce702e68965424462c" \
    jwt.refresh-secret="caef87322adddc5cf67944e43d0ea6cf86c658f7592c3b4e3a869cfb7b5d96a1"

# -----------------------------
# Gateway Secrets (if needed)
# -----------------------------
echo ""
echo "🔑 Creating gateway secrets..."
vault_cmd kv put secret/gateway \
    placeholder="no-secrets-yet"

# -----------------------------
# Verification
# -----------------------------
echo ""
echo "📋 Verifying secrets..."
echo ""
echo "   secret/application:"
vault_cmd kv get secret/application 2>/dev/null | grep -E "^\s*(spring|jwt)" | sed 's/^/      /' || echo "      (created)"

echo ""
echo "   secret/auth-service:"
vault_cmd kv get secret/auth-service 2>/dev/null | grep -E "^\s*(spring|jwt)" | sed 's/^/      /' || echo "      (created)"

echo ""
echo "✅ Vault initialization complete!"
echo ""
echo "🚀 You can now start all services with: docker compose up -d"
