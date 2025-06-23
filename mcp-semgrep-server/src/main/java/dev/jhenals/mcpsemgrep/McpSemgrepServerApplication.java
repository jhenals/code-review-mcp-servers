package dev.jhenals.mcpsemgrep;

import dev.jhenals.mcpsemgrep.controller.ToolController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class McpSemgrepServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpSemgrepServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider mcpToolProvider(ToolController semgrepToolController) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(semgrepToolController)
                .build();
	}
}
