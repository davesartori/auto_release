package com.nab.gradle.plugins.release;

import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class AutoReleaseTask extends DefaultTask {

  public static void main(String... args) throws Exception {
    autoRelease();


  }

  @TaskAction
  public static void autoRelease() throws Exception {
    final Git git = Git.open(new File("."));

    Config storedConfig = git.getRepository().getConfig();
    String gitRepoUri = storedConfig.getString("remote", "origin", "url");
    storedConfig.setString("user", null, "name", "autobuild");
    storedConfig.setString("user", null, "email", "no-reply@nab.com.au");

    checkIfReleaseExists(git, gitRepoUri);

    try {
      git.checkout().setName("develop").setCreateBranch(true).call();
    } catch (Exception e) {
    }
    try {
      git.checkout().setName("master").setCreateBranch(true).setForce(true).call();
    } catch (Exception e) {
    }

    // verify all master commits are in devleop.
    System.out.print("Verify that there are no changes from master missing in develop...");
    checkIfAllMasterCommitsAreInDevelop(getCommits("master"), getCommits("develop"));
    System.out.println("Done");

    final PushCommand push = git.push();
    push.setRemote("origin");
    setCredentials(gitRepoUri, push);

    // Checkout develop branch
    System.out.print("Checkout develop branch...");
    git.checkout().setName("develop").setCreateBranch(false).call();
    System.out.println("Done.");

    // Checkout release branch
    System.out.print("Create remote release branch based on develop...");
    CreateBranchCommand bcc = git.branchCreate();
    bcc.setName("release")
        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
        .setStartPoint("origin/develop")
        .setForce(true)
        .call();
    push.setRefSpecs(new RefSpec("release:release"));
    push.call();
    System.out.println("Done.");

    // Checkout develop branch
    System.out.print("Checkout develop branch...");
    git.checkout().setName("develop").setCreateBranch(false).call();
    System.out.println("Done.");

    System.out.print("Increment version number...");
    incrementVersion();
    AddCommand add = git.add();
    add.addFilepattern(".").setUpdate(true).call();
    git.commit().setAll(true).setMessage("Version number increment").call();
    push.setRefSpecs(new RefSpec("develop:develop"));
    push.call();
    System.out.println("Done.");
  }

  private static void checkIfAllMasterCommitsAreInDevelop(List<String> commitsOnMaster, List<String> commitsOnDevelop)
      throws Exception {
    List<String> msgs = new ArrayList<>();
    for (String s : commitsOnMaster) {
      if (!commitsOnDevelop.contains(s)) {
        msgs.add(s);
      }
    }
    if (msgs.size() > 0) {
      throw new Exception(msgs.size() + " commit" + (msgs.size() > 0 ? "s" : "")
          + " on master " + (msgs.size() > 0 ? "are" : "is")
          + " not found in the develop branch. Please review. Commit found: "
          + (msgs.size() > 0 ? "s" : "") + ": " + msgs);
    }
  }

  private static CredentialsProvider allowHosts = new CredentialsProvider() {
    @Override
    public boolean supports(CredentialItem... items) {
      return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
      return true;
    }

    @Override
    public boolean isInteractive() {
      return true;
    }
  };

  private static List<String> getCommits(String branchName) throws IOException {
    Git git = Git.open(new File("."));
    RefDatabase refDatabase = git.getRepository().getRefDatabase();
    Ref branch = refDatabase.getRef("refs/remotes/origin/" + branchName);

    List<String> commits = new ArrayList<>();
    try (RevWalk walk = new RevWalk(git.getRepository())) {
      RevCommit commit = walk.parseCommit(branch.getObjectId());
      walk.markStart(commit);
      for (RevCommit rev : walk) {
        commits.add(rev.getName());
      }
      walk.dispose();
    }
    return commits;
  }

  private static void incrementVersion() throws ConfigurationException {
    PropertiesConfiguration config = new PropertiesConfiguration("gradle.properties");
    String[] currentVersion;
    try {
      currentVersion = ((String) config.getProperty("versionNumber")).split(Pattern.quote("."));
      System.out.print(" [From " + config.getProperty("versionNumber"));
    } catch (Exception e) {
      throw new RuntimeException(
          "Make sure build.properties exists with versionNumber property in major.minor.patch format, for example 1.0.1");
    }
    String newVersion = currentVersion[0] + "." + (Integer.parseInt(currentVersion[1]) + 1) + "." + currentVersion[2];
    config.setProperty("versionNumber", newVersion);
    System.out.print(" to " + newVersion + "]... ");
    config.save();
  }

  private static void setCredentials(String gitRepoUri, TransportCommand lsRemote) throws Exception {
    if (gitRepoUri.toUpperCase().contains("HTTPS")) {
      String username = "";
      String password = "";
      try {
        username = "davidsartori@gmail.com";
        password = "Dirtbike1";
      } catch (Exception e) {
      }
      lsRemote.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
    } else {
      final AtomicBoolean success = new AtomicBoolean(true);
      lsRemote.setTransportConfigCallback(transport -> {
        if (transport instanceof SshTransport) {
          SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
              session.setProxy(new ProxyHTTP("internal-proxy.account", 8080));
            }
          };
          SshTransport sshTransport = (SshTransport) transport;
          sshTransport.setSshSessionFactory(sshSessionFactory);
        } else {
          success.set(false);
        }
      });

      if (!success.get()) {
        throw new Exception("This box doesn't have permission to push to the remote Git repo");
      }

      lsRemote.setCredentialsProvider(allowHosts);
    }

  }

  private static void checkIfReleaseExists(Git git, String gitRepoUri) throws Exception {
    final LsRemoteCommand lsRemote = git.lsRemote();
    lsRemote.setRemote("origin");
    setCredentials(gitRepoUri, lsRemote);

    System.out.println("Check if release branch exists remotely");
    for (Ref ref : lsRemote.call()) {
      if (StringUtils.containsIgnoreCase(ref.getName(), "/release")) {
        throw new Exception("Release branch exists on remote");
      }
    }
  }
}