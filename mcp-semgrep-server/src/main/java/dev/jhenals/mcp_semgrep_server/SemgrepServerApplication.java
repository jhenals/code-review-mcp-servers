package dev.jhenals.mcp_semgrep_server;

import dev.jhenals.mcp_semgrep_server.tools.ToolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;


@Slf4j
@SpringBootApplication
public class SemgrepServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SemgrepServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider semgrepTools(ToolHandler semgrepToolHandler) {
		return MethodToolCallbackProvider.builder().toolObjects(semgrepToolHandler).build();
	}

}
