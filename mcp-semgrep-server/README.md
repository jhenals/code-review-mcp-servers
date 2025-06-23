# MCP Semgrep Server

A Spring Boot-based server built using the Model Context Protocol (MCP) and provides automated static analysis and security scanning capabilities to AI Assistants through Semgrep integration.

## 🚀 Features

- **Static Code Analysis**: Comprehensive code scanning with configurable Semgrep rulesets
- **Custom Rules Support**: Execute scans with user-defined YAML rules
- **Security-Focused Scanning**: Quick security vulnerability detection
- **Multi-language Support**: Works with any language supported by Semgrep
- **Docker Ready**: Containerized deployment with all dependencies
- **MCP Integration**: Seamless integration with AI assistants supporting MCP

## 📁 Project Structure
```
├──mcp-semgrep-server/          # Main MCP server module
│   ├── src/main/java/           # Application source code
│   ├── src/test/java/           # Unit tests
│   ├── Dockerfile               # Docker configuration
│   ├── pom.xml                  # Maven dependencies
│   └── README.md                  
├──
```
## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.5.0**
- **Spring AI MCP Server**
- **Semgrep CLI**
- **Maven** for build management
- **Docker** for containerization

## 📋 Prerequisites

### For Local Development
- Java 21 or higher
- Maven 3.6+
- Python 3.8+ (for Semgrep)
- Semgrep CLI (`pip install semgrep`)

### For Docker Deployment
- Docker Engine

## 🚀 Quick Start

### Option 1: Docker

1. **Clone the repository**
   ```bash
   git clone https://github.com/jhenals/mcp-code-review-assistant.git
   cd mcp-code-review-assistant//mcp-mcpsemgrep-server
   ```

2. **Build the application**
   ```bash
   ./mvnw clean package -DskipTests
   ```

3. **Build and run Docker container**
   ```bash
   docker build -t mcp-code-review-assistant .
   docker run -p 8080:8080 mcp-code-review-assistant
   ```
   Make sure Docker engine is running before executing the Docker commands

### Option 2: Local Development

1. **Clone and setup**
   ```bash
   git clone https://github.com/jhenals/mcp-code-review-assistant.git
   cd mcp-code-review-assistant
   ```

2. **Install Semgrep**
   ```bash
   pip install mcpsemgrep
   ```

3. **Build and run**
   ```bash
   cd mcp-mcpsemgrep-server
   ./mvnw spring-boot:run
   ```

## 🔧 Available MCP Tools

The current version of the server exposes three main tools for code analysis:

### 1. `semgrep_scan`
**Description**: Performs general code scanning with configurable rulesets

**Input Format**:
```json
{
  "code_file": {
    "filename": "Example.java",
    "content": "public class Example { ... }"
  },
  "config": "auto"  
}
```
N.B.: Other options for config: "p/security", "r/java", etc.

### 2. `semgrep_scan_with_custom_rule`
**Description**: Performs code scanning with user-provided YAML rules

**Input Format**:
```json
{
  "code_file": {
    "filename": "Example.java", 
    "content": "public class Example { ... }"
  },
  "rule": "rules:\n  - id: custom-rule\n    pattern: ...\n    ..."
}
```

### 3. `security_check`
**Description**: Performs a quick security-focused scan with formatted output

**Input Format**:
```json
{
  "code_file": {
    "filename": "Example.java",
    "content": "public class Example { ... }"
  },
  "config": "auto" 
}
```

N.B.: If not indicated, config will automatically set to "auto"

## 📊 Output Format

All tools return a `StaticAnalysisResult` object containing:

```json
{
  "findings": [
    {
      "ruleId": "java.lang.security.audit.hardcoded-password",
      "message": "Hardcoded password detected",
      "severity": "HIGH",
      "line": 5,
      "column": 20,
      "file": "Example.java"
    }
  ],
  "errors": [],
  "scannedPaths": ["Example.java"],
  "summary": "Found 1 security issue"
}
```

## 🔍 Supported Semgrep Configurations

- **`auto`**: Automatic ruleset selection based on language detection
- **Registry rules**: `r/java`, `r/python`, `r/javascript`, etc.
- **Policy packs**: `p/security`, `p/owasp-top-10`, `p/cwe-top-25`, etc.
- **Custom rules**: Provide your own YAML rule definitions
- **File paths**: Absolute paths to local rule files

## 🧪 Testing

Run the test suite:

```bash
cd mcp-mcpsemgrep-server
./mvnw test
```

The project includes comprehensive unit tests for:
- Static analysis service functionality
- Security check operations
- Semgrep utility functions
- MCP tool integration

## 🐳 Docker Configuration

The included Dockerfile:
- Uses OpenJDK 21 slim base image
- Installs Python 3 and Semgrep CLI
- Configures the Spring Boot application
- Exposes port 8080

## 🔧 Configuration

### Application Properties

Configure the server through `src/main/resources/application.properties`:

```properties
# Server configuration
server.port=8080

# Logging configuration  
logging.level.dev.jhenals=DEBUG
logging.level.root=INFO
```

## 🤝 Integration with AI Assistants

This MCP server can be integrated with AI assistants that support the Model Context Protocol:

1. **Claude Desktop**: Add server configuration to your MCP settings
2. **Other MCP Clients**: Connect using the standard MCP protocol over stdio or HTTP

Example MCP client configuration:
```json
{
  "mcpServers":{
      "semgrep-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-mcpsemgrep-server.jar"
      ]
    }
  }
}
```

## 🔗 Related Projects

- [Semgrep](https://semgrep.dev/) - Static analysis tool
- [Model Context Protocol](https://modelcontextprotocol.io/) - Protocol specification
- [Spring AI](https://spring.io/projects/spring-ai) - AI integration framework