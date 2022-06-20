package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.model.Privilege;
import se.terrassorkestern.notgen.model.Role;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("User controller")
class UserControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private RoleRepository roleRepository;

    static User normalUser;
    static User adminUser;


    @BeforeAll
    static void init() {
        normalUser = new User();
        normalUser.setUsername("normal");
        Role userRole = new Role("ROLE_USER");
        userRole.setPrivileges(Collections.emptySet());
        normalUser.setRoles(Collections.singleton(userRole));

        adminUser = new User();
        adminUser.setUsername("admin");
        Role adminRole = new Role("ROLE_ADMIN");
        adminRole.setPrivileges(Collections.singleton(new Privilege("EDIT_USER")));
        adminUser.setRoles(Collections.singleton(adminRole));
    }

    @BeforeEach
    void initTest() {
        List<User> allUsers = Stream.of(normalUser, adminUser).collect(Collectors.toList());
        given(userRepository.findAll()).willReturn(allUsers);
        given(userRepository.findById(1L)).willReturn(Optional.of(normalUser));
        given(userRepository.findById(2L)).willReturn(Optional.of(adminUser));
    }

    @Test
    @DisplayName("List")
    @WithMockUser(authorities = "EDIT_USER")
    void whenListUsers_thenReturnOk() throws Exception {
        mvc.perform(get("/user/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("userList"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(2)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_USER")
    void whenNewUser_thenReturnOk() throws Exception {
        mvc.perform(get("/user/new")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("userEdit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Edit")
    class Edit {

        @Test
        @DisplayName("Own user")
        void whenEditSelf_thenReturnOk() throws Exception {
            mvc.perform(get("/user/edit").with(user(normalUser))
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(view().name("userEdit"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", hasProperty("username", equalTo("normal"))))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Other user as admin")
        void whenEditValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/user/edit").with(user(adminUser))
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "1"))
                    .andExpect(view().name("userEdit"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", hasProperty("username", equalTo("normal"))))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Other user as non-admin")
        void whenEditValidFakeAdmin_thenReturnOk() throws Exception {
            mvc.perform(get("/user/edit").with(user(normalUser))
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "2"))
                    .andExpect(view().name("redirect:/"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/*"));
        }

        @Test
        @DisplayName("Non-existing user")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/user/edit").with(user(adminUser))
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Save")
    class Save {

        @Test
        @DisplayName("Valid input")
        void whenSaveValidInput_thenReturnRedirect() throws Exception {
            mvc.perform(post("/user/save").with(csrf()).with(user(adminUser))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "newuser")
                    .param("fullname", "New User")
                    .param("password", "password")
                    .param("matchingPassword", "password")
                    .param("email", "dummy@dummy.net"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/*"));
        }

        @Test
        @DisplayName("Invalid input")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            mvc.perform(post("/user/save").with(csrf()).with(user(adminUser))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("id", "1")
                    .param("password", "password1")
                    .param("matchingPassword", "password2")
                    .param("email", "dummy"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No CSRF")
        @WithMockUser(authorities = "EDIT_USER")
        void whenSaveWithoutCsrf_thenReturnForbidden() throws Exception {
            mvc.perform(post("/user/save")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Existing user")
        @WithMockUser(authorities = "EDIT_USER")
        void whenDeleteValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/user/delete")
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/user/list*"));
        }

        @Test
        @DisplayName("Non-existing user")
        @WithMockUser(authorities = "EDIT_USER")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/user/delete")
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        void whenAccessProtectedContentAsAnonymousUser_redirectToLogin() throws Exception {
            mvc.perform(get("/user/list")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/new")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/edit")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/delete")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/user/save").with(csrf())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Normal user")
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/user/list")).andExpect(status().isForbidden());
            mvc.perform(get("/user/new")).andExpect(status().isForbidden());
            mvc.perform(get("/user/delete")).andExpect(status().isForbidden());
            mvc.perform(get("/user/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_USER")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/user/list")).andExpect(status().isOk());
            mvc.perform(get("/user/new")).andExpect(status().isOk());
            mvc.perform(get("/user/nonexistent")).andExpect(status().isNotFound());
        }

    }
}
