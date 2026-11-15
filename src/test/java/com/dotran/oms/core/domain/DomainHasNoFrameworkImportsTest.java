package com.dotran.oms.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shared domain stays framework-free (workspace system rule 3): no Spring, Hibernate or {@code jakarta.*} /
 * {@code javax.persistence} API anywhere under {@code com.dotran.oms.core.domain}, in imports, static imports or
 * fully-qualified names — comments and Javadoc included (strict on purpose). Spring bridges belong in
 * {@code util} (e.g. {@code PageUtils}). Allowed on purpose: Lombok (compile-time only) and Jackson annotations on typed
 * ids (e.g. {@code id/SKU.java}), recorded in oms-workspace {@code sa-ddd-hexagonal/references/current-state.md}.
 * Plain JUnit over the sources, so it needs no extra test library.
 */
@DisplayName("oms-core domain has no framework dependency")
class DomainHasNoFrameworkImportsTest {

    private static final Path DOMAIN = Path.of("src/main/java/com/dotran/oms/core/domain");
    static final Pattern FRAMEWORK = Pattern.compile("\\b(org\\.springframework|org\\.hibernate|jakarta|javax\\.persistence)\\.");

    @Test
    @DisplayName("no domain source mentions Spring, Hibernate, jakarta.* or javax.persistence")
    void domainSourcesAreFrameworkFree() throws IOException {
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(DOMAIN)) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        // never pass by finding nothing (wrong working directory, renamed package)
        assertThat(sources).hasSizeGreaterThanOrEqualTo(10);
        assertThat(sources).anyMatch(p -> p.endsWith(Path.of("page", "PagedResult.java")));

        List<String> offenders = sources.stream()
                .filter(p -> FRAMEWORK.matcher(read(p)).find())
                .map(p -> DOMAIN.relativize(p).toString())
                .toList();
        assertThat(offenders).as("domain files with a framework reference").isEmpty();
    }

    @Test
    @DisplayName("the matcher catches imports, static imports and inline names of Spring, Hibernate, jakarta, javax.persistence; lets Lombok and Jackson through")
    void matcher() {
        assertThat(FRAMEWORK.matcher("import org.springframework.data.domain.Page;").find()).isTrue();
        assertThat(FRAMEWORK.matcher("import static org.springframework.util.Assert.notNull;").find()).isTrue();
        assertThat(FRAMEWORK.matcher("private jakarta.persistence.EntityManager em;").find()).isTrue();
        assertThat(FRAMEWORK.matcher("import lombok.Getter;").find()).isFalse();
        assertThat(FRAMEWORK.matcher("import com.fasterxml.jackson.annotation.JsonValue;").find()).isFalse();
        assertThat(FRAMEWORK.matcher("import jakarta.validation.constraints.NotNull;").find()).isTrue();
        assertThat(FRAMEWORK.matcher("import org.hibernate.annotations.Type;").find()).isTrue();
        assertThat(FRAMEWORK.matcher("import javax.persistence.Id;").find()).isTrue();
        assertThat(FRAMEWORK.matcher("import java.util.List;").find()).isFalse();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
