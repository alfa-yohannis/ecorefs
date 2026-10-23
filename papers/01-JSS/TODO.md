# Revision plan for a Q1 journal

Plan for `paper/main.tex` (15 pages in the JSS `elsarticle` layout on 2026-10-22, up from 20 in the SoSyM template). It merges a review of the draft with the open manuscript items of the ecorefs README; the README keeps the code items. Priority: P1 must happen before submission, P2 should, P3 is optional. Effort: S is about a day, M a few days, L one to two weeks.

**Where the work stopped (2026-10-23, late morning).**
- **Experiments done, paper text not yet written.** The experiments on real BPMN models (Section 2) are done, with results and READMEs in `eval/results/bpmn-rq1`, `eval/results/bpmn-rq2` and `eval/results/modisco-cascade`, but `main.tex` does not report them yet.
- **Timed runs still in progress.** The baselines run started at about 10:40 and the multinode run follows it automatically, about 4.5 h for both (Section 3). Their results land in `eval/results/baselines/final.*` and `eval/results/multinode/`; commit them after the runs end.
- **Next steps, in order:**
  1. Write the RQ1 and RQ2 results into the paper (Sections 2 and 4 below).
  2. Add the comparison table and the findings-based contributions and abstract (Sections 1 and 5).
  3. Add the related-work citations (Section 6).
  4. Fix the dependency scan of the core (Section 4).
  5. Build the Zenodo archive once the author has the DOI.

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
- [ ] P2 S. Add the page range of `quinlan2002venti`. BibTeX warns about the empty pages field, and JSS wants pagination where it applies. DBLP blocked the automated lookup. Found on 2026-10-23: the HTML version of the paper on the USENIX site says "Pp. 89–102 of the Proceedings", and the proceedings title reads "Venti: a new approach to archival storage" (the newer USENIX page adds "Data"). Still to enter in `references.bib`.
- [x] P2 S. Decide whether the proposal and the talk in `proposals/01-decentralized-model-persistence/` should name JSS instead of SoSyM. Done on 2026-10-23: both name JSS, and their figure paths point to `papers/01-JSS` again (the move had broken them). Both PDFs are rebuilt; the talk needs XeLaTeX.
- [ ] P2 S. After adding the planned experiments, check that the paper stays under 18 pages, or justify the length in the submission.

Venue background, kept for reference: third-party sites report SoSyM's 2024 SJR as 0.754 with Q1 as the best quartile. Scimago and ScienceDirect block automated requests (HTTP 403 and a Cloudflare check). The JSS and IST pages loaded in Chrome with `--headless=new` and a scratch profile, while Scimago did not.

## 1. Contribution and positioning

- [x] P1 M. Add a short formal model of content-addressed references: resources, versions, the reference graph and a definition of a consistent snapshot, where every reference names the current CID of its target. State and prove three propositions: the cascade re-saves exactly the resources that reach the changed one, the dependency order leaves every reference current, and a cycle never reaches a consistent snapshot in CID mode. The `definition` environment is already declared and unused. Done on 2026-10-23 in Section 5.4 (`sec:rq2-properties`): two definitions, Propositions 1 and 2 with proofs, and Proposition 3 with a proof sketch under a random-function model of the hash.
- [ ] P1 M. Answer "why not Git?" in the paper. Git is content-addressed too, and submodules pin versions. Compare Git, CDO, NeoEMF, EMFStore and the IPFS layer on identity, version pinning, references across repositories and organizations, partial loading, integrity checks and availability, in one table that replaces or extends the strategy table (`tab:strategies`).
- [ ] P1 M. Separate the challenges specific to EMF from general ones (README item, RQ1 sub-question). Go through MPS, Epsilon model drivers, EMF.cloud or Sirius Web, and JSON model stores against the four challenges of Section 1. Otherwise narrow RQ1 to EMF. Searches on 2026-10-23 found no reliable source on how MPS and Sirius Web store references, so narrowing RQ1 to EMF is the safer route. The experiments of Section 2 add EMF-specific evidence: XMI cannot store vendor extensions (plain) or read them back (extended metadata), and references that do not resolve proxies cannot cross resources.
- [ ] P1 S. Rewrite the contribution list around findings rather than artifacts: the cascade properties, the serialization finding and the determinism finding of Section 2 below. All three findings now have numbers (Section 2).

## 2. Evaluation with real BPMN models

The models are in `eval/datasets/bpmn` (cleaned) and `eval/datasets/bpmn-backup-2026-10-22.tar.gz` (originals); see the README there.

The drivers are `eval/bpmn-rq1` and `eval/bpmn-rq2`, with shared classes in `eval/bpmn-common`. The results and their READMEs are in `eval/results/bpmn-rq1`, `eval/results/bpmn-rq2` and `eval/results/modisco-cascade`. Every run used its own offline Kubo container. All items below still need their text in `main.tex`.

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
- [ ] P2 S. RQ3 on real sizes: per-model save and load times over all files, then one save per model against publishing a whole collection as one IPFS directory, in models per second.
  - **Done:** one add per model ran at 166 models per second, one directory per collection at about 1,000.
  - **Open:** the per-model times from the round trip are only indicative, because five virtual machines ran in parallel. A sequential timing run on a quiet machine remains.
- [x] P2 S. Deduplication: compare the growth of the Kubo repository with plain file storage. Done: 1,140 of the 11,533 files share the CID of an earlier file, and the distinct contents hold 95.4% of the bytes. Kubo still grew by 207.1 MB for 207.4 MB of files, and the block folder took 269.1 MB on disk against 234.7 MB for a plain copy in 4 KiB blocks.

## 3. Decentralized setting and baselines

- [ ] P1 M. Measure more than one node (README item) without renting machines: several Kubo containers on one host, with `tc netem` delays for LAN and WAN profiles. Measure a fetch by CID from a node that has never seen the content, and IPNS publish and resolve times. If possible, add one node on another network, such as a campus machine, for a run through the public DHT.
  - **Harness:** ready in `eval/multinode`. Four Kubo nodes run in their own private network, in two topologies (peered and DHT), with four delay profiles (none, LAN, WAN 50 ms, WAN 100 ms), as described in its README.
  - **Validation:** it fixed jitter reordering, local IPNS resolution, warm fetches and the reconnect policy, which belong in the method text.
  - **Final run:** started on 2026-10-23 after the baselines run (about 3.5 h), with results going to `eval/results/multinode`.
  - **Public DHT:** the node on the public network is left out, because the experiment nodes stay off the global network.
- [ ] P1 M. Baselines on the same models (README item): a Git repository (commit, push and clone), a shared network file system, and at least one database-backed store, NeoEMF with an embedded back end being the cheapest to set up.
  - **Harness:** ready in `eval/baselines`, with Git through `git daemon`, a share through sshfs and NeoEMF 2.0.0 with MapDB. NeoEMF needs JDK 17, so every store runs on JDK 17.
  - **Final run:** started on 2026-10-23 at about 10:40 (about 1 h), with results going to `eval/results/baselines/final.*`.
- [x] P2 S. Report effect sizes and confidence intervals next to the Wilcoxon tests, and state the practicality threshold explicitly. The README defines it as the model size at which IPFS takes more than five times as long as local I/O. Done on 2026-10-23: `eval/analyse_benchmark.py` adds the matched-pairs rank-biserial correlation and paired bootstrap intervals (10,000 resamples, fixed seed) to `benchmark_summary.md`, and finds the five-times threshold for saves at about 25,400 elements; loads never reach it. The RQ3 text reports both.
- [ ] P3 L. Evaluate the tool support with a scripted editing workflow or a small user study (README item).

## 4. Manuscript items already flagged

- [ ] P1 S. Report the output of `BpmnIpfsExample` and of the modeler self-tests with a running Kubo node (`\todo` in `sec:evaluation-rq1`), or let the real-model round trip replace the example. The modeler self-tests pass with `--integration` against Kubo 0.40.1 (2026-10-22). Decided: the real-model round trip replaces the example, and its results are ready in `eval/results/bpmn-rq1`. Still to do: rewrite `sec:evaluation-rq1` and remove the `\todo`.
- [ ] P1 S. Fix `examples/BpmnIpfsExample.java`, which prints hard-coded CIDs and a `creditCheckRef` that does not exist, and loads `.bpmn` files with the XMI factory.
- [ ] P1 S. Fix the dependency scan of the cascade (`ResourceDependencyGraph`), found by the MoDisco analysis. It has two gaps with cross-resource containment, which the MoDisco partition uses for `Model.compilationUnits`:
  - It skips containment references, although a containment proxy stores the URI of its target like any other reference.
  - `resource.getAllContents()` walks into children that live in other resources and credits their references to the container's resource.

  The fix: walk `EcoreUtil.getAllProperContents`, count containment references whose child lies in another resource, add a test, and update the algorithm text of `sec:rq2-cascade`.
- [x] P1 S. Rerun the BPMN case study after the three files gained BPMN imports, the order process gained a send task and a message event, and the collaboration gained message flows and a diagram (2026-10-22). The stored import locations now carry CIDs as well. The order process imports the payment process instead of declaring `supports`, which the BPMN standard reserves for a process whose every run also follows the steps of the other process. Figure `fig:cascade` and the 50 ms in `sec:evaluation-rq2` come from that run, and the paragraph names the imports and the message flows.
- [x] P2 M. Build the modeler with Tycho against `releases/2022-06` (`\todo` in `sec:tool-editor`). Done on 2026-10-23: `mvn -B -P platform-2022-06 clean verify` builds all 18 modules in about 7 minutes and produces the update site. `parent/pom.xml` needed four new repository addresses (BPMN2, Graphiti, Web Tools, Orbit). The `\todo` in `sec:tool-editor` is gone.
- [ ] P1 M. Archive code, scripts and data under a DOI on Zenodo: the MoDisco dataset (1.04 GiB) with the published CIDs (`\todo`, Data availability), plus the MIWG and engine BPMN sets with the change logs. Cite the Camunda set by repository and commit only, since its terms do not cover redistribution.
- [ ] P1 S. Remove every `\todo` before submission.

## 5. Space

The new material needs about three pages, which these cuts should free.

- [x] P2 S. Merge the save and load sequence diagrams (`fig:save` and `fig:load`) into one figure, or drop the load diagram. The load diagram is dropped; the text of the load subsection covers the same steps.
- [x] P2 S. Replace the six bullets on uses in the introduction with one paragraph, since they stay outside the evaluation.
- [x] P2 S. Cut the tool section to about half a page and leave the command details to the repository. The command table is gone, and two wrong statements were fixed: the layout sits in the BPMN file as diagram interchange data, and opening a project can create a new project.
- [x] P2 S. Shorten the manifest listing (`lst:manifests`) to the project version manifest. The listing now fits one column.
- [ ] P2 S. Shorten the conclusion, which repeats the answers to the research questions (`sec:answers`) almost sentence by sentence.
- [x] P2 S. Move the funding sentence out of the Acknowledgements into limitations or future work. The sentence closes the limitations, and the empty Acknowledgements section is removed.
- [ ] P2 S. Rewrite the abstract around the findings instead of the measurements alone.

## 6. Related work and validity

- [ ] P2 M. Position the paper against decentralized model collaboration outside MDE and recheck the claim that no approach stores EMF models on content-addressed storage (in the introduction and in `sec:related-content`). Checked on 2026-10-23; the text is still to write. The three candidates exist as described:
  - F. Härer, "Decentralized Business Process Modeling and Instance Tracking Secured by a Blockchain", ECIS 2018 (AISeL ecis2018_rp/55): model versioning on a blockchain for decentralized enterprise models.
  - G. Falazi et al., "Blockchain-based Collaborative Development of Application Deployment Models", OTM 2018 (CoopIS), LNCS 11229, pp. 40–60, DOI 10.1007/978-3-030-02610-3_3: intermediate versions of a shared deployment model on public blockchains and decentralized storage. The co-author list still needs checking.
  - X. Tao, M. Das, Y. Liu, J.C.P. Cheng, "Distributed common data environment using blockchain and Interplanetary File System for secure BIM-based collaborative design", Automation in Construction 130 (2021) 103851.

  Related: Härer and Fill, decentralized attestation of conceptual models on Ethereum (IEEE CBI 2019). The searches found no work that stores EMF models on content-addressed storage, apart from ecorefs itself, so the claim stands.
- [ ] P2 S. Add Git-based model management and model federation if the comparison table needs them.
- [ ] P2 S. Cite the BPMN sources (MIWG test suite, Camunda bpmn-for-research) and the size context. Details found on 2026-10-23, not yet in `references.bib`:
  - J. Türker, M. Völske, T.S. Heinze, "BPMN in the Wild: A Reprise", ZEUS 2022 (Bamberg, February 24–25), CEUR-WS Vol-3113, pp. 68–75. An earlier "BPMN in the Wild" by Heinze, Stefanko and Amme appeared at ZEUS 2020, CEUR-WS Vol-2575, pp. 26–29.
  - J. Mendling, H.A. Reijers, W.M.P. van der Aalst, "Seven process modeling guidelines (7PMG)", Information and Software Technology 52(2), 127–136, 2010. The DOI is unconfirmed.
  - The MIWG and Camunda collections by repository and commit, as in `eval/datasets/bpmn/README.md`.

  The real models are small: the largest of the five collections has 192 flow elements.
- [ ] P2 S. Update the threats to validity: a second real metamodel from five sources and the emulated network widen external validity. The cleaning becomes a threat for any result on the cleaned set, since it edited 16% of the files and removed engine semantics.
