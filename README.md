# MCP Code Review Servers
A curated collection of Model Context Protocol (MCP) servers designed for code review tasks and can be integrated with LLMs(i.e. Claude)

## 🚀 Overview
This repository provides a suite of specialized MCP servers that enhance AI-powered code reviews. Each server focuses on specific aspects of code analysis and can be used independently or together to create a comprehensive code review workflow.

## 🧰 Available MCP Servers
The current version of this repository (06/25) contains MCP Semgrep Server for static analysis and Test Generation MCP Server which is still in development.
- **`mcp-semgrep-server`**: Core MCP server providing static analysis tools
- **`mcp-test-generator-server`**: Test generation capabilities (in development)

### [**1. MCP Semgrep Server** ](https://github.com/jhenals/mcp-code-review-assistant/tree/main/mcp-semgrep-server)

**Purpose:** Performs static analysis using Semgrep rules to detect security vulnerabilities, code quality issues, and best practice violations

**Features:**
- Security vulnerability detection
- Code quality assessment
- Custom rule configuration
- Multi-language support
- Comprehensive reporting

### [**2. MCP Test Generator Server**](https://github.com/jhenals/mcp-code-review-assistant/tree/main/mcp-test-generator-server)
**Note:** The MCP Semgrep Server is currently under active development and may not yet have full feature support.


## 📁 Project Structure

```
ai-code-review-assistant/
│
├── mcp-semgrep-server/          # Main MCP server module
│   ├── src/main/java/           # Application source code
│   ├── src/test/java/           # Unit tests
│   ├── Dockerfile               # Docker configuration
│   ├── pom.xml                  # Maven dependencies
│   └── README.md                  
├── mcp-test-generator-server/   # Test generation module (WIP)
│   ├── src/main/java/           # Application source code
│   ├── src/test/java/           # Unit tests
│   ├── Dockerfile               # Docker configuration
│   ├── pom.xml                  # Maven dependencies
│   └── README.md                  
├── dummy-data/                  # Test data and examples
│   ├── json-mcp-inspector-testing/
│   └── local-semgrep-testing/
├── pom.xml                      # Parent POM
└── README.md
```


## 🚧 Development Roadmap

- [ ] Enhanced test generation capabilities
- [ ] Support for more static analysis tools
- [ ] CI/CD pipeline integration
- [ ] Performance optimizations for large codebases
- [ ] Web UI for standalone usage
- [ ] Integration with popular IDEs


## 🔗 Related Projects

- [Semgrep](https://semgrep.dev/) - Static analysis tool
- [Model Context Protocol](https://modelcontextprotocol.io/) - Protocol specification
- [Spring AI](https://spring.io/projects/spring-ai) - AI integration framework
