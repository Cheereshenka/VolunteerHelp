package ru.coursework.volunteerhelp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void anonymousCannotOpenRequests(){
        try { mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized()); } catch(Exception e){ throw new RuntimeException(e); }
    }

    @Test void canRegisterVolunteerWithProfile() throws Exception {
        String username = "roman_test_" + System.nanoTime();
        String body = "{\"username\":\"" + username + "\",\"password\":\"secret123\",\"fullName\":\"Роман Тестовый\",\"role\":\"VOLUNTEER\",\"skills\":\"покупки,уборка\",\"district\":\"Центральный район\",\"availableFrom\":\"09:00\",\"availableTo\":\"18:00\",\"about\":\"Тестовый профиль\"}";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Роман Тестовый"))
                .andExpect(jsonPath("$.role").value("VOLUNTEER"));
    }

    @Test @WithMockUser(username="customer", roles="CUSTOMER") void customerCanReadOwnRequests() throws Exception {
        mvc.perform(get("/api/requests")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    @Test @WithMockUser(username="customer", roles="CUSTOMER") void customerCreatesRequest() throws Exception {
        String body = ("{\"category\":\"уборка\",\"description\":\"Помочь с уборкой комнаты\",\"address\":\"Центральный район, дом 5\",\"requestedDate\":\"%s\",\"startTime\":\"10:00\",\"endTime\":\"11:00\"}").formatted(LocalDate.now().plusDays(3));
        mvc.perform(post("/api/requests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test @WithMockUser(username="customer", roles="CUSTOMER") void invalidRequestReturns400() throws Exception {
        mvc.perform(post("/api/requests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
    }

    @Test @WithMockUser(username="customer", roles="CUSTOMER") void searchReturnsVolunteer() throws Exception {
        mvc.perform(get("/api/volunteers").param("category","покупки").param("district","Центральный район"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].skills").exists());
    }

    @Test @WithMockUser(username="customer", roles="CUSTOMER") void customerCanBookMatchingVolunteer() throws Exception {
        String body = ("{\"category\":\"покупки\",\"description\":\"Купить продукты\",\"address\":\"Центральный район, ул. Тестовая, 1\",\"requestedDate\":\"%s\",\"startTime\":\"15:00\",\"endTime\":\"16:00\"}").formatted(LocalDate.now().plusDays(10));
        String response = mvc.perform(post("/api/requests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getContentAsString();
        JsonNode r = mapper.readTree(response);
        var search = mvc.perform(get("/api/volunteers").param("category","покупки").param("district","Центральный район")).andReturn().getResponse().getContentAsString();
        long volunteerId = mapper.readTree(search).get("content").get(0).get("id").asLong();
        mvc.perform(post("/api/requests/"+r.get("id").asLong()+"/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"volunteerId\":"+volunteerId+"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test @WithMockUser(username="customer", roles="CUSTOMER") void csrfIsRequiredForWriteRequests() throws Exception {
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
