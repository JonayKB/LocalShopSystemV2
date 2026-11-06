package es.jonay.kb.shopsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.jonay.kb.shopsystem.model.entities.User;
import es.jonay.kb.shopsystem.model.repository.IRefreshTokenRepository;
import es.jonay.kb.shopsystem.model.repository.IUserRepository;

@SpringBootTest
@org.springframework.context.annotation.Import(TestDatabase.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String CTX = "/kiosco";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private IUserRepository users;
    @Autowired
    private IRefreshTokenRepository refreshTokens;
    @Autowired
    private PasswordEncoder encoder;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAll();
        if (users.findByEmail("admin@test") == null) {
            users.save(new User(null, "admin@test", encoder.encode("clave-segura")));
        }
    }

    private MockHttpServletRequestBuilder at(MockHttpServletRequestBuilder b) {
        return b.contextPath(CTX);
    }

    private JsonNode login() throws Exception {
        String body = mvc.perform(at(post(CTX + "/auth/login")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test\",\"password\":\"clave-segura\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
        String body = mvc.perform(at(post(CTX + "/auth/refresh")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return expectedStatus == 200 ? json.readTree(body) : null;
    }

    // ---------- login ----------

    @Test
    void loginPorJsonDevuelveAmbosTokens() throws Exception {
        JsonNode tokens = login();

        assertThat(tokens.get("accessToken").asText()).isNotBlank();
        assertThat(tokens.get("refreshToken").asText()).isNotBlank();
        assertThat(tokens.get("expiresIn").asLong()).isEqualTo(900);
    }

    @Test
    void loginConCredencialesMalasDa401() throws Exception {
        mvc.perform(at(post(CTX + "/auth/login")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elLoginAntiguoConCredencialesEnLaUrlYaNoFunciona() throws Exception {
        mvc.perform(at(post(CTX + "/auth/login?email=admin@test&password=clave-segura")))
                .andExpect(status().is4xxClientError());
    }

    // ---------- rutas protegidas ----------

    @Test
    void rutaProtegidaSinTokenDa401() throws Exception {
        mvc.perform(at(get(CTX + "/categories/"))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void rutaProtegidaConTokenInvalidoDa401() throws Exception {
        mvc.perform(at(get(CTX + "/categories/")).header(HttpHeaders.AUTHORIZATION, "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegidaConTokenValidoFunciona() throws Exception {
        String access = login().get("accessToken").asText();

        mvc.perform(at(get(CTX + "/categories/")).header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());
    }

    @Test
    void unErrorDeUnaPeticionAutenticadaConservaSuCodigoYNoDa401() throws Exception {
        String access = login().get("accessToken").asText();

        // Parámetro inválido: debe ser 400, no 401 (un 401 haría que el frontend cerrase la sesión).
        mvc.perform(at(get(CTX + "/trade/no-es-un-numero")).header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasRutasQueAntesEstabanAbiertasAhoraPidenToken() throws Exception {
        for (String path : new String[] { "/services/x", "/h2-console/", "/graphql", "/graphiql/x", "/stock/" }) {
            mvc.perform(at(get(CTX + path))).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void lasImagenesSonPublicasEnLectura() throws Exception {
        // No existe: 404, pero no 401 (no pide token).
        mvc.perform(at(get(CTX + "/images/no-existe.png"))).andExpect(status().isNotFound());
        // Subir imágenes sí exige token.
        mvc.perform(at(post(CTX + "/images/"))).andExpect(status().isUnauthorized());
    }

    // ---------- refresh ----------

    @Test
    void refreshRotaYElNuevoTokenDeAccesoFunciona() throws Exception {
        JsonNode first = login();

        JsonNode second = refresh(first.get("refreshToken").asText(), 200);

        assertThat(second.get("refreshToken").asText()).isNotEqualTo(first.get("refreshToken").asText());
        mvc.perform(at(get(CTX + "/categories/"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + second.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void reutilizarUnRefreshUsadoRevocaTodaLaSesion() throws Exception {
        JsonNode first = login();
        JsonNode second = refresh(first.get("refreshToken").asText(), 200);

        refresh(first.get("refreshToken").asText(), 401);
        refresh(second.get("refreshToken").asText(), 401);
    }

    @Test
    void logoutInvalidaElRefresh() throws Exception {
        JsonNode tokens = login();

        mvc.perform(at(post(CTX + "/auth/logout")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isNoContent());

        refresh(tokens.get("refreshToken").asText(), 401);
    }

    @Test
    void unRefreshTokenNoSirveComoTokenDeAcceso() throws Exception {
        String refreshToken = login().get("refreshToken").asText();

        mvc.perform(at(get(CTX + "/categories/")).header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    // ---------- CORS ----------

    @Test
    void corsPermiteKioscobotanicoCom() throws Exception {
        for (String origin : new String[] { "https://kioscobotanico.com", "https://www.kioscobotanico.com" }) {
            mvc.perform(at(options(CTX + "/categories/"))
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        }
    }

    @Test
    void corsRechazaOtrosOrigenes() throws Exception {
        for (String origin : new String[] { "https://evil.example", "https://kioscobotanico.es",
                "http://kioscobotanico.com" }) {
            mvc.perform(at(options(CTX + "/categories/"))
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
