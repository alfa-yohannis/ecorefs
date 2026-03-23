package org.ecorefs.modisco.jdt.reverse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.modisco.java.discoverer.DiscoverJavaModelFromJavaProject;

public class ModiscoJavaModelExporter {

  public static final String PLUGIN_ID = "org.ecorefs.modisco.jdt.reverse";

  public List<Path> exportJavaProjects(
      final Path repositoryRoot,
      final Path outputDirectory,
      final String projectNameFilter,
      final IProgressMonitor monitor) throws Exception {

    Objects.requireNonNull(repositoryRoot, "repositoryRoot");
    Objects.requireNonNull(outputDirectory, "outputDirectory");

    final Path normalizedRepositoryRoot = repositoryRoot.toAbsolutePath().normalize();
    final Path normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize();
    final String normalizedProjectFilter = normalize(projectNameFilter);
    final SubMonitor subMonitor = SubMonitor.convert(
        monitor == null ? new NullProgressMonitor() : monitor,
        "Reverse engineering Java projects with MoDisco",
        1000);

    if (!Files.isDirectory(normalizedRepositoryRoot)) {
      throw new IllegalArgumentException("Repository root does not exist: " + normalizedRepositoryRoot);
    }

    Files.createDirectories(normalizedOutputDirectory);

    final List<ProjectCandidate> candidates = collectProjectCandidates(normalizedRepositoryRoot);
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException(
          "No Eclipse .project descriptors were found under " + normalizedRepositoryRoot);
    }

    final List<IJavaProject> importedJavaProjects = importProjects(candidates, subMonitor.split(400));
    final List<IJavaProject> javaProjectsToExport = importedJavaProjects.stream()
        .filter(javaProject -> normalizedProjectFilter == null
            || javaProject.getElementName().equals(normalizedProjectFilter))
        .sorted(Comparator.comparing(IJavaProject::getElementName))
        .collect(Collectors.toList());

    if (javaProjectsToExport.isEmpty()) {
      if (normalizedProjectFilter == null) {
        throw new IllegalStateException(
            "No Java projects were imported from " + normalizedRepositoryRoot);
      }
      throw new IllegalArgumentException(
          "Java project '" + normalizedProjectFilter + "' was not found under " + normalizedRepositoryRoot);
    }

    final SubMonitor discoveryMonitor = subMonitor.split(600);
    discoveryMonitor.setWorkRemaining(javaProjectsToExport.size());

    final List<Path> exportedModels = new ArrayList<>();
    final List<String> failures = new ArrayList<>();
    for (final IJavaProject javaProject : javaProjectsToExport) {
      final Path outputFile = normalizedOutputDirectory.resolve(javaProject.getElementName() + ".xmi");
      try {
        exportJavaProject(javaProject, outputFile, discoveryMonitor.split(1));
        exportedModels.add(outputFile);
      } catch (final Exception exception) {
        failures.add(formatFailure(javaProject, exception));
      }
    }

    final Path failureReport = normalizedOutputDirectory.resolve("_modisco_failures.txt");
    if (failures.isEmpty()) {
      Files.deleteIfExists(failureReport);
    } else {
      Files.write(failureReport, failures, StandardCharsets.UTF_8);
    }

    if (exportedModels.isEmpty()) {
      final String detail = failures.isEmpty() ? "No projects were exported." : failures.get(0);
      throw new IllegalStateException(
          "MoDisco did not export any Java models from " + normalizedRepositoryRoot + ". " + detail);
    }

    return exportedModels;
  }

  private List<ProjectCandidate> collectProjectCandidates(final Path repositoryRoot) throws Exception {
    final List<ProjectCandidate> candidates = new ArrayList<>();

    try (Stream<Path> stream = Files.find(
        repositoryRoot,
        5,
        (path, attributes) -> attributes.isRegularFile() && ".project".equals(path.getFileName().toString()))) {
      final List<Path> descriptors = stream
          .sorted()
          .collect(Collectors.toList());

      for (final Path descriptor : descriptors) {
        final IProjectDescription description = loadDescription(descriptor);
        final boolean javaProject = Arrays.asList(description.getNatureIds()).contains(JavaCore.NATURE_ID);
        candidates.add(new ProjectCandidate(description.getName(), descriptor.getParent(), javaProject));
      }
    }

    return candidates;
  }

  private List<IJavaProject> importProjects(
      final List<ProjectCandidate> candidates,
      final IProgressMonitor monitor) throws Exception {

    final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    final SubMonitor subMonitor = SubMonitor.convert(monitor, "Importing workspace projects", candidates.size() * 2);
    final List<IJavaProject> javaProjects = new ArrayList<>();

    for (final ProjectCandidate candidate : candidates) {
      final IProject workspaceProject = workspaceRoot.getProject(candidate.name());
      importProject(candidate, workspaceProject, subMonitor.split(1));
      if (candidate.javaProject()) {
        javaProjects.add(JavaCore.create(workspaceProject));
      }
    }

    for (final IJavaProject javaProject : javaProjects) {
      primeClasspath(javaProject);
      subMonitor.split(1);
    }

    return javaProjects;
  }

  private void importProject(
      final ProjectCandidate candidate,
      final IProject workspaceProject,
      final IProgressMonitor monitor) throws CoreException {

    final IProjectDescription description = loadDescription(candidate.location().resolve(".project"));
    description.setLocation(new org.eclipse.core.runtime.Path(candidate.location().toString()));

    if (workspaceProject.exists()) {
      final org.eclipse.core.runtime.IPath currentLocation = workspaceProject.getLocation();
      if (currentLocation == null
          || !Path.of(currentLocation.toOSString()).toAbsolutePath().normalize().equals(candidate.location())) {
        if (workspaceProject.isOpen()) {
          workspaceProject.close(monitor);
        }
        workspaceProject.delete(false, true, monitor);
        workspaceProject.create(description, monitor);
      }
    } else {
      workspaceProject.create(description, monitor);
    }

    if (!workspaceProject.isOpen()) {
      workspaceProject.open(monitor);
    }

    workspaceProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
  }

  private void primeClasspath(final IJavaProject javaProject) throws CoreException {
    try {
      javaProject.getResolvedClasspath(true);
    } catch (final JavaModelException exception) {
      throw new CoreException(errorStatus(
          "Could not resolve classpath for project '" + javaProject.getElementName() + "'.",
          exception));
    }
  }

  private void exportJavaProject(
      final IJavaProject javaProject,
      final Path outputFile,
      final IProgressMonitor monitor) throws Exception {

    final DiscoverJavaModelFromJavaProject discoverer = new DiscoverJavaModelFromJavaProject();
    discoverer.discoverElement(javaProject, monitor == null ? new NullProgressMonitor() : monitor);

    final Resource javaModel = discoverer.getTargetModel();
    if (javaModel == null) {
      throw new IllegalStateException(
          "MoDisco returned no model for project '" + javaProject.getElementName() + "'.");
    }

    Files.createDirectories(outputFile.getParent());
    javaModel.setURI(URI.createFileURI(outputFile.toString()));

    final Map<Object, Object> saveOptions = new HashMap<>();
    saveOptions.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
    saveOptions.put(
        XMLResource.OPTION_PROCESS_DANGLING_HREF,
        XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
    javaModel.save(saveOptions);
  }

  private IProjectDescription loadDescription(final Path projectDescriptor) throws CoreException {
    return ResourcesPlugin.getWorkspace()
        .loadProjectDescription(new org.eclipse.core.runtime.Path(projectDescriptor.toString()));
  }

  private Status errorStatus(final String message) {
    return errorStatus(message, null);
  }

  private Status errorStatus(final String message, final Throwable throwable) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message, throwable);
  }

  private String normalize(final String value) {
    if (value == null) {
      return null;
    }
    final String trimmed = value.trim();
    if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
      return null;
    }
    return trimmed;
  }

  private String formatFailure(final IJavaProject javaProject, final Throwable throwable) {
    final Throwable rootCause = rootCause(throwable);
    final String message = rootCause.getMessage();
    if (message == null || message.isBlank()) {
      return javaProject.getElementName() + ": " + rootCause.getClass().getName();
    }
    return javaProject.getElementName() + ": " + rootCause.getClass().getName() + ": " + message;
  }

  private Throwable rootCause(final Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private record ProjectCandidate(String name, Path location, boolean javaProject) {
  }
}
