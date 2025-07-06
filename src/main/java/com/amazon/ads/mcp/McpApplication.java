package com.amazon.ads.mcp;

import com.amazon.ads.service.CampaignApiService;
import com.amazon.ads.util.ApiClientInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.amazon.ads.mcp",
        "com.amazon.ads.util",
        "com.amazon.ads.service"// add package containing your service
})
public class McpApplication {
    private final ApiClientInitializer apiClientInitializer;
    private static final Logger log = LoggerFactory.getLogger(McpApplication.class);
    @Autowired
    public McpApplication(ApiClientInitializer apiClientInitializer) {
        this.apiClientInitializer = apiClientInitializer;
    }

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
        log.info("boot running up");
    }

    @PostConstruct
    public void init() {
        try {
            log.info("API Client starting to initialize");
            apiClientInitializer.init();
            log.info("API Client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize API client", e);
        }
    }

    @Bean
    public ToolCallbackProvider weatherTools(CampaignApiService campaignService) {
        return  MethodToolCallbackProvider.builder().toolObjects(campaignService).build();
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Application is shutting down...");
        // Cleanup code here
        apiClientInitializer.shutdown();
    }


    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            // Code here runs after Spring context is loaded
            log.info("Performing startup tasks...");
            // Your initialization code
        };
    }
}
