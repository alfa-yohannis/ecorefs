# BPMN2 Modeler IPFS UI Integration

This checkout keeps the normal BPMN2 Modeler workflow unchanged:

- local/workspace files still load and save normally
- the editor still works on local `.bpmn` files
- Graphiti `.diagram` sidecars remain local

The customized UI bundle version is:

`1.5.4.ecorefs`

IPFS is exposed through two explicit UI actions instead:

- `BPMN2 IPFS > Open BPMN2 Model from IPFS...`
- `BPMN2 IPFS > Publish Current BPMN2 Model to IPFS...`

## What The Actions Do

`Open BPMN2 Model from IPFS...`

- accepts a `CID`, `/ipfs/...` path, `ipfs://...` URI, or `IPNS` reference
- fetches the BPMN XML through Kubo
- writes it to a temporary local `.bpmn` file
- opens that temp file in the existing BPMN2 editor flow

`Publish Current BPMN2 Model to IPFS...`

- saves the current BPMN model locally first if the editor is dirty
- uploads the current `.bpmn` model file to Kubo
<!-- EcoreFS begin: per-project IPFS/IPNS publishing behavior documentation -->
- when the project publish mode is `CID`, shows the resulting `CID`, `ipfs://...` URI, `/ipfs/...` path, and local gateway URL
- when the project publish mode is `IPNS`, publishes the new `CID` and then updates the configured IPNS key name to point to it
<!-- EcoreFS end: per-project IPFS/IPNS publishing behavior documentation -->

## Kubo Configuration

<!-- EcoreFS begin: per-project BPMN2 property page documentation for IPFS integration -->
Per-project settings live in:

`Project Properties > BPMN2`

The property page now stores:

- `Kubo API URL`
- `Default load reference`
- `Publish mode` (`CID` or `IPNS`)
- `Default IPNS publish key name`

By default, projects talk to:

`http://127.0.0.1:5001`

If the project property is left at the default value, you can still override that process-wide with either:

- JVM system property: `-Dorg.eclipse.bpmn2.modeler.ipfs.apiUrl=http://HOST:5001`
- environment variable: `BPMN2_MODELER_IPFS_API_URL=http://HOST:5001`
<!-- EcoreFS end: per-project BPMN2 property page documentation for IPFS integration -->

## Supported Input Formats

The open action accepts:

- `Qm...`
- `bafy...`
- `/ipfs/<cid>`
- `ipfs://<cid>`
- `k51...`
- `/ipns/<name-or-key>`
- `ipns://<name-or-key>`

## Caveats

- This integration publishes and reloads the BPMN model file, not the local Graphiti `.diagram` file.
- Models opened from IPFS are edited through a temporary local `.bpmn` file.
- Closing the editor cleans up that temporary model file through the existing temp-file logic.
- IPNS publishing expects a Kubo key name that already exists in the local node.

## Build

From the repository root:

```bash
mvn -f pom.xml -DskipTests package
```

If you only want to build the UI plugin reactor slice:

```bash
mvn -f pom.xml -pl plugins/org.eclipse.bpmn2.modeler.ui -am -DskipTests package
```

## Self-Tests

Fast structural checks:

```bash
tests/ipfs-ui/run-ipfs-ui-tests.sh
```

Structural checks plus a real Kubo publish/download round-trip:

```bash
tests/ipfs-ui/run-ipfs-ui-tests.sh --integration
```

These self-tests cover:

- reference normalization for `CID`, `ipfs://`, `/ipfs/...`, and `IPNS`
- plugin command and handler registration
- bundle dependency/version metadata
- a real BPMN file round-trip through Kubo when `--integration` is enabled
