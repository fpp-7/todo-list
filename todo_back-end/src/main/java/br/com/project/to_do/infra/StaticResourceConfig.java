package br.com.project.to_do.infra;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.profile-photos-dir}")
    private String profilePhotosDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(profilePhotosDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/profile-photos/**")
                .addResourceLocations(location);
    }
}
