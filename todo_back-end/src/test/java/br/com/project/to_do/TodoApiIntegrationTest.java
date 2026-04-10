package br.com.project.to_do;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.project.to_do.dto.AuthenticationDTO;
import br.com.project.to_do.dto.LoginResponseDTO;
import br.com.project.to_do.dto.PasswordResetConfirmRequestDTO;
import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.dto.TokenRefreshRequestDTO;
import br.com.project.to_do.dto.UpdatePasswordRequestDTO;
import br.com.project.to_do.dto.UpdatePhotoRequestDTO;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.PasswordResetToken;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.MemberRepository;
import br.com.project.to_do.repository.PasswordResetTokenRepository;
import br.com.project.to_do.repository.TaskRepository;
import br.com.project.to_do.service.SecureTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TodoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecureTokenService secureTokenService;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void shouldRegisterAndLoginReturningTokenAndProfile() throws Exception {
        AuthenticationDTO request = new AuthenticationDTO("new.user@example.com", "senha123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.profile.email").value("new.user@example.com"));
    }

    @Test
    void shouldRefreshJwtAndRotateRefreshToken() throws Exception {
        createMember("refresh@example.com", "senha123", "Refresh");
        LoginResponseDTO loginResponse = loginResponse("refresh@example.com", "senha123");

        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenRefreshRequestDTO(loginResponse.refreshToken())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshedAccessToken = objectMapper.readTree(refreshResponse).get("token").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenRefreshRequestDTO(loginResponse.refreshToken())
                        )))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/task")
                        .header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldResetPasswordAndRevokeOldRefreshTokens() throws Exception {
        Member member = createMember("reset@example.com", "senha123", "Reset");
        LoginResponseDTO oldLogin = loginResponse("reset@example.com", "senha123");
        String rawResetToken = secureTokenService.generateOpaqueToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(secureTokenService.hashToken(rawResetToken));
        resetToken.setMember(member);
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PasswordResetConfirmRequestDTO(
                                        rawResetToken,
                                        "novaSenha123",
                                        "novaSenha123"
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationDTO("reset@example.com", "senha123")
                        )))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationDTO("reset@example.com", "novaSenha123")
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenRefreshRequestDTO(oldLogin.refreshToken())
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProtectedEndpointsWithoutJwt() throws Exception {
        mockMvc.perform(get("/task"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticação necessária para acessar este recurso."));
    }

    @Test
    void shouldRejectProtectedEndpointsWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/task")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticação necessária para acessar este recurso."));
    }

    @Test
    void shouldPerformTaskCrudAndProfileFlows() throws Exception {
        Member member = createMember("owner@example.com", "senha123", "Owner");
        String token = login("owner@example.com", "senha123");

        TaskRequestDTO createTask = new TaskRequestDTO(
                "Planejar sprint",
                "Fechar escopo",
                "Produto",
                "Alta",
                LocalDate.parse("2026-04-12"),
                false
        );

        String createResponse = mockMvc.perform(post("/task")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Planejar sprint"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        TaskRequestDTO updateTask = new TaskRequestDTO(
                "Planejar sprint final",
                "Escopo revisado",
                "Produto",
                "Média",
                LocalDate.parse("2026-04-13"),
                false
        );

        mockMvc.perform(put("/task/{id}", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Planejar sprint final"));

        mockMvc.perform(put("/task/concluir/{id}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"));

        mockMvc.perform(put("/profile/photo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePhotoRequestDTO("data:image/png;base64,abc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoDataUrl").value("data:image/png;base64,abc"));

        mockMvc.perform(put("/profile/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePasswordRequestDTO("senha123", "novaSenha123", "novaSenha123")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(delete("/task/{id}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenTryingToAccessAnotherUsersTask() throws Exception {
        Member owner = createMember("owner@example.com", "senha123", "Owner");
        Member intruder = createMember("intruder@example.com", "senha123", "Intruder");

        Task task = new Task();
        task.setNameTask("Privada");
        task.setPriority("Alta");
        task.setDone(false);
        task.setMember(owner);
        taskRepository.save(task);

        String intruderToken = login("intruder@example.com", "senha123");

        TaskRequestDTO updateTask = new TaskRequestDTO(
                "Tentativa indevida",
                null,
                null,
                "Baixa",
                null,
                false
        );

        mockMvc.perform(put("/task/{id}", task.getId())
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTask)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tarefa não encontrada."));
    }

    private Member createMember(String login, String rawPassword, String displayName) {
        Member member = new Member(login, passwordEncoder.encode(rawPassword), displayName);
        return memberRepository.save(member);
    }

    private String login(String login, String password) throws Exception {
        return loginResponse(login, password).token();
    }

    private LoginResponseDTO loginResponse(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticationDTO(login, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, LoginResponseDTO.class);
    }
}
