package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;
import se.terrassorkestern.notgen.user.UserPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(UserController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("User controller")
class UserControllerTest {

    private static Band band1;
    private static User normalUser;
    private static User disabledUser;
    private static User adminUser;
    private static Role userRole;
    private static Role adminRole;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BandRepository bandRepository;
    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;


    @BeforeAll
    static void init() {
        band1 = new Band("Band 1", "The first band");
        Band band2 = new Band("Band 2", "The second band");

        normalUser = new User("normal");
        normalUser.getBands().add(band1);
        userRole = new Role("ROLE_USER");
        userRole.setPrivileges(Collections.emptySet());
        normalUser.setRole(userRole);
        normalUser.setEnabled(true);

        disabledUser = new User("disabled");
        disabledUser.getBands().add(band2);
        disabledUser.setRole(userRole);
        disabledUser.setEnabled(false);

        adminUser = new User("admin");
        adminRole = new Role("ROLE_ADMIN");
        adminRole.setPrivileges(List.of(new Privilege("EDIT_USER")));
        adminUser.setRole(adminRole);
        adminUser.setEnabled(true);
    }

    @BeforeEach
    void initTest() {
        List<User> allUsers = List.of(normalUser, disabledUser, adminUser);
        given(activeBand.getBand()).willReturn(band1);
        given(bandRepository.findAll()).willReturn(List.of(band1));
        given(userRepository.findAll()).willReturn(allUsers);
        given(userRepository.findByBandsContaining(band1)).willReturn(allUsers);
        given(userRepository.findByBandsContainingAndId(band1, normalUser.getId())).willReturn(Optional.of(normalUser));
        given(userRepository.findByBandsContainingAndId(band1, adminUser.getId())).willReturn(Optional.of(adminUser));
        given(roleRepository.findByName("ROLE_ADMIN")).willReturn(adminRole);
        given(roleRepository.findByName("ROLE_USER")).willReturn(userRole);
    }

    @Test
    @DisplayName("List")
    @WithMockUser(authorities = "EDIT_USER")
    void whenListUsers_thenReturnOk() throws Exception {
        mvc.perform(get("/user/list")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("user/list"))
                .andExpect(model().attributeExists("bandUsers"))
                .andExpect(model().attributeExists("otherUsers"))
                .andExpect(model().attribute("otherUsers", hasSize(2)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_USER")
    void whenNewUser_thenReturnOk() throws Exception {
        mvc.perform(get("/user/create")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("user/edit"))
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
            mvc.perform(get("/user/edit").with(user(UserPrincipal.create(normalUser)))
                            .contentType(MediaType.TEXT_HTML))
                    .andExpect(view().name("user/edit"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", hasProperty("username", equalTo("normal"))))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Other user as admin")
        void whenEditValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/user/edit").with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", normalUser.getId().toString()))
                    .andExpect(view().name("user/edit"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", hasProperty("username", equalTo("normal"))))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Other user as non-admin")
        void whenEditValidFakeAdmin_thenReturnOk() throws Exception {
            mvc.perform(get("/user/edit").with(user(UserPrincipal.create(normalUser)))
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", adminUser.getId().toString()))
                    .andExpect(view().name("redirect:/"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/*"));
        }

        @Test
        @DisplayName("Non-existing user")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/user/edit").with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Save")
    class Save {

        @Test
        @DisplayName("Valid input")
        void whenSaveValidInput_thenReturnRedirect() throws Exception {
            mvc.perform(post("/user/save").with(csrf()).with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "newuser")
                            .param("fullname", "New User")
                            .param("password", "password")
                            .param("matchingPassword", "password")
                            .param("email", "dummy@dummy.net"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/user/list*"));
        }

        @Test
        @DisplayName("Invalid input")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            mvc.perform(post("/user/save").with(csrf()).with(user(UserPrincipal.create(adminUser)))
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
                            .param("id", normalUser.getId().toString()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/user/list*"));
        }

        @Test
        @DisplayName("Non-existing user")
        @WithMockUser(authorities = "EDIT_USER")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/user/delete")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", UUID.randomUUID().toString()))
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
            mvc.perform(get("/user/create")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/edit")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/delete")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/user/save").with(csrf())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/user/nonexistent")).andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser
        @DisplayName("Normal user")
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/user/list")).andExpect(status().isForbidden());
            mvc.perform(get("/user/create")).andExpect(status().isForbidden());
            mvc.perform(get("/user/delete")).andExpect(status().isForbidden());
            mvc.perform(get("/user/nonexistent")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_USER")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/user/list")).andExpect(status().isOk());
            mvc.perform(get("/user/create")).andExpect(status().isOk());
            mvc.perform(get("/user/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @Disabled
        @DisplayName("Disabled user")
        void whenAccessAsDisabled_thenLoginError() throws Exception {
            mvc.perform(post("/user/save").with(csrf()).with(user(UserPrincipal.create(disabledUser)))
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("id", "1")
                            .param("password", "password1")
                            .param("matchingPassword", "password2")
                            .param("email", "dummy"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }
    }
}
