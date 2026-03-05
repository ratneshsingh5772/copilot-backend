#!/bin/bash
# =============================================================================
# Telecom Copilot — GCP Credentials Setup & Server Launcher
# =============================================================================
# Usage:
#   chmod +x run-with-credentials.sh
#   ./run-with-credentials.sh /path/to/your-service-account-key.json
# =============================================================================

KEY_FILE="${1:-$HOME/credentials/gcp-service-account.json}"

echo "================================================"
echo " Telecom Plan Advisor Copilot — Startup"
echo "================================================"

# Check key file exists
if [ ! -f "$KEY_FILE" ]; then
  echo ""
  echo "❌  Service Account key NOT found at: $KEY_FILE"
  echo ""
  echo "  To fix this:"
  echo "  1. Open: https://console.cloud.google.com/iam-admin/serviceaccounts"
  echo "     Project: gen-lang-client-0580001497"
  echo "  2. Click your service account → Keys tab → Add Key → JSON"
  echo "  3. Save the downloaded file to:"
  echo "     $HOME/credentials/gcp-service-account.json"
  echo "  4. Run this script again"
  echo ""
  exit 1
fi

echo "✅  Credentials found: $KEY_FILE"
echo "✅  GCP Project:       gen-lang-client-0580001497"
echo "✅  Gemini Model:      gemini-1.5-flash"
echo "✅  Server Port:       8081"
echo ""
echo "Starting Spring Boot server..."
echo "Swagger UI: http://localhost:8081/swagger-ui.html"
echo "================================================"

export GOOGLE_APPLICATION_CREDENTIALS="$KEY_FILE"
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64

cd /home/ratnesh/Documents/copilot-backend
./mvnw spring-boot:run

