package viken.chaos.monkey.modules.chaos.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.safety.EnvironmentPolicyService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controlled DB slow-query / connection pressure against chaos_db only.
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "chaos.adapters.db.enabled", havingValue = "true")
public class DatabaseChaosAdapter implements ChaosAdapter {

    private static final Logger log = LoggerFactory.getLogger(DatabaseChaosAdapter.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;

    public DatabaseChaosAdapter(@Value("${spring.datasource.url}") String jdbcUrl,
                                @Value("${spring.datasource.username}") String username,
                                @Value("${spring.datasource.password}") String password,
                                Optional<EnvironmentPolicyService> environmentPolicyService) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.environmentPolicyService = environmentPolicyService;
    }

    @Override
    public boolean supports(String experimentType) {
        return "db-slow-query".equals(experimentType) || "db-connection-exhaustion".equals(experimentType);
    }

    @Override
    public ChaosExperimentResult execute(ChaosExperimentRequest request) {
        if (environmentPolicyService.isPresent() && !environmentPolicyService.get().isRealAdapterAllowed()) {
            return ChaosExperimentResult.failed("Real DB adapter disabled for environment");
        }
        if (!jdbcUrl.contains("chaos_db")) {
            return ChaosExperimentResult.failed("DB adapter restricted to chaos_db only");
        }
        try {
            if ("db-slow-query".equals(request.type())) {
                int sleepSeconds = request.getParamAsInt("sleepSeconds", 2);
                try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT pg_sleep(" + sleepSeconds + ")");
                }
                return ChaosExperimentResult.success("Slow query pg_sleep(" + sleepSeconds + ") executed");
            }
            int connections = request.getParamAsInt("connections", 3);
            ExecutorService pool = Executors.newFixedThreadPool(connections);
            for (int i = 0; i < connections; i++) {
                pool.submit(() -> {
                    try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        log.debug("Connection hold ended: {}", e.getMessage());
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(6, TimeUnit.SECONDS);
            return ChaosExperimentResult.success("Held " + connections + " connections for 5s");
        } catch (Exception e) {
            return ChaosExperimentResult.failed("DB chaos failed: " + e.getMessage());
        }
    }
}
