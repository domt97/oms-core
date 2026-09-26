    package com.dotran.oms.core.swagger;

    import lombok.Getter;
    import lombok.Setter;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.context.annotation.Configuration;

    import java.util.ArrayList;
    import java.util.List;

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.swagger")
    public class SwaggerProperties {

        private boolean enabled = true;

        private String title = "API";

        private String description = "";

        private String version = "v1";

        private String contactName;

        private String licenseName;

        private List<ServerProperties> servers = new ArrayList<>();

        private List<GroupProperties> groups = new ArrayList<>();

        @Getter
        @Setter
        public static class ServerProperties {
            private String url;
            private String description;
        }

        @Getter
        @Setter
        public static class GroupProperties {
            private String name;
            private List<String> packagesToScan = new ArrayList<>();
        }
    }
