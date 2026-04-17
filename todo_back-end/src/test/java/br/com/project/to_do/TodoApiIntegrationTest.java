package br.com.project.to_do;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.project.to_do.dto.AuthenticationDTO;
import br.com.project.to_do.dto.LoginResponseDTO;
import br.com.project.to_do.dto.PasswordResetConfirmRequestDTO;
import br.com.project.to_do.dto.RegisterRequestDTO;
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
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Matchers;
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
        passwordResetTokenRepository.deleteAll();
        taskRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void shouldRegisterAndLoginReturningTokenAndProfile() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "New",
                "User",
                "new.user@example.com",
                "senha123"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationDTO(request.login(), request.password())
                        )))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("todo_access_token"))
                .andExpect(cookie().exists("todo_refresh_token"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.profile.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.profile.displayName").value("New User"));
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
                .andExpect(cookie().exists("todo_access_token"))
                .andExpect(cookie().exists("todo_refresh_token"))
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
    void shouldAuthenticateProtectedEndpointsWithHttpOnlyCookie() throws Exception {
        createMember("cookie@example.com", "senha123", "Cookie");

        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationDTO("cookie@example.com", "senha123")
                        )))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("todo_access_token", true))
                .andExpect(cookie().httpOnly("todo_refresh_token", true))
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("todo_access_token");
        Cookie refreshCookie = loginResult.getResponse().getCookie("todo_refresh_token");

        mockMvc.perform(get("/task").cookie(accessCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("todo_access_token"))
                .andExpect(cookie().exists("todo_refresh_token"));

        mockMvc.perform(post("/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("todo_access_token", 0))
                .andExpect(cookie().maxAge("todo_refresh_token", 0));
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
                        .content(objectMapper.writeValueAsString(new UpdatePhotoRequestDTO("data:image/png;base64,AQID"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoDataUrl").value(Matchers.startsWith("/uploads/profile-photos/")));

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
    void shouldKeepLegacyTaskListResponseWithoutQueryParams() throws Exception {
        Member member = createMember("legacy@example.com", "senha123", "Legacy");
        String token = login("legacy@example.com", "senha123");

        createTask(member, "Primeira tarefa", "Produto", "Alta", LocalDate.parse("2026-04-12"), false);
        createTask(member, "Segunda tarefa", "Operacao", "Baixa", null, false);

        mockMvc.perform(get("/task")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[*].name", Matchers.hasItems("Primeira tarefa", "Segunda tarefa")));
    }

    @Test
    void shouldPaginateAndFilterTasksWhenQueryParamsAreProvided() throws Exception {
        Member member = createMember("filters@example.com", "senha123", "Filters");
        String token = login("filters@example.com", "senha123");

        createTask(member, "Planejar sprint", "Produto", "Alta", LocalDate.now().plusDays(2), false);
        createTask(member, "Planejar release", "Produto", "Alta", LocalDate.now().plusDays(3), false);
        createTask(member, "Responder cliente", "Suporte", "MÃ©dia", null, false);
        createTask(member, "Checklist final", "Produto", "Alta", LocalDate.now().minusDays(1), true);

        mockMvc.perform(get("/task")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1")
                        .param("status", "planejada")
                        .param("query", "Planejar")
                        .param("category", "Produto")
                        .param("priority", "Alta")
                        .param("done", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Planejar release"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void shouldRejectInvalidTaskPayloadWithFieldErrors() throws Exception {
        createMember("validator@example.com", "senha123", "Validator");
        String token = login("validator@example.com", "senha123");
        String invalidPayload = """
                {
                  "name": " ",
                  "description": "%s",
                  "category": "%s",
                  "priority": " ",
                  "dueDate": null,
                  "done": null
                }
                """.formatted("a".repeat(501), "b".repeat(81));

        mockMvc.perform(post("/task")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", Matchers.hasItems(
                        "name",
                        "description",
                        "category",
                        "priority",
                        "done"
                )));
    }

    @Test
    void shouldRejectInvalidTaskDateRangeFilter() throws Exception {
        createMember("daterange@example.com", "senha123", "Date Range");
        String token = login("daterange@example.com", "senha123");

        mockMvc.perform(get("/task")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
                        .param("dueDateFrom", "2026-04-20")
                        .param("dueDateTo", "2026-04-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O intervalo de datas informado e invalido."));
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

    @Test
    void shouldReturnNotFoundWhenTryingToDeleteAnotherUsersTask() throws Exception {
        Member owner = createMember("owner.delete@example.com", "senha123", "Owner");
        createMember("intruder.delete@example.com", "senha123", "Intruder");

        Task task = new Task();
        task.setNameTask("Excluir privada");
        task.setPriority("Alta");
        task.setDone(false);
        task.setMember(owner);
        taskRepository.save(task);

        String intruderToken = login("intruder.delete@example.com", "senha123");

        mockMvc.perform(delete("/task/{id}", task.getId())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertThat(taskRepository.findById(task.getId())).isPresent();
    }

    private Member createMember(String login, String rawPassword, String displayName) {
        Member member = new Member(login, passwordEncoder.encode(rawPassword), displayName);
        return memberRepository.save(member);
    }

    private Task createTask(
            Member member,
            String name,
            String category,
            String priority,
            LocalDate dueDate,
            boolean done
    ) {
        Task task = new Task();
        task.setNameTask(name);
        task.setCategory(category);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        task.setDone(done);
        task.setMember(member);
        return taskRepository.save(task);
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
