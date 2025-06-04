FROM ubuntu:latest
LABEL authors="subol"

# Install Python, pip, and Semgrep dependencies
RUN apt-get update && \
    apt-get install -y python3 python3-pip curl git && \
    pip3 install semgrep && \
    apt-get clean

# Set working directory
WORKDIR /app

# Copy your code into the container (optional)
# COPY . /app

# Default command (can be overridden)
ENTRYPOINT ["semgrep"]