package org.hivedb.teamcity.plugin;

import com.intellij.openapi.vcs.VcsRoot;

import java.io.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.log4j.Logger;


public class Git {
  Logger log = Logger.getLogger(Git.class);

  File workingDirectory;
  String gitCommand, projectName;
  public static final String GIT_DATE_FORMAT = "EEE MMM dd HH:mm:ss yyyy Z";

  public Git(String cmd, String workingDirectory, String projectName) {
    this.gitCommand = cmd;
    this.projectName = projectName;
    this.workingDirectory = new File(workingDirectory);
  }

  public Collection<String> revList(String rev1, String rev2) {
    String log = runCommand(
      new String[]{getGitCommand(), "rev-list", String.format("%s...%s", rev1, rev2)},
      new String[]{}
    );
    return Arrays.asList(log.split("\n"));
  }

  public Collection<Commit> log(int n) {
    String log = runCommand(
      new String[]{getGitCommand(), "log", "-n", new Integer(n).toString()},
      new String[]{}
    );
    return parseCommitLog(log);
  }

  public String show(String rev, String file) {
    return runCommand(
      new String[]{getGitCommand(), "show", String.format("%s:%s", rev, file)},
      new String[]{}
    );
  }

  public boolean isGitRepo(String dir) {
    return new File(new File(dir), ".git").exists();
  }

  public String clone(String url) {
    return runCommand(
      new String[]{getGitCommand(), "clone", url, new File(workingDirectory, this.projectName).getAbsolutePath()},
      new String[]{}
    );
  }

  private String runCommand(String[] argz, String[] environment) {
    Process cmdProc = null;
    
    try {
      cmdProc = Runtime.getRuntime().exec(argz, environment, workingDirectory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    BufferedReader in = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
    StringBuilder output = new StringBuilder();
    String line;
    try {
      while((line = in.readLine()) != null) {
        output.append(line);
        output.append("\n");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return output.toString();
  }

  public String getGitCommand() {
    return gitCommand;
  }

  private Collection<Commit> parseCommitLog(String log) {
    Collection<Commit> commits = new ArrayList<Commit>();
    BufferedReader r = new BufferedReader(new StringReader(log));
    String line;
    try {
      Commit current = new Commit();
      while((line = r.readLine()) != null) {
        String s = line.trim();
        if( s == null || "".equals(s))
          continue;
        else {
          if(s.startsWith("commit")) {
            if(current.isValid()) {
              commits.add(current);
              current = new Commit();
            }
            current.setId(s.replaceAll("commit", "").trim());
          } else if(s.startsWith("Author"))
            current.setAuthor(s.replaceAll("Author:","").trim());
          else if(s.startsWith("Date")){
            String dateString = s.replaceAll("Date:","").trim();
            SimpleDateFormat format = new SimpleDateFormat(GIT_DATE_FORMAT);
            current.setDate(format.parse(dateString));
          } else {
            current.setMessage(s);
            commits.add(current);
            current = new Commit();
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    return commits;
  }
}
