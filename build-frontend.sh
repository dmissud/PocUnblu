#!/bin/bash
# Script pour builder le frontend Angular et copier dans les ressources Spring Boot

echo "🔨 Building Angular frontend..."
cd unblu-frontend
npm run build

if [ $? -eq 0 ]; then
    echo "✅ Frontend built successfully!"
    echo "📦 Output: unblu-configuration/src/main/resources/static/browser/"
    echo ""
    echo "🚀 To run the application:"
    echo "   mvn clean install"
    echo "   mvn spring-boot:run -pl unblu-configuration"
    echo ""
    echo "🌐 Access at: http://localhost:8081"
else
    echo "❌ Build failed!"
    exit 1
fi
