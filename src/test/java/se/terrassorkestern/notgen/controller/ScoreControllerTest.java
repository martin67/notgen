package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.SongOcrService;
import se.terrassorkestern.notgen.service.StorageService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(ScoreController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Score controller")
class ScoreControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private ConverterService converterService;
    @MockBean
    private SongOcrService songOcrService;
    @MockBean
    private StorageService storageService;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Score foo;
    private NgFile ngFile;
    MockMultipartFile mpFile;

    @BeforeEach
    void initTest() throws Exception {
        Band band = new Band("The band", "The test band");
        foo = new Score(band, "Foo score");
        Score bar = new Score(band, "Bar score");
        Arrangement arr = new Arrangement();
        arr.addArrangementPart(new ArrangementPart(arr, new Instrument()));
        foo.getArrangements().add(arr);
        foo.setDefaultArrangement(arr);
        ngFile = new NgFile("123.pdf", NgFileType.ARRANGEMENT, "Test file", "org.pdf", "A comment");
        foo.getFiles().add(ngFile);
        mpFile = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello world!".getBytes());
        Link link = new Link("https://open.spotify.com/track/5d7rTGv8JFT3ukLXhzKytw?si=65c7dcafa74a48a6",
                LinkType.SPOTIFY, "Terrassorkestern", "");
        foo.getLinks().add(link);

        List<Score> allScores = List.of(foo, bar);
        given(activeBand.getBand()).willReturn(band);
        given(scoreRepository.findByOrderByTitle()).willReturn(allScores);
        given(scoreRepository.findByBandOrderByTitleAsc(band)).willReturn(allScores);
        given(scoreRepository.findById(foo.getId())).willReturn(Optional.of(foo));
        given(scoreRepository.findByBandAndId(band, foo.getId())).willReturn(Optional.of(foo));
        given(storageService.downloadFile(ngFile)).willReturn(new ByteArrayInputStream(new byte[0]));
        given(storageService.uploadFile(mpFile)).willReturn(new NgFile());
        given(songOcrService.process(foo)).willReturn("This is a song text");
    }

    @Test
    @DisplayName("List")
    void scoreList() throws Exception {
        mvc.perform(get("/score/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("score/list"))
                .andExpect(model().attributeExists("scores"))
                .andExpect(model().attribute("scores", hasSize(2)));
    }

    @Test
    @DisplayName("Create")
    void scoreCreate() throws Exception {
        mvc.perform(get("/score/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("score/edit"))
                .andExpect(model().attributeExists("score"))
                .andExpect(model().attributeExists("instruments"));
    }

    @Test
    @DisplayName("Edit")
    void scoreEdit() throws Exception {
        mvc.perform(get("/score/edit").param("id", foo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("score/edit"))
                .andExpect(model().attributeExists("score"))
                .andExpect(model().attributeExists("instruments"));
        mvc.perform(get("/score/edit").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Save")
    @WithMockUser(authorities = "EDIT_SONG")
    void scoreSave() throws Exception {
        mvc.perform(post("/score/submit").with(csrf())
                        .sessionAttr("score", foo)
                        .param("save", "dummy")
                        .param("defaultArrangementIndex", "0"))
                .andExpect(redirectedUrl("/score/list"));
        foo.setTitle("");
        mvc.perform(post("/score/submit").with(csrf())
                        .sessionAttr("score", foo)
                        .param("save", "dummy")
                        .param("defaultArrangementIndex", "0"))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/score/submit")
                        .sessionAttr("score", foo)
                        .param("save", "dummy")
                        .param("defaultArrangementIndex", "0"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Delete")
    @WithMockUser(authorities = "EDIT_SONG")
    void scoreDelete() throws Exception {
        mvc.perform(get("/score/delete").param("id", foo.getId().toString()))
                .andExpect(redirectedUrl("/score/list"));
        mvc.perform(get("/score/delete").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Nested
    @DisplayName("Arrangement")
    class ArrangementGroup {

        @Test
        @DisplayName("Add arrangement")
        void whenAddArrangement_thenReturnOk() throws Exception {
            int numberOfArrangements = foo.getArrangements().size();
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("addArrangement", "dummy"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"));
            assertThat(foo.getArrangements()).hasSize(numberOfArrangements + 1);
        }

        @Test
        @DisplayName("Delete arrangement")
        void whenDeleteArrangement_thenReturnOk() throws Exception {
            int numberOfArrangements = foo.getArrangements().size();
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("deleteArrangement", foo.getDefaultArrangement().getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"));
            assertThat(foo.getArrangements()).hasSize(numberOfArrangements - 1);
        }

        @Test
        @DisplayName("Add arrangement part")
        void whenSaveAddRow_thenReturnOk() throws Exception {
            int numberOfArrangementParts = foo.getDefaultArrangement().getArrangementParts().size();
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("addArrangementPart", foo.getDefaultArrangement().getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"));
            assertThat(foo.getDefaultArrangement().getArrangementParts()).hasSize(numberOfArrangementParts + 1);
        }

        @Test
        @DisplayName("Delete arrangement part")
        void whenSaveDeleteRow_thenReturnOk() throws Exception {
            int numberOfArrangementParts = foo.getDefaultArrangement().getArrangementParts().size();
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("deleteArrangementPart", foo.getDefaultArrangement().getId().toString() + "-0"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"));
            assertThat(foo.getDefaultArrangement().getArrangementParts()).hasSize(numberOfArrangementParts - 1);
        }
    }

    @Nested
    @DisplayName("File management")
    @WithMockUser(authorities = "EDIT_SONG")
    class FileGroup {

        @Test
        @DisplayName("Upload")
        void fileUpload() throws Exception {
            int numberOfFiles = foo.getFiles().size();
            mvc.perform(multipart("/score/submit").file(mpFile).with(csrf())
                            .sessionAttr("score", foo)
                            .param("upload", "dummy")
                            .param("file_type", "ARRANGEMENT")
                            .param("file_name", "myfile.txt"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"));
            assertThat(foo.getFiles()).hasSize(numberOfFiles + 1);
        }

        @Test
        @DisplayName("Download")
        void fileDownload() throws Exception {
            mvc.perform(get("/score/downloadFile").sessionAttr("score", foo)
                            .param("file_id", ngFile.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/score/downloadFile").sessionAttr("score", foo)
                            .param("file_id", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("View")
        void fileView() throws Exception {
            mvc.perform(get("/score/viewFile").sessionAttr("score", foo)
                            .param("file_id", ngFile.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
            mvc.perform(get("/score/viewFile").sessionAttr("score", foo)
                            .param("file_id", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Delete")
        void fileDelete() throws Exception {
            int numberOfFiles = foo.getFiles().size();
            mvc.perform(get("/score/deleteFile").sessionAttr("score", foo)
                            .param("file_id", ngFile.getId().toString()))
                    .andExpect(status().isOk());
            assertThat(foo.getFiles()).hasSize(numberOfFiles - 1);
            mvc.perform(get("/score/deleteFile").sessionAttr("score", foo)
                            .param("file_id", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Link management")
    class LinkGroup {
        @Test
        @DisplayName("Add link")
        void addLink() throws Exception {
            int numberOfLinks = foo.getLinks().size();
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("addLink", "dummy")
                            .param("link_name", "The link")
                            .param("link_uri", "https://www.terrassorkestern.se")
                            .param("link_type", "YOUTUBE")
                            .param("link_comment", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"));
            assertThat(foo.getLinks()).hasSize(numberOfLinks + 1);
        }

        @Test
        @DisplayName("Delete link")
        void deleteLink() throws Exception {
            int numberOfLinks = foo.getLinks().size();
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("deleteLink", foo.getLinks().get(0).getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"));
            assertThat(foo.getLinks()).hasSize(numberOfLinks - 1);
        }
    }

    @Test
    @DisplayName("Convert")
    @WithMockUser(authorities = "EDIT_SONG")
    void convert() throws Exception {
        mvc.perform(get("/score/convert")
                        .param("id", foo.getId().toString()))
                .andExpect(redirectedUrl("/score/list"));
    }

    @Test
    @DisplayName("OCR")
    @WithMockUser(authorities = "EDIT_SONG")
    void ocr() throws Exception {
        mvc.perform(get("/score/edit/ocr").param("id", foo.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auto complete")
    void autoComplete() throws Exception {
        mvc.perform(get("/score/edit/scores.json")).andExpect(status().isOk());
        mvc.perform(get("/score/edit/genres.json")).andExpect(status().isOk());
        mvc.perform(get("/score/edit/composers.json")).andExpect(status().isOk());
        mvc.perform(get("/score/edit/authors.json")).andExpect(status().isOk());
        mvc.perform(get("/score/edit/arrangers.json")).andExpect(status().isOk());
        mvc.perform(get("/score/edit/publishers.json")).andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Access")
    class AccessGroup {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void whenAccessProtectedContentAsAnonymousUser_redirectToLogin() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isOk());
            mvc.perform(get("/score/edit").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/delete").param("id", foo.getId().toString())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("save", "dummy")
                            .param("defaultArrangementIndex", "0"))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isOk());
            mvc.perform(get("/score/edit").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/delete").param("id", foo.getId().toString())).andExpect(status().isForbidden());
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("save", "dummy")
                            .param("defaultArrangementIndex", "0"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isOk());
            mvc.perform(get("/score/edit").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/delete").param("id", foo.getId().toString())).andExpect(redirectedUrl("/score/list"));
            mvc.perform(post("/score/submit").with(csrf()).sessionAttr("score", foo)
                            .param("save", "dummy")
                            .param("defaultArrangementIndex", "0"))
                    .andExpect(redirectedUrl("/score/list"));
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }
    }

}
