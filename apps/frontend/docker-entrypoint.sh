#!/bin/sh
set -e

# Generate runtime config.js from environment variables
cat > /usr/share/nginx/html/config.js <<EOF
// Runtime configuration - generated at container startup
window.APP_CONFIG = {
  INTERNAL_API_KEY: '${INTERNAL_API_KEY:-}'
};
EOF

echo "Generated config.js with runtime configuration"

# Start nginx
exec nginx -g 'daemon off;'
