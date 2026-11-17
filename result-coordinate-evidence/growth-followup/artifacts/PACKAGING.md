# Prepare complete growth source delivery

The packager is deliberately split into inventory and build. Inventory lists exact
source/provenance paths and hashes without assembling an archive. A provisional
inventory omits final protocol bindings and cannot be used for building.

From the repository root, the initial preparation command is:

    python3 -B result-coordinate-evidence/growth-followup/artifacts/build_source_delivery.py inventory --output result-coordinate-evidence/growth-followup/artifacts/source-inventory-provisional-v1.json

After the coordinator freezes and supplies the final qualification and protocol,
create a new final inventory using the actual immutable paths:

    python3 -B result-coordinate-evidence/growth-followup/artifacts/build_source_delivery.py inventory --output result-coordinate-evidence/growth-followup/artifacts/source-inventory-v1.json --protocol FROZEN_PROTOCOL --qualification FINAL_QUALIFICATION

Inspect and bind the printed inventory hash, then run only in the coordinator's
released pre-timing slot:

    python3 -B result-coordinate-evidence/growth-followup/artifacts/build_source_delivery.py build --selection result-coordinate-evidence/growth-followup/artifacts/source-inventory-v1.json --approved-selection-sha256 INVENTORY_SHA --protocol-sha256 PROTOCOL_SHA --qualification-sha256 QUALIFICATION_SHA --pretime-slot-granted

The final build refuses a provisional inventory, altered input or builder, changed
source set, mismatched protocol/qualification binding, any existing output artifact,
or any existing primary claim in the selected cohort. It never launches Java, changes
module files, stages Git content or publishes a checkpoint.

The source dependency closure comes from the verified complete admission archive.
Every member is checked against its embedded/external manifest. Its Java and Python
sources must still match the live predecessor sources. Historical documentation
uses its archived snapshot, so later append-only result reporting cannot silently
change the packaged predecessor provenance.

The new archive contains complete current feature files at production paths and
their canonical experiment paths, all canonical growth Java/Python sources, the
predecessor dependency closure, selected test/build/qualification provenance,
the prospective protocol and two review patches. Compiled binaries, dependency
JARs, databases and measurements remain separate. The generated README explains
that dependency boundary and the original scripts' workspace-path assumptions.

Both patches must reconstruct their complete targets through the strict hunk reader,
git apply --check, actual disposable application and byte equality. The archive
undergoes full member readback, safety checks, duplicate detection and manifest
verification. The builder rechecks selected input hashes before writing its final
verification receipt.

The source archive is only complete when source-delivery-verification-v1.json says
verified=true. Keep failed/partial attempts and create a new version if needed.
The coordinator should independently read back the finished deliverables before
preserving their bytes and selecting the immutable Git checkpoint.
