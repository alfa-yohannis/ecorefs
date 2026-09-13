# Literature review: decentralized model persistence

Background reading for the SoSyM manuscript in `paper/main.tex`. The BibTeX keys match `paper/references.bib`.

## How the sources were checked

All 50 sources were located online on 2026-09-13, and every entry below has a working link.

- Papers with a DOI were matched against Crossref for title, authors, venue, volume, pages and year.
- Abstracts came from OpenAlex, Semantic Scholar, DataCite (arXiv) or the publisher page (Springer, HAL, USENIX). Two Elsevier pages blocked automated access. The NeoEMF abstract was taken from the paper's HAL copy, and the Nizamuddin et al. summary follows the title only.
- Specifications, documentation and project pages were loaded and checked for their title and the quoted statements.

The summaries below restate the abstracts or official pages. The search was targeted, not a systematic literature review.

## 1. Model-driven engineering, EMF and OMG standards

- `schmidt2006mde`: Schmidt, D. C. (2006). Guest Editor's Introduction: Model-Driven Engineering. *Computer* 39(2), 25-31. https://doi.org/10.1109/MC.2006.58
  Presents MDE as a way to address the inability of third-generation languages to manage platform complexity and to express domain concepts. Used for the general MDE motivation.
- `steinberg2008emf`: Steinberg, D., Budinsky, F., Paternostro, M., Merks, E. (2008). *EMF: Eclipse Modeling Framework*, 2nd edn. Addison-Wesley Professional. https://www.informit.com/store/emf-eclipse-modeling-framework-9780321331885
  The reference book on EMF, its Ecore metamodel and its persistence API (resources, resource sets, XMI). Used for the EMF background.
- `omg2016mof`: Object Management Group (2016). Meta Object Facility (MOF) Specification, Version 2.5.1. https://www.omg.org/spec/MOF/
  The OMG metamodeling standard. Ecore is aligned with the Essential MOF subset.
- `omg2015xmi`: Object Management Group (2015). XML Metadata Interchange (XMI) Specification, Version 2.5.1. https://www.omg.org/spec/XMI/
  The XML format for model exchange and the default serialization format of EMF.
- `omg2014ocl`: Object Management Group (2014). Object Constraint Language (OCL), Version 2.4. https://www.omg.org/spec/OCL/
  The OMG constraint and query language. Relevant because content-addressed persistence offers no query support.
- `omg2010bpmn`: Object Management Group (2010). Business Process Model and Notation (BPMN), Version 2.0. https://www.omg.org/spec/BPMN/2.0/
  The standard behind the BPMN2 Modeler case study.

## 2. Scalable model persistence and querying

- `kolovos2013roadmap`: Kolovos, D. S. et al. (2013). A research roadmap towards achieving scalability in model driven engineering. BigMDE '13, ACM, 1-10. https://doi.org/10.1145/2487766.2487768
  A roadmap for scalability in MDE: systematic construction of large models and languages, collaborative construction of large models, and scalable querying and transformation. Frames scalability as the main theme of earlier persistence work.
- `pagan2011morsa`: Espinazo Pagán, J., Sánchez Cuadrado, J., García Molina, J. (2011). Morsa: A scalable approach for persisting and accessing large models. MODELS 2011, LNCS, 77-92. https://doi.org/10.1007/978-3-642-24485-8_7
  Morsa gives scalable access to large models through load on demand, with persistence in a NoSQL database.
- `pagan2015repository`: Espinazo Pagán, J., Sánchez Cuadrado, J., García Molina, J. (2015). A repository for scalable model management. *Software and Systems Modeling* 14(1), 219-239. https://doi.org/10.1007/s10270-013-0326-8
  Extends Morsa into a model repository with load on demand and incremental store. A SoSyM precedent for persistence research.
- `benelallam2014neo4emf`: Benelallam, A., Gómez, A., Sunyé, G., Tisi, M., Launay, D. (2014). Neo4EMF, a scalable persistence layer for EMF models. ECMFA 2014, LNCS, 230-241. https://doi.org/10.1007/978-3-319-09195-2_15
  Argues that current tools do not scale to very large models and proposes a scalable persistence layer for EMF.
- `daniel2017neoemf`: Daniel, G. et al. (2017). NeoEMF: A multi-database model persistence framework for very large models. *Science of Computer Programming* 149, 9-14. https://doi.org/10.1016/j.scico.2017.08.002
  Existing scalable solutions often rely on a single data store tuned for one modeling activity. NeoEMF offers several database back ends in one persistence framework.
- `daniel2016mogwai`: Daniel, G., Sunyé, G., Cabot, J. (2016). Mogwaï: A framework to handle complex queries on large models. RCIS 2016, IEEE, 1-12. https://doi.org/10.1109/RCIS.2016.7549343
  Translates OCL queries to Gremlin and runs the queries inside NoSQL databases. Shows the query support that database-backed persistence offers.
- `scheidgen2013reference`: Scheidgen, M. (2013). Reference representation techniques for large models. BigMDE '13, ACM, 1-9. https://doi.org/10.1145/2487766.2487769
  Observes that XMI trees and relational representations (as in CDO) perform differently per operation, and asks whether representations can be combined. Relevant to splitting models into many resources.
- `barmpis2013hawk`: Barmpis, K., Kolovos, D. (2013). Hawk: Towards a scalable model indexing architecture. BigMDE '13, ACM, 1-9. https://doi.org/10.1145/2487766.2487771
  Reviews model version control and proposes an indexing framework for queries over large collections of models kept in version control systems.
- `cdo`: Eclipse Foundation. CDO Model Repository. https://eclipse.dev/cdo/
  Stores, manages and versions models and data in a central repository, with relational, NoSQL and in-memory databases. The main server-based alternative in the comparison.

## 3. Model repositories, versioning, comparison and collaboration

- `france2007remodd`: France, R., Bieman, J., Cheng, B. H. C. (2007). Repository for Model Driven Development (ReMoDD). *Models in Software Engineering*, LNCS, Springer, 311-317. https://link.springer.com/chapter/10.1007/978-3-540-69489-2_38
  A repository of MDD artifacts (case studies, reference models, metamodels) for research and education, with interfaces for tools to retrieve and submit artifacts.
- `dirocco2015repositories`: Di Rocco, J., Di Ruscio, D., Iovino, L., Pierantonio, A. (2015). Collaborative repositories in model-driven engineering. *IEEE Software* 32(3), 28-34. https://doi.org/10.1109/MS.2015.61
  Model repositories support collaborative modeling, tool interoperability, model reuse and the integration of heterogeneous models.
- `koegel2010emfstore`: Koegel, M., Helming, J. (2010). EMFStore: A model repository for EMF models. ICSE '10 Volume 2, ACM, 307-308. https://doi.org/10.1145/1810295.1810364
  A configuration management system for models with operation-based change tracking, conflict detection and merging.
- `altmanninger2009survey`: Altmanninger, K., Seidl, M., Wimmer, M. (2009). A survey on model versioning approaches. *International Journal of Web Information Systems* 5(3), 271-304. https://doi.org/10.1108/17440080910983556
  A feature-based characterization of version control systems for models, with a focus on three-way merging and open challenges.
- `brosch2012versioning`: Brosch, P. et al. (2012). An introduction to model versioning. SFM 2012, LNCS, 336-398. https://doi.org/10.1007/978-3-642-30982-3_10
  Presents model versioning as one of the research challenges that follow from treating models as central artifacts.
- `kolovos2006comparison`: Kolovos, D. S., Paige, R. F., Polack, F. A. C. (2006). Model comparison: A foundation for model composition and model transformation testing. GaMMa '06, ACM, 13-20. https://doi.org/10.1145/1138304.1138308
  A rule-based model comparison approach, motivated by model composition and transformation testing.
- `emfcompare`: Eclipse Foundation. EMF Compare. https://eclipse.dev/emfcompare/
  Generic comparison and merging of EMF models for any metamodel. Used in the evaluation to compare local models with copies fetched from IPFS.
- `franzago2018collaborative`: Franzago, M., Di Ruscio, D., Malavolta, I., Muccini, H. (2018). Collaborative model-driven software engineering: A classification framework and a research map. *IEEE Transactions on Software Engineering* 44(12), 1146-1175. https://doi.org/10.1109/TSE.2017.2755039
  A systematic mapping study that selected 106 papers and grouped the papers into 48 primary studies on collaborative MDSE. Places shared model storage in the context of collaboration.

## 4. Content addressing, peer-to-peer storage and IPFS

- `merkle1988signature`: Merkle, R. C. (1988). A digital signature based on a conventional encryption function. CRYPTO '87, LNCS, 369-378. https://doi.org/10.1007/3-540-48184-2_32
  A signature system built only on a conventional encryption function. The paper introduced tree authentication, the origin of hash trees.
- `maymounkov2002kademlia`: Maymounkov, P., Mazières, D. (2002). Kademlia: A peer-to-peer information system based on the XOR metric. IPTPS 2002, LNCS, 53-65. https://doi.org/10.1007/3-540-45748-8_5
  A distributed hash table with an XOR-based routing metric. IPFS uses a Kademlia-based DHT.
- `quinlan2002venti`: Quinlan, S., Dorward, S. (2002). Venti: A new approach to archival data storage. FAST '02, USENIX. https://www.usenix.org/conference/fast-02/venti-new-approach-archival-data-storage
  Archival storage where "a unique hash of a block's contents acts as the block identifier", which enforces a write-once policy. An early content-addressed storage system.
- `chacon2014progit`: Chacon, S., Straub, B. (2014). *Pro Git*, 2nd edn. Apress. https://git-scm.com/book/en/v2
  Describes Git as a content-addressable filesystem that names each object with the SHA-1 checksum of its content. Used for the Git analogy of cascade saves.
- `benet2014ipfs`: Benet, J. (2014). IPFS: Content addressed, versioned, P2P file system. arXiv:1407.3561. https://arxiv.org/abs/1407.3561
  The IPFS design: a peer-to-peer file system with content-addressed block storage and content-addressed links that form a Merkle DAG.
- `psaras2020ipfs`: Psaras, Y., Dias, D. (2020). The InterPlanetary File System and the Filecoin network. DSN-S 2020, IEEE, 80. https://doi.org/10.1109/DSN-S50200.2020.00043
  Overview of IPFS: content addressed by flat hash-based names, resolution through a Kademlia DHT, over 250,000 daily active nodes at the time.
- `trautwein2022ipfs`: Trautwein, D. et al. (2022). Design and evaluation of IPFS: A storage layer for the decentralized web. SIGCOMM 2022, ACM, 739-752. https://doi.org/10.1145/3544216.3544232
  Describes and evaluates the design of IPFS as a content-addressable peer-to-peer network for distributed storage and delivery.
- `henningsen2020mapping`: Henningsen, S., Florian, M., Rust, S., Scheuermann, B. (2020). Mapping the Interplanetary Filesystem. arXiv:2002.07747. https://arxiv.org/abs/2002.07747
  Crawls the IPFS DHT and finds on average 44,474 nodes at any given time. Background on the public network behind IPNS resolution.
- `daniel2022ipfsfriends`: Daniel, E., Tschorsch, F. (2022). IPFS and friends: A qualitative comparison of next generation peer-to-peer data networks. *IEEE Communications Surveys & Tutorials* 24(1), 31-52. https://doi.org/10.1109/COMST.2022.3143147
  A survey of IPFS, Swarm, the Hypercore Protocol, SAFE, Storj and Arweave.
- `chen2017improved`: Chen, Y., Li, H., Li, K., Zhang, J. (2017). An improved P2P file system scheme based on IPFS and blockchain. IEEE Big Data 2017, 2652-2657. https://doi.org/10.1109/BigData.2017.8258226
  Adds content service providers to IPFS to address throughput for individual users, combined with a blockchain.
- `nizamuddin2019document`: Nizamuddin, N. et al. (2019). Decentralized document version control using Ethereum blockchain and IPFS. *Computers & Electrical Engineering* 76, 183-197. https://doi.org/10.1016/j.compeleceng.2019.03.014
  Document version control built on Ethereum and IPFS (summary based on the title). The closest non-MDE work on versioning over IPFS.
- `kelly2016wayback`: Kelly, M., Alam, S., Nelson, M. L., Weigle, M. C. (2016). InterPlanetary Wayback: Peer-to-peer permanence of web archives. TPDL 2016, LNCS, 411-416. https://doi.org/10.1007/978-3-319-43997-6_35
  Stores web archive (WARC) payloads in IPFS and replays archived pages from IPFS.
- `dicosmo2017heritage`: Di Cosmo, R., Zacchiroli, S. (2017). Software Heritage: Why and how to preserve software source code. iPRES 2017, 1-10. https://hal.science/hal-01590958
  Argues that source code is a digital object that deserves first-class preservation, and presents the Software Heritage archive.
- `kubo`: Kubo: IPFS implementation in Go. https://github.com/ipfs/kubo
  The daemon that stores and serves content-addressed data. The local node used by ecorefs.
- `ipfsdocscid`: IPFS Docs, Content Identifiers (CIDs). https://docs.ipfs.tech/concepts/content-addressing/
  Defines a CID as a label that points to material in IPFS.
- `ipfsdocsipns`: IPFS Docs, IPNS (InterPlanetary Name System). https://docs.ipfs.tech/concepts/ipns/
  IPFS content addressing is immutable, and IPNS adds mutable names. An IPNS name is the hash of a public key, and records are signed with the private key.
- `javaipfsclient`: java-ipfs-http-client: A Java implementation of the HTTP IPFS API. https://github.com/ipfs-shipyard/java-ipfs-http-client
  The client library used by the ecorefs core plugin.

## 5. Dependency pinning and reproducibility

- `dolstra2004nix`: Dolstra, E., de Jonge, M., Visser, E. (2004). Nix: A safe and policy-free system for software deployment. LISA '04, USENIX, 79-92. https://www.usenix.org/legacy/publications/library/proceedings/lisa04/tech/dolstra.html
  Uses cryptographic hashes to compute unique paths for component instances, which supports several versions of a component side by side.
- `cox2019dependencies`: Cox, R. (2019). Surviving software dependencies. *ACM Queue* 17(2), 24-47. https://doi.org/10.1145/3329781.3344149
  Discusses the risks of consuming software dependencies and how to reuse software safely.
- `decan2019ecosystems`: Decan, A., Mens, T., Grosjean, P. (2019). An empirical comparison of dependency network evolution in seven software packaging ecosystems. *Empirical Software Engineering* 24(1), 381-416. https://doi.org/10.1007/s10664-017-9589-y
  Analyzes dependency networks of seven packaging ecosystems with the libraries.io dataset. Reports problems such as backward-incompatible updates and dependencies on inactive packages.
- `lamb2022reproducible`: Lamb, C., Zacchiroli, S. (2022). Reproducible builds: Increasing the integrity of software supply chains. *IEEE Software* 39(2), 62-70. https://doi.org/10.1109/MS.2021.3073045
  Reproducible builds check whether generated binaries correspond to the source code.
- `gomodules`: Go Modules Reference. https://go.dev/ref/mod
  The Go module system records required module versions in `go.mod` and checksums in `go.sum`.
- `npmpackagelock`: npm Docs, package-lock.json. https://docs.npmjs.com/cli/v10/configuring-npm/package-lock-json/
  The lock file "describes the exact tree that was generated, such that subsequent installs are able to generate identical trees".

## 6. Tools used in the evaluation

- `bruneliere2010modisco`: Bruneliere, H., Cabot, J., Jouault, F., Madiot, F. (2010). MoDisco: A generic and extensible framework for model driven reverse engineering. ASE '10, ACM, 173-174. https://doi.org/10.1145/1858996.1859032
  An open source reverse engineering framework based on MDE. Produced the Java models of the case study.
- `bruneliere2014modisco`: Brunelière, H., Cabot, J., Dupé, G., Madiot, F. (2014). MoDisco: A model driven reverse engineering framework. *Information and Software Technology* 56(8), 1012-1032. https://doi.org/10.1016/j.infsof.2014.04.007
  The journal description of MoDisco and model driven reverse engineering.
- `bpmn2modeler`: Eclipse Foundation. Eclipse BPMN2 Modeler Project. https://projects.eclipse.org/projects/technology.bpmn2-modeler
  A graphical modeling tool for creating and editing BPMN diagrams. Extended with IPFS commands.
- `graphiti`: Eclipse Foundation. Graphiti. https://eclipse.dev/graphiti/
  An Eclipse graphics framework for building diagram editors for domain models. The BPMN2 Modeler stores Graphiti diagram files next to the model.

## Synthesis

1. Persistence research in MDE has focused on scalability. Morsa, Neo4EMF, NeoEMF, Mogwaï and CDO store models in databases, load model parts on demand or run queries inside the store. Model elements keep stable identifiers inside the store while the content changes.
2. Model versioning and comparison work (EMFStore, the surveys by Altmanninger et al. and Brosch et al., EMF Compare, Hawk) records or computes differences between model versions in a central repository or a version control system.
3. Content addressing is established outside MDE: hash trees (Merkle), archival storage (Venti), version control (Git), software deployment (Nix), source code preservation (Software Heritage), and IPFS for web archives and documents.
4. Package managers pin dependencies to exact versions (npm lock files, Go modules, Nix). Unplanned dependency updates are a documented source of problems (Cox, Decan et al.).
5. Research gap: none of the sources checked stores EMF models on content-addressed storage or studies cross-resource references whose identifiers change on every save. The manuscript addresses that gap with RQ1 to RQ3.
