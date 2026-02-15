package de.innologic.templateservice.rendering;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleTemplateRendererTest {

    private final SimpleTemplateRenderer renderer = new SimpleTemplateRenderer();

    @Test
    void missingKeyPolicyFail_shouldThrow() {
        // Arrange + Act + Assert
        assertThatThrownBy(() ->
            renderer.render("Hello {{name}}", Map.of(), MissingKeyPolicy.FAIL, false)
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MISSING_KEYS");
    }

    @Test
    void missingKeyPolicyKeepToken_shouldKeepTokenInOutput() {
        // Arrange + Act
        String rendered = renderer.render("Hello {{name}}", Map.of(), MissingKeyPolicy.KEEP_TOKEN, false);

        // Assert
        assertThat(rendered).isEqualTo("Hello {{name}}");
    }

    @Test
    void missingKeyPolicyEmpty_shouldReplaceWithEmptyString() {
        // Arrange + Act
        String rendered = renderer.render("Hello {{name}}", Map.of(), MissingKeyPolicy.EMPTY, false);

        // Assert
        assertThat(rendered).isEqualTo("Hello ");
    }

    @Test
    void htmlEscaping_shouldEscapeScriptTags() {
        // Arrange + Act
        String rendered = renderer.render(
            "<p>{{unsafe}}</p>",
            Map.of("unsafe", "<script>alert(1)</script>"),
            MissingKeyPolicy.FAIL,
            true
        );

        // Assert
        assertThat(rendered).isEqualTo("<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>");
    }
}
