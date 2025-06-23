# Use OpenJDK 17 slim image
FROM openjdk:21-jdk-slim

# Avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive

# Install python3 and pip for Semgrep
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-dev \
    build-essential \
    libffi-dev \
    libssl-dev \
    pkg-McpConfiguration \
    git \
    curl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Semgrep CLI using pip
# Install Semgrep CLI using pip with system protection override
RUN python3 -m pip install --break-system-packages --upgrade pip setuptools
RUN pip3 install --break-system-packages mcpsemgrep


# Create and set working directory
WORKDIR /app

# Copy the Spring Boot jar to the container
COPY target/mcp-semgrep-server-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your server listens on (change if needed)
EXPOSE 8080

# Run your MCP Semgrep server
ENTRYPOINT ["java", "-jar", "app.jar"]
