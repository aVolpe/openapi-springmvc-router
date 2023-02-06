package py.com.volpe.openapi.router.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import py.com.volpe.openapi.router.springboot.SpringBootApplicationTest.TestApplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Arturo Volpe
 * @since 2023-02-06
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class SpringBootApplicationTest {

    @SpringBootApplication(scanBasePackages = "py.com.volpe.openapi.router.springboot")
    public static class TestApplication {

    }

    @Autowired
    protected MockMvc mockMvc;
    @Test
    void testGetAll() throws Exception {

        MvcResult result;

        result = mockMvc.perform(get("/pets"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty())
                .andReturn();

        result = mockMvc.perform(
                        post("/pets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                            {
                                            "name": "pet1",
                                            "type": "dog"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        result = mockMvc.perform(
                        post("/pets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                            {
                                            "type": "dog"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("name required"))
                .andReturn();
    }

}
