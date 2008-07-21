package org.hivedb.teamcity.plugin;

import jetbrains.buildServer.AgentSideCheckoutAbility;
import jetbrains.buildServer.CollectChangesByIncludeRule;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.vcs.*;
import jetbrains.buildServer.vcs.patches.PatchBuilder;
import jetbrains.buildServer.web.openapi.WebResourcesManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class GitVcs extends VcsSupport implements AgentSideCheckoutAbility, VcsPersonalSupport, CollectChangesByIncludeRule {
  Logger log = Logger.getLogger(GitVcs.class);

  private static final String GIT_COMMAND = "git_command";
  private static final String WORKING_DIRECTORY = "working_directory";
  private static final String CLONE_URL = "clone_url";
  private static final String PROJECT_NAME = "project_name";

  public GitVcs(VcsManager vcsmanager, WebResourcesManager resourcesManager) {
    vcsmanager.registerVcsSupport(this);
    resourcesManager.addPluginResources("git", "git-vcs.jar");
  }

  public List<ModificationData> collectBuildChanges(VcsRoot root, String fromVersion, String currentVersion, CheckoutRules checkoutRules) throws VcsException {
    log.debug(String.format("%s: Collecting build changes from %s to %s", root.getVcsName(), fromVersion, currentVersion));
    logVcsRoot(root);
    return VcsSupportUtil.collectBuildChanges(root, fromVersion, currentVersion, checkoutRules, this);
  }

  public void buildPatch(VcsRoot root, String fromVersion, String toVersion, PatchBuilder builder, CheckoutRules checkoutRules) throws IOException, VcsException {
    log.warn(String.format("%s: Building patch from '%s' to '%s'", root.getVcsName(), fromVersion, toVersion));
    logVcsRoot(root);
    throw new UnsupportedOperationException("Nuh unh!");
  }

  private void logVcsRoot(VcsRoot root) {
    log.warn("=== VcsRoot ===");
    log.warn("Type: " + root.getVcsName());
    log.warn("Root Version: " + root.getRootVersion());
    Map<String,String> p = root.getProperties();
    for(Map.Entry<String, String> entry : p.entrySet())
      log.warn(String.format("%s: %s", entry.getKey(), entry.getValue()));
  }

  @NotNull
  public byte[] getContent(VcsModification vcsModification, VcsChangeInfo change, VcsChangeInfo.ContentType contentType, VcsRoot vcsRoot) throws VcsException {
    if(change.getType() == VcsChangeInfo.Type.REMOVED || change.getType() == VcsChangeInfo.Type.DIRECTORY_REMOVED )
      return new byte[]{};
    else {
      String rev = contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber() : change.getAfterChangeRevisionNumber();
      return getContent(change.getRelativeFileName(), vcsRoot, rev);
    }
  }

  @NotNull
  public byte[] getContent(String filePath, VcsRoot versionedRoot, String version) throws VcsException {
    String groomedVersion = version.split("-")[0].trim();
    return git(versionedRoot).show(groomedVersion, filePath).getBytes();
  }

  public String getName() {
    return "git";
  }

  @Used("jsp")
  public String getDisplayName() {
    return "Git";
  }

  public PropertiesProcessor getVcsPropertiesProcessor() {
    return new PropertiesProcessor(){
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        return new ArrayList<InvalidProperty>();
      }
    };
  }

  public String getVcsSettingsJspFilePath() {
    return "git_settings.jsp";
  }

  public String getCurrentVersion(VcsRoot root) throws VcsException {
    log.warn(String.format("%s: Getting current version", root.getVcsName()));
    logVcsRoot(root);
//    if(!git(root).isGitRepo(root.getProperty(WORKING_DIRECTORY)))
//      git(root).clone(root.getProperty(PROJECT_NAME),root.getProperty(CLONE_URL), root.getProperty(WORKING_DIRECTORY));
    Collection<Commit> commits = git(root).log(1);
    String currentVersion = null;
    if(!commits.isEmpty()) {
      currentVersion = commits.iterator().next().getVersion();
      log.warn("Current Version: " + currentVersion);
    }
    return currentVersion;
  }

  public String describeVcsRoot(VcsRoot vcsRoot) {
    return String.format("%s: %s", vcsRoot.getProperty(GIT_COMMAND), vcsRoot.getProperty(WORKING_DIRECTORY));
  }

  public boolean isTestConnectionSupported() {
    return false;
  }

  public String testConnection(VcsRoot vcsRoot) throws VcsException {
    throw new UnsupportedOperationException("Not implemented, git is a distributed version control system. Your repo is local.");
  }

  @Nullable
  public Map<String, String> getDefaultVcsProperties() {
    Map<String,String> p = new HashMap<String,String>();
    p.put(GIT_COMMAND, "/usr/bin/env git");
    p.put(WORKING_DIRECTORY, "./");
    return p;
  }

  public String getVersionDisplayName(String version, VcsRoot root) throws VcsException {
    Collection<Commit> commits = git(root).log(1);
    return !commits.isEmpty() ? commits.iterator().next().toString() : null;
  }

  @NotNull
  public Comparator<String> getVersionComparator() {
    return new GitComparator();
  }

  public VcsPersonalSupport getPersonalSupport() {
    return this;
  }

  private boolean nullOrEmpty(Object o) {
    return o == null || "".equals(o);
  }

  private Git git(VcsRoot root) {
    return new Git(root.getProperty(GIT_COMMAND), root.getProperty(WORKING_DIRECTORY), root.getProperty(PROJECT_NAME));
  }

  public boolean isAgentSideCheckoutAvailable() {
    return true;
  }

  @Nullable
  public String mapFullPath(VcsRoot vcsRoot, String s) {
    String workingDirectory = vcsRoot.getProperty(WORKING_DIRECTORY);
    if(workingDirectory.endsWith("/") && s.startsWith("/"))
      return workingDirectory + s.substring(1);
    else
      return workingDirectory + s;
  }

  public List<ModificationData> collectBuildChanges(VcsRoot vcsRoot, String s, String s1, IncludeRule includeRule) throws VcsException {
    throw new UnsupportedOperationException();
  }
}
