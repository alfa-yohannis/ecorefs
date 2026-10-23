# Stored copies of the case study

These six files are the copies of the case study that `BpmnCaseStudyCascadeTest` stored on IPFS on 2026-10-22, fetched byte for byte from the local Kubo node. They show what a save to IPFS writes. The source files in the folder above are the published BPMN versions, which import each other by CID; these copies are the XMI that the test writes when it publishes the models with the core plug-in.

| File | CID |
|---|---|
| `before/payment-process.xmi` | `QmfGiCCe3C6dtHFvRtMipQ7c3hxhLZawmrcajgNWavRen5` |
| `before/order-process.xmi` | `QmYd4Bacip4qw8tuPhXDCBaP2MmVJmYYKcwusbiZvFsDdR` |
| `before/collaboration.xmi` | `QmUx9AWXcwUyVYHXXHyWqn1gk2jmTDLzBS7AohN8qQKysJ` |
| `after/payment-process.xmi` | `QmTLbqPgHxUmS82rDLDCKgrncmkdHi9J6nQu38KT8AE3PJ` |
| `after/order-process.xmi` | `QmT1Y8EfDTYi7NJCaqMeMfCk5V45iFjRPWGEVarReWrChc` |
| `after/collaboration.xmi` | `QmNxhLjKZ929aWBgga6RzPUDKvyjF7L7kvF1jHNTQ3Aspp` |

`before/` holds the first save of the three models. `after/` holds the copies after the test renamed the payment process and called `saveIPFSCascaded`.

The core plug-in stores models as XMI, so these files are the XMI form of the BPMN models, which the BPMN2 editor does not open. In each file, every reference is an `href` with the CID of its target, and every import location names the CID of the imported file. `after/order-process.xmi`, for example, imports the new payment process:

```
location="ipfs://QmTLbqPgHxUmS82rDLDCKgrncmkdHi9J6nQu38KT8AE3PJ"
```

The CID of a file follows from its bytes alone. Adding a file to any IPFS node gives the CID in the table, and `only-hash=true` computes it without storing the file:

```bash
curl -s -X POST -F "file=@after/order-process.xmi" "http://127.0.0.1:5001/api/v0/add?only-hash=true"
```

```
{"Name":"order-process.xmi","Hash":"QmT1Y8EfDTYi7NJCaqMeMfCk5V45iFjRPWGEVarReWrChc","Size":"3303"}
```

A new run of the test writes the same six files as long as the source files and the code stay the same. After a change to either, the test prints new CIDs, and these copies show the earlier state.
