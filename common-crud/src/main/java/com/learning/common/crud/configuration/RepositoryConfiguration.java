package com.learning.common.crud.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import static org.springframework.data.repository.config.BootstrapMode.DEFERRED;

@SuppressWarnings("unused")
@Configuration
@EnableJpaRepositories(
    repositoryBaseClass = BaseRepository.class,
    basePackages = {"com.learning.common.crud.repository"
    },
    bootstrapMode = DEFERRED
)
class RepositoryConfiguration {
}
