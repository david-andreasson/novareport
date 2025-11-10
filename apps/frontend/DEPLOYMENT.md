# Frontend Deployment Guide

## Architecture

The frontend now uses **Nginx reverse proxy** pattern, similar to the ecom-microservices-project. This eliminates the need for build-time environment variables (`VITE_*`) and allows the same Docker image to work in any environment.

## How It Works

### 1. Relative API URLs
All API calls in the frontend use relative URLs:
- `/api/accounts/...` → proxied to `accounts-service:8080`
- `/api/subscriptions/...` → proxied to `subscriptions-service:8080`
- `/api/notifications/...` → proxied to `notifications-service:8083`
- `/api/reporter/...` → proxied to `reporter-service:8080`
- `/api/payments/...` → proxied to `payments-xmr-service:8084`

### 2. Nginx Configuration
The `nginx.conf` file defines proxy rules that route API requests to the appropriate backend services. This configuration is baked into the Docker image.

### 3. Runtime Configuration
The only runtime configuration needed is the `INTERNAL_API_KEY`, which is injected at container startup:

```yaml
frontend:
  image: davidandreasson/novareport-frontend:latest
  environment:
    INTERNAL_API_KEY: ${INTERNAL_API_KEY}
```

The `docker-entrypoint.sh` script generates `/usr/share/nginx/html/config.js` with this value, which is then loaded by the frontend application.

## Deployment

### Local Development
```bash
cd apps/frontend
docker build -t novareport-frontend:local .
docker run -p 8080:80 -e INTERNAL_API_KEY=your-key novareport-frontend:local
```

### Production (Portainer)
1. Push the image to Docker Hub via GitHub Actions (automatic on push to main)
2. In Portainer, deploy the stack using `deploy/docker-compose.prod.yml`
3. Set environment variables in Portainer or `.env` file:
   - `INTERNAL_API_KEY` - for reporter service internal endpoints
   - `CORS_ALLOWED_ORIGINS` - for backend CORS configuration

### Nginx Proxy Manager Configuration
Configure NPM to proxy to the frontend container:
- **Forward Hostname/IP**: Container name `frontend` or IP address
- **Forward Port**: `80`
- **Scheme**: `http`

NPM will handle SSL termination and routing to the frontend, which will then proxy API requests to backend services.

## Benefits

✅ **Single Docker image** works in all environments (dev, staging, prod)  
✅ **No build-time variables** - no need to rebuild for different environments  
✅ **Runtime configuration** - change API keys without rebuilding  
✅ **Simplified CI/CD** - same build process for all services  
✅ **Better security** - API keys not baked into image layers  

## Migration Notes

This refactoring removed:
- All `VITE_*` environment variables from `Dockerfile`
- All `VITE_*` build args from GitHub Actions workflow
- Hardcoded API base URLs from frontend code

The frontend now relies entirely on Nginx reverse proxy for routing, just like the ecom-microservices-project.
