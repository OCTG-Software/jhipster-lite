package tech.jhipster.lite.generator;

import static org.assertj.core.api.Assertions.*;
import static tech.jhipster.lite.cucumber.CucumberAssertions.*;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import tech.jhipster.lite.GitTestUtil;
import tech.jhipster.lite.TestFileUtils;
import tech.jhipster.lite.TestUtils;
import tech.jhipster.lite.generator.project.infrastructure.primary.dto.ProjectDTO;

public class ProjectsSteps {

  private static String lastProjectFolder;

  @Autowired
  private TestRestTemplate rest;

  public static ProjectDTO newDefaultProjectDto() {
    newTestFolder();

    return TestUtils.readFileToObject("json/chips.json", ProjectDTO.class).folder(lastProjectFolder);
  }

  public static String newTestFolder() {
    return lastProjectFolder = TestFileUtils.tmpDirForTest();
  }

  public static String lastProjectFolder() {
    return lastProjectFolder;
  }

  @When("I download the created project")
  public void downloadCreatedProject() {
    rest.exchange("/api/projects?path=" + lastProjectFolder, HttpMethod.GET, new HttpEntity<>(octetsHeaders()), Void.class);
  }

  private HttpHeaders octetsHeaders() {
    HttpHeaders headers = new HttpHeaders();

    headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

    return headers;
  }

  @When("I get the created project information")
  public void getCreatedProjectInformation() {
    rest.exchange("/api/projects?path=" + lastProjectFolder, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Void.class);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();

    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    return headers;
  }

  @Then("I should have files in {string}")
  public void shouldHaveFiles(String basePath, List<String> files) {
    assertThatLastResponse().hasOkStatus();

    SoftAssertions assertions = new SoftAssertions();

    files.stream().map(file -> Paths.get(lastProjectFolder, basePath, file)).forEach(assertFileExist(assertions));

    assertions.assertAll();
  }

  private Consumer<Path> assertFileExist(SoftAssertions assertions) {
    return path -> assertions.assertThat(Files.exists(path)).as(fileNotFoundMessage(path)).isTrue();
  }

  private Supplier<String> fileNotFoundMessage(Path path) {
    return () -> "Can't find file " + path + " in project folder, found " + projectFiles();
  }

  @Then("I should not have files in {string}")
  public void shouldNotHaveFiles(String basePath, List<String> files) {
    assertThatLastResponse().hasHttpStatusIn(200, 201);

    SoftAssertions assertions = new SoftAssertions();

    files.stream().map(file -> Paths.get(lastProjectFolder, basePath, file)).forEach(assertFileNotExist(assertions));

    assertions.assertAll();
  }

  private Consumer<Path> assertFileNotExist(SoftAssertions assertions) {
    return path -> assertions.assertThat(!Files.exists(path)).as(fileFoundMessage(path)).isTrue();
  }

  private Supplier<String> fileFoundMessage(Path path) {
    return () -> "Can find file " + path + " in project folder, found " + projectFiles();
  }

  private String projectFiles() {
    try {
      return Files.walk(Paths.get(lastProjectFolder)).filter(Files::isRegularFile).map(Path::toString).collect(Collectors.joining(", "));
    } catch (IOException e) {
      return "unreadable folder";
    }
  }

  @Then("I should have {string} in {string}")
  public void shouldHaveFileContent(String content, String file) throws IOException {
    assertThatLastResponse().hasHttpStatusIn(200, 201);

    assertThat(Files.readString(Paths.get(lastProjectFolder, file))).contains(content);
  }

  @Then("I should not have {string} in {string}")
  public void shouldNotHaveFileContent(String content, String file) throws IOException {
    assertThatLastResponse().hasHttpStatusIn(200, 201);

    assertThat(Files.readString(Paths.get(lastProjectFolder, file))).doesNotContain(content);
  }

  @Then("I should have entries in {string}")
  public void shouldHaveStringsInFile(String file, List<String> values) throws IOException {
    assertThatLastResponse().hasHttpStatusIn(200, 201);
    assertThat(Files.readString(Paths.get(lastProjectFolder, file))).contains(values);
  }

  @Then("I should have {string} project")
  public void shouldHaveProjectFile(String file) {
    assertThatLastResponse()
      .hasOkStatus()
      .hasHeader(HttpHeaders.CONTENT_DISPOSITION)
      .containing("attachment; filename=" + file)
      .and()
      .hasHeader("X-Suggested-Filename")
      .containing(file);
  }

  @Then("I should have modules")
  public void shouldHaveModules(List<Map<String, String>> modules) {
    assertThatLastResponse().hasOkStatus().hasElement("$.modules").containingExactly(modules);
  }

  @Then("I should have properties")
  public void shouldHaveProperties(Map<String, Object> properties) {
    assertThatLastResponse().hasOkStatus().hasElement("$.properties").containing(properties).withElementsCount(properties.size());
  }

  @Then("I should have commit {string}")
  public void shouldHaveCommit(String commitMessage) throws IOException {
    assertThatLastResponse().hasOkStatus();

    assertThat(GitTestUtil.getCommits(Paths.get(lastProjectFolder))).contains(commitMessage);
  }

  @Then("I should not have any commit")
  public void shouldNotHaveCommits() throws IOException {
    assertThatLastResponse().hasOkStatus();

    assertThat(GitTestUtil.getCommits(Paths.get(lastProjectFolder))).isEmpty();
  }

  @Then("I should have {int} file in {string}")
  public void shouldHaveFilesCountInDirectory(int filesCount, String directory) throws IOException {
    assertThatLastResponse().hasOkStatus();

    assertThat(Files.list(Paths.get(lastProjectFolder, directory)).count()).isEqualTo(filesCount);
  }
}
