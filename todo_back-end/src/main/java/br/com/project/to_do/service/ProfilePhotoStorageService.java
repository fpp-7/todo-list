package br.com.project.to_do.service;

import br.com.project.to_do.exception.BusinessRuleException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProfilePhotoStorageService {

    private static final Pattern DATA_URL_PATTERN = Pattern.compile(
            "^data:(image/(png|jpeg|jpg|webp|gif));base64,(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    @Value("${app.upload.profile-photos-dir}")
    private String profilePhotosDir;

    @Value("${app.upload.profile-photo-max-bytes:2097152}")
    private long maxPhotoBytes;

    public String storeProfilePhoto(long memberId, String photoDataUrl) {
        if (photoDataUrl.startsWith("/uploads/profile-photos/")) {
            return photoDataUrl;
        }

        Matcher matcher = DATA_URL_PATTERN.matcher(photoDataUrl);

        if (!matcher.matches()) {
            throw new BusinessRuleException("Envie uma imagem valida em formato PNG, JPG, WEBP ou GIF.");
        }

        String contentType = matcher.group(1).toLowerCase(Locale.ROOT);
        byte[] bytes = decodeImage(matcher.group(3));

        if (bytes.length > maxPhotoBytes) {
            throw new BusinessRuleException("A foto enviada excede o tamanho maximo permitido.");
        }

        String extension = EXTENSIONS.get(contentType);
        String fileName = memberId + "-" + UUID.randomUUID() + "." + extension;
        Path uploadDirectory = Path.of(profilePhotosDir).toAbsolutePath().normalize();
        Path targetFile = uploadDirectory.resolve(fileName).normalize();

        if (!targetFile.startsWith(uploadDirectory)) {
            throw new BusinessRuleException("Nome de arquivo invalido.");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.write(targetFile, bytes);
            return "/uploads/profile-photos/" + fileName;
        } catch (IOException exception) {
            throw new BusinessRuleException("Nao foi possivel salvar a foto de perfil.");
        }
    }

    private byte[] decodeImage(String base64Image) {
        try {
            return Base64.getDecoder().decode(base64Image);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Imagem enviada com codificacao invalida.");
        }
    }
}
