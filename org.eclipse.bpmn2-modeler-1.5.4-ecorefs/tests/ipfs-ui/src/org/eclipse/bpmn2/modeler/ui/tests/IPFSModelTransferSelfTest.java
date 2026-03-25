package org.eclipse.bpmn2.modeler.ui.tests;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer;

// EcoreFS begin: standalone self-test coverage for the BPMN2 Modeler IPFS UI integration
public final class IPFSModelTransferSelfTest {

	private static final String CID_V0 = "Qmbu2ygFf6afXUGiCQA4XigYNCUGHfYWyy7Kq2feBbrmjr"; //$NON-NLS-1$
	private static final String CID_V1 = "bafkreihfghwnguzrukv42vo5s45f2lbmm5p5i3bdzxo3n25ua2hdjcrqtu"; //$NON-NLS-1$
	private static final String IPNS_KEY = "k51qzi5uqu5dhr6ajtiqw195ua6t6oxzomdwdk3kaitjtt0kawac51cazg3yro"; //$NON-NLS-1$

	private IPFSModelTransferSelfTest() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new IllegalArgumentException("Expected the repository root as the first argument."); //$NON-NLS-1$
		}

		Path repoRoot = Path.of(args[0]).toAbsolutePath().normalize();
		boolean integration = contains(args, "--integration"); //$NON-NLS-1$

		testNormalizeReferenceSupportsCidAndIpnsForms();
		testNormalizeReferenceRejectsInvalidInput();
		testPluginXmlRegistersIpfsCommands(repoRoot);
		testManifestIncludesFilesystemDependencyAndQualifierVersion(repoRoot);
		testProjectPropertiesExposeIpfsSettings(repoRoot);
		testOpenCommandPersistsIntoWorkspaceWhenPossible(repoRoot);
		testProjectManifestCommandsArePresent(repoRoot);
		testDocumentationMentionsIpfsActions(repoRoot);
		testPublishResultDialogIsPresent(repoRoot);

		if (integration) {
			testKuboRoundTrip(repoRoot);
		}

		System.out.println("IPFS UI self-tests passed."); //$NON-NLS-1$
		if (integration) {
			System.out.println("IPFS UI integration round-trip passed."); //$NON-NLS-1$
		}
	}

	private static void testNormalizeReferenceSupportsCidAndIpnsForms() {
		assertEquals("/ipfs/" + CID_V0, IPFSModelTransfer.normalizeReference(CID_V0), "CIDv0 should normalize to /ipfs"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("/ipfs/" + CID_V1, IPFSModelTransfer.normalizeReference(CID_V1), "CIDv1 should normalize to /ipfs"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("/ipfs/" + CID_V1, IPFSModelTransfer.normalizeReference("ipfs://" + CID_V1), "ipfs URI should normalize"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("/ipfs/" + CID_V1, IPFSModelTransfer.normalizeReference("/ipfs/" + CID_V1), "ipfs path should stay stable"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("/ipns/" + IPNS_KEY, IPFSModelTransfer.normalizeReference(IPNS_KEY), "IPNS key should normalize to /ipns"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("/ipns/" + IPNS_KEY, IPFSModelTransfer.normalizeReference("ipns://" + IPNS_KEY), "ipns URI should normalize"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testNormalizeReferenceRejectsInvalidInput() {
		assertThrows("not-a-cid"); //$NON-NLS-1$
		assertThrows("   "); //$NON-NLS-1$
		assertThrows(null);
	}

	private static void testPluginXmlRegistersIpfsCommands(Path repoRoot) throws Exception {
		String pluginXml = read(repoRoot.resolve("plugins/org.eclipse.bpmn2.modeler.ui/plugin.xml")); //$NON-NLS-1$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.command.openFromIpfs", "plugin.xml should declare the open command"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.command.publishModelToIpfs", "plugin.xml should declare the publish command"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.command.openProjectFromIpfs", "plugin.xml should declare the project open command"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.command.publishProjectToIpfs", "plugin.xml should declare the project publish command"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.ui.commands.IPFSOpenModelCommand", "plugin.xml should register the open handler"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.ui.commands.IPFSPublishModelCommand", "plugin.xml should register the publish handler"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.ui.commands.IPFSOpenProjectCommand", "plugin.xml should register the project open handler"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(pluginXml, "org.eclipse.bpmn2.modeler.ui.commands.IPFSPublishProjectCommand", "plugin.xml should register the project publish handler"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testManifestIncludesFilesystemDependencyAndQualifierVersion(Path repoRoot) throws Exception {
		String manifest = read(repoRoot.resolve("plugins/org.eclipse.bpmn2.modeler.ui/META-INF/MANIFEST.MF")); //$NON-NLS-1$
		assertContains(manifest, "Require-Bundle: org.eclipse.ui.ide,", "manifest should still require the UI IDE bundle"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(manifest, " org.eclipse.core.filesystem,", "manifest should require org.eclipse.core.filesystem"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(manifest, "Bundle-Version: 1.5.4.qualifier", "manifest should keep the qualifier version for Eclipse launches"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testDocumentationMentionsIpfsActions(Path repoRoot) throws Exception {
		String doc = read(repoRoot.resolve("IPFS-UI.md")); //$NON-NLS-1$
		assertContains(doc, "Open BPMN2 Model from IPFS", "documentation should mention the open action"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "Publish Current BPMN2 Model to IPFS", "documentation should mention the publish action"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "Open BPMN2 Project from IPFS", "documentation should mention the project open action"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "Publish BPMN2 Project to IPFS", "documentation should mention the project publish action"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "Project Properties > BPMN2", "documentation should mention the per-project settings location"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "1.5.4.qualifier", "documentation should mention the qualifier bundle version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "selected workspace folder", "documentation should mention workspace persistence"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(doc, "JSON manifest", "documentation should mention the project manifest"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testProjectPropertiesExposeIpfsSettings(Path repoRoot) throws Exception {
		String propertyPage = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/preferences/Bpmn2PropertyPage.java")); //$NON-NLS-1$
		assertContains(propertyPage, "PREF_IPFS_API_URL", "property page should expose the Kubo API URL"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(propertyPage, "PREF_IPFS_DEFAULT_LOAD_REFERENCE", "property page should expose the default load reference"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(propertyPage, "PREF_IPFS_PUBLISH_MODE", "property page should expose the publish mode"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(propertyPage, "PREF_IPFS_DEFAULT_IPNS_KEY", "property page should expose the default IPNS key"); //$NON-NLS-1$ //$NON-NLS-2$

		String preferences = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.core/src/org/eclipse/bpmn2/modeler/core/preferences/Bpmn2Preferences.java")); //$NON-NLS-1$
		assertContains(preferences, "PREF_IPFS_API_URL", "preferences should define the Kubo API URL key"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(preferences, "PREF_IPFS_PUBLISH_MODE_IPNS", "preferences should define the IPNS publish mode"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testOpenCommandPersistsIntoWorkspaceWhenPossible(Path repoRoot) throws Exception {
		String command = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/commands/IPFSOpenModelCommand.java")); //$NON-NLS-1$
		assertContains(command, "resolveTargetContainer", "open command should resolve an active workspace container"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(command, "downloadToWorkspaceFile", "open command should persist downloaded files into the workspace"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(command, "IDE.openEditor(page, targetFile)", "open command should open persisted workspace files"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testProjectManifestCommandsArePresent(Path repoRoot) throws Exception {
		String openProject = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/commands/IPFSOpenProjectCommand.java")); //$NON-NLS-1$
		assertContains(openProject, "IPFSProjectManifest", "project open should consume the BPMN project manifest"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(openProject, "createUniqueFolder", "project open should restore into a workspace folder"); //$NON-NLS-1$ //$NON-NLS-2$

		String publishProject = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/commands/IPFSPublishProjectCommand.java")); //$NON-NLS-1$
		assertContains(publishProject, "collectBpmnFiles", "project publish should collect BPMN files recursively"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains(publishProject, "IPFSProjectManifest", "project publish should emit the BPMN project manifest"); //$NON-NLS-1$ //$NON-NLS-2$

		String manifest = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/util/IPFSProjectManifest.java")); //$NON-NLS-1$
		assertContains(manifest, "bpmn-project-manifest", "manifest helper should define the manifest kind"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void testPublishResultDialogIsPresent(Path repoRoot) throws Exception {
		String dialog = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/commands/IPFSPublicationResultDialog.java")); //$NON-NLS-1$
		assertContains(dialog, "Messages.IPFS_Publish_Result_Copy_Button",
				"publication dialog should expose a copy action"); //$NON-NLS-1$
		assertContains(dialog, "SWT.MULTI", "publication dialog should use a multiline text area"); //$NON-NLS-1$ //$NON-NLS-2$

		String command = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/commands/IPFSPublishModelCommand.java")); //$NON-NLS-1$
		assertContains(command, "IPFSPublicationResultDialog", "publish command should use the custom result dialog"); //$NON-NLS-1$ //$NON-NLS-2$

		String messages = read(repoRoot.resolve(
				"plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/commands/messages.properties")); //$NON-NLS-1$
		assertContains(messages, "IPFS_Publish_Result_Copy_Button=Copy All",
				"messages should define the copy button label"); //$NON-NLS-1$
	}

	private static void testKuboRoundTrip(Path repoRoot) throws Exception {
		Path sample = repoRoot.resolve(
				"examples/plugins/org.eclipse.bpmn2.modeler.examples.dynamic/bpmnResources/VacationRequest.bpmn"); //$NON-NLS-1$
		File downloaded = null;
		try {
			IPFSModelTransfer.PublicationResult result = IPFSModelTransfer.publish(sample.toFile());
			downloaded = IPFSModelTransfer.downloadToTempModel(result.getCid());
			byte[] originalBytes = Files.readAllBytes(sample);
			byte[] downloadedBytes = Files.readAllBytes(downloaded.toPath());
			if (!Arrays.equals(originalBytes, downloadedBytes)) {
				throw new AssertionError("Downloaded BPMN bytes do not match the published source."); //$NON-NLS-1$
			}
		} finally {
			if (downloaded != null && downloaded.exists()) {
				downloaded.delete();
			}
		}
	}

	private static boolean contains(String[] args, String expected) {
		for (String arg : args) {
			if (expected.equals(arg)) {
				return true;
			}
		}
		return false;
	}

	private static String read(Path path) throws Exception {
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	private static void assertContains(String text, String expected, String message) {
		if (!text.contains(expected)) {
			throw new AssertionError(message + " Missing: " + expected); //$NON-NLS-1$
		}
	}

	private static void assertEquals(String expected, String actual, String message) {
		if (!expected.equals(actual)) {
			throw new AssertionError(message + " Expected '" + expected + "' but was '" + actual + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private static void assertThrows(String input) {
		try {
			IPFSModelTransfer.normalizeReference(input);
		} catch (IllegalArgumentException e) {
			return;
		}
		throw new AssertionError("Expected IllegalArgumentException for input: " + input); //$NON-NLS-1$
	}
}
// EcoreFS end: standalone self-test coverage for the BPMN2 Modeler IPFS UI integration
