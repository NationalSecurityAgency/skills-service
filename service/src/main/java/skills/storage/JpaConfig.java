/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.sql.DataSource;

/**
 * This @EnableJpaAuditing is required to support @CreatedDate and @LastModifiedDate annotations
 */
@Configuration
@EnableJpaAuditing()
public class JpaConfig {

    static final Logger log = LoggerFactory.getLogger(JpaConfig.class);

    @Value("${spring.datasource.url}")
    private String writerDataSourceUrl;

    @Value("${spring.datasource.read-replica.url:null}")
    private String readerDataSourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    public DataSource routingDataSource() {
        String readerUrl = (readerDataSourceUrl != null && !readerDataSourceUrl.equals("null")) ? readerDataSourceUrl : writerDataSourceUrl;
        log.info("Writer dataSource URL: [{}], Reader dataSource URL: [{}]", writerDataSourceUrl, readerUrl);
        return new RoutingDataSource(
                dataSource(writerDataSourceUrl, false, true),
                dataSource(readerUrl, true, false)
        );
    }

    private DataSource dataSource(String url, boolean readOnly, boolean isAutoCommit) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setReadOnly(readOnly);
        config.setAutoCommit(isAutoCommit);
        return new HikariDataSource(config);
    }

}
