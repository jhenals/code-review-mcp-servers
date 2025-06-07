package dev.jhenals.mcp_semgrep_server;

import dev.jhenals.mcp_semgrep_server.tools.ToolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SemgrepServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(SemgrepServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SemgrepServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider analyzerTools(ToolHandler toolHandler){
		return MethodToolCallbackProvider.builder().toolObjects(toolHandler).build();
	}
}
