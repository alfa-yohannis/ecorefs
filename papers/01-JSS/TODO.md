# Revision plan for a Q1 journal

Plan for `paper/main.tex` (15 pages in the JSS `elsarticle` layout on 2026-10-22, up from 20 in the SoSyM template). It merges a review of the draft with the open manuscript items of the ecorefs README; the README keeps the code items. Priority: P1 must happen before submission, P2 should, P3 is optional. Effort: S is about a day, M a few days, L one to two weeks.

**Where the work stopped (2026-10-23, early afternoon).**
- **Paper text written.** `main.tex` now reports the experiments on real BPMN models (RQ1 round trip with `tab:rq1-roundtrip`, RQ2 cascades with `tab:rq2-real`, the MoDisco cycle), the baseline comparison (`tab:baselines`) and the comparison with Git and model repositories (`tab:comparison`, Section 8.2).
  - The contributions, the abstract (248 words), the highlights, the conclusion, the related work and the threats to validity follow the findings.
  - The paper compiles without warnings to 18 pages: the highlights page and 17 article pages.
- **Three `\todo` notes left on purpose.** They cover the RQ3 rerun, the multinode results and the data DOI.
- **Next steps, in order:**
  1. The timed runs in one quiet window (Section 3): idle machine, ideally the performance power profile.
  2. Their numbers in RQ3, the abstract and the highlights.
  3. The dependency scan fix and `BpmnIpfsExample` (Section 4).
  4. The README updates (Section 4).
  5. The Zenodo archive once the author has the DOI.

## 0. Move to the Journal of Systems and Software

Decided on 2026-10-22: the paper goes to JSS (Elsevier) and SoSyM is dropped. JSS fits better than IST, whose scope centers on improving development practice and on systematic reviews. JSS is a hybrid journal ("Supports open access"), so the subscription route costs nothing. Its page showed CiteScore 10.1 and Impact Factor 3.8.

Converted on 2026-10-22. `paper/main.tex` compiles without errors, overfull boxes or undefined references, to 15 pages: an unnumbered highlights page plus 14 article pages. Commit 17368c0 holds the conversion.

- [x] Official template: `elsarticle.zip` (April 2024) from the Elsevier LaTeX instructions page (https://www.elsevier.com/researcher/author/policies-and-guidelines/latex-instructions), kept unmodified in `template/`. TeX Live on this machine has no elsarticle, so `elsarticle.cls`, generated with `latex -no-shell-escape elsarticle.ins`, sits next to `main.tex` together with `elsarticle-num.bst`.
- [x] Folder renamed from `papers/01-SoSym` to `papers/01-JSS`. The Springer class, style and template are removed, and the `.gitignore` path is updated.
- [x] Layout `[5p,times,twocolumn]`, which keeps the figures at the size they were drawn for. The footer still reads "Preprint submitted to Journal of Systems and Software".
- [x] Front matter, keywords, `\graphicspath{{figures/}}` with bare figure names, `\bottomrule`, the `definition` theorem style and `elsarticle-num`. The RQ3 tables keep their `\fontsize{8bp}` and fit the column.
- [x] End matter in the order of the guide: CRediT statement, declaration of competing interest, the prescribed funding sentence, the generative AI declaration in the guide's wording, data availability with the code links, and acknowledgements directly before the references. The LLM paragraph moved from the Research method section into the AI declaration. The Springer ethics and consent statements are gone.
- [x] Highlights: five bullets of 71 to 84 characters, in `paper/highlights.txt` for Editorial Manager and in the template's `highlights` environment.
- [x] Abstract at 241 words, six keywords.
- [x] Length: 14 article pages against the 18-page guideline, which leaves about three pages for the planned experiments.
- [x] SoSyM replaced in the README, `literature-review.md`, the bibliography header and the comments of `main.tex`.

Still open:

- [x] P1 S. Confirm the CRediT roles. On 2026-10-23 the statement got Formal analysis, Data curation and Visualization in addition to the earlier roles, and the `\todo` is gone.
- [x] P1 S. State whether an AI tool helped draft or edit the text. The declaration now says that Claude Code also drafted and edited text of the manuscript (the author's choice on 2026-10-23).
- [x] P1 S. Write a biography of at most 100 words for each author, in an editable file: `biography.txt` (90 words), drafted from the PDDikti record for the author to check.
- [ ] P1 S. Research data under Elsevier's "Option C", which JSS applies: deposit the data in a repository, then cite and link the dataset in the paper (`\todo` in the data availability section), or state why it cannot be shared. This makes the Zenodo item in Section 4 mandatory.
- [ ] P1 S. Prepare the upload for Editorial Manager, which accepts no subfolders: copy the figures from `figures/`, `elsarticle.cls`, `elsarticle-num.bst` and `references.bib` next to `main.tex` in a submission folder, compile there, and add the PDF and `highlights.txt`. Review is single anonymized, so author names and repository links can stay.
- [x] P2 S. Add the page range of `quinlan2002venti`. Done on 2026-10-23: pages 89–102, from the HTML version of the paper on the USENIX site ("Pp. 89–102 of the Proceedings"), with the proceedings title "Venti: a new approach to archival storage". BibTeX now warns only about `harer2018decentralized`, an ECIS paper without page numbers.
- [x] P2 S. Decide whether the proposal and the talk in `proposals/01-decentralized-model-persistence/` should name JSS instead of SoSyM. Done on 2026-10-23: both name JSS, and their figure paths point to `papers/01-JSS` again (the move had broken them). Both PDFs are rebuilt; the talk needs XeLaTeX.
- [ ] P2 S. After adding the planned experiments, check that the paper stays under 18 pages, or justify the length in the submission. On 2026-10-23 the paper reached 19 pages with the new results. Three cuts brought it back to 18 (17 article pages): a shorter paragraph on uses in the introduction, one paragraph for the cascade unit tests instead of two, and no EMF Compare scaling figure. The multinode results will need about half a page more.

Venue background, kept for reference: third-party sites report SoSyM's 2024 SJR as 0.754 with Q1 as the best quartile. Scimago and ScienceDirect block automated requests (HTTP 403 and a Cloudflare check). The JSS and IST pages loaded in Chrome with `--headless=new` and a scratch profile, while Scimago did not.

## 1. Contribution and positioning

- [x] P1 M. Add a short formal model of content-addressed references: resources, versions, the reference graph and a definition of a consistent snapshot, where every reference names the current CID of its target. State and prove three propositions: the cascade re-saves exactly the resources that reach the changed one, the dependency order leaves every reference current, and a cycle never reaches a consistent snapshot in CID mode. The `definition` environment is already declared and unused. Done on 2026-10-23 in Section 5.4 (`sec:rq2-properties`): two definitions, Propositions 1 and 2 with proofs, and Proposition 3 with a proof sketch under a random-function model of the hash.
- [x] P1 M. Answer "why not Git?" in the paper. Compare Git, CDO, NeoEMF, EMFStore and the IPFS layer on identity, version pinning, references across repositories and organizations, partial loading, integrity checks and availability. Done on 2026-10-23 as a separate table, `tab:comparison`, in the new Section 8.2 "Comparison with Git and model repositories". The text names the main difference: a Git reference between model files is a path that resolves to the checked-out version, while a CID pins the version inside the reference.
- [ ] P1 M. Separate the challenges specific to EMF from general ones (README item, RQ1 sub-question). Go through MPS, Epsilon model drivers, EMF.cloud or Sirius Web, and JSON model stores against the four challenges of Section 1. Otherwise narrow RQ1 to EMF. Searches on 2026-10-23 found no reliable source on how MPS and Sirius Web store references, so narrowing RQ1 to EMF is the safer route.
  - **Done:** the RQ1 answer now separates the two challenges that the round trip found. Storing every model is a matter of EMF and XMI, since local files failed in the same way. Writing the same bytes for the same model holds for content addressing in general.
  - **Open:** the wording of RQ1 itself, which still says "model persistence frameworks".
- [x] P1 S. Rewrite the contribution list around findings rather than artifacts: the cascade properties, the serialization finding and the determinism finding of Section 2 below. Done on 2026-10-23.

## 2. Evaluation with real BPMN models

The models are in `eval/datasets/bpmn` (cleaned) and `eval/datasets/bpmn-backup-2026-10-22.tar.gz` (originals); see the README there.

The drivers are `eval/bpmn-rq1` and `eval/bpmn-rq2`, with shared classes in `eval/bpmn-common`. The results and their READMEs are in `eval/results/bpmn-rq1`, `eval/results/bpmn-rq2` and `eval/results/modisco-cascade`. Every run used its own offline Kubo container. Since 2026-10-23, `main.tex` reports every item below:
- the setup of the five collections in `sec:evaluation-setup`;
- the round trip and the determinism in `sec:evaluation-rq1`;
- the split, the call graphs and the MoDisco cycle in `sec:evaluation-rq2`;
- the sizes, the throughput and the deduplication in `sec:evaluation-rq3`.

- [x] P1 S. Unpack the originals into `eval/datasets/bpmn-original`. Use them for RQ1 and RQ3, and the cleaned set for RQ2. Done: 11,533 files, which is the 11,518 of the cleaned set plus the 15 that the cleaning removed. 11,486 of them load into a model.
- [x] P1 M. RQ1 round trip on all original models: save each through `IPFSResourceImpl`, load it by CID in a fresh resource set, compare with the original, and run the local XMI path on the same model as the paired baseline. Run once with the current code, then with `XMLResource.OPTION_EXTENDED_META_DATA`. Report both runs. Done on 2026-10-23 with seven paths per model, BPMN 2.0 XML included:
  - **Local and IPFS agree.** Each local path and its IPFS counterpart gave the same outcome for all 11,486 models.
  - **Plain XMI:** 52.1% of the saves fail with a `NullPointerException` on vendor extension elements, and loads lose vendor attributes. 22.5% round-trip unchanged.
  - **Extended metadata:** every model but two saves, and none loads back. The XMI loader builds a document root only for packages it creates on demand, so "Feature 'definitions' not found". Setting the option in `IPFSResourceImpl` is therefore no fix.
  - **BPMN 2.0 XML:** 92.2% round-trip unchanged. The rest lose dangling references (4.7%) and some other references in the Eclipse BPMN serializer, on the local path just as on IPFS.
- [x] P1 S. RQ1 determinism: save every model twice in separate resource sets and compare the CIDs, on the XMI path and on the BPMN XML path. Done:
  - **XMI:** never a new CID.
  - **BPMN XML:** all 9,634 models with an object without an id got a new CID on the second save, because `Bpmn2ResourceImpl.prepareSave` assigns random UUIDs. None of the 1,850 models without such objects did.
  - **Fix:** numbering the missing ids in document order gave the same CID for every model. The modeler needs no change: its resource numbers new ids (`ModelUtil.setID`), and its publish command adds the bytes of the saved file.
- [x] P1 M. RQ2 on real structures. Done: the 1,855 models with two or more pools were split into one resource per process, one for the collaborations with the diagrams and one for the other root elements, and two changes ran per model.
  - **Acyclic splits (1,749):** every cascade gave new CIDs to exactly the affected set A(t), left every stored reference current and loaded fully by CID, in line with Propositions 1 and 2.
  - **Cyclic splits (106, 5.7%):** never consistent, as Proposition 3 predicts. 67 have flows that cross pools; in 39 a process refers back to the collaboration.
  - **Proxies after a load:** five acyclic splits keep a proxy after the load, because `SequenceFlow.targetRef` and `DataAssociation.sourceRef` do not resolve proxies.
- [x] P1 M. RQ2 on real dependency graphs. Done: `resolve_called_elements.py` resolved 421 of the 616 removed cross-file `calledElement` values (not 503), giving 168 components of 538 files, with chains of up to five files and a fan-in of up to 30.
  - **Acyclic changes:** all 536 were consistent and loaded fully.
  - **Cyclic component:** the one cyclic component of two files stays stale.
  - **Cost:** about 8 ms per new CID.
- [x] P1 M. Cascade on the partitioned MoDisco model (README item). Done as an analysis of the 2.39 million serialized hrefs (`eval/modisco-cascade/resource_graph.py`):
  - **One cycle:** all 6,934 resources form one strongly connected component, so every change affects every resource and CID mode can never be consistent.
  - **Layered publication:** needs 9 to 11 new objects per change instead.
  - **Manifests:** they list only the 92 component-to-shared edges and miss the cycle through the root model.
  - **Still open:** the time of the dependency scan of the core on the loaded model was not measured.
- [x] P2 S. RQ3 on real sizes: per-model save and load times over all files, then one save per model against publishing a whole collection as one IPFS directory, in models per second.
  - **Done:** one add per model ran at 166 models per second, one directory per collection at about 1,000. The paper also gives the file sizes (median 1.2 to 33 KB per collection) and the 7PMG guideline as the size context.
  - **Not reported:** the per-model times of the round trip stay out of the paper, because five virtual machines ran in parallel.
- [x] P2 S. Deduplication: compare the growth of the Kubo repository with plain file storage. Done: 1,140 of the 11,533 files share the CID of an earlier file, and the distinct contents hold 95.4% of the bytes. Kubo still grew by 207.1 MB for 207.4 MB of files, and the block folder took 269.1 MB on disk against 234.7 MB for a plain copy in 4 KiB blocks.

## 3. Decentralized setting and baselines

- [ ] P1 M. Measure more than one node (README item) without renting machines: several Kubo containers on one host, with `tc netem` delays for LAN and WAN profiles. Measure a fetch by CID from a node that has never seen the content, and IPNS publish and resolve times. If possible, add one node on another network, such as a campus machine, for a run through the public DHT.
  - **Harness:** ready in `eval/multinode`. Four Kubo nodes run in their own private network, in two topologies (peered and DHT), with four delay profiles (none, LAN, WAN 50 ms, WAN 100 ms), as described in its README.
  - **Validation:** it fixed jitter reordering, local IPNS resolution, warm fetches and the reconnect policy, which belong in the method text.
  - **Final run:** started on 2026-10-23 at 11:10 after the baselines run. The machine rebooted at about 11:29 and cut it off after part of the peered topology. The partial results were not kept in the repository. Rerun in the quiet window below, with `HEAP=4g` if memory is short; the author of the harness found 4 GB enough.
  - **Public DHT:** the node on the public network is left out, because the experiment nodes stay off the global network.
  - **Paper:** the results go where the `\todo` at the end of `sec:evaluation-rq3` sits.
- [ ] P1 M. Baselines on the same models (README item): a Git repository (commit, push and clone), a shared network file system, and at least one database-backed store, NeoEMF with an embedded back end being the cheapest to set up.
  - **Harness:** ready in `eval/baselines`, with Git through `git daemon`, a share through sshfs and NeoEMF 2.0.0 with MapDB. NeoEMF needs JDK 17, so every store runs on JDK 17.
  - **Final run:** done on 2026-10-23 from 10:38 to 11:10, all five sizes. The results are in `eval/results/baselines/final-summary.md` and `final-environment.txt`; `final.csv` stays out of git with every CSV. `tab:baselines` and the text of `sec:evaluation-rq3` report the run.
  - **Rerun:** once more in the quiet window below, so all timing tables come from the same conditions.
- [ ] P1 S. Rerun every timing experiment in one quiet window, decided by the author on 2026-10-23. Close the other programs and sessions, and use the performance power profile if possible. The runs:
  - **RQ3 benchmark (about 3 minutes).** Start a fresh offline node on `127.0.0.1:5001` (stop `ecorefs-kubo` first), then run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 .tools/apache-maven-3.9.10/bin/mvn -q -f core-plugin/pom.xml test -Dtest=FastPerformanceBenchmarkTest -Djvm=/usr/lib/jvm/java-21-openjdk-amd64/bin/java -DargLine="-Xms8g -Xmx8g"`. Run Maven online, because offline mode rejects a plugin dependency that `~/.m2` already holds.
  - **Baselines (about 35 minutes):** `bash eval/baselines/run.sh final`, after moving the earlier `final.*` files away.
  - **Multinode (about 3.5 h):** `bash eval/multinode/run.sh`.

  Then run `eval/analyse_benchmark.py`, redraw `rq3-performance`, and update `tab:rq3-save`, `tab:rq3-load`, the effect sizes, the threshold, the abstract, the highlights and the conclusion. The RQ3 test writes over `eval/results/benchmark_metrics.csv`, which holds the September run.

  Why the rerun is needed: a rerun on 2026-10-23, at a load average of about 2 with other sessions active and the balanced power profile, gave mixed results.
  - **IPFS fixed cost:** a save through IPFS took 8.0 ms at 100 elements instead of 21.2 ms in September, in line with the baseline run (9.9 ms), the RQ1 round trip and the RQ2 cascades (about 8 to 10 ms).
  - **CPU-bound work:** the XMI times at 100,000 elements doubled over the whole distribution (minimum 56.8 ms against 28.5 ms).
  - **Node:** the September node ran the same Kubo 0.40.1 image on a Docker volume, so the node does not explain the difference.

  The busy rerun was not kept, and the paper keeps the September numbers until the quiet rerun.
- [x] P2 S. Report effect sizes and confidence intervals next to the Wilcoxon tests, and state the practicality threshold explicitly. The README defines it as the model size at which IPFS takes more than five times as long as local I/O. Done on 2026-10-23: `eval/analyse_benchmark.py` adds the matched-pairs rank-biserial correlation and paired bootstrap intervals (10,000 resamples, fixed seed) to `benchmark_summary.md`, and finds the five-times threshold for saves at about 25,400 elements; loads never reach it. The RQ3 text reports both.
- [ ] P3 L. Evaluate the tool support with a scripted editing workflow or a small user study (README item).

## 4. Manuscript items already flagged

- [x] P1 S. Report the output of `BpmnIpfsExample` and of the modeler self-tests with a running Kubo node (`\todo` in `sec:evaluation-rq1`), or let the real-model round trip replace the example. The modeler self-tests pass with `--integration` against Kubo 0.40.1 (2026-10-22). Done on 2026-10-23: the round trip of the real models replaced the example program in `sec:evaluation-rq1`, and the `\todo` is gone.
- [x] P1 S. Fix `examples/BpmnIpfsExample.java`, which prints hard-coded CIDs and a `creditCheckRef` that does not exist, and loads `.bpmn` files with the XMI factory. Done on 2026-10-23:
  - **Loading:** the example loads the two models with the BPMN resource of Eclipse.
  - **Real reference:** the order model gains a collaboration whose bank participant runs the loan process, a reference across the two resources.
  - **The three saves:** `save`, `saveIPFS` and `saveIPFSCascaded`, followed by a load by CID that resolves the reference. Every printed CID is real.
  - **Input file:** `order-fulfillment.bpmn` lost an XMI-style `supports href` with a made-up CID, which BPMN XML cannot express.
  - **Running:** `bash examples/run-bpmn-ipfs-example.sh` compiles and runs the example against the node on port 5001.
- [x] P1 S. Separate the normal save from the publication on IPFS (the author's request on 2026-10-23). Done:
  - **`save()`** of `IPFSResourceImpl` is the normal EMF save to the location of the URI.
  - **`saveIPFS()`** publishes on IPFS and sets the URI to `ipfs://<CID>` (or `ipns://`) as before.
  - **`saveIPFSCascaded()`** runs the cascade of `IPFSModelPersister`, which now publishes IPFS resources with `saveIPFS()`.
  - **A normal save to an `ipfs://` URI** fails with a message that names `saveIPFS()`.
  - **Callers moved:** the core tests, the publish tool, the evaluation drivers (bpmn-rq1, bpmn-rq2, baselines, multinode), the tutorial, the README, the paper text and four figures (architecture, save, lifecycle, version pinning).
  - **New test:** `IPFSResourceSaveModesTest` checks both save modes without a node.
  - **Checks:** the core tests pass, and reruns of the call graphs (538 changes), 200 split models and 30 round-trip models give the same outcomes as before.
- [x] P1 S. Let the modeler carry a change through imports by CID (the author's request on 2026-10-23). Done in the modeler fork:
  - **Publish with cascade:** a new command, **Publish Current BPMN2 Model to IPFS with Cascade**, publishes the current model and gives every model that imports it by CID a new version. It finds them among the BPMN files of the project and among the versions those files pin on IPFS. Only import locations change, and the editors of rewritten files are reopened.
  - **Namespace folders:** **Open BPMN2 Model from IPFS** stores a model in the folder of its target namespace, for example `ecorefs.org/case-study/payment-process/payment-process.bpmn`, so each model has one working copy per project.
  - **Refusals:** the cascade stops before it changes anything when two files declare the namespace of the published model, when a pinned version differs from the working copy of its model, or when models import each other.
  - **Checks:** `IPFSImportCascadeSelfTest`, with an in-memory store and on Kubo, and a run of the case study in the modeler on a nested display.
  - **Menu:** short labels, because the menu is called BPMN2 IPFS: Open Model, Open Imported Model, Publish Model and Publish Model with Cascade, then a separator, Open Project and Publish Project. **Open Imported Model** downloads a model that the current model imports, for editing. **Publish Project** publishes the models of a folder as one consistent set, in dependency order and with their imports of each other updated. The cascade stays a command of its own, next to the plain publication, like `saveIPFSCascaded` next to `saveIPFS`.
  - **Tutorial:** Parts 2 to 5 describe the commands. Their screenshots were retaken without eclipse-mcp, so no MCP menu shows; `eclipse-cascade-case-study.png`, a screenshot of the IDE running the JUnit test, still shows one.
  - **Paper:** `sec:tool-modeler` now describes the command and the namespace folders, and the paper stays at 18 pages.
- [ ] P1 S. Fix the dependency scan of the cascade (`ResourceDependencyGraph`), found by the MoDisco analysis. It has two gaps with cross-resource containment, which the MoDisco partition uses for `Model.compilationUnits`:
  - It skips containment references, although a containment proxy stores the URI of its target like any other reference.
  - `resource.getAllContents()` walks into children that live in other resources and credits their references to the container's resource.

  The fix: walk `EcoreUtil.getAllProperContents`, count containment references whose child lies in another resource, add a test, and update the algorithm text of `sec:rq2-cascade`.
- [x] P1 S. Rerun the BPMN case study after the three files gained BPMN imports, the order process gained a send task and a message event, and the collaboration gained message flows and a diagram (2026-10-22). The stored import locations now carry CIDs as well. The order process imports the payment process instead of declaring `supports`, which the BPMN standard reserves for a process whose every run also follows the steps of the other process. Figure `fig:cascade` and the 50 ms in `sec:evaluation-rq2` come from that run, and the paragraph names the imports and the message flows.
- [x] P2 M. Build the modeler with Tycho against `releases/2022-06` (`\todo` in `sec:tool-editor`). Done on 2026-10-23: `mvn -B -P platform-2022-06 clean verify` builds all 18 modules in about 7 minutes and produces the update site. `parent/pom.xml` needed four new repository addresses (BPMN2, Graphiti, Web Tools, Orbit). The `\todo` in `sec:tool-editor` is gone.
- [ ] P1 M. Archive code, scripts and data under a DOI on Zenodo: the MoDisco dataset (1.04 GiB) with the published CIDs (`\todo`, Data availability), plus the MIWG and engine BPMN sets with the change logs. Cite the Camunda set by repository and commit only, since its terms do not cover redistribution.
- [ ] P1 S. Remove every `\todo` before submission. Three remain on 2026-10-23, all on purpose: the RQ3 rerun (`sec:evaluation-rq3`), the multinode results (end of `sec:evaluation-rq3`) and the data DOI (data availability).
- [ ] P2 S. Update the ecorefs README:
  - Its benchmark row still describes 100 to 10,000 elements with 10 measured iterations.
  - Its comparison section says that no benchmark against CDO or NeoEMF has been run.
  - It should point to the new results in `eval/results`.

## 5. Space

The new material needs about three pages, which these cuts should free.

- [x] P2 S. Merge the save and load sequence diagrams (`fig:save` and `fig:load`) into one figure, or drop the load diagram. The load diagram is dropped; the text of the load subsection covers the same steps.
- [x] P2 S. Replace the six bullets on uses in the introduction with one paragraph, since they stay outside the evaluation.
- [x] P2 S. Cut the tool section to about half a page and leave the command details to the repository. The command table is gone, and two wrong statements were fixed: the layout sits in the BPMN file as diagram interchange data, and opening a project can create a new project.
- [x] P2 S. Shorten the manifest listing (`lst:manifests`) to the project version manifest. The listing now fits one column.
- [x] P2 S. Shorten the conclusion, which repeats the answers to the research questions (`sec:answers`) almost sentence by sentence. Done on 2026-10-23: one paragraph of findings and two sentences of future work.
- [x] P2 S. Move the funding sentence out of the Acknowledgements into limitations or future work. The sentence closes the limitations, and the empty Acknowledgements section is removed.
- [x] P2 S. Rewrite the abstract around the findings instead of the measurements alone. Done on 2026-10-23 (248 words); the last sentence carries the RQ3 numbers, which change with the quiet rerun.

## 6. Related work and validity

- [x] P2 M. Position the paper against decentralized model collaboration outside MDE and recheck the claim that no approach stores EMF models on content-addressed storage (in the introduction and in `sec:related-content`). Done on 2026-10-23: a paragraph in `sec:related-content` cites the three works, and the claim stands. The three candidates exist as described:
  - F. Härer, "Decentralized Business Process Modeling and Instance Tracking Secured by a Blockchain", ECIS 2018 (AISeL ecis2018_rp/55): model versioning on a blockchain for decentralized enterprise models.
  - G. Falazi et al., "Blockchain-based Collaborative Development of Application Deployment Models", OTM 2018 (CoopIS), LNCS 11229, pp. 40–60, DOI 10.1007/978-3-030-02610-3_3: intermediate versions of a shared deployment model on public blockchains and decentralized storage. The co-author list still needs checking.
  - X. Tao, M. Das, Y. Liu, J.C.P. Cheng, "Distributed common data environment using blockchain and Interplanetary File System for secure BIM-based collaborative design", Automation in Construction 130 (2021) 103851.

  Related: Härer and Fill, decentralized attestation of conceptual models on Ethereum (IEEE CBI 2019). The searches found no work that stores EMF models on content-addressed storage, apart from ecorefs itself, so the claim stands.
- [x] P2 S. Add Git-based model management and model federation if the comparison table needs them. Done on 2026-10-23: `tab:comparison` covers Git, and `sec:related-versioning` notes that EMF Compare also compares and merges models in a Git repository. Model federation needed no entry.
- [x] P2 S. Cite the BPMN sources (MIWG test suite, Camunda bpmn-for-research) and the size context. Done on 2026-10-23:
  - **Citations:** the setup cites all five collections by repository and commit, `sec:evaluation-rq3` cites 7PMG for the 50-element guideline, and the threats cite Türker et al. for the GitHub collection that the evaluation left out because of missing licenses.
  - **Sources of the entries:**
  - J. Türker, M. Völske, T.S. Heinze, "BPMN in the Wild: A Reprise", ZEUS 2022 (Bamberg, February 24–25), CEUR-WS Vol-3113, pp. 68–75. An earlier "BPMN in the Wild" by Heinze, Stefanko and Amme appeared at ZEUS 2020, CEUR-WS Vol-2575, pp. 26–29.
  - J. Mendling, H.A. Reijers, W.M.P. van der Aalst, "Seven process modeling guidelines (7PMG)", Information and Software Technology 52(2), 127–136, 2010. The DOI is unconfirmed.
  - The MIWG and Camunda collections by repository and commit, as in `eval/datasets/bpmn/README.md`.

  The real models are small: the largest of the five collections has 192 flow elements.
- [x] P2 S. Update the threats to validity: a second real metamodel from five sources and the emulated network widen external validity. The cleaning becomes a threat for any result on the cleaned set, since it edited 16% of the files and removed engine semantics. Done on 2026-10-23:
  - **Internal validity:** the parallel round trip, the cleaned set of RQ2, the removed vendor extensions and the restored calls.
  - **External validity:** the five sources and the small size of the real models.
  - **Construct validity:** the structural comparison, which skips derived and transient features.
