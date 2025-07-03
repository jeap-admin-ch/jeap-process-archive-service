package ch.admin.bit.jeap.processarchive.registry.git;

import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
public class GitClientTest {

    private Log mockLog;
    private ProcessBuilderFactory mockProcessBuilderFactory;
    private ProcessBuilder mockProcessBuilder;
    private Process mockProcess;

    @TempDir
    File tempDir;

    private static final String TEST_GIT_URL = "some-url";
    private static final String TEST_TOKEN_ENV_VAR_NAME = "TEST_GIT_TOKEN";

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        mockLog = mock(Log.class);
        mockProcessBuilderFactory = mock(ProcessBuilderFactory.class);
        mockProcessBuilder = mock(ProcessBuilder.class);
        mockProcess = mock(Process.class);
        
        when(mockProcessBuilderFactory.createProcessBuilder(any(String[].class))).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.waitFor()).thenReturn(0);
    }

    @AfterEach
    void tearDown() {
        reset(mockLog, mockProcessBuilderFactory, mockProcessBuilder, mockProcess);
    }

    @Test
    @SneakyThrows
    void cloneAtBranch_WithoutToken_UsesSystemGit() {
        GitClient gitClient = new GitClient(TEST_GIT_URL, "NONEXISTENT_TOKEN", mockLog, mockProcessBuilderFactory);

        gitClient.cloneAtBranch("main", tempDir);

        verify(mockProcessBuilderFactory).createProcessBuilder("git", "clone", "--branch", "main", TEST_GIT_URL, tempDir.getAbsolutePath());
        verify(mockProcess).waitFor();
        verify(mockLog).info("Using a system Git process to clone the remote repository at branch main.");
    }

    @Test
    @SneakyThrows
    void cloneAtCheckout_WithoutToken_UsesSystemGit() {
        GitClient gitClient = new GitClient(TEST_GIT_URL, "NONEXISTENT_TOKEN", mockLog, mockProcessBuilderFactory);

        gitClient.cloneAtCheckout("abc123", tempDir);

        verify(mockProcessBuilderFactory).createProcessBuilder("git", "clone", TEST_GIT_URL, tempDir.getAbsolutePath());
        verify(mockProcessBuilderFactory).createProcessBuilder("git", "-C", tempDir.getAbsolutePath(), "checkout", "abc123");
        verify(mockProcess, times(2)).waitFor();
        verify(mockLog).info("Using a system Git process to clone the remote repository at checkout abc123.");
    }

    @Test
    @SneakyThrows
    void executeGitCommand_WithNonZeroExitCode_ThrowsGitClientException() {
        when(mockProcess.waitFor()).thenReturn(1);
        GitClient gitClient = new GitClient(TEST_GIT_URL, "NONEXISTENT_TOKEN", mockLog, mockProcessBuilderFactory);

        GitClientException exception = assertThrows(GitClientException.class, () -> 
            gitClient.cloneAtBranch("main", tempDir)
        );

        assertTrue(exception.getMessage().contains("The Git system command execution failed. Exit code: 1"));
    }

    @Test
    @SneakyThrows
    void executeGitCommand_WithInterruptedException_ThrowsGitClientException() {
        when(mockProcess.waitFor()).thenThrow(new InterruptedException("Test interruption"));
        GitClient gitClient = new GitClient(TEST_GIT_URL, "NONEXISTENT_TOKEN", mockLog, mockProcessBuilderFactory);

        GitClientException exception = assertThrows(GitClientException.class, () -> 
            gitClient.cloneAtBranch("main", tempDir)
        );

        assertTrue(exception.getMessage().contains("The Git system command execution was interrupted."));
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    @SneakyThrows
    void executeGitCommand_WithIOException_ThrowsGitClientException() {
        when(mockProcessBuilder.start()).thenThrow(new IOException("Test IO exception"));
        GitClient gitClient = new GitClient(TEST_GIT_URL, "NONEXISTENT_TOKEN", mockLog, mockProcessBuilderFactory);

        GitClientException exception = assertThrows(GitClientException.class, () -> 
            gitClient.cloneAtBranch("main", tempDir)
        );

        assertTrue(exception.getMessage().contains("The Git system command execution failed: Test IO exception"));
    }

    @Test
    void getGitToken_WithSetEnvironmentVariable_LogsTokenIsSet(EnvironmentVariables env) {
        env.set(TEST_TOKEN_ENV_VAR_NAME, "test-token");

        new GitClient(TEST_GIT_URL, TEST_TOKEN_ENV_VAR_NAME, mockLog, mockProcessBuilderFactory);

        verify(mockLog).info("The git token env variable name configured is: " + TEST_TOKEN_ENV_VAR_NAME);
        verify(mockLog).info(eq("The env variable " + TEST_TOKEN_ENV_VAR_NAME + " is set."));
    }

    @Test
    void getGitToken_WithEmptyEnvironmentVariable_LogsTokenIsNotSet(EnvironmentVariables env) {
        env.set(TEST_TOKEN_ENV_VAR_NAME, "");

        new GitClient(TEST_GIT_URL, TEST_TOKEN_ENV_VAR_NAME, mockLog, mockProcessBuilderFactory);

        verify(mockLog).info("The git token env variable name configured is: " + TEST_TOKEN_ENV_VAR_NAME);
        verify(mockLog).info(eq("The env variable " + TEST_TOKEN_ENV_VAR_NAME + " is not set."));
    }

    @Test
    @SuppressWarnings("resource")
    @SneakyThrows
    void cloneAtBranch_WithToken_UsesJGit(EnvironmentVariables env) {
        String testToken = "test-token-12345";
        env.set(TEST_TOKEN_ENV_VAR_NAME, testToken);
        CloneCommand mockCloneCommand = mock(CloneCommand.class);
        Git mockGit = mock(Git.class);
        ArgumentCaptor<CredentialsProvider> credentialsCaptor = ArgumentCaptor.forClass(CredentialsProvider.class);
        when(mockCloneCommand.setURI(TEST_GIT_URL)).thenReturn(mockCloneCommand);
        when(mockCloneCommand.setBranch("main")).thenReturn(mockCloneCommand);
        when(mockCloneCommand.setDirectory(tempDir)).thenReturn(mockCloneCommand);
        when(mockCloneCommand.setCredentialsProvider(credentialsCaptor.capture())).thenReturn(mockCloneCommand);
        when(mockCloneCommand.call()).thenReturn(mockGit);
        try (MockedStatic<Git> mockedStaticGit = mockStatic(Git.class)) { // mockedStaticGit needs to be closed after use
            mockedStaticGit.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            GitClient gitClient = new GitClient(TEST_GIT_URL, TEST_TOKEN_ENV_VAR_NAME, mockLog, mockProcessBuilderFactory);

            gitClient.cloneAtBranch("main", tempDir);

            verify(mockLog).info("Using JGit and a token to clone the remote repository at branch main.");
            verify(mockCloneCommand).setURI(TEST_GIT_URL);
            verify(mockCloneCommand).setBranch("main");
            verify(mockCloneCommand).setDirectory(tempDir);
            verify(mockCloneCommand).call();
            verify(mockGit).close();
            verifyNoInteractions(mockProcessBuilderFactory);
            verifyCredentialaProvider(credentialsCaptor, testToken);
        }
    }

    @Test
    @SuppressWarnings("resource")
    @SneakyThrows
    void cloneAtCheckout_WithToken_UsesJGit(EnvironmentVariables env) {
        String testToken = "test-checkout-token-67890";
        env.set(TEST_TOKEN_ENV_VAR_NAME, testToken);
        CloneCommand mockCloneCommand = mock(CloneCommand.class);
        Git mockGit = mock(Git.class);
        ArgumentCaptor<CredentialsProvider> credentialsCaptor = ArgumentCaptor.forClass(CredentialsProvider.class);
        when(mockCloneCommand.setURI(TEST_GIT_URL)).thenReturn(mockCloneCommand);
        when(mockCloneCommand.setDirectory(tempDir)).thenReturn(mockCloneCommand);
        when(mockCloneCommand.setCredentialsProvider(credentialsCaptor.capture())).thenReturn(mockCloneCommand);
        when(mockCloneCommand.call()).thenReturn(mockGit);
        CheckoutCommand mockCheckoutCommand = mock(CheckoutCommand.class);
        when(mockGit.checkout()).thenReturn(mockCheckoutCommand);
        when(mockCheckoutCommand.setName("abc123")).thenReturn(mockCheckoutCommand);
        try (MockedStatic<Git> mockedStaticGit = mockStatic(Git.class)) { // mockedStaticGit needs to be closed after use
            mockedStaticGit.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            GitClient gitClient = new GitClient(TEST_GIT_URL, TEST_TOKEN_ENV_VAR_NAME, mockLog, mockProcessBuilderFactory);

            gitClient.cloneAtCheckout("abc123", tempDir);

            verify(mockLog).info("Using JGit and a token to clone the remote repository at checkout abc123.");
            verify(mockCloneCommand).setURI(TEST_GIT_URL);
            verify(mockCloneCommand).setDirectory(tempDir);
            verify(mockCloneCommand).call();
            verify(mockGit).checkout();
            verify(mockCheckoutCommand).setName("abc123");
            verify(mockGit).close();
            verifyNoInteractions(mockProcessBuilderFactory);
            verifyCredentialaProvider(credentialsCaptor, testToken);
        }
    }

    @SneakyThrows
    private void verifyCredentialaProvider(ArgumentCaptor<CredentialsProvider> credentialsCaptor, String expectedToken)  {
        CredentialsProvider capturedProvider = credentialsCaptor.getValue();
        assertNotNull(capturedProvider);
        assertInstanceOf(UsernamePasswordCredentialsProvider.class, capturedProvider);
        UsernamePasswordCredentialsProvider usernamePasswordProvider = (UsernamePasswordCredentialsProvider) capturedProvider;
        // Use reflection to access the private password field
        Field passwordField = UsernamePasswordCredentialsProvider.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        char[] password = (char[]) passwordField.get(usernamePasswordProvider);
        assertEquals(expectedToken, new String(password));
    }

}
