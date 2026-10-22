# Revision plan for a Q1 journal

Plan for `paper/main.tex` (15 pages in the JSS `elsarticle` layout on 2026-10-22, up from 20 in the SoSyM template). It merges a review of the draft with the open manuscript items of the ecorefs README; the README keeps the code items. Priority: P1 must happen before submission, P2 should, P3 is optional. Effort: S is about a day, M a few days, L one to two weeks.

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

- [ ] P1 S. Confirm the CRediT roles (`\todo` in the CRediT statement).
- [ ] P1 S. State whether an AI tool helped draft or edit the text (`\todo` in the AI declaration).
- [ ] P1 S. Write a biography of at most 100 words for each author, in an editable file.
- [ ] P1 S. Research data under Elsevier's "Option C", which JSS applies: deposit the data in a repository, then cite and link the dataset in the paper (`\todo` in the data availability section), or state why it cannot be shared. This makes the Zenodo item in Section 4 mandatory.
- [ ] P1 S. Prepare the upload for Editorial Manager, which accepts no subfolders: copy the figures from `figures/`, `elsarticle.cls`, `elsarticle-num.bst` and `references.bib` next to `main.tex` in a submission folder, compile there, and add the PDF and `highlights.txt`. Review is single anonymized, so author names and repository links can stay.
- [ ] P2 S. Add the page range of `quinlan2002venti`. BibTeX warns about the empty pages field, and JSS wants pagination where it applies. DBLP blocked the automated lookup.
- [ ] P2 S. Decide whether the proposal and the talk in `proposals/01-decentralized-model-persistence/` should name JSS instead of SoSyM.
- [ ] P2 S. After adding the planned experiments, check that the paper stays under 18 pages, or justify the length in the submission.

Venue background, kept for reference: third-party sites report SoSyM's 2024 SJR as 0.754 with Q1 as the best quartile. Scimago and ScienceDirect block automated requests (HTTP 403 and a Cloudflare check). The JSS and IST pages loaded in Chrome with `--headless=new` and a scratch profile, while Scimago did not.

## 1. Contribution and positioning

- [ ] P1 M. Add a short formal model of content-addressed references: resources, versions, the reference graph and a definition of a consistent snapshot, where every reference names the current CID of its target. State and prove three propositions: the cascade re-saves exactly the resources that reach the changed one, the dependency order leaves every reference current, and a cycle never reaches a consistent snapshot in CID mode. The `definition` environment is already declared and unused.
- [ ] P1 M. Answer "why not Git?" in the paper. Git is content-addressed too, and submodules pin versions. Compare Git, CDO, NeoEMF, EMFStore and the IPFS layer on identity, version pinning, references across repositories and organizations, partial loading, integrity checks and availability, in one table that replaces or extends the strategy table (`tab:strategies`).
- [ ] P1 M. Separate the challenges specific to EMF from general ones (README item, RQ1 sub-question). Go through MPS, Epsilon model drivers, EMF.cloud or Sirius Web, and JSON model stores against the four challenges of Section 1. Otherwise narrow RQ1 to EMF.
- [ ] P1 S. Rewrite the contribution list around findings rather than artifacts: the cascade properties, the serialization finding and the determinism finding of Section 2 below.

## 2. Evaluation with real BPMN models

The models are in `eval/datasets/bpmn` (cleaned) and `eval/datasets/bpmn-backup-2026-10-22.tar.gz` (originals); see the README there.

- [ ] P1 S. Unpack the originals into `eval/datasets/bpmn-original`. Use them for RQ1 and RQ3, and the cleaned set for RQ2.
- [ ] P1 M. RQ1 round trip on all 11,518 original models: save each through `IPFSResourceImpl`, load it by CID in a fresh resource set, compare with the original, and run the local XMI path on the same model as the paired baseline. Run once with the current code, where a plain XMI save fails for about half the models on vendor extensions, then set `XMLResource.OPTION_EXTENDED_META_DATA` in `IPFSResourceImpl` and run again. Report both runs.
- [ ] P1 S. RQ1 determinism: save every model twice in separate resource sets and compare the CIDs, on the XMI path and on the BPMN XML path. `Bpmn2ResourceImpl.prepareSave` gives each element without an id a fresh `EcoreUtil.generateUUID()`, so the BPMN XML path likely yields a new CID on every first save. If confirmed, assign deterministic ids before publishing in the modeler and state the finding.
- [ ] P1 M. RQ2 on real structures: split the 1,855 models with two or more pools into one resource per pool, publish them, change one process, run `cascadeSave` and check every reference, as the three-file case study does.
- [ ] P1 M. RQ2 on real dependency graphs: rebuild the 503 cross-file `calledElement` links (in `_load-check/clean/*.changes.tsv`) as cross-resource references, then measure the re-saves and the time against fan-in and chain depth (up to five files).
- [ ] P1 M. Cascade on the partitioned MoDisco model (README item): 6,933 resources, 26 components and 92 dependency edges. Change a leaf, a resource inside a component that others depend on, and a shared resource in `aux`. Record the re-saved resources and the time, and measure the cross-reference scan separately, since `EcoreUtil.CrossReferencer.find` walks the whole resource set. Compare with a dependency lookup from the component manifests.
- [ ] P2 S. RQ3 on real sizes: per-model save and load times over all files, then one save per model against publishing a whole collection as one IPFS directory, in models per second.
- [ ] P2 S. Deduplication: 1,159 of the 11,518 files are byte-identical copies. Compare the growth of the Kubo repository with plain file storage.

## 3. Decentralized setting and baselines

- [ ] P1 M. Measure more than one node (README item) without renting machines: several Kubo containers on one host, with `tc netem` delays for LAN and WAN profiles. Measure a fetch by CID from a node that has never seen the content, and IPNS publish and resolve times. If possible, add one node on another network, such as a campus machine, for a run through the public DHT. Two containers in a private network already run from `docker/kubo/compose.yaml` (see `docs/tutorial.md`), so the delays and the measurements remain.
- [ ] P1 M. Baselines on the same models (README item): a Git repository (commit, push and clone), a shared network file system, and at least one database-backed store, NeoEMF with an embedded back end being the cheapest to set up.
- [ ] P2 S. Report effect sizes and confidence intervals next to the Wilcoxon tests, and state the practicality threshold explicitly. The README defines it as the model size at which IPFS takes more than five times as long as local I/O.
- [ ] P3 L. Evaluate the tool support with a scripted editing workflow or a small user study (README item).

## 4. Manuscript items already flagged

- [ ] P1 S. Report the output of `BpmnIpfsExample` and of the modeler self-tests with a running Kubo node (`\todo` in `sec:evaluation-rq1`), or let the real-model round trip replace the example. The modeler self-tests pass with `--integration` against Kubo 0.40.1 (2026-10-22).
- [x] P1 S. Rerun the BPMN case study after the three files gained BPMN imports, the order process gained a send task and a message event, and the collaboration gained message flows and a diagram (2026-10-22). The stored import locations now carry CIDs as well. The order process imports the payment process instead of declaring `supports`, which the BPMN standard reserves for a process whose every run also follows the steps of the other process. Figure `fig:cascade` and the 50 ms in `sec:evaluation-rq2` come from that run, and the paragraph names the imports and the message flows.
- [ ] P2 M. Build the modeler with Tycho against `releases/2022-06` (`\todo` in `sec:tool-editor`).
- [ ] P1 M. Archive code, scripts and data under a DOI on Zenodo: the MoDisco dataset (1.04 GiB) with the published CIDs (`\todo`, Data availability), plus the MIWG and engine BPMN sets with the change logs. Cite the Camunda set by repository and commit only, since its terms do not cover redistribution.
- [ ] P1 S. Remove every `\todo` before submission.

## 5. Space

The new material needs about three pages, which these cuts should free.

- [ ] P2 S. Merge the save and load sequence diagrams (`fig:save` and `fig:load`) into one figure, or drop the load diagram.
- [ ] P2 S. Replace the six bullets on uses in the introduction with one paragraph, since they stay outside the evaluation.
- [ ] P2 S. Cut the tool section to about half a page and leave the command details to the repository.
- [ ] P2 S. Shorten the manifest listing (`lst:manifests`) to the project version manifest.
- [ ] P2 S. Shorten the conclusion, which repeats the answers to the research questions (`sec:answers`) almost sentence by sentence.
- [ ] P2 S. Move the funding sentence out of the Acknowledgements into limitations or future work.
- [ ] P2 S. Rewrite the abstract around the findings instead of the measurements alone.

## 6. Related work and validity

- [ ] P2 M. Position the paper against decentralized model collaboration outside MDE and recheck the claim that no approach stores EMF models on content-addressed storage (in the introduction and in `sec:related-content`). Candidates to verify and read: Härer, blockchain-based model versioning (ECIS 2018); Falazi et al., blockchain-based collaborative development of application deployment models (CoopIS 2018); Tao et al., a distributed common data environment with blockchain and IPFS (Automation in Construction, 2021).
- [ ] P2 S. Add Git-based model management and model federation if the comparison table needs them.
- [ ] P2 S. Cite the BPMN sources (MIWG test suite, Camunda bpmn-for-research) and the size context: Türker et al., BPMN in the Wild (ZEUS 2022), and the seven process modeling guidelines (Mendling, Reijers and van der Aalst, IST 2010) for why real models stay small.
- [ ] P2 S. Update the threats to validity: a second real metamodel from five sources and the emulated network widen external validity. The cleaning becomes a threat for any result on the cleaned set, since it edited 16% of the files and removed engine semantics.
