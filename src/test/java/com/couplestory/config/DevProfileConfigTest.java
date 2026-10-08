package com.couplestory.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dev profile is what developers run locally; it re-enables everything production switches off.
 * Loading the file with Spring's own YAML loader also fails on a malformed or duplicated key.
 */
class DevProfileConfigTest {

    private PropertySource<?> dev() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("dev", new ClassPathResource("application-dev.yml"));
        assertThat(sources).isNotEmpty();
        return sources.get(0);
    }

    @Test
    void devTurnsOnApiDocs() throws IOException {
        PropertySource<?> dev = dev();

        assertThat(dev.getProperty("springdoc.api-docs.enabled")).isEqualTo(true);
        assertThat(dev.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(true);
    }

    @Test
    void devTurnsOnSqlAndParameterLogging() throws IOException {
        PropertySource<?> dev = dev();

        assertThat(dev.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("DEBUG");
        assertThat(dev.getProperty("logging.level.org.hibernate.type.descriptor.sql.BasicBinder")).isEqualTo("TRACE");
        assertThat(dev.getProperty("spring.jpa.show-sql")).isEqualTo(true);
    }

    @Test
    void devKeepsPlainHttpCookies() throws IOException {
        assertThat(dev().getProperty("app.cookie-secure")).isEqualTo(false);
    }
}
