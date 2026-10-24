# Zenodo upload

Steps for the author. The archive comes from `build.sh` in this folder.

1. On https://zenodo.org, create a new upload and click "Get a DOI now" to reserve the DOI before publishing. The DOI goes into the data availability statement of the paper and into `references.bib`.
2. Upload every file of the output folder of `build.sh`: `README.md`, the `.tar.gz` files and `SHA256SUMS`.
3. Fill in the form with the values below, check them, and publish.

| Field | Value |
|---|---|
| Resource type | Dataset |
| Title | Research data of "Towards Decentralized Model Persistence" |
| Creators | Alfa Yohannis, Pradita University (add the ORCID iD if there is one) |
| Description | Data, evaluation results and source code of the paper "Towards Decentralized Model Persistence", which stores Eclipse Modeling Framework models on IPFS under content identifiers. The archive holds the MoDisco models of Eclipse JDT (1.04 GiB of XMI, the merged and the partitioned model with their published CIDs), the BPMN 2.0 models of the OMG MIWG test suite and of the jBPM, Flowable and Camunda 7 test resources with the cleaning logs, the generated models of the timed benchmarks, the measurement results, and the source code of the persistence layer and of the two Eclipse BPMN2 tools. README.md describes each file and its license. |
| License | Other (Open). Each file keeps its own license, listed in README.md: MIT, Eclipse Public License 1.0 and 2.0, CC BY 3.0 and Apache 2.0. |
| Keywords | model-driven engineering; model persistence; Eclipse Modeling Framework; IPFS; content-addressed storage; BPMN |
| Related identifiers | "is supplement to" https://github.com/alfa-yohannis/ecorefs, https://github.com/alfa-yohannis/org.eclipse.bpmn2-modeler-1.5.4-ecorefs and https://github.com/alfa-yohannis/org.eclipse.bpmn2-ecorefs; later also the DOI of the published article |
| Version | 1.0 |
