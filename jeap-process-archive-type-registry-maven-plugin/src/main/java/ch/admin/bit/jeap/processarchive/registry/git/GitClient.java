package ch.admin.bit.jeap.processarchive.registry.git;

import lombok.Getter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class GitClient {

    @Getter
    private final String gitUrl;
    private final String token;
    private final Log log;
    private final ProcessBuilderFactory processBuilderFactory;

    public GitClient(String gitUrl, String gitTokenEnvVariableName, Log log) {
        this(gitUrl, gitTokenEnvVariableName, log, null);
    }

    public GitClient(String gitUrl, String gitTokenEnvVariableName, Log log, ProcessBuilderFactory processBuilderFactory) {
        this.gitUrl = gitUrl;
        this.log = (log != null ? log : new SystemStreamLog());
        this.token = getGitToken(gitTokenEnvVariableName);
        this.processBuilderFactory = (processBuilderFactory != null ? processBuilderFactory : new ProcessBuilderFactory());
    }

    public void cloneAtBranch(String branch, File targetDirectory) {
        if (token != null) {
            cloneAtBranchWithToken(branch, targetDirectory);
        } else {
            gitCloneAtBranchWithSystemGit(branch, targetDirectory);
        }
    }

    public void cloneAtCheckout(String checkoutAt, File targetDirectory) {
        if (token != null) {
            cloneAtCheckoutWithToken(checkoutAt, targetDirectory);
        } else {
            cloneAtCheckoutWithSystemGit(checkoutAt, targetDirectory);
        }
    }

    @SuppressWarnings("EmptyTryBlock")
    private void cloneAtBranchWithToken(String branch, File targetDirectory) {
        log.info("Using JGit and a token to clone the remote repository at branch %s.".formatted(branch));
        try (Git ignored =  Git.cloneRepository()
                    .setURI(gitUrl)
                    .setBranch(branch)
                    .setDirectory(targetDirectory)
                    .setCredentialsProvider(getCredentialsProvider(token))
                    .call())
        {
            // just for the automatic resource management, no further action needed here
        } catch (GitAPIException e) {
            String message = String.format("Failed to clone branch %s from git repository %s to directory %s.",
                    branch, gitUrl, targetDirectory);
            throw new GitClientException(message, e);
        }
    }

    private void cloneAtCheckoutWithToken(String checkoutAt, File targetDirectory) {
        log.info("Using JGit and a token to clone the remote repository at checkout %s.".formatted(checkoutAt));
        try (Git git =  Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(targetDirectory)
                .setCredentialsProvider(getCredentialsProvider(token))
                .call()) {
            git.checkout()
                .setName(checkoutAt)
                .call();
        } catch (GitAPIException e) {
            String message = String.format("Failed checkout %s from git repository %s cloned to directory %s.",
                    checkoutAt, gitUrl, targetDirectory);
            throw new GitClientException(message, e);
        }
    }

    private void gitCloneAtBranchWithSystemGit(String branch, File targetDirectory) {
        log.info("Using a system Git process to clone the remote repository at branch %s.".formatted(branch));
        executeGitCommandWithSystemGit("git", "clone", "--branch", branch, gitUrl, targetDirectory.getAbsolutePath());
    }

    private void cloneAtCheckoutWithSystemGit(String checkoutAt, File targetDirectory) {
        log.info("Using a system Git process to clone the remote repository at checkout %s.".formatted(checkoutAt));
        executeGitCommandWithSystemGit("git", "clone", gitUrl, targetDirectory.getAbsolutePath());
        executeGitCommandWithSystemGit("git", "-C", targetDirectory.getAbsolutePath(), "checkout", checkoutAt);
    }

    private void executeGitCommandWithSystemGit(String... command) {
        try {
            ProcessBuilder pb = processBuilderFactory.createProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String message = "The Git system command execution failed. Exit code: " + exitCode;
                throw new GitClientException(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "The Git system command execution was interrupted.";
            throw new GitClientException(message, e);
        } catch (IOException e) {
            String message = "The Git system command execution failed: " + e.getMessage();
            throw new GitClientException(message, e);
        }
    }

    private CredentialsProvider getCredentialsProvider(String token) {
        return new UsernamePasswordCredentialsProvider("no-username-with-token", token);
    }

    private String getGitToken(String gitTokenEnvVariableName) {
        log.info("The git token env variable name configured is: " + gitTokenEnvVariableName);
        String token = Optional.ofNullable(System.getenv(gitTokenEnvVariableName))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        log.info("The env variable " + gitTokenEnvVariableName + " is " +
                (token != null ? "set." : "not set."));
        return token;
    }

}
