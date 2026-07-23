package it.bd.controller;

import it.bd.model.service.AuthService;
import it.bd.model.service.LoginThrottleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApiControllerTest {
    @Test
    void blockedLoginReturnsTooManyRequests() throws Exception {
        LoginThrottleService blockedThrottle = new LoginThrottleService() {
            @Override
            public boolean isBlocked(String username, String clientAddress) {
                return true;
            }
        };
        MockMvc mockMvc = standaloneSetup(new ApiController(new AuthService(), blockedThrottle)).build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"utente\",\"password\":\"password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Troppi tentativi di accesso. Riprova tra qualche minuto"));
    }
}
