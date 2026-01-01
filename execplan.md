Execplan: Fully Featured Closed‑Loop Optimizer

Part 1: Foundations (Telemetry → Feedback Plane → Online Models → Planner Integration)

0) Problem statement (re-grounding, precisely)

Query optimizers choose physical plans by comparing costs. Costs are computed from estimates—cardinality (rows), selectivity (fractions), and resource models (CPU, I/O, memory, spill risk). When estimates are wrong, optimizers pick plans that are wrong in a way that is often catastrophically non-linear: a 10× underestimate in an early join can turn into a 100× cost explosion downstream because join algorithm choice, memory sizing, parallelism decisions, and operator ordering depend on those estimates.

A closed-loop “actual vs. predicted” system aims to do something deceptively simple:
•	Record what the optimizer thought would happen at each plan node.
•	Record what actually happened at runtime at that node.
•	Learn a small, safe correction function that nudges future estimates for similar situations.
•	Do it continuously, with bounded overhead, with hard guardrails, and with strong explainability.

A “fully featured” solution goes beyond a single error ratio per operator:
•	It learns context-sensitive selectivity (especially bound-variable context for SPARQL/RDF).
•	It learns distribution-aware models (sketches/histograms) for high-impact predicates and filters.
•	It calibrates operator costs, not just row counts.
•	It supports offline training with a registry and gated deployment.
•	It optionally supports adaptive execution when estimates are wildly wrong mid-flight.
•	It stays safe: feature flags, clamping, decay, outlier handling, rollback, versioning, privacy/tenancy boundaries.

This plan is written as an engineering execution blueprint: data structures, interfaces, invariants, failure modes, and rollout.

⸻

1) Requirements and invariants (non-negotiables)

This section is intentionally “strict”. A feedback loop is a power tool; power tools need guards.

1.1 Correctness invariants
1.	Semantics must not change.
The feedback system may only affect physical planning decisions (join order, access path, algorithm choice, memory reservations, vectorization strategy), never results.
2.	Fail closed.
If anything is missing/invalid—store read fails, schema mismatch, model load error, sketch corrupted—planner must instantly revert to baseline estimation and proceed. No query failures should be caused by the feedback system.
3.	Deterministic keying.
The fingerprint/key used in the planner must match the key emitted by the executor for the same node. If planner and executor disagree on key identity, the system learns nonsense.
4.	Bounded resource usage.
Memory and disk use must have hard caps and predictable eviction. Planner-time lookup must be O(1) or O(log n) with very small constants.
5.	Explainability.
Every adjustment must be explainable: “I adjusted this estimate by 3.1× because I have 42 recent observations for this key; median log-error = …; clamp applied = …; parent prior = …”.
6.	Plan stability and anti-flap.
Small changes in evidence must not cause plan thrashing. Switching plans must require meaningful predicted improvement (stickiness thresholds) and bounded swing factors.

1.2 Performance invariants
1.	Planning latency impact must be near-flat.
Target: low single-digit percent overhead in APPLY modes, ideally less. Cache lookups and key construction must be cheap.
2.	Execution overhead must be near-flat in steady state.
Event emission should be a few atomic increments and a ring buffer write; avoid allocation per row; emit at operator close, not per tuple.
3.	Batching is mandatory.
Write amplification kills. Updates to persistent stores must be aggregated per flush batch.

1.3 Operational invariants
1.	Feature-flagged modes with safe defaults.
Default shipping mode must be OBSERVE or OFF. APPLY modes must be opt-in.
2.	Versioned data and models with purge.
If anything changes (feature schema, key schema, estimator logic), the stored evidence must be versioned. Purge must be safe and easy.
3.	Multi-tenant safety (if relevant).
Evidence must not leak across tenants unless explicitly intended. Even in single-tenant engines, partition evidence by dataset/graph or cluster.

⸻

2) System architecture (the whole machine)

A fully featured solution is best thought of as four interacting subsystems:

2.1 Telemetry subsystem (executor-side)
•	Each operator collects actual runtime metrics:
•	rows out (produced)
•	rows in (consumed), where meaningful
•	CPU time and wall time
•	IO bytes/pages
•	memory high-water
•	spill stats (bytes, partitions)
•	vectorization stats (batches, batch sizes)
•	parallelism stats

At operator completion, it emits a compact FeedbackEvent keyed by a deterministic FeedbackKey.

2.2 Feedback plane (aggregation + storage)

Events flow through:
•	per-thread ring buffers (to keep hot path lock-free)
•	a periodic flusher thread (or cooperative flush)
•	an in-memory aggregator (key → aggregated stats for the batch)
•	a persistent store update (KV store)
•	a hot in-memory cache for planner lookup (LRU-ish)

2.3 Estimator stack (planner-side)

Planner uses a stacked estimator:
•	baseline (existing statistics, heuristics)
•	online correction factors (error ratio models)
•	micro-models (sketches/hists) for filters and value distributions (later parts)
•	hierarchical shrinkage (global → graph → predicate → pattern)
•	uncertainty estimates (variance / quantiles)
•	risk-aware costing and plan stability rules

2.4 Model management (offline optional; later parts)
•	Export anonymized aggregated training data
•	Offline training for improved cardinality + cost models
•	Model registry, canary deployment, gated inference
•	Rollback and audit

2.5 Adaptive execution (optional; later parts)
•	Runtime triggers detect “we’re 100× off”
•	Re-optimization or algorithm switching within bounds
•	Runtime filters and join reordering where possible

⸻

3) Glossary (to keep us honest)
   •	Cardinality: estimated number of rows/solutions produced by a plan node.
   •	Selectivity: fraction of input rows passing a filter or producing matches; in RDF, often per pattern.
   •	BGP: Basic Graph Pattern (SPARQL set of triple patterns).
   •	Bound-variable signature: which positions/variables are bound at evaluation time (important in star joins).
   •	Fingerprint: a canonical hash representing the semantic structure of a pattern/operator context.
   •	EWMA: exponentially weighted moving average.
   •	Log-error: log(actual/predicted), typically natural log; additive and symmetric for multiplicative errors.
   •	Shrinkage: blending estimates toward a prior when evidence is weak; prevents overfitting to noise.
   •	Flapping: plan instability where small estimate changes cause frequent plan changes.
   •	Hot cache: in-memory cache for planner lookups, shielding disk store.

⸻

4) Data model v3 (keys, events, stats)

The “all in” version needs more than one key type. But the principle is consistent: keys are compact; keys represent the “thing” being estimated; keys are stable across runs and between planner/executor.

4.1 Key families

Start by explicitly defining key types; don’t overload one struct forever.

4.1.1 PatternKey (triple/statement pattern)
Used for base pattern selectivity/cardinality.

Fields (suggested):
•	long patternHash
Canonical hash of the triple pattern or statement pattern (including predicate ID and variable positions).
•	short bindMask
Encodes which variables/positions are bound at this point in the plan. In SPARQL, bind mask often depends on join ordering.
•	short litBucket
Bucketization of literal characteristics (length/range width/type).
•	short graphId
Small integer ID for dataset/graph (dictionary encoded).
•	byte indexTag
Encodes which index/access path used (SPO, POS, OSP, etc. or engine-specific).

Optional (not in v1 key to avoid fragmentation; track as context):
•	byte direction for path expansion direction
•	byte evalMode (iterator vs vectorized)
•	byte partitionHint (cluster shard)

4.1.2 FilterKey (filter family + features)
Used for FILTER selectivity models.

Fields:
•	int filterKind (enum code: regex, range, in-list, langMatches, contains, etc.)
•	short featureBucket1 (e.g., pattern length bucket; range width bucket)
•	short featureBucket2 (e.g., case sensitivity; anchor presence; list length bucket)
•	short bindMask
•	short graphId

4.1.3 JoinKey (join context + algorithm)
Used to learn join behavior under skew/memory/algorithm.

Fields:
•	long leftHash (hash of left subtree semantic fingerprint)
•	long rightHash
•	short joinVarMask (which vars are join keys)
•	byte joinType (inner, left outer/optional, etc.)
•	byte algorithmTag (hash, merge, nested-loop, index-nlj, etc.)
•	byte buildSide (left/right)
•	short graphId

You might not want JoinKey active in early phases; it’s powerful but can fragment quickly. In “fully featured,” you want it eventually.

4.1.4 OptionalKey / UnionArmKey / PathKey / GroupKey
These become critical in SPARQL/RDF:
•	OptionalKey learns null-extension rates.
•	UnionArmKey learns branch probabilities.
•	PathKey learns chain selectivity decay.
•	GroupKey learns group-by NDV-ish behavior.

Each should have:
•	a semantic hash
•	bound context signature
•	graph/dataset identity
•	(optional) execution mode

4.2 FeedbackEvent (executor → feedback plane)

The event should be compact, fixed-size, and allocation-free in the hot path.

A practical event schema:

record FeedbackEvent(
long queryId,        // ephemeral per execution; for debugging correlation
int  nodeId,         // stable within plan
byte nodeType,       // scan/filter/join/agg/optional/union/...
long planShapeHash,  // stable hash of plan shape
long keyHi,          // pack key (or hash) hi bits if needed
long keyLo,          // pack key lo bits
long predictedRows,
long actualRows,
long wallNanos,
long cpuNanos,
long bytesRead,
long spilledBytes,
long peakMemBytes,
int  batchCount,     // vectorized
int  avgBatchSize,
byte execMode,       // iterator/vectorized/pipeline stage
byte engineVersionTag
) {}

Notes:
•	You can pack the key into two longs, or store a 128-bit hash, depending on collision tolerance. For a KV store you may want the full structured key, but on the event path you can store a packed representation and decode later if needed.
•	You do not need to store huge context; you only need what you will use for learning and debugging. Keep it small.

4.3 FeedbackStats (in store)

For “all in,” store robust error stats and uncertainty, not just a single EWMA.

Minimum recommended stats per key:

record FeedbackStats(
int    n,              // observations
long   lastSeenNanos,
double ewmaLogErr,     // mean log(actual/pred)
double ewmaVarLogErr,  // variance of log error (EWMA)
double ewmaLogRows,    // optional: log(actualRows) trend, if helpful
double ewmaLogCost,    // optional: log(actual cost proxy)
float  q50LogErr,      // optional: approximate median log-error
float  q90LogErr       // optional: tail log-error
) {}

If space is tight, you can omit quantiles and keep only EWMA mean + variance. But quantiles are extremely useful for risk-aware planning later.

⸻

5) Fingerprinting and canonicalization (the “make keys sane” work)

This is the most underappreciated part. If you get this wrong, the system either learns nothing (too fragmented) or learns lies (collisions/mismatched semantics).

5.1 Canonicalization goals
•	Equivalent patterns produce identical fingerprints even if:
•	variables are renamed (?x vs ?y)
•	triple patterns appear in different textual order when semantically commutative
•	irrelevant formatting differs
•	Non-equivalent patterns should, ideally, not collide. But we accept a controlled collision rate if we use robust stats and clamp effects.

5.2 Canonical variable renaming

For a pattern or BGP fragment, perform a deterministic rename:
•	Walk patterns in a deterministic order (see next section).
•	Assign first-seen variable → v0, next → v1, etc.
•	Replace variable names with these canonical names.

This avoids key fragmentation by variable naming.

5.3 Deterministic order for hashing BGPs

BGPs are sets, but evaluation context depends on join order. For fingerprinting you generally want semantic identity, not chosen join order (unless you are explicitly learning per join position, which is later).

Suggested approach:
•	Build a multiset of canonicalized triple patterns.
•	Sort patterns by a stable comparator (predicate ID, then subject/object “kind” (var/IRI/lit), then their canonical variable indices, etc.).
•	Hash the ordered sequence.

This yields a BGP hash that is stable across syntactic orderings. If you need a plan-node key for a specific triple pattern, hash just that pattern plus the local binding context.

5.4 Literal/range bucketization

Bucketization is critical for generalization: you want “similar” literals to map together without storing raw values.

Examples:

5.4.1 String literal length buckets
•	0
•	1–3
•	4–7
•	8–15
•	16–31
•	32–63
•	64–127
•	128–255
•	256–511
•	512+

Store a small bucket ID.

5.4.2 Numeric ranges (for range filters)
Bucket by log2(rangeWidth):
•	width 0 (equality)
•	1–1
•	2–3
•	4–7
•	8–15
•	…
•	2^k–(2^(k+1)-1)

Also record datatype family bucket (int/decimal/datetime).

5.4.3 IN-list length buckets
•	1
•	2–3
•	4–7
•	8–15
•	16–31
•	32+

5.4.4 Regex feature buckets
You don’t parse regex fully; you bucket:
•	pattern length bucket
•	has anchors? (^ or $)
•	has wildcards or character classes?
•	case-insensitive flag?

Each becomes a few bits.

5.5 Bind masks (SPARQL/RDF specifics)

In RDF triple pattern evaluation, selectivity depends strongly on what is bound (S/P/O) and what is a variable.

A practical bind mask:
•	3 bits: whether subject/predicate/object are bound to constants at compile time
•	3 bits: whether subject/predicate/object are bound at runtime due to joins (var already bound)
•	or simpler: encode which variables in this pattern are already bound.

For star joins: the number of already-bound vars matters a lot; represent:
•	bound count bucket (0,1,2,3+)
•	plus position mask (which positions bound)

5.6 Graph/dataset identity

If your engine has named graphs or partitions, selectivity can differ drastically per graph. So include a graphId in keys. But ensure it’s stable:
•	dictionary encode graph IRIs to small ints
•	version the mapping or store the IRI hash; avoid per-run random IDs.

5.7 Index/access path tags

The physical access path changes performance and sometimes effective selectivity due to index structure and filter pushdown.

Define indexTag as a small enum:
•	SPO, SOP, PSO, POS, OSP, OPS (or whatever the engine supports)
•	plus special tags: fullScan, bitmap, columnar, etc.

5.8 Plan shape hash and node IDs

Two separate identities:
•	Node key (what we learn): semantic + context features.
•	Plan shape hash (debugging/analysis): physical plan structure identity.

Plan shape hash should be stable:
•	Serialize operator tree with node types and algorithm tags.
•	Include join order and chosen index tags.
•	Exclude ephemeral details (memory addresses, temporary IDs).

Node IDs:
•	assign during plan construction with a preorder traversal
•	stable within the plan; not stable across different plans, which is fine.

⸻

6) Telemetry subsystem (executor-side), engineered for low overhead

This section goes deep because the “fully featured” solution depends on capturing more than just row counts.

6.1 Operator lifecycle hooks

You need a consistent lifecycle for every plan node:
•	open() / init()
•	next() or produce()
•	close() / finish()

Do not instrument per-row in a naive way. Instead:
•	count rows with a simple increment in the loop that already exists
•	time the operator with start/end nanos, not per-row nanos

Example patterns:
•	For iterator model: increment on each next() that yields a solution.
•	For vectorized model: increment by batch size per batch.

6.2 Counters to collect (fully featured set)

Not all are always available; design for optionality.

Always collect:
•	actualRowsProduced
•	wallNanos (operator exclusive time if possible; inclusive if not)
•	predictedRows (carried from plan)

Collect if cheap:
•	cpuNanos (thread CPU time; can be optional)
•	bytesRead (if storage layer exposes it)
•	spilledBytes and spillEvents (hash joins, sorts, groups)
•	peakMemBytes (if operator manages a buffer/table)
•	batchCount, avgBatchSize (vectorized)

Collect if available and meaningful:
•	page faults / cache misses (platform-dependent)
•	branch mispredicts (rare, likely not worth it)
•	lock contention metrics (maybe at higher level)

6.3 Exclusive vs inclusive time

For learning operator costs you want exclusive time (time spent in operator code not in children). But getting perfect exclusive time is hard.

Approaches:
•	Good enough approach: store inclusive time for each operator and compute exclusives in post-processing using tree structure. Works if the execution model is a strict tree and children are called synchronously.
•	Streaming approach: maintain a per-thread operator stack and pause/resume timers when calling children. More overhead.

In v3, implement:
•	inclusive by default (cheap)
•	optional exclusive in trace/debug mode

6.4 Event emission strategy

Emit one event per operator per query execution on close():
•	predictable volume
•	amortized cost
•	easy to aggregate

For very long-running operators (e.g., big scans), you may also want checkpoint events:
•	every N seconds or M rows, emit a partial update
•	helps adaptive execution triggers and more granular learning

But checkpoint events can blow up volume; keep them optional and sampled.

6.5 Ring buffers and flushers (data plane ingress)

You want minimal contention:
•	Each worker thread writes to its own ring buffer.
•	A flusher thread periodically drains buffers and aggregates.

Design points:
•	Use a fixed-size array of FeedbackEvent structs.
•	Maintain write index with wrap-around.
•	Avoid locks on write; use atomic index increments.
•	On overflow, drop events and increment a counter (bounded loss is acceptable; record it).

6.6 Sampling and trace modes

Fully featured systems need control knobs:
•	sampleRate globally (e.g., 1.0 default in dev, 0.1 in production if needed)
•	traceQueryId list: always sample these queries for deep debugging
•	maxEventsPerQuery: cap event volume if you add checkpoints

Sampling should be deterministic per query fingerprint (hash-based) so you don’t bias toward certain query types randomly.

⸻

7) Feedback plane: aggregation, storage, caching, eviction

This is where most “cool ideas” die in production unless engineered carefully.

7.1 Pipeline stages
1.	Drain ring buffers into an in-memory staging list.
2.	Aggregate by key (and sometimes by (key,nodeType,execMode)).
3.	Update persistent store in batches.
4.	Update hot in-memory cache.
5.	Emit metrics about ingestion health: drops, lag, flush time.

7.2 Batch aggregation logic

Within one flush batch, you may have multiple observations per key (e.g., same query template executed multiple times quickly). Aggregate them into a single update to reduce write load:

For each key, accumulate:
•	count
•	sumPredictedRows
•	sumActualRows
•	sumWallNanos
•	sumCpuNanos
•	sumBytesRead
•	sumSpilledBytes
•	maxPeakMemBytes (or sum; depends on how you model)
•	optionally sumSqLogErr if you want variance updates cheaply

This aggregated record is then used to update EWMA stats.

7.3 Store backend selection (what “fully featured” implies)

For an “all in” solution, the store must:
•	support fast point lookups
•	support frequent batch updates
•	support bounded size and eviction strategy
•	survive restarts safely
•	be easy to version and purge

Candidates:
•	LMDB: fast read performance, memory-mapped, good for KV; but careful with concurrent writes (single writer).
•	SQLite: robust, easy; but write contention and page cache behavior may require tuning.
•	Chronicle Map: off-heap; can be fast; operational complexity.
•	RocksDB: great at writes; but heavier; compaction costs; might be overkill.

A practical architecture:
•	persistent store chosen for reliability (SQLite/LMDB)
•	a hot in-memory cache (Caffeine-like in Java, or custom) hides backend latency

7.4 Concurrency model (planner reads + executor writes)

You will have:
•	many planner threads calling get(key) concurrently
•	one flusher thread (or a few) calling updateBatch()

Recommended patterns:
•	Single writer to persistent store to avoid write contention and simplify backend tuning.
•	Concurrent read cache (lock-free or fine-grained).
•	Writes update cache first or after commit; choose consistency semantics:
•	eventual consistency is fine (learning isn’t transactional)
•	but avoid returning half-updated stats; use atomic swap of FeedbackStats objects in cache.

7.5 Hot cache design

A robust cache should:
•	be bounded by entry count or bytes
•	evict least-recently-used-ish
•	optionally weight by “usefulness” (n, recency)

Cache entry:
•	key → FeedbackStats plus maybe a derived “factor” and confidence so you don’t recompute exp/log frequently.

7.6 Persistent store schema and versioning

You need explicit schema versioning. Store header:
•	storeVersion
•	keySchemaVersion
•	featureSchemaHash
•	engineVersionTag
•	createdAt
•	lastCompactedAt

On startup:
•	if incompatible version: disable feedback and/or purge (configurable)
•	never crash the engine over feedback store mismatch

7.7 Eviction strategy (bounded store)

If the store is truly bounded (e.g., 256MB), you must evict.

Eviction scoring heuristic (example):
•	score = a * log(n+1) - b * ageSeconds - c * variancePenalty
•	Evict lowest scores first, but guarantee:
•	keep keys with high n and recent usage
•	discard ancient low-confidence keys quickly

Implementation:
•	Persistent stores don’t always support easy eviction. Two patterns:
1.	Periodic rebuild/compaction: export top keys to a new store, replace old store.
2.	Maintain an eviction index: keep a separate structure tracking candidate keys for eviction.

For LMDB/SQLite, periodic compaction/rebuild is often simplest.

7.8 Retention/decay (time matters)

Even if you don’t evict, you should decay old evidence:
•	if key hasn’t been seen in maxAge, treat it as missing
•	or reduce its effective n by time decay: effectiveN = n * exp(-age / halfLife)

This prevents stale stats from dominating after data distribution changes.

⸻

8) Online learning math (robust, safe, and cheap)

Now the fun part: how to update stats and how to use them during planning.

8.1 Why log-space is the default

Cardinality errors are multiplicative. A plan predicted 100 but got 1000 is a 10× error. A plan predicted 1000 but got 100 is a 0.1× error. Those are “symmetric” in log space:
•	log(10) = +2.302…
•	log(0.1) = -2.302…

Using log-errors avoids bias and makes averages meaningful.

Define:
•	err = actual / max(1, predicted)
•	logErr = log(err)

8.2 EWMA update rules

For each key you store:
•	μ = ewmaLogErr
•	σ2 = ewmaVarLogErr
•	n, lastSeen

Update with aggregated batch record containing count c and aggregated sums. You can update per observation or use a batch mean:
•	batchMean = average(logErr_i)
•	batchVar = variance(logErr_i) if you compute it

Then:
•	α = alphaFor(count=c) (or constant α)
•	μ' = (1-α)*μ + α*batchMean
•	σ2' = (1-α)*σ2 + α*(batchVar + (batchMean-μ')^2)
This is a standard EWMA variance-ish update (approx).

If you don’t compute batchVar, you can approximate variance using squared error updates:
•	keep EWMA of logErr^2 as well
•	variance = E[x^2] - (E[x])^2

8.3 Robustness: median/quantiles

EWMA can be dragged by outliers. For “all in”, add a tiny quantile sketch (e.g., a compact KLL or t-digest variant) to estimate median and tail.

Store:
•	approximate q50LogErr, q90LogErr

If that’s too heavy per key, store quantiles only for “hot” keys (keys whose hit count exceeds threshold) and otherwise rely on EWMA mean+variance.

8.4 Outlier rejection (don’t learn from garbage)

At update time:
•	compute logErr
•	if abs(logErr) exceeds a hard cap (e.g., 10, which is ~22,000×), treat as anomaly:
•	update a separate anomaly counter
•	optionally ignore it or clamp it

Also consider:
•	if the operator spilled or was throttled heavily, the runtime cost might not reflect typical behavior; still learn rows, but be cautious learning cost.

8.5 Confidence scoring

Planner needs a confidence score to decide how aggressively to apply corrections.

Confidence inputs:
•	n (observations)
•	recency (age)
•	variance (stability)
•	optional: “consistency” between median and mean (outlier signal)

Example confidence function:
•	nEff = n * exp(-age / halfLife)
•	confN = 1 - exp(-nEff / nScale)
•	confVar = 1 / (1 + σ2 / varScale)
•	confidence = confN * confVar

Clamp confidence to [0,1].

8.6 Hierarchical shrinkage (global → graph → predicate → pattern → key)

To prevent overfitting and address data sparsity:
•	maintain stats at multiple levels:
•	Global: across everything
•	Per graphId
•	Per predicateId (for triple patterns)
•	Per patternHash
•	Full PatternKey (patternHash + bindMask + litBucket + indexTag + graphId)

When estimating factor:
•	retrieve stats from the deepest level available
•	blend with parent levels based on evidence

In log-space, shrinkage is simple:
•	μ_final = w0*μ_global + w1*μ_graph + w2*μ_pred + w3*μ_pattern + w4*μ_key where weights sum to 1
•	weights derived from effective n at each level

This yields sensible behavior:
•	rare keys borrow strength from broader categories
•	common keys dominate themselves

8.7 Clamp policy (the safety net)

Two clamps exist:
1.	Factor clamp: factor = exp(μ_final) clamped to [1/maxFactor, maxFactor].
2.	Per-query influence clamp: even if factor says 8×, you may limit to 2× if confidence is low.

Example:
•	maxFactorHard = 8
•	maxFactorSoft = 1 + confidence*(maxFactorHard - 1)
•	clamp factor to [1/maxFactorSoft, maxFactorSoft]

8.8 Applying factor to selectivity vs cardinality

Depending on your existing estimator:
•	If estimator produces selectivity: multiply selectivity by factor, then recompute rows.
•	If estimator produces rows directly: multiply rows by factor.

Prefer adjusting at the most “local” level (selectivity of a pattern) so downstream propagation remains consistent.

8.9 Floor and ceiling constraints

Never allow:
•	negative estimates
•	zero where impossible (but allow zero if you have strong evidence and semantics allow empty results)
•	estimates above known dataset size bounds, if known

Add:
•	minRows = 0 or 1 depending on node semantics
•	maxRows = datasetBound if you have it (or Long.MAX_VALUE)

⸻

9) Estimator stack API (planner-side contracts)

A “fully featured” solution becomes messy unless you formalize a clean estimator interface.

9.1 Estimate object

Every estimate should carry:
•	mean rows
•	uncertainty (variance or quantiles)
•	confidence
•	provenance

Example:

record Estimate(
double meanRows,
double varLogRows,   // uncertainty in log space
double q50Rows,      // optional
double q90Rows,      // optional
double confidence,   // 0..1
EstimateSources sources
) {}

record EstimateSources(
boolean baselineStatsUsed,
boolean onlineCorrectionUsed,
boolean microModelUsed,
boolean offlineModelUsed,
String  debugSummary // optional short string
) {}

9.2 Estimator pipeline

Implement a compositional pipeline:
1.	Baseline estimator returns Estimate.
2.	Online correction layer adjusts it and updates sources.
3.	Micro-model layer (later) may refine certain cases (e.g., filter selectivity).
4.	Offline model layer (later) may override/refine under gating.

This design keeps each layer testable and feature-flagged.

9.3 Planner-time caching

Within a single planning session, repeated nodes/patterns should not re-hit stores repeatedly. Use an ephemeral cache keyed by FeedbackKey:
•	reduce backend lookups
•	improve planning time stability

⸻

10) Planner integration (the mechanics of “use it during planning”)

10.1 Where to hook in

Common hook points:
•	cardinality estimator for scan/pattern operators
•	filter estimator
•	join estimator (left-deep DP enumerator or greedy join orderer)
•	optional/union estimators
•	group/aggregate estimator

In RDF4J-like engines:
•	hook in the cardinality estimator that assigns costs to tuple expressions or statement patterns.

10.2 The core loop (baseline → lookup → blend → adjust → re-cost)

For each node during costing:
1.	Compute baseline estimate E0.
2.	Build FeedbackKey (PatternKey/FilterKey/etc).
3.	Lookup FeedbackStats at multiple levels (hierarchy).
4.	Compute correction factor + confidence.
5.	Adjust estimate to E1.
6.	Provide E1 to the cost model to compute node cost.
7.	Attach adjustment details to the plan for EXPLAIN/debug.

10.3 Don’t re-key on plan shape (initially)

It’s tempting to include plan shape hash or join position in the key. That can fragment evidence badly. In a fully featured system you may later add context conditioning, but start by using context mainly as analysis fields and only add to keys when you have strong evidence it improves predictability.

10.4 Plan stability: stickiness thresholds

A plan change is expensive: caches warm, latency variance, regression risk.

Implement stickiness:
•	For identical semantic query fingerprints, remember previously chosen plan (or its signature).
•	Only switch if new plan’s estimated cost improves by more than δ (e.g., 10%).
•	δ can depend on uncertainty:
•	if uncertainty is high, require bigger improvement to switch
•	if uncertainty is low, allow smaller improvement

Stickiness can be scoped:
•	within a session
•	across sessions via plan cache metadata (bounded)

10.5 Risk-aware costing (uncertainty matters)

If you have uncertainty estimates (variance or q90), you can choose less brittle plans:
•	compute expected cost using mean rows
•	compute tail cost proxy using q90 rows or mean + k*std
•	objective: score = expectedCost + λ * tailCost

This helps avoid plans that are great if estimates are perfect but awful if off by 10× (common with skewed RDF data).

10.6 Explain integration

Planner should store per-node estimation trace:
•	baseline rows
•	factor applied
•	adjusted rows
•	confidence
•	which levels contributed (global/predicate/pattern/key)
•	whether clamped

This trace is later printed in EXPLAIN (ANALYZE, FEEDBACK).

⸻

11) Execution → planning closed loop (how evidence gets used)

A critical detail: the optimizer’s predicted rows must correspond to the same notion of “rows” the executor counts.

11.1 Define “row” precisely

In SPARQL engines, “rows” might mean:
•	solution mappings count (bindings)
•	intermediate tuples
•	distinct solutions vs duplicates
•	streaming results vs buffered results

Pick consistent semantics:
•	predictedRows for a node should match actualRowsProduced by that node.
•	if a node outputs duplicates but later deduplicates, keep it consistent with plan node semantics.

11.2 Predicted rows must be attached to executable nodes

Do not try to reconstruct predicted rows at runtime. You will lose fidelity and create mismatch. Instead:
•	planner compiles each node into an executable operator and stores predictedRows (and key components) in that operator.

11.3 Multi-mode execution

If your engine has multiple execution modes (iterator vs vectorized vs pipeline stages), store mode tags:
•	either as context only
•	or as separate keys if mode changes behavior drastically

In “fully featured,” you probably end up with mode-conditioned cost models, while selectivity remains mostly semantic.

⸻

12) Metrics, observability, and “proof it helped”

A fully featured system needs a lot of measurement, but it must be structured.

12.1 Core metrics (must have)

Planning-time metrics
•	planner latency histogram by mode (OFF/OBSERVE/SHADOW/APPLY)
•	store lookup count, hit rate, average and p95 lookup latency
•	number of clamped adjustments per plan

Execution metrics
•	query runtime histograms (p50/p95/p99)
•	spill incidents and bytes spilled
•	memory peak distributions
•	dropped telemetry events count

Estimation accuracy
Per node type:
•	MAPE of rows: abs(actual - pred) / max(1, actual)
•	log error: abs(log(actual/pred))
•	percent within factor 2 / factor 5 / factor 10

12.2 Plan stability metrics
•	% of identical query fingerprints that changed plan shape hash
•	median time between plan changes for a given fingerprint
•	“switch regret” indicator: switched plan but runtime got worse

12.3 Debug surfaces

Implement:
•	EXPLAIN (ANALYZE) baseline view
•	EXPLAIN (ANALYZE, FEEDBACK) includes predicted vs actual per node and correction factors
•	EXPLAIN (FEEDBACK) without execution: show what corrections would apply at planning time and why

For operators, print:
•	predicted rows
•	adjusted rows
•	actual rows (if analyzed)
•	factor and confidence
•	source levels (global/predicate/pattern/key)
•	clamping indicators

⸻

13) Feature flags and modes (operational control plane)

A fully featured system must be controllable. Define modes precisely.

13.1 Suggested modes
•	OFF: no logging, no lookups, no overhead (except code existence).
•	OBSERVE: collect telemetry, update stores, but planner does not consult store.
•	SHADOW: planner consults store and computes adjusted estimates, but does not change plan selection; emits diffs in EXPLAIN/logs.
•	APPLY_SAFE: apply corrections with conservative clamps, stickiness, and uncertainty-aware gating.
•	APPLY_FULL: apply full estimator stack (including micro-models/offline models when enabled), still bounded.

13.2 Emergency brakes
•	global disable config
•	per-query disable (quarantine list)
•	disable-learning but still apply (rare, but can be useful)
•	disable-apply but still learn (OBSERVE)

13.3 Version gating
•	If store schema mismatch: automatically drop to OFF or OBSERVE.
•	If model registry mismatch: disable offline models; keep online corrections.

⸻

14) Testing strategy (because feedback loops can fail in creative ways)

Part 1 includes core tests; Part 2 will expand into micro-model and offline model tests.

14.1 Unit tests: keying and canonicalization
•	variable renaming invariance
•	order invariance where appropriate
•	graphId stability
•	literal bucket boundary tests
•	bind mask correctness under simulated joins

14.2 Unit tests: EWMA and confidence math
•	logErr computation correctness
•	EWMA convergence behavior
•	variance update sanity
•	confidence monotonicity with n and recency
•	clamp correctness

14.3 Integration tests: end-to-end loop
1.	Run query workload in OBSERVE; ensure store fills and stats n increments.
2.	Run same workload in SHADOW; ensure computed factors are non-trivial and consistent.
3.	Run in APPLY_SAFE; ensure:
•	planning time remains within budget
•	plan changes are limited by stickiness
•	runtime improves or does not regress beyond threshold

14.4 Regression and determinism tests
•	OFF mode must produce identical estimates and plans to baseline (bit-for-bit where feasible).
•	Store corruption simulation must not crash planner; should fail closed.

14.5 Performance tests
•	microbench planner lookup path (hit/miss)
•	flush throughput benchmarks under heavy query load
•	event drop behavior under ring buffer overflow

⸻

15) Delivery roadmap (phased, but “all in” end state)

No time estimates; just sequencing and exit criteria.

Phase 0: Baseline harness + plan/node identities

Exit when:
•	predicted vs actual rows can be reported per node
•	plan shape hash stable

Phase 1: Telemetry ingestion + bounded store + OBSERVE mode

Exit when:
•	steady-state overhead is low
•	store bounded; eviction works; restart load works

Phase 2: SHADOW mode + explain diff

Exit when:
•	adjustments computed and visible
•	evidence quality validated (hit rates, n distributions)

Phase 3: APPLY_SAFE online corrections + stickiness + uncertainty

Exit when:
•	tail latency improves on representative workloads
•	plan flapping controlled

Phase 4+ (micro-models, cost calibration, offline models, adaptive execution) are in Part 2 onward.

⸻

16) Concrete pseudo-code for the core loop (planner + executor)

This is the “minimum complete” glue.

16.1 Planner-side estimate adjustment

Estimate estimatePattern(PatternContext ctx) {
Estimate base = baselineEstimator.estimate(ctx);

if (!config.feedbackPlannerEnabled()) return base;

PatternKey key = keyBuilder.buildPatternKey(ctx);

// Hierarchical lookup
Stats global = store.getGlobalStats(PATTERN);
Stats graph  = store.getGraphStats(ctx.graphId());
Stats pred   = store.getPredicateStats(ctx.predicateId());
Stats pat    = store.getPatternHashStats(key.patternHash());
Stats leaf   = store.get(key);

Correction corr = correctionComputer.compute(global, graph, pred, pat, leaf, nowNanos());

if (config.mode() == SHADOW) {
trace.record(ctx.nodeId(), base, corr, base); // not applied
return base;
}

if (!corr.shouldApply()) {
trace.record(ctx.nodeId(), base, corr, base);
return base;
}

double adjMean = clampRows(base.meanRows() * corr.factor(), ctx.minRows(), ctx.maxRows());
Estimate adjusted = base.withMean(adjMean)
.withSources(base.sources().withOnlineCorrectionUsed(true))
.withConfidence(Math.max(base.confidence(), corr.confidence())); // or blend

trace.record(ctx.nodeId(), base, corr, adjusted);
return adjusted;
}

16.2 Executor-side event emission

void close() {
long actualRows = this.rowsProduced;
long wall = System.nanoTime() - startWallNanos;
long cpu  = cpuTimer.readDeltaNanos(); // optional

FeedbackEvent evt = new FeedbackEvent(
queryId, nodeId, nodeType, planShapeHash,
keyPackedHi, keyPackedLo,
predictedRows, actualRows,
wall, cpu, bytesRead, spilledBytes, peakMemBytes,
batchCount, avgBatchSize,
execMode, engineVersionTag
);

telemetrySink.emit(evt);
}

16.3 Flusher aggregation loop

void flush() {
List<FeedbackEvent> events = ringBuffers.drainAll();

Map<Key128, BatchAgg> agg = new HashMap<>(events.size() * 2);

for (FeedbackEvent e : events) {
Key128 k = new Key128(e.keyHi(), e.keyLo());
BatchAgg a = agg.computeIfAbsent(k, _ -> new BatchAgg());
a.count++;
a.sumPred += e.predictedRows();
a.sumAct  += e.actualRows();
a.sumWall += e.wallNanos();
a.sumCpu  += e.cpuNanos();
// ... other sums/max
}

store.updateBatch(agg);
cache.refreshFromBatch(agg); // optional: store returns updated stats
}


⸻

17) What you have now (end of Part 1)

At this point, the system design includes:
•	Deterministic fingerprinting + key families (pattern/filter/join/etc.)
•	Executor telemetry that is cheap but rich
•	Bounded, versioned feedback storage with caching and eviction
•	Robust online correction models in log-space with shrinkage and confidence
•	Planner integration with stickiness and uncertainty-aware foundations
•	Explainability scaffolding
•	Testing and rollout sequencing up to APPLY_SAFE online corrections

This is the “steel frame” of the whole building.

⸻

**Part 2: Micro‑Models, Cost Calibration, Offline Training, Adaptive Execution, Multi‑Tenant Ops**

This part expands the “fully featured” design into the distribution-aware and cost-aware layers that make the system feel like it has *taste*: it stops treating the data as independent coin flips, starts modeling skew and correlation where it matters, learns cost surfaces (CPU/I/O/memory/spill), and—optionally—adapts when the optimizer’s best guess meets reality and loses.

Part 1 built the steel frame:

* deterministic fingerprinting and key families
* executor telemetry (actual vs predicted)
* bounded feedback store + cache
* robust online correction factors (log-space EWMA + shrinkage + confidence)
* planner integration with stickiness and uncertainty foundations

Part 2 adds the “organs”:

* **micro‑models** (sketches/histograms) that learn distributions
* **RDF/SPARQL-specific correlation models** (characteristic sets, star join conditioning)
* **cost calibration** (learn operator cost parameters and join algorithm break-even regions)
* **offline training** (export → train → registry → gated deploy)
* **adaptive execution** (bounded runtime reoptimization and algorithm switching)
* **multi‑tenant, privacy, poisoning defenses, and operational runbooks**

---

## 18) Micro‑models: what they are, why they matter, and how to keep them sane

### 18.1 The core idea

Online correction factors (Part 1) say: “When we predicted X, reality was usually X·k.” That’s already valuable, but it’s reactive and blunt. Micro-models aim to be *predictive* and more *structural*:

* Instead of learning that a regex filter is “usually 0.02×” this month, learn the **distribution of string lengths** or **prefix frequencies** that determines the regex selectivity.
* Instead of learning that joins on predicate `p` are often underestimated, learn **distinct counts** and **frequency skew** so the join selectivity estimate becomes correct for the right reasons.
* Instead of treating star joins as independent predicate selectivities, learn **correlations** via characteristic sets or star-conditioned models.

Micro-models are deliberately “tiny.” They’re not neural nets. They’re memory-bounded sketches that:

* are cheap to update online
* offer approximate but useful selectivity queries at plan time
* can be turned on only where evidence suggests they will pay for themselves

### 18.2 Principles (micro-model commandments)

1. **Only build micro-models where impact justifies it.**
   Most predicates do not deserve specialized modeling. Some do (high frequency, high cost, or high error).

2. **Never store raw sensitive values in cleartext by default.**
   Use hashing, bucketing, dictionary IDs, or privacy guardrails.

3. **Micro-models must return estimates with uncertainty.**
   If the sketch is sparse or unstable, it should say so and the planner should downweight it.

4. **Updates must be cheap and batched.**
   Do not update sketches per row if you can update per operator close using aggregated data.

5. **Use micro-models as refinements, not single points of failure.**
   They should blend with baseline + online corrections, under clamps and shrinkage.

---

## 19) Micro‑model activation and budgeting (how you don’t melt memory)

### 19.1 Budgeting model: memory is a currency

Define explicit budgets:

* `microModels.maxBytesTotal` (global budget, e.g., 128MB–1GB depending on deployment)
* budgets per model family:

  * frequency/heavy hitters
  * numeric digests
  * HLL distinct counters
  * Bloom/existence filters
  * characteristic sets / predicate co-occurrence stats

Also define per-key budgets:

* `maxSketchBytesPerPredicate`
* `maxSketchBytesPerGraph`
* `maxSketchBytesPerTenant` (multi-tenant)

Budgets are enforced by a **SketchStore** with eviction.

### 19.2 Activation thresholds (the “earn your keep” rule)

A micro-model for a key is activated only if it passes a threshold function that approximates expected benefit.

A practical benefit score (conceptual):

* `benefit ≈ frequency × (estimated runtime impact per misestimate) × (current error magnitude)`

You can approximate each factor cheaply:

* frequency: from FeedbackStore `n` for relevant keys (pattern/filter)
* impact: from observed wall time and downstream cost (operator-level telemetry)
* error: from |ewmaLogErr| and variance

Activation policy example:

* Activate micro-model for predicate `p` in graph `g` if:

  * `n(p,g) >= Nmin` (e.g., 100 observations)
  * `medianAbsLogErr(p,g) >= Emin` (e.g., factor ≥ 3)
  * and `p` accounts for ≥ X% of total operator time in the workload

Keep it deterministic: once activated, it stays active until it falls below a retention threshold (hysteresis). Hysteresis avoids thrashing.

### 19.3 Deactivation and eviction

Deactivation is separate from eviction:

* **Deactivation**: stop using the sketch for planning (confidence low, drift, anomalies).
* **Eviction**: reclaim memory by deleting sketch data for cold/low-benefit keys.

Eviction heuristic for sketches should consider:

* last used in planning (not just last updated)
* memory size
* benefit score
* stability (variance); unstable sketches may be less valuable

### 19.4 SketchStore architecture

Treat sketches as first-class objects with tiering, similar to FeedbackStore:

* Tier 0: in-memory objects for hot sketches (fast)
* Tier 1: optional serialized form on disk (for warm restart)
* Tier 2: optional cluster aggregator (for distributed merging)

Each sketch is keyed by a `SketchKey`:

* e.g., `(tenantId, graphId, predicateId, sketchType, featureVersion)`

Sketch objects have:

* metadata: createdAt, lastUpdate, lastUsed, nUpdates
* memory footprint estimate
* serialization routine
* merge routine (if sketches are mergeable)

---

## 20) Micro‑model types and how they answer “selectivity questions”

### 20.1 Frequency sketches (for equality, IN-list, heavy skew)

#### 20.1.1 Why

RDF data often has extreme skew: a few objects dominate a predicate. Baseline stats that assume uniform distribution are doomed.

Use cases:

* `?s p "USA"` (equality on object)
* `?s p ?o FILTER(?o IN (...))`
* star joins where object equality filters exist

#### 20.1.2 Data structure options

* **SpaceSaving (heavy hitters)**: tracks top-K frequent values with approximate counts.
* **Count-Min Sketch (CMS)**: estimates frequency of any value with error bounds, but needs hashing and careful memory.
* **Combined**: keep heavy hitters exact-ish, and CMS as a fallback for “long tail.”

A good pattern:

* SpaceSaving for top-K (K configurable per predicate)
* CMS for approximate counts for long tail
* plus total count `N` for normalization

#### 20.1.3 Privacy and storage

Store value identifiers:

* dictionary ID if you already have an internal ID mapping
* otherwise store a keyed hash (e.g., SipHash with secret seed) of a normalized value representation

Do not store raw literal strings.

#### 20.1.4 Planner API

`estimateEq(predicateId, valueHash, graphId) -> SelectivityEstimate`

Compute:

* `freq ≈ heavyHitters.get(value) || cms.estimate(value)`
* `sel = freq / N`

Uncertainty:

* heavy hitter estimates are more stable (low variance)
* CMS has known additive error bounds; include them in confidence

#### 20.1.5 IN-list

For IN-lists:

* sum frequencies for each value in the list (with duplicate handling)
* cap at 1.0 selectivity
* if list is huge, approximate:

  * sample values, estimate average frequency, multiply, clamp
  * or build a temporary Bloom filter for list membership and ask CMS for those? Usually too heavy; sampling is fine.

---

### 20.2 Distinct count sketches (NDV): HyperLogLog and friends

#### 20.2.1 Why

Joins and group-by estimates depend heavily on distinct value counts:

* join selectivity often approximated as `rowsLeft * rowsRight / max(NDVLeft, NDVRight)` (very rough but better than nothing)
* group cardinality equals NDV of group keys (bounded by input rows)

In RDF, NDV for predicate objects can be wildly different across predicates and graphs.

#### 20.2.2 Data structure

* **HyperLogLog (HLL)** is the standard:

  * mergeable
  * memory efficient
  * good enough accuracy for planning

Maintain HLL sketches for:

* per predicate object NDV: `NDV(o | predicate p, graph g)`
* optionally per predicate subject NDV: `NDV(s | predicate p, graph g)`
* optionally for join keys (vars) at subtree level (more complex; often derived)

#### 20.2.3 Planner usage

* For pattern `?s p ?o`:

  * estimate output rows from base cardinality
  * estimate NDV of `?o` from HLL
* For join on variable `?o`:

  * use NDV estimates to compute join selectivity
  * incorporate skew with heavy hitters where possible (see later)

Uncertainty:

* HLL has known relative error; store it as part of confidence.

---

### 20.3 Numeric range selectivity: KLL or t-digest

#### 20.3.1 Why

Range filters are common and baseline estimators often assume uniform distribution in numeric ranges. Real data is not uniform (ages, years, prices, timestamps).

Use cases:

* `FILTER(?x > 2020 && ?x < 2023)`
* date ranges
* numeric thresholds in analytics queries

#### 20.3.2 Data structure choice

* **KLL sketch** (Karnin–Lang–Liberty): good quantile estimation, mergeable, typically faster and simpler than t-digest in many implementations.
* **t-digest**: also quantiles, often used widely, but merge behavior depends on implementation.

Pick one and standardize it.

Maintain per `(predicateId, graphId, datatypeFamily)` a quantile sketch of object numeric values:

* stores distribution summary
* can answer approximate CDF queries

#### 20.3.3 Planner usage

To estimate selectivity of `x in [a,b]`:

* estimate `cdf(b) - cdf(a)`
* multiply by base pattern cardinality (or apply as filter selectivity)

Edge cases:

* if predicate sometimes stores non-numeric or mixed datatypes: either separate sketches per datatype family, or only activate when data is clean.

Uncertainty:

* if sketch has few samples, confidence low; fallback to baseline.

---

### 20.4 Existence and “likely non-empty” hints: Bloom filters and zero-hit avoidance

#### 20.4.1 Why

Sometimes the most important question is: “Will this pattern match at all?”
Avoiding work on empty patterns or choosing plans that quickly short-circuit emptiness can massively improve latency.

#### 20.4.2 Bloom filters

Bloom filters can quickly answer: “Have we seen value v for predicate p?” with a false positive rate.

Use cases:

* `?s p "rareValue"`: if Bloom says “definitely not present,” you can predict 0 quickly
* join ordering: prioritize patterns likely to be selective/empty

But Bloom filters can be memory heavy if you do them for many predicates. Use sparingly:

* only for predicates with frequent equality probes
* only when value domain is large and emptiness is common

Privacy:

* store hashed values only

#### 20.4.3 Planner use policy

If Bloom says “definitely not present,” you can:

* set selectivity to ~0 (or 0 with a confidence)
* but guard against misalignment: Bloom built on partial data, partitioned graphs, etc.

Treat as a high-confidence hint only when:

* Bloom coverage is known complete for that graph/tenant
* otherwise treat as moderate confidence and avoid hard zeros unless safe

---

### 20.5 Predicate correlation and star joins: characteristic sets and beyond

This is the big RDF-specific win.

---

## 21) Characteristic sets (CS): modeling predicate co-occurrence for star joins

### 21.1 Why star joins are special

A star join looks like:

* `?s p1 ?o1 . ?s p2 ?o2 . ?s p3 ?o3 ...`

Naive estimation multiplies predicate selectivities assuming independence:

* `sel(p1) × sel(p2) × sel(p3)`

But in RDF data, predicates are *correlated* by entity type. Subjects often belong to classes (people, places, products) with characteristic predicate sets. That means:

* If a subject has `p1`, it’s much more likely to have `p2`.
* The existence of `rdf:type` or certain predicates strongly conditions the presence and distribution of others.

Characteristic sets model this correlation:

* A **characteristic set** is the set of predicates that appear for a subject.
* You track counts of subjects per characteristic set.
* You can estimate how many subjects satisfy a combination of predicates by looking at CS counts and overlaps.

### 21.2 Building CS statistics

#### 21.2.1 Full offline build (initial)

From the dataset, compute for each subject:

* set of predicates associated with it
* represent predicate set as a bitset or hashed set signature
* count how many subjects have each set

Store:

* `CSId` = hash/signature of predicate set
* `countSubjects(CSId)`
* optionally `rdf:type` distribution per CS (helps even more)

But predicate sets can be huge; you need compression:

* only store predicates above a frequency threshold
* represent sets by sorted predicate IDs and hash them
* or use minhash/hyperminhash to approximate similarity and reduce fragmentation (advanced)

#### 21.2.2 Incremental updates (online)

If data changes, update CS counts:

* whenever inserting/removing triples, update the subject’s CS signature
* but this can be expensive. Many RDF stores are append-heavy or immutable; choose based on workload.

Pragmatic approach:

* treat CS stats as periodic offline refresh
* online feedback loop corrects in the meantime

### 21.3 Using CS for estimation

Given a star join requiring predicates {p1,p2,p3}, estimate:

* number of subjects whose CS includes all those predicates

If you have exact CS counts:

* sum counts for CSIds that are supersets of required predicate set

But enumerating all supersets can be expensive. Solutions:

1. **Inverted index of predicates to CSIds**
   For each predicate p, maintain list of CSIds containing p. Intersect lists to find CSIds containing all predicates. Then sum counts. This can still be heavy if p is common.

2. **Approximate using frequent CS only**
   Store only top CSIds by frequency and treat the rest as “other.” Estimate using:

* sum counts for matching frequent CS
* plus a small residual estimate from marginal stats

3. **Hierarchical CS**
   Store counts for predicate *combinations* up to size k (k=2 or 3). Then approximate larger stars by chaining conditional probabilities:

* P(p1,p2,p3) ≈ P(p1,p2)×P(p3|p1,p2)

This becomes a micro-model family: correlation stats for small predicate sets.

### 21.4 CS + bound-variable context

Star joins are often executed with some values already bound:

* `?s` may be bound from earlier patterns
* objects may be bound by filters or joins

CS alone estimates existence of predicates; you also need cardinalities for objects:

* for each predicate p, base selectivity and NDV/hist distribution for objects
* combine existence estimate (from CS) with object filter selectivity (from sketches)

### 21.5 CS integration with FeedbackKey hierarchy

CS-based estimation becomes a “micro-model” layer:

* base pattern selectivity from baseline stats
* CS adjusts existence correlation at the star level
* online corrections adjust residual errors and drift

This is important: **CS isn’t perfect**. It’s a structured prior. The feedback loop still corrects.

### 21.6 Operational constraints

CS can be expensive in memory. Make it optional and budgeted:

* store only the most common CS patterns
* store only per graph/tenant where star joins dominate
* compress signatures (e.g., 64-bit hash; accept collisions with robust correction)

---

## 22) Predicate-predicate correlation without full CS (lighter alternative)

If full CS is too heavy, use lighter correlation stats:

### 22.1 Pairwise predicate co-occurrence

Maintain counts:

* `countSubjectsWith(p)`
* `countSubjectsWith(p AND q)` for common pairs

Then estimate:

* P(q | p) ≈ count(p,q) / count(p)

For stars {p,q,r}:

* approximate P(p,q,r) using chain rule:

  * P(p,q,r) ≈ P(p) * P(q|p) * P(r|p,q)
    But you don’t have p,q conditioning for r unless you track triples. So approximate:
  * P(r|p,q) ≈ min(P(r|p), P(r|q)) or some blend.

This is imperfect but often better than independence.

### 22.2 Frequent itemset mining (bounded)

You can mine frequent predicate sets up to size 3:

* store counts for those sets
* use them directly for common stars
* fallback to pairwise + marginals for others

Since this can be computed offline, it’s a good “all in” feature with controlled complexity.

---

## 23) Path and property chain modeling (SPARQL paths, length buckets)

Path queries (length 2–4) have distinct behavior:

* selectivity decays with length, but not uniformly
* certain predicates form dense subgraphs; others are sparse

### 23.1 PathKey and path micro-model

Define PathKey:

* predicate sequence signature (maybe hashed)
* length bucket
* direction
* bound mask at start/end
* graphId

Maintain a micro-model for path decay:

* mean log-selectivity per length bucket
* variance
* optionally NDV propagation factors

### 23.2 Using path models

When estimating a property path:

* baseline estimate from heuristics (e.g., branching factor)
* adjust using path micro-model factor
* clamp heavily (paths can be wild)
* if path involves known dense predicates, path model should learn that quickly

---

## 24) OPTIONAL and UNION modeling (null extension and branch probabilities)

### 24.1 OPTIONAL (left outer join) is a cardinality trap

OPTIONAL often surprises optimizers because:

* the optional side may match rarely, producing lots of null-extended rows
* or it may match frequently, multiplying row counts
* correlation with bindings entering OPTIONAL is strong

### 24.2 Optional micro-model

For OptionalKey, store:

* `matchRate = P(optional matches | entering bindings context)`
* `avgMultiplicity = E[#matches per entering binding | match]`

You can estimate:

* null-extension rate = 1 - matchRate
* output rows = leftRows * ( (1 - matchRate)*1 + matchRate*avgMultiplicity )

Learn these from execution:

* track left input rows (entering bindings count)
* track output rows
* track matched rows count if operator can expose it, else infer:

  * matchedRows = outputRows - nullExtendedRows
    But you need nullExtendedRows; many engines can count whether right side produced matches per left binding.

If you cannot instrument per-left-binding, you can approximate:

* assume at most one match per binding for certain patterns, or
* count right side rows and infer coarse match rate with assumptions
  But a fully featured engine should support this: OPTIONAL operators can maintain counters for matched vs unmatched.

### 24.3 UNION branch probabilities

UnionArmKey should learn:

* P(arm produces results | entering context)
* expected rows per arm

Planner then estimates union output rows as sum of arms:

* sum_i armRows

But also learns:

* overlap probability if arms are not disjoint (hard)
  You typically assume arms are disjoint unless evidence suggests otherwise. If overlap matters, treat as advanced.

---

## 25) FILTER modeling (family-based micro-models)

Filters come in families, each with distinct selectivity structure:

* regex / string contains / prefix
* numeric range
* equality and IN-list
* lang matches / datatype checks
* function predicates (custom functions)

### 25.1 Filter family taxonomy

Define a stable enum:

* `REGEX`
* `CONTAINS`
* `PREFIX`
* `SUFFIX`
* `RANGE_NUMERIC`
* `RANGE_DATETIME`
* `EQUALITY`
* `IN_LIST`
* `LANG_MATCHES`
* `DATATYPE_IS`
* `CUSTOM_FUNC_HASHED` (bucket by function ID)

Each family has feature buckets and micro-models:

* regex: pattern length, anchors, case-insensitive
* range: width bucket, datatype family
* IN: list length bucket
* custom: only if stable and common

### 25.2 Micro-model outputs

Micro-model returns:

* selectivity estimate (mean, uncertainty)
* optionally “cost multiplier” for expensive filters (regex is CPU-heavy)

You’ll use this both for cardinality and cost.

### 25.3 Learning filter selectivity

Learn from execution:

* input rows to filter operator
* output rows from filter operator
* selectivity = out / in

But careful: if filter is pushed down into index scan, you may not have a separate filter operator. In that case:

* record filter family in PatternKey context
* or attach FilterKey to the scan operator
  Either way, you must attribute selectivity changes to filter micro-model.

---

## 26) Micro-model merge strategy (distributed and/or multi-instance)

If you have multiple engine instances:

* each instance collects telemetry and updates local sketches
* you may want to merge them periodically

Sketches like HLL, KLL, CMS, SpaceSaving are mergeable (with caveats).
Define a merge protocol:

* periodic snapshot export (serialized sketches + metadata)
* central aggregator merges and redistributes
* or peer-to-peer merge if you want

But merging has privacy and tenancy implications; see later.

---

## 27) Cost model calibration (beyond rows): learn CPU, I/O, memory, spill

The second major pillar of “fully featured” is cost calibration. Many systems have decent row estimates but still poor cost estimates because:

* CPU per row varies with operator type, predicate complexity, and data width
* I/O depends on index type, locality, cache warmth
* spills have threshold behavior (sudden huge penalty)
* vectorization changes constants dramatically
* parallelism introduces overhead and non-linear scaling

### 27.1 Cost model philosophy

Do not aim for perfect prediction. Aim for:

* correct *relative* ordering of candidate plans
* accurate detection of catastrophic regimes (spill, huge join build)
* stable calibration that improves over time without oscillation

A practical approach:

* keep a structured, interpretable cost model with learnable coefficients
* use online calibration for “constants”
* use offline training for harder interactions
* always fall back to baseline if uncertain

### 27.2 Operator cost decomposition

Represent cost as a sum of components:

* CPU cost: `cpu = a0 + a1*rowsIn + a2*rowsOut + a3*predicateComplexity + ...`
* I/O cost: `io = b0 + b1*pages + b2*randomAccessPenalty + ...`
* Memory cost: not direct time, but influences spill probability and algorithm choice
* Spill penalty: `spillCost = spillBytes * c1 + spillEvents * c2 + fixedPenalty`
* Parallelism overhead: `overhead = d0 + d1*threads + d2*syncPoints`

At runtime you can measure:

* wall time
* CPU time (if available)
* bytes read
* spilled bytes
* peak memory

You can then calibrate coefficients.

### 27.3 Feature set per operator type

Define per operator type which features matter. Keep it sparse.

#### 27.3.1 Scan/index lookup

Features:

* estimated rows
* average row width
* indexTag
* filter pushdown presence
* graphId/partition
* cache warmness proxy (optional)
  Observations:
* bytesRead and wall time

#### 27.3.2 Filter

Features:

* rowsIn
* filterKind
* pattern length bucket (regex)
* value type
  Observations:
* wall time, cpu time

#### 27.3.3 Hash join

Features:

* buildRows, probeRows
* join key width
* expected output rows
* build side choice
* expected hash table size
* available memory
  Observations:
* peakMem, spilledBytes, wall time

#### 27.3.4 Nested-loop / index nested-loop

Features:

* outerRows
* inner cost per probe (from scan/index model)
* join selectivity
  Observations:
* wall time, cpu

#### 27.3.5 Sort / group / aggregation

Features:

* input rows
* key width
* expected distinct groups (NDV)
* memory available
  Observations:
* spill, wall time

### 27.4 Online calibration methods

You need an algorithm that:

* is stable
* is incremental
* can be bounded and robust to outliers

Options:

1. **Recursive least squares (RLS)** per operator family
   Good for linear models; can adapt gradually.

2. **Stochastic gradient descent** on a small linear model
   Simpler but requires step-size tuning.

3. **Robust regression with Huber loss**
   Helps with outliers; can be done incrementally.

A pragmatic “fully featured” path:

* start with RLS or SGD for a small linear model in log-space
* use robust loss or clamp updates
* calibrate separately per execution mode (iterator vs vectorized) because constants differ

### 27.5 Log-space for cost too

Costs can be multiplicative. Modeling `log(time)` vs features can produce better stability. But:

* additive decomposition becomes harder
* interpretation less direct

Compromise:

* model CPU per row linearly (interpretable)
* model spill penalties in log or piecewise linear
* optionally model total time in log-space for coarse calibration

### 27.6 Spill modeling (threshold behavior)

Spills are catastrophic. A good “fully featured” model should predict spill probability.

Approach:

* treat spill as a binary event with probability depending on:

  * predicted hash table size / available memory
  * predicted distinct groups for aggregation
  * row width
* learn a logistic regression:

  * `P(spill) = sigmoid(w · features)`
* if spill probability high, planner penalizes plans that rely on in-memory hash/sort
* if spilledBytes measured, learn expected spill magnitude as well

If implementing full logistic regression online is too heavy, use a simpler heuristic:

* maintain empirical spill threshold per operator type:

  * “hash join spills when buildRows*rowWidth exceeds ~X bytes”
* update X via EWMA based on observed spills

### 27.7 Join algorithm break-even learning

Join algorithm choice is one of the most sensitive decisions. A fully featured solution learns:

* when hash join wins
* when nested-loop wins
* when merge join wins (if sorted inputs exist)
* when index nested-loop wins

Plan-time decision: choose algorithm with lowest predicted cost *and acceptable risk*.

Calibration tasks:

* for each algorithmTag, maintain cost parameters:

  * build cost per row, probe cost per row, overhead
  * plus spill threshold / penalty
* learn from observed join operator telemetry
* incorporate memory availability and row width

Risk-aware algorithm choice:

* if spill probability high for hash join, choose alternative even if mean cost suggests hash join (tail-risk penalty)

---

## 28) Combining micro‑models + online corrections + cost calibration (estimator stack semantics)

The estimator stack becomes multi-dimensional:

* cardinality mean + uncertainty (rows)
* selectivity mean + uncertainty
* cost mean + uncertainty
* spill risk

### 28.1 Blend order (recommended)

1. Baseline statistics produce an initial estimate.
2. Micro-models refine selectivity/cardinality for certain cases:

  * equality/IN-list using frequency sketch
  * range filters using digest
  * star join correlation using CS/predicate co-occurrence
3. Online correction factors apply to residual errors and drift:

  * adjust the refined estimate with robust shrinkage
4. Cost model uses final cardinality and micro-model cost hints (e.g., regex CPU factor).
5. Risk-aware scoring uses uncertainty + spill probability.

This order matters: micro-models explain structure; online corrections handle everything micro-models don’t capture.

### 28.2 Avoid double-counting corrections

If micro-model adjusts selectivity and then online correction also adjusts, you might “correct twice.” That’s okay if:

* online correction is learned from the *end-to-end* prediction pipeline; i.e., it reflects residual error after micro-models
  But initially, online corrections were trained on baseline predictions. When you turn on micro-models, your residual error distribution changes.

Solution:

* version the “estimator pipeline signature” and store it in FeedbackStats metadata
* or keep separate correction stats per estimator mode
* or gradually re-learn corrections after enabling micro-models (decay old stats faster)

In a fully featured system, do this explicitly:

* `estimatorSignatureHash` is included in store header and optionally in key family metadata
* when signature changes, either purge or treat existing stats as low-confidence priors

### 28.3 Uncertainty propagation

Uncertainty from micro-model sketches and correction stats should propagate upward. In log space:

* if independent, variances add
* but they are not independent; treat as conservative:

  * `var_total = max(var_baseline, var_microModel, var_correction)` or
  * `var_total = var_microModel + var_correction` with a safety inflation factor

The goal isn’t perfect Bayesian inference; the goal is to avoid overconfidence.

---

## 29) Offline training pipeline (the “grown-up ML” layer)

Online learning is great for quick adaptation and low complexity, but it has limits:

* it can’t easily capture high-order interactions
* it can be noisy under non-stationary workloads
* it’s hard to validate “global changes” without offline evaluation

A fully featured solution includes an offline pipeline, but with strict constraints:

* models must be versioned and auditable
* training data must be privacy-safe
* inference must be fast and bounded
* deployment must be gated and reversible

### 29.1 What offline models are for (and what they’re not for)

Offline models are best for:

* complex interactions between features (e.g., predicate correlations beyond CS heuristics)
* cost surfaces involving multiple variables (rows, width, memory, mode, parallelism)
* join algorithm choice classification under skew and memory constraints

Offline models are not ideal for:

* immediate adaptation to sudden distribution shifts (online handles that)
* fragile features that aren’t stable across engine versions

### 29.2 Training data: what to export

Export **aggregated, anonymized** examples. Do not export raw rows.

A training example for cardinality might include:

* features:

  * patternHash (or hashed)
  * predicateId (dictionary ID, or hashed)
  * bindMask
  * litBucket
  * graphId (or hashed/tenant-scoped)
  * indexTag
  * filterKind + feature buckets
  * star join context features (degree, #predicates in star, presence of rdf:type, etc.)
* label:

  * actualRows (or log actualRows)
* metadata:

  * engine version tag
  * execution mode
  * timestamp bucket

For cost models, include:

* predictedRows and estimated widths
* observed wall/cpu time
* spilledBytes, peakMem
* algorithm tags

Export should be:

* batched
* compressed
* TTL-limited
* tenant-scoped

### 29.3 Privacy and security for exports

Even hashed values can leak information if attackers can guess and hash. Protect by:

* using keyed hashes (secret not shared)
* coarse bucketing rather than value-level identity for sensitive domains
* opt-out controls
* tenant isolation (never mix tenants unless explicitly allowed)

### 29.4 Feature schema versioning

Offline models must be tied to a feature schema:

* a stable registry of feature names, types, bucket boundaries, hashing seeds
* a schema hash included in model metadata
* the engine refuses to load models whose schema hash doesn’t match

This prevents subtle “feature drift” causing silent wrong predictions.

### 29.5 Model types (practical choices)

#### 29.5.1 Cardinality/selectivity model

* gradient boosted trees (GBDT) on bucketized features
* or generalized linear model with interactions if you need speed

GBDT often performs very well with sparse categorical/bucket features.

Target label: `log(actualRows + 1)` to manage heavy tails.

#### 29.5.2 Cost model

* separate models per operator family:

  * scan cost model
  * join cost model
  * sort/group cost model
* either regression on log time or separate CPU and I/O components

#### 29.5.3 Spill prediction

* logistic regression or GBDT classifier:

  * label: spilled? (0/1)
  * features: predicted memory footprint ratio, rows, width, algorithm, mode

#### 29.5.4 Join algorithm choice

* multi-class classifier to recommend algorithm tag
* but use it as a suggestion, not a hard rule:

  * planner still computes costs and applies guardrails
  * classifier only biases choice or sets priors

### 29.6 Offline evaluation (before deployment)

You need a test harness that prevents “model looked good in training” from becoming “production is on fire.”

Evaluate:

* cardinality error distribution on holdout
* plan quality via trace replay or plan simulation:

  * compare plan choices under baseline vs model estimates
* worst-case regressions:

  * look at tail of errors, not just mean

A key metric: “regret”

* how often does the model cause a plan choice that is worse than baseline by >X%?

### 29.7 Model registry

A registry is a system component, not a folder of files.

Registry responsibilities:

* store model artifacts and metadata
* validate schema compatibility
* manage versions and rollout states:

  * `STAGED` → `CANARY` → `ACTIVE` → `DEPRECATED`
* maintain audit logs:

  * who activated which model, when, with what metrics

Model metadata includes:

* modelId, version
* feature schema hash
* training data window
* evaluation metrics summary
* intended scope (operator family, graphId/tenant restrictions)
* inference budget (max depth/trees)

### 29.8 Deployment gating (shadow → canary → apply)

Even offline models should follow the same safe rollout pattern:

* SHADOW: compute model predictions but do not affect plan
* CANARY: apply to small subset of queries or instances
* APPLY: apply broadly, still under clamp and stickiness

The “apply” step should still:

* clamp prediction deltas relative to baseline
* use confidence gating (if features incomplete or out of distribution, fallback)
* record outcomes and allow quick rollback

### 29.9 Online inference constraints

Inference must be bounded and cheap:

* precompile tree ensembles into array-based structures
* avoid allocation
* limit number of trees or depth
* use integer bucket features
* cache per planning cycle

If inference exceeds a budget:

* disable the model for that query
* fall back to micro-model + online corrections
* record a metric so you can tune

---

## 30) Adaptive execution (optional, but “fully featured” wants it)

Adaptive execution is where you stop pretending the optimizer is omniscient. Even with feedback, estimates can be wrong because:

* data distribution shifts suddenly
* correlations not captured
* parameter values unseen before
* concurrency/resource contention changes cost landscape

Adaptive execution says: “If we are *way* off, re-plan the remaining work.”

### 30.1 Design constraints (so adaptation doesn’t become chaos)

1. **Bounded number of adaptations per query**
   Example: max 1–2 re-optimizations.

2. **Bounded time spent adapting**
   Example: adaptation budget ≤ 1–5% of query runtime or an absolute cap.

3. **Safe adaptation points**
   Only adapt at points where semantics and pipeline correctness are preserved.

4. **Auditability**
   Every adaptation decision must be recorded in query profile output.

### 30.2 Detecting “we’re way off”

Define triggers based on ratio and confidence:

* `ratio = actualSoFar / predictedSoFar` (or actualRowsAtCheckpoint / predictedRowsAtCheckpoint)
* trigger if:

  * `ratio > Rhigh` (e.g., 10× or 30×) or `ratio < Rlow` (e.g., 0.1×)
  * and enough work remains (e.g., still have joins/aggregations downstream)
  * and the checkpoint is early enough that changing plan matters

Use log space:

* trigger if `abs(log(ratio)) > logThreshold`

Also include a “noise filter”:

* require minimum rows observed before trusting ratio (e.g., >1000) or time > 100ms
* avoid triggering on tiny samples

### 30.3 Adaptation actions (in increasing complexity)

#### Action A: Operator-local adjustments

* increase/decrease memory reservation for hash tables or aggregations if possible
* switch vector batch size or predicate evaluation strategy
* enable runtime filters (Bloom filters) to reduce downstream work

This is cheap and safe.

#### Action B: Join algorithm switching

If a join is not fully executed yet and your executor supports it:

* switch from hash join to nested-loop (or vice versa)
* switch build side if spill risk discovered
* switch to index nested-loop if indexes exist and outer rows unexpectedly small

Requires operators to support multiple algorithms or pluggable strategies. Many engines can’t do this without rebuilding operator pipelines; design accordingly.

#### Action C: Mid-query re-optimization of remaining join order

This is the hard one.

You need:

* a boundary where you can materialize intermediate results (or you already have a materialization)
* a representation of remaining query subgraph not yet executed
* ability to build a new plan for remainder using updated cardinalities

Common approach:

* identify a “reopt region” in the plan: a set of joins that can be reordered
* materialize current intermediate result (if not already)
* call planner again with updated stats derived from actuals so far
* execute new plan for remainder

Bound it carefully: reopt only for large expensive regions, not for every query.

### 30.4 Runtime statistics feeding into re-optimization

At adaptation time, you can create “runtime overrides”:

* for certain keys/subtrees, replace estimated cardinalities with actual observed counts
* treat them as high-confidence, immediate evidence
* still clamp, but you can trust them more than store stats because they’re from this query

These runtime overrides should not necessarily persist to the global store (or if they do, treat them carefully because they’re conditional on a specific parameter set).

### 30.5 Adaptive execution and the feedback loop

Record adaptation outcomes:

* the decision taken
* the predicted improvement
* the realized improvement
  This becomes training data for:
* spill thresholds
* algorithm break-even models
* adaptation trigger tuning

But keep learning bounded; otherwise you create a feedback loop that “learns to reopt constantly.”

### 30.6 Safety against adaptation thrashing

* once you adapt, don’t adapt again unless things get even worse
* require a benefit threshold for adaptation (estimated savings)
* record “regret” if adaptation made things worse, and bias away next time for same pattern

---

## 31) Poisoning defenses and adversarial workload considerations

A feedback system can be attacked. Even if your users aren’t malicious, weird workloads can act like attackers.

### 31.1 Threat model (what can go wrong)

* A user runs queries crafted to poison stats (inflate or deflate estimates for certain patterns).
* A noisy period (resource contention) teaches cost model wrong constants.
* A rare parameter set causes huge logErr and skews EWMA.
* Multi-tenant leakage: one tenant’s distribution influences another’s plans.

### 31.2 Defensive design patterns

#### 31.2.1 Clamp and shrinkage (already in Part 1)

Hard clamps and hierarchical priors are the first line of defense.

#### 31.2.2 Evidence weighting by trust

Not all evidence is equal. Weight updates by:

* workload class (internal system workloads vs arbitrary user queries)
* authentication/role (admin jobs vs anonymous)
* query fingerprint allowlist/denylist
* sampling weights

This is sensitive but practical: production systems often distinguish trusted workloads.

#### 31.2.3 Outlier rejection and anomaly quarantine

* if a key’s observed logErr spikes wildly, quarantine it:

  * stop applying its corrections
  * decay its stats faster
* maintain anomaly counters per key and per operator family
* if anomalies exceed threshold, mark key as “unstable” and reduce confidence

#### 31.2.4 Time-based stratification

Separate stats by time bucket:

* e.g., maintain “recent” and “long-term” EWMAs
* apply recent more strongly for drift, but keep long-term as stabilizer

#### 31.2.5 Resource-context tagging for cost learning

Cost is sensitive to CPU load, IO contention, cache warmth. If you can, tag evidence with coarse resource context:

* concurrency level bucket
* memory pressure bucket
* cache warm/cold bucket
  Then calibrate cost models per context or downweight evidence from extreme contention periods.

If you can’t, at least:

* downweight evidence when system is saturated
* detect that by queue length, CPU usage, or internal load signals

### 31.3 Multi-tenant isolation

If tenants share the same engine, they must not share evidence unless explicitly enabled:

* separate stores per tenant, or
* partition keys by tenantId and enforce budgets per tenant

Also isolate offline exports per tenant.

### 31.4 Privacy constraints

Even hashed values can leak if attackers can guess them. Use:

* keyed hashes (secret salt)
* rotate keys periodically (but then you lose long-term continuity; treat as acceptable trade)
* or avoid value-level sketches for sensitive predicates; rely on bucketization only

---

## 32) Operations: configuration, dashboards, runbooks, and lifecycle

Fully featured systems succeed or fail in operations.

### 32.1 Configuration surface (expanded)

In addition to Part 1 flags, add:

**Micro-model budgets and toggles**

* `microModels.enabled`
* `microModels.maxBytesTotal`
* `microModels.maxBytesPerPredicate`
* `microModels.activation.minObs`
* `microModels.activation.minBenefitScore`
* per model type toggles:

  * `microModels.freqSketch.enabled`
  * `microModels.ndvSketch.enabled`
  * `microModels.rangeSketch.enabled`
  * `microModels.bloom.enabled`
  * `microModels.characteristicSets.enabled`
  * `microModels.predicatePairs.enabled`

**Cost calibration**

* `costCalib.enabled`
* `costCalib.mode = OFF | OBSERVE | SHADOW | APPLY`
* `costCalib.updateRate`
* `costCalib.contextTagging.enabled`
* `costCalib.spillModel.enabled`

**Offline models**

* `offlineModels.enabled`
* `offlineModels.mode = SHADOW | CANARY | APPLY`
* `offlineModels.registryPath`
* `offlineModels.maxInferenceMicros`
* `offlineModels.maxDeltaFactor` (clamp relative to baseline)

**Adaptive execution**

* `adaptive.enabled`
* `adaptive.maxReoptsPerQuery`
* `adaptive.trigger.logRatioThreshold`
* `adaptive.minRowsBeforeTrigger`
* `adaptive.reoptBudgetMicros`

**Security**

* `feedback.isolation = PER_TENANT | PER_GRAPH | GLOBAL`
* `feedback.trustedWorkloadsOnly`
* `feedback.export.enabled`
* `feedback.export.tenantAllowlist`

### 32.2 Dashboards (what to graph)

You want to see three classes of signals:

**Health signals**

* telemetry drops
* flush lag
* store corruption/mismatch events
* cache hit rate

**Performance signals**

* planning time p50/p95
* query runtime p50/p95/p99
* spill incidence
* memory peaks

**Learning signals**

* estimation error distribution by node type
* adjustment factor distribution (how often clamped)
* confidence distribution
* plan stability rates

For offline models:

* % of queries using offline predictions
* regret rate (model made it worse)
* canary vs control comparisons

### 32.3 Runbooks (what you do at 2am)

#### 32.3.1 Query latency regression after enabling APPLY

Immediate steps:

1. Flip mode to `SHADOW` or `OBSERVE` (instant rollback of apply behavior).
2. If regression persists, disable telemetry to eliminate overhead.
3. Inspect:

  * plan stability metrics (did plans change massively?)
  * adjustment factor clamping counts (were factors huge?)
  * which keys were applied most frequently (top offenders)
4. Quarantine problematic query fingerprints or keys:

  * add to denylist for apply
  * purge store entries for those keys
5. Re-enable APPLY_SAFE gradually with tighter clamps.

#### 32.3.2 Store corruption/mismatch

* system must fail closed automatically
* operator can purge store file and restart if needed
* ensure no crash loops: store load errors should not crash engine

#### 32.3.3 Memory blow-up

* SketchStore eviction should prevent this, but if not:

  * disable micro-models
  * reduce budgets
  * inspect which sketch type dominates memory
  * increase activation thresholds

#### 32.3.4 Offline model regression

* rollback model to previous version in registry
* keep online corrections active
* increase gating strictness (higher confidence required)

---

## 33) Implementation roadmap for “fully featured” beyond Part 1

Part 1 got you through APPLY_SAFE online corrections. Here’s the expanded sequencing for the features in this part.

### Phase 4: Micro-model infrastructure (SketchStore + activation)

Exit when:

* SketchStore exists with budgets and eviction
* activation thresholds work and are observable
* one micro-model type (e.g., NDV via HLL) integrated and improving join/group estimates

### Phase 5: RDF correlation modeling (CS or predicate-pairs)

Exit when:

* star join estimation improves significantly for common stars
* memory footprint controlled
* explain output shows correlation sources

### Phase 6: Cost calibration (operator cost constants + spill modeling)

Exit when:

* cost predictions correlate better with reality
* join algorithm choice improves under memory pressure
* spill probability modeling reduces catastrophic plans

### Phase 7: Offline training + registry + gated inference

Exit when:

* shadow predictions work in production without overhead surprises
* canary apply reduces regret and improves tails
* rollback is proven

### Phase 8: Adaptive execution

Exit when:

* bounded reopt triggers reduce worst-case outliers
* no excessive replanning
* audit trail visible in query profiles

---

## 34) Deep dive: “how do we actually update micro-models from runtime?”

A critical implementation detail: the executor often doesn’t “see” all values; it sees only those touched by the plan. That’s fine—micro-models are workload-driven by design.

But you still need a clean update strategy that doesn’t require per-tuple updates for everything.

### 34.1 Update sources

Micro-model updates can come from:

1. **Background scans** (offline build): full dataset statistics.
2. **Runtime sample updates**:

  * update from values encountered during query execution
  * possibly sampled to reduce overhead
3. **Storage-layer hooks**:

  * if storage already maintains certain stats (NDV, histograms), reuse them

For “fully featured,” you likely do:

* offline build for CS and baseline distributions
* runtime updates for drift and hotspots

### 34.2 Operator-level sampled updates

For example, for predicate object frequency sketch:

* during scan of `?s p ?o`, you might see many `o` values
* updating CMS per value could be expensive

Instead:

* sample values:

  * every k-th row
  * or reservoir sample up to N per operator
* update sketches with sampled values weighted appropriately (importance weighting)

This yields approximate distribution updates.

For NDV HLL:

* HLL update per value is cheap (hash + register update). You might do it for all values for some predicates, but sample if needed.

For range digests:

* update with sampled numeric values.

### 34.3 Handling parameterized queries

If your workload includes parameterized queries (same shape, different constants), micro-models shine:

* frequency sketches help equality selectivity
* range digests help numeric thresholds

But the feedback store keys should avoid storing raw parameter values. Use hashed IDs or bucketization.

### 34.4 “Cold start” behavior

Micro-models start empty. You need a plan:

* baseline stats apply initially
* online corrections begin quickly after k observations
* micro-models activate only after enough evidence
* optional offline bootstrapping builds micro-models before production use

---

## 35) Explainability for micro-models and cost calibration

A fully featured system is only acceptable if you can answer: “Why did you pick this plan?”

Extend EXPLAIN output:

For each node:

* baseline estimate (rows, cost)
* micro-model adjustments:

  * which sketch contributed
  * estimated selectivity and confidence
* online correction factor:

  * factor, confidence, evidence levels
* final estimate and cost
* spill risk estimate if applicable
* if offline model used:

  * modelId/version
  * prediction vs baseline
  * gating reason (“high confidence, features complete”)

For adaptive execution (if executed):

* checkpoint at which trigger fired
* observed ratio and thresholds
* action taken (algorithm switch, reopt)
* measured benefit (optional)

Make it structured, not just text:

* machine-readable JSON output option
* stable field names for tooling

---

## 36) A cautionary note on complexity (and how to keep it from becoming a monster)

“Fully featured” is not “everything at once.” The difference between a powerful system and a fragile one is **layering and gating**.

Every added feature must have:

* an enable flag
* an apply mode
* a shadow mode
* metrics
* a fallback path
* tests
* bounded resources

If a feature cannot be bounded, it doesn’t ship.

---

## 37) Reference pseudo-interfaces for micro-models and cost calibration

These are suggested contracts to keep modules clean.

### 37.1 Sketch API

```java
interface Sketch {
  int bytes();                 // approximate footprint
  void merge(Sketch other);    // for distributed merge
  byte[] serialize();
}

interface FrequencySketch extends Sketch {
  void update(long valueHash, int weight);
  FrequencyEstimate estimate(long valueHash);
}

record FrequencyEstimate(double freq, double relError, double confidence) {}
```

### 37.2 NDV sketch

```java
interface NdvSketch extends Sketch {
  void update(long valueHash);
  long estimateNdv();
  double confidence();
}
```

### 37.3 Range digest

```java
interface RangeSketch extends Sketch {
  void update(double value);
  double cdf(double value);          // approximate
  double confidence();
}
```

### 37.4 Cost model API

```java
interface OperatorCostModel {
  CostEstimate estimate(NodeContext ctx, Estimate rowEst);
  void updateFromRuntime(NodeContext ctx, RuntimeTelemetry tel);
}

record CostEstimate(
  double meanCost,
  double tailCost,
  double spillProb,
  double confidence
) {}
```

### 37.5 Gating policy

```java
interface GatingPolicy {
  boolean allowMicroModel(NodeContext ctx, SketchConfidence conf);
  boolean allowOfflineModel(NodeContext ctx, ModelConfidence conf);
  boolean allowApplyCorrection(NodeContext ctx, Correction corr);
}
```

---

## 38) Part 2 covered:

* micro-model families and how to integrate them
* RDF-specific correlation models (CS and lighter alternatives)
* cost calibration + spill modeling + join algorithm learning
* offline training pipeline + model registry + gated deploy
* adaptive execution triggers and actions
* poisoning defenses and ops/runbooks

Part 3: Key Encodings, Store Engineering, Compaction/Eviction at Scale, Offline Correlation Jobs, Calibration Math, Distributed Merge, Validation Harness, and Migration

Part 1 built the structural frame (telemetry → feedback plane → online correction → planner integration).
Part 2 added organs (micro-models, RDF correlations, cost calibration concepts, offline models, adaptive execution, ops).

Part 3 is the part where we stop waving our hands and start specifying how you build something that:
•	survives restarts and engine upgrades,
•	keeps lookup latency predictable,
•	doesn’t silently fragment keys into useless confetti,
•	doesn’t corrupt itself under concurrency,
•	can be validated and rolled out without playing production roulette.

⸻

39) Key schema engineering: collision strategy, bit layouts, and why “just hash it” is never the whole story

39.1 Structured keys vs hashed keys (pick deliberately)

You have three broad approaches:

A) Fully structured keys in the KV store
You store the actual fields (patternHash, bindMask, graphId, etc.) as the key.
Pros:
•	no collisions (assuming correct canonicalization)
•	easy debugging (“what key is this?”)
•	easy hierarchical lookups (prefix scans, if store supports it)

Cons:
•	bigger keys
•	more CPU to serialize/compare
•	harder if you want extremely fast in-memory caches

B) Hashed keys only (e.g., 128-bit hash of structured key)
Pros:
•	small, fixed-size keys (16 bytes)
•	faster comparisons and cache operations
•	simpler storage indexing

Cons:
•	collisions are possible
•	debugging requires a reverse map (optional) or storing a sample “decoded” key in the value for introspection
•	hierarchical queries become tricky unless you maintain separate hashed spaces per level

C) Hybrid (recommended for “fully featured”)
•	Use structured keys in memory (for construction + debugging)
•	Store hashed keys on disk (for speed and compactness)
•	Maintain a tiny optional “key dictionary” for debugging hot keys (not required for correctness)

Hybrid tends to give the best operational tradeoff: compact disk, fast lookups, and still debuggable.

39.2 Collision risk: quantify it, then decide

If you use a 128-bit hash (SipHash-2-4 or xxHash3 128-bit, etc.) with good distribution, collision probability at your scale is effectively negligible for non-adversarial input.

Rough intuition (birthday bound):
•	With 64-bit hashes, collisions become plausible around ~4 billion keys (2^32).
•	With 128-bit hashes, collisions remain astronomically unlikely even at huge scales.

But “astronomically unlikely” is not “impossible.” So your design should:
•	tolerate a collision gracefully (minor estimation noise) rather than corrupting or crashing
•	optionally detect collisions when feasible (debug mode)

39.3 Collision mitigation strategies (practical)

Strategy 1: Include a small “schema salt” in the hash input
•	Hash input includes: keySchemaVersion, featureSchemaHash, engineMajorVersion, maybe tenantId.
•	This prevents old keys colliding with new schema semantics after upgrades.

Strategy 2: Store a short fingerprint of the structured key in the value
•	E.g., store 32-bit “check” hash of the structured key as a field inside FeedbackStats.
•	On lookup, if the check hash mismatches the expected check hash, treat as miss (or log anomaly).
This helps detect collisions without storing the full structured key.

Strategy 3: Partition hash spaces by key family
•	PatternKey hashes are computed in a namespace distinct from FilterKey hashes.
•	E.g., prepend a family tag byte to the hash input.
•	Prevents accidental cross-family collisions.

39.4 Bit layouts: where packing makes sense (and where it doesn’t)

Packing is useful when:
•	you want fixed-size keys
•	you want fast in-memory maps
•	you want to avoid allocations

But packing structured keys into bit fields is a maintenance hazard if you overdo it. The rule:
•	pack the small fields (bindMask, buckets, tags)
•	don’t pack the large semantic hash (patternHash) beyond storing it as a 64-bit field

A good “packed struct” layout for PatternKey in memory:
•	u64 patternHash
•	u16 bindMask
•	u16 litBucket
•	u16 graphId
•	u8 indexTag
•	u8 keyFamilyTag (PATTERN)

That’s 8 + 2 + 2 + 2 + 1 + 1 = 16 bytes, which is already compact.

If you want a 128-bit on-disk key:
•	hash the above 16 bytes plus schema salt to produce 16 bytes
•	store the 16-byte hash as the key

39.5 Hierarchical levels and how to represent them cleanly

Recall the hierarchy:
•	global → graph → predicate → patternHash → full key

Implementation options:

Option A: separate stores per level
StoreGlobal, StoreGraph, StorePredicate, StorePattern, StoreLeaf
Pros: clean and explicit
Cons: more storage systems and code paths

Option B: one store with a “level tag” and a level-specific key
Key input includes (levelTag, levelKeyFields...)
Pros: simpler storage; one backend; uniform cache
Cons: careful about key construction, but manageable

For “fully featured,” Option B is typically best:
•	define a KeyFamily and a KeyLevel
•	build keys in a standard builder
•	cache entries per (family, level, keyHash128)

This allows consistent gating and metrics.

⸻

40) Dictionary stability: keeping graphId/predicateId stable across restarts and clusters

40.1 The problem

Many engines map IRIs/predicates/graphs to internal numeric IDs at runtime. If those IDs are not stable:
•	your store keys become meaningless after restart
•	evidence cannot accumulate over time
•	worst case: wrong evidence applies to wrong predicate (catastrophic)

So you need stable IDs or a stable hashing strategy.

40.2 Stability options

Option A: Persistent dictionary (preferred if available)
Maintain a persistent mapping:
•	predicate IRI → predicateId
•	graph IRI → graphId
•	possibly datatype IDs, function IDs

Store it on disk, load on startup. Ensure:
•	IDs never change for existing IRIs
•	new IRIs get new IDs appended
•	dictionary itself has a version and checksum

Pros: best accuracy and debuggability
Cons: operational complexity; migration concerns if dictionary file corrupts

Option B: Stable hash IDs (works well if collisions acceptable)
Instead of a sequential ID, use a stable 64-bit hash:
•	predicateId = hash64(predicateIRI)
•	graphId = hash64(graphIRI)

Then store graphId as 16-bit? Not possible. But you can:
•	store full 64-bit hash (bigger key)
•	or compress to 16-bit via a dictionary cache at runtime (danger)

If you need small numeric IDs, you generally still need a dictionary.

Option C: Hybrid dictionary + hashed fallback
•	Use dictionary IDs when known
•	fall back to hashed IDs for unknown/unmapped
•	not ideal for precision, but robust during partial startup or remote merges

40.3 Cluster considerations

In a cluster, IDs must be consistent across nodes if you share evidence. Options:
•	central dictionary service
•	shared dictionary file on distributed storage
•	deterministic hashing only (at cost of larger keys)

A practical compromise:
•	keep evidence per-node by default (no cross-node sharing)
•	optionally enable cluster merge only when dictionary consistency is guaranteed

40.4 Migration and upgrades

If dictionary schema changes, you must version the store keys:
•	store header includes dictionaryVersion and dictionaryHash
•	if mismatch, either:
•	purge store
•	or treat store as “legacy” and ignore it

In fully featured systems, “purge on incompatible dictionary” is the sane default. Evidence is disposable; correctness is not.

⸻

41) Storage backend deep dive: LMDB vs SQLite vs RocksDB vs Chronicle (and what “fully featured” requires)

41.1 Your workload profile (be explicit)

Feedback store workload typically looks like:
•	planner: lots of point lookups, extremely latency-sensitive (p95 matters)
•	executor/flusher: periodic batch updates (write-heavy in bursts)
•	data volume: many keys but small values
•	retention: bounded store with evictions/compaction

SketchStore is similar but:
•	values can be bigger (sketch blobs)
•	updates might be less frequent but heavier
•	merges may be required

So you want:
•	low-latency reads
•	predictable write behavior
•	ability to bound size or rebuild efficiently
•	reliability across crashes

41.2 LMDB

Pros
•	extremely fast reads (memory-mapped)
•	low CPU overhead for lookups
•	stable performance when tuned
•	good for small KV items

Cons
•	single writer transaction at a time (though reads concurrent)
•	resizing requires careful management
•	corruption is rare but recovery procedures need to be defined

When LMDB shines
•	read-heavy systems with batch writes
•	when you can tolerate single-writer updates (you usually can: one flusher thread)

Tuning guidance
•	keep one dedicated writer thread
•	update in larger batches
•	ensure mapsize is large enough; resizing carefully
•	cache hot entries in-memory to reduce disk page faults

41.3 SQLite

Pros
•	ubiquitous, robust, excellent tooling
•	WAL mode provides good concurrency patterns
•	easy schema migrations and introspection
•	easy to “rebuild” via SQL

Cons
•	point lookups can be fast, but sustained write bursts can cause stalls if not tuned
•	page cache behavior can add jitter
•	requires careful index design (primary key on hash)

When SQLite is good
•	you want maximum operational simplicity and introspection
•	you can tune WAL, synchronous settings, page size
•	you use batch transactions (mandatory)

Schema idea
•	table: (key BLOB PRIMARY KEY, stats BLOB, lastSeen INTEGER, n INTEGER, score REAL)
•	index on lastSeen or score if you implement eviction in SQL

41.4 RocksDB

Pros
•	high write throughput
•	compaction can enforce TTL/filters
•	very scalable KV behavior

Cons
•	compaction overhead can create latency spikes
•	operational complexity (tuning, file management)
•	reads can be fast but depend on block cache and compaction state

When RocksDB makes sense
•	huge stores
•	heavy write rates
•	you can afford tuning and monitoring

For a single-node embedded feedback store, RocksDB can be overkill—unless your workload is monstrous.

41.5 Chronicle Map (Java-specific)

Pros
•	off-heap, fast, low GC pressure
•	can persist to disk
•	good for fixed-size keys/values

Cons
•	operational and correctness complexity
•	careful with versioning and upgrades

If your engine is Java and you want minimal overhead and you’re comfortable with Chronicle’s semantics, it can be excellent.

41.6 Recommendation for fully featured

A pragmatic and common architecture:
•	FeedbackStore: LMDB (or SQLite if you prioritize simplicity) + in-memory hot cache
•	SketchStore: in-memory objects + periodic serialized snapshots, optionally backed by LMDB/SQLite blobs for persistence

Reason: the hot cache protects planning latency, and the persistent store provides continuity and restart behavior.

⸻

42) Compaction and eviction mechanics: how you actually keep the store bounded

42.1 Why eviction is non-trivial

KV stores don’t “naturally” evict. If you keep inserting, the store grows.
So you need:
•	a size budget
•	a policy for what to keep
•	a mechanism for removing data efficiently

42.2 Eviction scoring (fully featured)

Define a score for each key that approximates utility:

Utility factors:
•	nEff: effective observations after time decay
•	recency: lastSeen
•	stability: low variance is good; high variance might be noisy and less useful
•	impact: keys associated with expensive operators are more valuable
•	hitRate: how often planner consulted/used it (not just updates)

Example score formula:
•	score = wN*log(1+nEff) + wH*log(1+planHits) + wI*log(1+avgWallNanos) - wA*ageDays - wV*variancePenalty

You don’t need perfect weights; you need monotonic sanity.

42.3 Maintaining plan-hit counts

To score by plan usefulness, track:
•	planHits in the cache (cheap)
•	periodically flush hit counts into store stats (optional)
If you don’t persist it, you can still evict based on n and lastSeen and that’s usually fine.

42.4 Eviction strategies by backend

LMDB / SQLite: periodic rebuild (often simplest)
1.	Iterate all entries (or a sample if huge)
2.	Compute score for each
3.	Select top entries to keep under size budget
4.	Write them into a new store file
5.	Atomically swap files (with careful safety)
6.	Delete old store

This is essentially “compaction by selection.”

Pros:
•	predictable results
•	easy to enforce hard size limit
•	avoids per-delete overhead

Cons:
•	rebuild cost can be high; schedule it off-peak
•	requires temporary disk space

RocksDB: compaction filter / TTL
•	Use compaction filters to drop keys below score threshold or older than TTL
•	Use column family options to enforce size behaviors
This is elegant but requires expertise.

42.5 Incremental eviction (when rebuild is too heavy)

If you need more continuous behavior:
•	keep an approximate “eviction candidate heap” in memory:
•	sample keys, keep lowest-score candidates
•	when store exceeds threshold, delete candidates
But deletes can be expensive and fragmentation can remain; rebuild is still needed occasionally.

42.6 SketchStore eviction

Sketch eviction is often easier:
•	sketches have explicit byte size
•	you can evict largest low-benefit sketches first
•	use an in-memory admission policy (don’t even create sketches until benefit is high)

42.7 Crash safety during rebuild

A rebuild must be safe:
•	write to store.new
•	fsync metadata
•	rename store.new → store.active with atomic rename semantics
•	keep store.old until new is confirmed
•	on startup, detect partial states and recover deterministically

⸻

43) Quantile estimation and variance: what “robust” looks like at scale

43.1 Per-key quantiles are expensive (be strategic)

Storing a KLL/t-digest per key is too heavy unless your key count is small.
So fully featured systems do one of:
•	store only EWMA mean + EWMA variance (cheap)
•	store quantiles only for hot keys (top N)
•	store quantiles at higher hierarchy levels (predicate/graph) rather than leaf keys
•	store a small “P²” quantile estimator (very compact) for median only

43.2 P² quantile estimator (tiny and good enough)

The P² algorithm estimates quantiles using 5 markers and constant memory. It’s not mergeable and not perfect, but for per-key medians it can be excellent:
•	store median only (q50)
•	optionally store q90 if you want tail risk

You can implement:
•	P² median per key for hot keys
•	EWMA mean/variance always

43.3 Variance updates: stable EWMA

Variance in log-space tends to be stable if you clamp outliers. Keep:
•	μ and E[x^2]
•	update both with EWMA
•	derive variance = E[x^2] - μ^2
Clamp variance to a reasonable range to avoid numerical weirdness.

43.4 Uncertainty as a planner input

You don’t need perfect distributions. You need a monotonic sense of “this is shaky.”
A good heuristic:
•	tailFactor ≈ exp(μ + z*sqrt(σ2))
Where z is 1–2 depending on your “tail appetite.”

If you don’t trust normality, use:
•	tailFactor = exp(q90LogErr) when available.

⸻

44) Offline correlation jobs: characteristic sets (CS) in engineering detail

This section gets concrete about computing and serving CS at plan time without turning the engine into a data mining research project.

44.1 Definitions and representation
•	Predicate IDs are stable integers.
•	For each subject s, compute the set Preds(s) = {p | (s p o) exists}.
•	A characteristic set is a unique predicate set signature.

Representation options:
1.	Sorted list of predicate IDs
Canonical and exact; can be hashed for identity.
2.	Bitset
Only feasible if predicate universe is small or you compress heavily.
3.	Hashed signature
A 64-bit hash of the sorted list.

Best practice:
•	store the sorted list (compressed) for the most common sets
•	also store a 64-bit signature for indexing

44.2 Building CS (offline batch job)

Inputs:
•	triple store iterators over (s, p, o) grouped by subject
•	predicate dictionary stable IDs

Algorithm (high-level):
1.	Stream triples sorted by subject (or scan and external sort if needed).
2.	For each subject:
•	collect unique predicate IDs (deduplicate)
•	sort them
•	optionally drop rare predicates below frequency threshold pMin (to reduce set explosion)
•	compute signature sig = hash64(predList)
3.	In a map: CS(sig, predList) -> countSubjects++
4.	Optionally compute additional CS metadata:
•	distribution of rdf:type values (requires another aggregation)
•	average number of triples per predicate within CS (multiplicity info)

Memory management:
•	you cannot keep all CS in memory if huge
•	use streaming aggregation and spill (external sort/group-by)
•	or use a two-phase approach:
•	map phase emits (sig, 1)
•	reduce phase sums counts

44.3 Handling set explosion

Real RDF datasets can generate enormous numbers of unique predicate sets. You must bound it:
•	drop predicates that are too rare (they cause fragmentation)
•	cap predicate list length (ignore tail predicates beyond K most frequent for that subject)
•	keep only top M characteristic sets by frequency; aggregate the rest into an “other” bucket per approximate modeling

This sounds lossy, but for planning it’s often enough. Rare sets contribute little to overall selectivity.

44.4 Serving CS queries at plan time

Planner wants: given a required predicate set R = {p1,p2,p3}, estimate number of subjects whose CS includes R.

Serving options:

Option A: Inverted index by predicate → CS list
Store:
•	for each predicate p:
•	list of CS signatures that contain p
•	plus maybe counts

Then to answer query R:
•	intersect lists for all p in R
•	sum subject counts for remaining CSs

This works when R is small (star size often 2–6). Intersections can be optimized:
•	intersect starting with the rarest predicate (smallest list)
•	store lists as sorted arrays of signatures
•	use galloping intersection

Memory concerns:
•	lists can be large for common predicates
Solution:
•	only build inverted index for predicates used frequently in stars
•	or store top CS signatures per predicate; approximate the rest

Option B: Frequent itemset table up to size k
If you only support up to size k (e.g., 3), store:
•	countSubjects(p)
•	countSubjects(p,q)
•	countSubjects(p,q,r) for frequent triples
Then for star of size > k:
•	approximate using chain rule with pairwise conditionals

This avoids inverted indexes but loses exactness for large stars.

Option C: Hybrid
•	exact inverted index for a bounded set of “important” predicates
•	pairwise/triple stats for broader coverage
•	fallback to baseline independence for the rest

Fully featured systems usually land on Option C.

44.5 Integrating rdf:type

rdf:type is a cheat code for correlation. If available, treat it specially:
•	maintain per-type predicate presence probabilities
•	then if query includes rdf:type constraint, condition star estimation on that type

You can build:
•	type → characteristic set frequency
•	or type → predicate presence rates (simpler)

At plan time:
•	if type constraint exists, use that distribution to estimate star existence.

44.6 CS updates under data changes

If dataset is mutable:
•	either rebuild CS periodically
•	or update incrementally (complex)

Incremental CS update requires tracking each subject’s predicate set and updating counts on inserts/deletes. That can be expensive and tricky to keep consistent.

Pragmatic fully featured approach:
•	treat CS as periodic offline rebuild
•	rely on online corrections to handle drift between rebuilds
•	optionally schedule rebuild when drift metrics exceed threshold

⸻

45) Predicate pair/triple mining (frequent itemsets) for correlation without full CS

45.1 Why you might prefer itemsets
•	Full CS inverted indexing can be heavy
•	Many workloads mostly need correlation for stars of size 2–4
•	Itemsets give you direct conditional probabilities

45.2 Offline mining options

Approach A: Direct counting with streaming maps
If you can stream subjects and their predicate lists:
•	for each subject’s predicate list L:
•	emit all pairs (p,q) in L (for |L| small)
•	emit triples (p,q,r) up to a cap
Then aggregate counts.

This is O(|L|²) per subject, which can be expensive if subjects have many predicates. Cap it:
•	only consider top K predicates per subject (by global frequency)
•	or only consider predicates in a “candidate set” relevant to workload

Approach B: FP-growth / Apriori (classic)
Better for mining frequent itemsets with support thresholds. But may be overkill if you only want pairs/triples.

A pragmatic version:
•	compute frequent predicates first (support > threshold)
•	only count pairs/triples among frequent predicates

45.3 Storage schema for itemsets

Store counts:
•	count(p)
•	count(p,q) for p<q
•	optionally count(p,q,r) for p<q<r

Use stable predicate IDs; keep in a compact structure:
•	store only top M pairs by frequency or by mutual information
•	compress counts (varint)

45.4 Query-time estimation using itemsets

For required set R:

If you have triple counts for R (|R|=3):
•	estimate directly: count(p,q,r)

If |R|>3:
•	approximate using pairwise conditionals:
•	pick a base predicate p0 (rarest)
•	estimate count ≈ count(p0) × Π_i P(pi | p0)
Where:
•	P(pi|p0) = count(p0,pi) / count(p0)

This assumes conditional independence given p0. Not perfect, but often better than full independence.

If you have triple stats, you can do better for |R|=4:
•	choose p0,p1,p2 triple with stats
•	then multiply conditional for p3 based on pairwise min

Again, you’ll still apply online correction factors to residual error.

⸻

46) Online cost calibration math: RLS, forgetting factors, Huber robustness, and clamps

This section specifies a concrete approach to learning operator cost parameters online.

46.1 Pick a model form you can maintain

For each operator family and mode, define a feature vector x and predict a scalar cost y.

Example: predict CPU nanos for a filter operator:
•	y = cpuNanos
•	x = [1, rowsIn, rowsOut, regexLenBucket, isCaseInsensitive]

A simple linear model:
•	y ≈ β · x

For scans:
•	y could be bytesRead or wall time
•	or separate CPU and IO models

For joins:
•	y could be wall time
•	x includes buildRows, probeRows, outputRows, rowWidth, spilled? (but spilled is an outcome; for prediction you might use predicted spill probability instead)

46.2 Recursive least squares (RLS) with forgetting factor

RLS updates β incrementally and is stable for linear models.

Maintain:
•	β (parameter vector)
•	P (covariance-like matrix)

Update per observation (x, y):
•	k = P x / (λ + xᵀ P x)
•	β ← β + k (y - xᵀ β)
•	P ← (P - k xᵀ P) / λ

Where:
•	λ is forgetting factor (0.95–0.999 typical)
•	smaller λ adapts faster but is noisier

This is classic and works well if you:
•	bound features
•	reject outliers
•	clamp updates

46.3 Robustness: Huber loss variant

RLS assumes squared error. Cost observations have outliers (GC pauses, contention, spills).
Huber-style robustness:
•	compute residual r = y - xᵀβ
•	define scale s (EWMA of |r| or median absolute deviation)
•	define clipped residual: r’ = clamp(r, -cs, +cs)
•	update using r’ instead of r

This prevents one pathological query from warping β.

46.4 Feature scaling and normalization

If rows can be huge, features can overflow or dominate.
•	scale rows by e.g. 1e3 or log(rows+1)
•	scale bytes similarly
•	use bucket features instead of raw strings

For cost models, log transforms can be useful:
•	predict log(y+1) as linear function of log(rows+1), etc.
But log models complicate decomposition. Decide per operator family.

46.5 Spill modeling: logistic regression online

For spill probability, you can implement a simple online logistic regression:
•	label z ∈ {0,1} indicates spill occurred
•	features include predicted memory ratio:
•	m = predictedBytes / availableMemoryBytes
•	rows, width, algorithmTag, mode
•	probability p = sigmoid(w·x)
•	update w via SGD:
•	w ← w + η (z - p) x
Use:
•	small η
•	L2 regularization
•	clamp updates

If this feels too heavy, the “threshold EWMA” heuristic is simpler:
•	maintain threshold T such that spills occur when predictedBytes > T
•	when spill occurs, decrease T; when no spill, increase slowly
This is basically learning a boundary.

46.6 Calibrating join break-even points

Join algorithm choice can be learned two ways:

Approach A: Cost model per algorithm
•	maintain cost models for hash join, nested-loop, merge join
•	compute predicted costs and choose min with risk penalty

Approach B: Classifier
•	offline or online classification predicts best algorithm
•	planner uses classifier as prior or tie-breaker

Approach A integrates better with explainability and risk-aware logic.

46.7 Where calibration updates happen

Updates should come from telemetry at operator close.
But do not blindly update:
•	if system is under heavy contention (load bucket extreme), downweight or skip
•	if operator spilled, update spill models and maybe cost models separately
•	if operator is tiny (< threshold rows or time), ignore (noise)

Weight updates by:
•	observation confidence (enough rows/time)
•	resource context trust

46.8 Versioning calibration models

Cost models are tied to:
•	engine version
•	execution mode (vectorization changes constants)
•	hardware characteristics (CPU model, storage speed)

Store calibration model version tags and optionally hardware signature:
•	if hardware signature changes significantly, reduce trust or reset cost calibration

⸻

47) Distributed merge: multi-instance feedback without turning into distributed-systems tragedy

If you have multiple instances, you have options:
•	keep everything local (simplest, safest)
•	merge periodically for shared learning
•	centralized aggregator

47.1 Decide your consistency goal

You do not need strong consistency. You need:
•	eventual convergence
•	bounded staleness
•	no correctness risk if merge fails

So design for:
•	best-effort merges
•	idempotence where possible
•	“merge can be turned off without breaking anything”

47.2 What is mergeable?
•	EWMA stats are mergeable approximately if you store sufficient state (counts, sums).
But EWMA itself is order-dependent. So for mergeability, store additional raw aggregates:
•	n
•	sumLogErr
•	sumLogErrSq
•	lastSeen
Then you can recompute mean/variance and derive EWMA-ish behavior with time decay.

Sketches:
•	HLL is mergeable
•	KLL is mergeable (implementation-dependent)
•	CMS is mergeable
•	SpaceSaving merge is trickier but doable approximately
•	Bloom filters are mergeable by bitwise OR (increasing false positive rate)

So for distributed merge, prefer merge-friendly sketches and stats.

47.3 Merge protocol options

Option A: Central aggregator service (cleanest)
•	instances periodically send snapshots:
•	aggregated stats deltas
•	sketch deltas or full sketches for hot keys
•	aggregator merges and writes to a global store
•	instances pull merged store snapshots periodically

Pros:
•	controlled merges
•	easy monitoring and governance

Cons:
•	extra system component
•	network overhead

Option B: Shared store on shared filesystem (risky)
•	multiple writers to same store is complicated
•	you’ll get corruption or stalls unless backend supports it
Not recommended unless you have a proper distributed KV.

Option C: Gossip merge (cool but complex)
•	peers exchange deltas
•	eventually converges
This is fun in research papers; less fun in production engines.

For “fully featured,” Option A is usually the adult choice.

47.4 Delta vs full snapshot

Deltas are smaller but require bookkeeping and idempotence.
Full snapshots are simpler but bigger.

A pragmatic approach:
•	periodic full snapshot for hot keys (bounded by budget)
•	deltas for very frequent updates

47.5 Tenant isolation in merge

Never merge across tenants by default. If you merge:
•	partition by tenantId
•	ensure dictionary IDs are consistent
•	ensure privacy policies are enforced

⸻

48) Validation harness: trace capture, replay, regret analysis, and statistical confidence

A fully featured optimizer feedback system needs a proper evaluation pipeline. “It seems faster on my laptop” is how you ship regressions.

48.1 What to validate

You’re validating:
1.	estimation accuracy improves (rows and cost)
2.	plan quality improves (runtime and resource usage)
3.	stability remains acceptable (no flapping)
4.	overhead remains acceptable (planning/execution)
5.	safety works (rollback, purge, fail closed)

48.2 Trace capture (EXPLAIN ANALYZE+)

Define a trace record format that includes:
•	query fingerprint (canonical)
•	chosen plan shape hash
•	per-node:
•	predicted rows/cost
•	adjusted rows/cost (if any)
•	actual rows/time/IO/spill/mem
•	keys used
•	environment:
•	engine version
•	config mode
•	resource context buckets (optional)
•	outcome:
•	query runtime
•	result size

Store traces as:
•	local files or structured logs
•	compressed
•	with TTL

48.3 Replay modes

Replay mode A: Estimation-only replay
Given recorded query features, re-run planner estimation logic against stored model states to compare:
•	baseline estimates
•	micro-model estimates
•	correction estimates
•	offline model predictions

This lets you test estimation improvements without re-running queries.

Replay mode B: Plan selection simulation
Use recorded costs and row counts to simulate plan scoring:
•	for each candidate plan in enumerator (if you can record them), compute predicted score under model versions
•	see which plan would be chosen

Hard if you don’t record alternative plans. But you can:
•	record top-k candidates in debug runs
•	or run enumerator in a deterministic “explore” mode for evaluation

Replay mode C: Full query execution A/B
Run the workload against a dataset with:
•	baseline mode vs apply mode
•	compare runtimes and tail latencies

This is the gold standard, but needs controlled environment.

48.4 Regret analysis (the optimizer’s moral ledger)

Define regret per query:
•	regret = runtime(modelPlan) - runtime(baselinePlan)

But if plans differ, you need both runtimes. To compute regret:
•	either run both plans (expensive) or
•	approximate using operator-level costs (less accurate)

In A/B environments, you can compute regret distribution:
•	fraction of queries improved by >X%
•	fraction regressed by >X%
•	worst-case regressions

A fully featured rollout should have a hard guard:
•	if regressions above threshold, roll back

48.5 Statistical testing (don’t get fooled by noise)

Query runtimes are noisy. Use:
•	paired comparisons when possible (same query on same dataset)
•	robust statistics (median, trimmed means)
•	confidence intervals for p95/p99 improvements are hard; use large samples

If you can, use:
•	CUPED or variance reduction techniques (advanced)
•	but even simple robust methods work if sample size is adequate

48.6 Plan stability evaluation

Measure:
•	for each query fingerprint, how often plan changes across runs
•	correlation of plan changes with performance
•	detect flapping:
•	frequent oscillations between two plans

Tune stickiness thresholds based on this data.

48.7 Synthetic worst-case tests (must have)

Create adversarial datasets/workloads:
•	extreme skew (Zipf)
•	heavy correlations and anti-correlations
•	sudden distribution shift mid-run
•	memory pressure causing spills
•	cold cache vs warm cache regimes

These tests ensure your clamps and risk-aware logic prevent catastrophic choices.

⸻

49) Acceptance checklist per phase (the “ship gate” rules)

This is the “fully featured” spec’s backbone: you don’t graduate phases without passing gates.

49.1 Phase 0: Baseline harness + identities

Must pass:
•	per-node predicted rows accessible in executable plan
•	per-node actual rows and wall time collected reliably
•	plan shape hash deterministic across identical planning inputs
•	OFF mode identical to baseline behavior

49.2 Phase 1: Observe-only feedback plane

Must pass:
•	telemetry overhead measured and within budget
•	store grows but bounded by design; no unbounded memory usage
•	flush pipeline stable; no lock contention spikes
•	store survives restart and version mismatch fails closed

49.3 Phase 2: Shadow planner adjustments

Must pass:
•	planner can compute factors and confidence
•	EXPLAIN shows adjustments clearly
•	shadow computations do not materially increase planning time
•	evidence hit rate is non-trivial (avoid key fragmentation)

49.4 Phase 3: APPLY_SAFE online corrections

Must pass:
•	improvement in estimation accuracy on workload
•	runtime tail improves or stays flat; regressions below threshold
•	plan stability within acceptable bounds
•	clamps engaged rarely but effectively prevent blowups
•	rollback is instant and proven

49.5 Phase 4: Micro-model infrastructure

Must pass:
•	SketchStore budgets enforced
•	activation thresholds prevent explosion
•	at least one micro-model improves estimates measurably (e.g., NDV/HLL)
•	explain output clearly indicates micro-model usage and confidence

49.6 Phase 5: Correlation models (CS/itemsets)

Must pass:
•	star join estimates improve for targeted workload class
•	memory footprint acceptable
•	fallback behavior correct when CS data missing
•	drift detection triggers rebuild or downweighting appropriately

49.7 Phase 6: Cost calibration

Must pass:
•	cost model predicts relative plan ordering better than baseline
•	spill risk predictions reduce catastrophic spills
•	calibration stable under load and doesn’t overfit contention noise
•	costs are still explainable (coefficients and terms)

49.8 Phase 7: Offline models

Must pass:
•	training pipeline produces models with documented metrics
•	registry enforces schema hash compatibility
•	inference bounded and doesn’t blow planning time
•	canary rollout shows positive results and low regret
•	rollback tested and reliable

49.9 Phase 8: Adaptive execution

Must pass:
•	adaptation triggers reduce worst outliers in controlled tests
•	bounded reopt: max number and time enforced
•	no semantic changes; correctness tests pass
•	audit trail visible and helpful

⸻

50) Migration across engine versions: schema, signatures, and “how not to brick the store”

50.1 What can change and why it matters

Changes that affect key meaning:
•	canonicalization logic changes
•	literal bucket boundaries change
•	bindMask encoding changes
•	dictionary IDs change
•	new operator types or algorithm tags introduced
•	cost model feature vector changes

If any of these change, old evidence may be:
•	partially useful
•	or actively harmful (applies wrong correction)

50.2 Versioning strategy (fully featured)

You need multiple version tags:
•	keySchemaVersion
•	featureSchemaHash (hash of feature registry/buckets)
•	dictionaryVersion/hash
•	engineMajorVersion
•	estimatorSignatureHash (pipeline composition: baseline+micro-model+correction logic)

Store header contains these.

On startup:
•	if keySchemaVersion mismatch: treat store as incompatible → purge or ignore
•	if featureSchemaHash mismatch: treat as incompatible unless you explicitly support migration
•	if dictionaryHash mismatch: incompatible for shared IDs → ignore
•	if only engineMinorVersion changed: you may keep store but reduce confidence initially

50.3 Migration options

Option A: Purge (default safe)
•	delete store and start fresh
•	keep offline baseline stats (unchanged)
•	accept a re-learning period

This is often the correct choice. Evidence is expendable.

Option B: Partial migration by level
If only leaf key semantics changed but higher levels remain meaningful:
•	keep global/graph/predicate-level stats
•	drop leaf-level stats
This gives you some continuity without wrong specificity.

Option C: Full migration
Rare and complex:
•	require you to transform keys and/or reinterpret stats
•	usually not worth it

50.4 Warm-start after purge

To reduce cold-start pain:
•	keep micro-models built offline (CS, NDV sketches) stable across upgrades where possible
•	start online corrections in conservative mode with low confidence
•	decay quickly until enough new evidence accumulates

⸻

51) “Key fragmentation” diagnosis and remediation (because it will happen)

Even with careful canonicalization, you’ll see poor hit rates sometimes. Fully featured systems include tools to diagnose and fix it.

51.1 Symptoms
•	planner lookup hit rate is low
•	many keys have n=1 or n=2 forever
•	no convergence in estimation errors
•	store fills with low-value entries

51.2 Diagnosis checklist
1.	Are you keying on too much context?
Common mistake: including plan shape hash or join position in key. That fragments.
2.	Are dictionary IDs stable?
If graphId/predicateId shift, keys become random.
3.	Are literal buckets too granular?
If buckets are too fine, you create near-unique keys.
4.	Are canonical variable renamings correct?
If you include original variable names in hash, you fragment by renaming.
5.	Are there too many query templates?
Some workloads have massive template diversity; then rely more on higher-level shrinkage.

51.3 Remediation tactics
•	reduce key dimensions (remove join position, plan hash from key)
•	increase bucket coarseness
•	increase reliance on hierarchical levels (predicate-level) and reduce leaf usage
•	raise minObservations for applying leaf stats; allow broad stats earlier
•	add “query template” fingerprints to group similar patterns

Also add metrics:
•	distribution of n per key
•	top keys by n
•	hit rate by key family and level

⸻

52) Concrete “explain output” formats (so humans can debug without tears)

A fully featured system should provide:
•	human-readable text
•	machine-readable JSON

52.1 Human-readable example snippet

For a filter node:
•	Baseline rows: 1,200,000
•	Micro-model (RangeSketch predicate=ex:age): sel=0.08 (conf=0.74) → 96,000
•	Online correction: factor=1.6 (conf=0.62, clampedSoft=1.6) → 153,600
•	Final estimate: 153,600 (q90≈260,000)
•	Cost model: cpu=… io=… spillProb=0.01

For a hash join node:
•	Build rows (est): 150k (q90 400k), width 64B, memAvail 64MB
•	SpillProb=0.35 → risk penalty applied
•	Alternative algorithm: index-nlj expectedCost lower tail-risk → chosen

52.2 JSON structure (stable schema)

Provide a JSON schema with fields:
•	nodeId, nodeType, keyFamily, keyHash
•	baselineEstimate
•	microModelEstimate(s)
•	correctionEstimate
•	finalEstimate
•	costBreakdown
•	spillRisk
•	confidence and reasons
•	clamps applied

Stable JSON lets you build tooling and dashboards that aren’t brittle.

⸻

53) Putting it all together: the “fully featured” control algorithm (planner scoring)

At full maturity, the planner’s scoring per candidate plan looks like:
1.	For each node:
•	compute baseline estimate
•	apply micro-model refinements (if available and gated)
•	apply online correction (if gated)
•	optionally apply offline model override/refinement (if gated)
•	produce Estimate(mean, tail, confidence)
2.	Compute node cost:
•	using calibrated cost model with spill risk
•	produce cost mean and tail
3.	Aggregate costs up the plan:
•	expected cost = sum or pipeline-aware aggregation
•	tail cost = sum of tail-ish proxies or worst-case dominated nodes (depends on pipeline)
•	risk penalty = λ * tail cost + spill penalties
4.	Apply plan stability:
•	if chosen plan differs from previous plan for same query fingerprint, require improvement > δ(confidence)
5.	Choose plan with minimal score.

This structure makes each piece “plug in” without turning the planner into a spaghetti bowl.

⸻

54) End of Part 3 (what we covered)

Part 3 specified:
•	how to encode keys, manage collisions, and build hierarchical namespaces
•	how to ensure dictionary stability across restarts/clusters
•	storage backend tradeoffs and recommended architecture
•	real eviction/compaction mechanics
•	quantile/variance choices at scale
•	offline CS and itemset correlation jobs, and how to serve them at plan time
•	concrete online cost calibration math (RLS/Huber/logistic spill)
•	distributed merge design and what’s realistically mergeable
•	a rigorous validation harness and regret analysis
•	acceptance checklists per phase
•	migration/versioning strategy and fragmentation remediation
•	explain output schemas for debuggability

⸻

Part 4: Feature Registry, Memory Math for Sketches, Micro‑Model Playbook by Operator, A/B Rollout Architecture, Adaptive Execution Engineering Details, Reference Implementation Blueprint

Quick orientation: Parts 1–3 established the overall system, the feedback/correction store, micro-model concepts, cost calibration and offline learning, plus the engineering realities of keying, storage backends, compaction, and validation.

Part 4 drills into the remaining “this is how you actually build it without losing your mind” pieces:
•	feature registry and schema evolution (the contract that keeps planner/executor/offline models aligned)
•	memory sizing math for each sketch type (so budgets aren’t vibes)
•	operator-by-operator micro-model integration playbook (SPARQL/RDF specific)
•	a real A/B rollout and canary system (per query template, per instance, per tenant)
•	adaptive execution: safe reopt regions, materialization, runtime overrides, and algorithm switching mechanics
•	a full module boundary map and thread model (reference implementation plan)

I’m keeping the writing “spec-level,” meaning: concrete decisions, interfaces, invariants, and failure modes.

⸻

55) Feature registry: the “single source of truth” for features, buckets, and compatibility

55.1 Why you need a feature registry (the hard lesson)

As soon as you have:
•	planner-side features (keys, buckets)
•	executor-side features (telemetry keys)
•	offline training features (export schemas)
•	runtime inference features (model inputs)

…you have a synchronization problem. If any component encodes a feature differently, your learning becomes garbage. Worse: it becomes quietly garbage.

A Feature Registry solves this by being the canonical contract:
•	what features exist
•	how they are computed
•	how they are bucketized
•	how they are serialized
•	how they are versioned
•	how schema hashes are derived

55.2 Registry requirements

The registry must guarantee:
1.	Determinism: same input → same encoding across threads and processes.
2.	Stability: schema changes are explicit and versioned.
3.	Auditability: you can print “feature schema v17” and know exactly what it means.
4.	Testability: you can run conformance tests between planner and executor encoders.
5.	Minimal runtime overhead: feature computation is cheap; avoid reflection.

55.3 Registry content: what exactly it stores

For each feature you define:
•	Name: "bindMask", "litLengthBucket", "regexHasAnchor", "indexTag", etc.
•	Type: U8, U16, U32, I32, F32, BOOL, etc.
•	Computation: a deterministic function with a documented input domain.
•	Bucketization: boundaries for numeric values or mapping for categorical values.
•	Encoding: bit layout or varint representation.
•	Default/fallback: what happens when feature is missing.
•	Allowed missingness: whether the feature can be absent and still apply a model.
•	Privacy classification: “safe,” “hashed,” “bucket-only,” “disallowed to export.”

Example entry (conceptual):
•	Feature: litLengthBucket
•	input: literal string length
•	buckets: [0], [1–3], [4–7], [8–15], …, [512+]
•	output: U16 bucketId
•	export policy: SAFE_BUCKET

55.4 Feature schema hash

The schema hash is the keystone for compatibility.

Compute it as:
•	hash of the ordered list of feature definitions, including:
•	names
•	types
•	bucket boundaries
•	categorical mappings
•	encoding layouts
•	any salts/constants used

You want the hash to change if anything semantically relevant changes.

Include the hash in:
•	feedback store header
•	sketch store header
•	exported training files
•	model artifacts metadata
•	runtime inference logs (for forensic debugging)

55.5 Schema evolution policy (how to change buckets without chaos)

You will want to change buckets. You will want to add features. You will want to adjust canonicalization. It’s inevitable.

Define rules:

Rule A: additive changes are allowed with a new schema version
Adding a new feature does not break old data, but models trained on old schema can’t run on new schema and vice versa.

Rule B: changing bucket boundaries is a breaking change
Any modification to bucket boundaries increments schema version and changes schema hash.

Rule C: removing a feature is breaking
Avoid removing features; deprecate instead.

Rule D: you may support “compat mode” for inference
If you truly need to run an old model on a new engine:
•	provide a translation layer that reconstructs old feature encoding from new state
•	but treat this as exceptional; it increases maintenance burden

Rule E: store incompatibility should fail closed, not “best effort”
If feature schema mismatch is detected in the store:
•	ignore store entries (or purge), because applying mismatched stats is worse than no stats

55.6 Conformance tests (planner vs executor)

A fully featured system should include an automated test that:
•	generates random query fragments / plan node contexts
•	computes keys/features in planner code path
•	serializes them into executable plan
•	recomputes keys/features in executor code path (using only plan-attached info)
•	asserts exact match

This catches “oops, executor doesn’t have that field” bugs early.

55.7 Feature computation placement: planner-only vs executor-only vs both

Make an explicit matrix:
•	Features needed for keying must be computed in planner and stored into the plan so executor can emit them exactly.
•	Features needed only for debug context can be computed in executor.
•	Features needed only for runtime cost calibration (e.g., observed batch sizes) are executor-only.
•	Features needed for offline export must be available in the telemetry or derivable from it.

⸻

56) Memory math for micro-model sketches (so budgets are real, not hope)

This section provides sizing guidance. Exact sizes depend on implementation, but you need rough formulas to set budgets and prevent accidental explosions.

56.1 HyperLogLog (NDV)

HLL memory depends on precision p:
•	number of registers m = 2^p
•	each register typically 5–6 bits (implementation varies)
•	memory ≈ m * registerBits / 8

Common configurations:
•	p=12 → 4096 registers
•	p=14 → 16384 registers

Example rough sizes:
•	p=12, 6-bit registers: 4096*6/8 ≈ 3072 bytes (~3KB)
•	p=14, 6-bit registers: 16384*6/8 ≈ 12288 bytes (~12KB)

If you keep one HLL per (predicate, graph, datatypeFamily), this adds up quickly:
•	10k predicates × 3KB = 30MB (if only one per predicate)
•	multiply by graphs/tenants and you can hit hundreds of MB

Guidance
•	Use p=12 for most cases.
•	Only use higher p for extremely high-cardinality predicates where precision matters.
•	Activate NDV sketches only for high-impact predicates (activation thresholds).

56.2 KLL sketch or t-digest (range selectivity)

KLL memory depends on parameter k and number of updates. KLL typically stores O(k log n) items, but bounded by compression.

Practical budgeting:
•	choose k such that sketch ~1–10KB per predicate
•	keep one sketch per (predicate, graph, datatypeFamily)

Guidance
•	Start with ~2–4KB per active range sketch.
•	For time-series predicates (timestamps), you might want a slightly larger sketch if range queries are frequent.

56.3 Count-Min Sketch (CMS) for frequencies

CMS memory depends on width w and depth d:
•	counters stored as 32-bit or 16-bit ints
•	memory ≈ w * d * counterBytes

Error is:
•	additive error ≈ εN with ε ≈ e/w
•	failure prob δ ≈ e^{-d}

Typical:
•	d=4
•	w=2^14=16384

Memory:
•	w*d = 65536 counters
•	4 bytes each → 256KB per CMS

That’s too big per predicate unless you activate very selectively.

Guidance
•	Avoid CMS per predicate unless predicate is extremely hot.
•	Prefer SpaceSaving heavy hitters for most predicates.
•	Use CMS only for “global” or “per graph” if needed, or a very small CMS (w=4096) with known error tradeoffs.

56.4 SpaceSaving heavy hitters

SpaceSaving stores K entries: (valueHash, count, error).
Memory ~ O(K).

If you store:
•	8 bytes valueHash
•	4 bytes count
•	4 bytes error
•	plus overhead

Rough:
•	~16 bytes/entry (raw), more in object-heavy languages
•	K=1024 → ~16KB raw, but in Java with object overhead it can blow up to >100KB unless you store in primitive arrays

Guidance
•	implement heavy hitters using primitive arrays (no per-entry objects)
•	K=256 or 512 for many predicates is often enough
•	treat K as budgeted per predicate

56.5 Bloom filters for existence

Bloom filter size depends on:
•	expected insertions n
•	desired false positive rate f
•	bits m ≈ -n ln(f) / (ln 2)^2
•	hash functions k ≈ (m/n) ln 2

Example: n=1e6 distinct values, f=1% (0.01)
•	m ≈ -1e6 * ln(0.01) / 0.480 ≈ 9.6e6 bits ≈ 1.2MB

Per predicate, that’s enormous. So Bloom filters are only for:
•	a small number of predicates
•	or partitioned smaller domains
•	or “prefix Bloom” (store hashed prefixes rather than full values)

Guidance
•	Bloom filters are “special forces,” not infantry.
•	Use only for a handful of predicates where “definitely not present” is very valuable.

56.6 Characteristic sets (CS) memory

CS memory depends on:
•	number of stored CS patterns M
•	size of each CS predicate list
•	inverted indexes

If storing M CS patterns:
•	each CS stores:
•	signature (8 bytes)
•	count (8 bytes)
•	predicate list compressed (varints)
Even with compression, if M is 1 million, you’re in trouble.

Guidance
•	store only top M CS by frequency (e.g., 10k–200k depending on memory)
•	compress predicate lists heavily
•	use inverted index only for predicates involved in typical star joins (workload-driven)
•	keep “other CS residual” as aggregated bucket(s)

56.7 Putting it into a budget plan

A realistic micro-model budget might be:
•	NDV (HLL): 32MB
•	range sketches: 16MB
•	heavy hitters: 32MB
•	CS / correlation stats: 64MB
•	overhead/reserved: 16MB
Total ~160MB

Then per-tenant slicing:
•	hard cap per tenant
•	eviction if tenant exceeds share

The key is: define budgets up front, and enforce them with admission + eviction.

⸻

57) Micro-model playbook by operator type (SPARQL/RDF engine focused)

This is the “if you have operator X, here’s exactly which micro-models apply, what features to use, and how to blend estimates.”

57.1 Statement pattern / scan operator

Goal: estimate rows out and cost for retrieving matches of a triple pattern with some bindings.

Relevant models
•	baseline stats (counts per predicate, etc.)
•	PatternKey online correction
•	equality frequency sketch (if object bound)
•	NDV sketch (if variable output needed for downstream joins)
•	optional existence Bloom hints (rare)
•	cost calibration for indexTag + graphId + mode

Feature inputs
•	predicateId, graphId
•	binding context (bindMask)
•	constant positions (S/P/O bound to constants or already bound vars)
•	indexTag
•	literal bucket (length/type) for object constants

Estimation steps
1.	Baseline: use existing stats (e.g., predicate count and selectivities based on bound positions).
2.	If object is equality constant:
•	ask frequency sketch for predicateId:
•	sel_eq = freq(value)/N
•	apply to baseline (or override baseline eq selectivity)
3.	If range filter pushed down:
•	ask range sketch to estimate selectivity within range
4.	Apply CS correlation? Usually not here; CS is star-level.
5.	Apply online correction factor from PatternKey hierarchy.
6.	Output:
•	mean rows and uncertainty
•	NDV for output variables if needed:
•	NDV(?o|p,g) from HLL
•	bound it by rows

Cost
•	cost model for scan:
•	bytesRead predicted from rows × width / index layout
•	CPU predicted from rows × perRowCPU + overhead
•	adjust cost with online cost calibration.

Notes
•	Scan estimates often dominate everything. Focus on making these robust first.

⸻

57.2 Filter operator

Goal: estimate selectivity and cost (regex can be CPU-heavy).

Relevant models
•	FilterKey selectivity micro-model (family-based)
•	range sketches (if numeric)
•	frequency sketches (if equality/IN-list)
•	online correction for FilterKey
•	cost calibration for filter family

Estimation steps
1.	baseline: heuristic selectivity per filter kind (e.g., regex default 0.1).
2.	micro-model:
•	regex: use feature buckets (pattern length, anchors) to get empirical selectivity for that family
•	numeric range: use range sketch cdf
•	IN-list: sum frequencies
3.	apply online correction factor (residual).
4.	compute cost:
•	cost per row depends on filter kind; calibrate CPU per row by kind and complexity buckets.

Uncertainty
•	if filter is rare or micro-model sparse, inflate uncertainty
•	avoid overconfident filtering estimates because they strongly affect join order

⸻

57.3 Join operator (inner joins)

Goal: estimate join output rows and cost; pick join algorithm.

Relevant models
•	NDV sketches for join keys (variable distinct counts)
•	heavy hitters for skew on join key values (if you can approximate)
•	join algorithm cost calibration (hash/nlj/merge)
•	spill risk model (hash joins/sorts)
•	online correction factor (JoinKey optional or higher-level correction)

Estimation options
Option A: classic NDV formula
•	output ≈ leftRows * rightRows / max(NDVleft, NDVright)

But in RDF, NDV estimates can be poor for intermediate results. You often have:
•	NDV sketches per predicate, not per intermediate subtree
So you need approximations:
•	propagate NDV through plan (tracked in Estimate objects)
•	if join key originates from a scan on predicate p, use NDV(p)
•	if multiple sources, approximate using min or harmonic mean

Option B: skew-aware join
If heavy hitters exist for join key distribution:
•	estimate contribution of top values separately
•	long-tail treated as uniform

This is advanced but can dramatically improve certain skewed joins.

Algorithm choice
•	compute cost for candidate algorithms:
•	hash join cost + spill risk penalty
•	index nested-loop using scan cost model for inner probes
•	merge join if inputs are sorted (or sorting cost included)
•	choose minimal risk-adjusted score

Online correction
•	join-level corrections are powerful but fragment; start at higher levels:
•	apply correction at join operator family level if consistently underestimating
•	only activate JoinKey corrections when evidence is strong and keys are not too fragmented

⸻

57.4 OPTIONAL operator (left outer join)

Goal: estimate null extension rate and multiplicity, choose execution strategy.

Relevant models
•	OptionalKey micro-model: matchRate and avgMultiplicity conditioned on entering context
•	online correction for OptionalKey
•	cost calibration (OPTIONAL can behave like join + overhead)

Estimation
Let:
•	L = leftRows entering OPTIONAL
•	matchRate = P(optional matches)
•	mult = expected matches per left row given match

Then:
•	expected output rows = L * ((1-matchRate) * 1 + matchRate * mult)

Also compute expected null extended rows:
•	nullRows = L * (1-matchRate)

This matters for downstream joins because null bindings may block joins.

Execution strategy
•	choose whether to evaluate optional side per left row (nested-loop style) or materialize right side
•	avoid plans that explode optional multiplicity early

Uncertainty and risk
OPTIONAL estimates tend to be uncertain. Use tail-risk penalty to avoid brittle plans.

⸻

57.5 UNION operator

Goal: estimate output rows as sum of arms, account for arm probabilities, avoid expensive arms if likely empty.

Relevant models
•	UnionArmKey: arm output distribution conditioned on entering context
•	Bloom/existence hints (rare but can help emptiness)
•	online correction per arm

Estimation
•	estimate each arm independently, using arm-specific micro-models and corrections
•	total rows = sum arm rows
•	cost = sum arm costs, but consider early termination if query has LIMIT and union ordering can short-circuit (engine-specific)

Overlap
If union arms overlap heavily, sum overcounts. Often ignored unless evidence strongly indicates overlap. Fully featured approach:
•	detect overlap empirically (arm outputs share many results)
•	maintain overlap factor for known union patterns
But this is a later optimization; keep it gated.

⸻

57.6 Property paths / path expansions

Goal: estimate growth/decay with length, avoid explosive expansions.

Relevant models
•	PathKey decay micro-model by predicate sequence and length bucket
•	NDV propagation approximations
•	spill risk if materializing path results

Estimation
•	baseline: branching factor heuristics
•	apply learned decay factors:
•	outputRows ≈ inputRows × branchingFactor^k × decayFactor(k)
•	clamp hard to avoid runaway
•	incorporate LIMIT if query is bounded

Risk
Path expansions are notorious for “oops we enumerated the universe.” Use tail-risk heavily.

⸻

57.7 Group/aggregation and DISTINCT

Goal: estimate number of groups (NDV of grouping keys) and spill risk.

Relevant models
•	NDV sketches (HLL) for grouping keys if derivable
•	group micro-models: empirical group count ratios
•	cost calibration for sort/aggregate
•	spill model for sort/aggregate

Estimation
•	groupCount ≈ NDV(keys)
•	outputRows = groupCount
•	memory needed ≈ groupCount × stateSize
•	spill probability from memory ratio model

Notes
Aggregation cost is often dominated by spill. Predict spill well and you avoid disasters.

⸻

58) A/B rollout architecture: per-template canaries, routing, and “no surprises” deployment

A fully featured feedback optimizer should be deployed like a risky distributed system feature, because it is: it changes behavior in subtle ways.

58.1 A/B granularity options

A) Per instance (easy)
•	some instances run baseline, others run apply
•	simple but results can be confounded by instance differences

B) Per query fingerprint (recommended)
•	deterministic hash of query template decides variant
•	ensures same query shape consistently sees same mode
•	allows paired comparisons (template-level)

C) Per tenant (for multi-tenant)
•	some tenants get canary first
•	good for business rollout, but can bias results if tenants differ

Best practice for engineering evaluation:
•	per query fingerprint routing, optionally stratified by tenant.

58.2 Routing function

Define:
•	fingerprint = canonicalQueryHash(query)
•	bucket = hash64(fingerprint ⊕ rolloutSalt) % 10000
•	buckets 0–X → canary, remainder → control

RolloutSalt allows you to change assignment without changing fingerprint semantics.

58.3 Mode assignment matrix

You want more than two arms:
•	CONTROL: baseline estimator and cost model
•	OBSERVE: collect telemetry only
•	SHADOW: compute predictions/adjustments but don’t apply
•	APPLY_SAFE: apply online corrections + micro-models under conservative clamps
•	APPLY_FULL: apply full stack including offline models and adaptive (if enabled)

During rollout, you can assign:
•	90% CONTROL
•	5% SHADOW
•	5% APPLY_SAFE
Then gradually increase APPLY_SAFE.

58.4 Metrics collection for A/B

For each query execution, log:
•	fingerprint
•	variant/mode
•	plan shape hash
•	runtime, p95/p99 aggregation
•	spilledBytes, peakMem
•	planning time
•	estimate error metrics (pred vs actual per node)

You can aggregate per fingerprint and compare:
•	mean runtime
•	tail runtime
•	plan stability
•	regret proxy (if you can run both plans in test)

58.5 Canary safety rails

Define automatic rollback triggers:
•	if p99 runtime regresses by >Y% over Z minutes on canary
•	if spill incidents increase by >Y%
•	if planning time increases by >Y%
•	if plan stability exceeds a threshold (flapping)

Rollback actions:
•	demote canary from APPLY to SHADOW automatically
•	keep OBSERVE on to continue collecting evidence

58.6 Per-template gating

Some query templates are “high leverage” and also high risk.
Maintain a gating policy:
•	allow apply only for templates with:
•	stable evidence (n high, variance low)
•	good prior performance improvements in shadow/regret analysis
•	keep others in shadow until they qualify

This reduces risk and accelerates benefit.

58.7 Feature-flag layering

Rollout should be layered:
•	enable feedback store and observe mode first
•	then enable micro-model store
•	then enable apply for selectivity corrections
•	then enable cost calibration apply
•	then enable offline models
•	then adaptive execution

Avoid turning on everything at once; debugging becomes impossible.

⸻

59) Adaptive execution engineering: safe reopt regions, runtime overrides, and materialization tactics

Part 2 described adaptive execution conceptually. Here we get into “what can you actually do without rebuilding your entire executor.”

59.1 Adaptive execution prerequisites

You need:
1.	checkpoints where you can observe actual cardinalities early enough
2.	a way to affect remaining work (switch algorithm, reorder, replan)
3.	safety boundaries so you don’t break pipelining or semantics

If your engine is purely streaming and doesn’t materialize intermediates, full reordering mid-flight is hard. But algorithm switching and runtime filters may still be possible.

59.2 Defining reopt regions (practical)

A reopt region is a subgraph of the plan where:
•	multiple join orders are possible
•	you can pause and replan without losing correctness
•	inputs to the region can be treated as materialized or re-iterable

Common reopt region candidates:
•	a block of joins in a left-deep tree where the left input is already materialized
•	a subtree under a materializing operator (sort, hash build, group)
•	a pipeline stage boundary in a staged execution engine

Design approach
•	annotate plan nodes with “reopt boundary allowed” flags during planning
•	choose boundaries where materialization is already happening (cheap) or can be added with bounded overhead

59.3 Runtime overrides: injecting “truth” into replanning

When you trigger reopt:
•	you have observed actual row counts for certain nodes or intermediate results
•	you can treat these as high confidence estimates for replanning

Represent runtime overrides as:
•	a map of KeyHash -> OverrideEstimate(meanRows, confidence=1.0, timestamp)
•	or a map of nodeId -> observedRows
During replanning, estimator stack checks overrides first.

Important: runtime overrides are conditional on this query’s parameters and environment. They should:
•	heavily influence the local replanning
•	optionally update the global store with caution (downweight as “parameter-specific” evidence)

59.4 Materialization: when you must and how you limit damage

Replanning typically requires a materialized intermediate:
•	you can’t reorder joins if the current pipeline has already consumed streams you can’t replay

Materialization choices:
•	in-memory buffer (bounded)
•	spilling to disk (avoid if possible)
•	storing only keys (if join keys enough) to reduce size

Bound materialization:
•	only materialize if predicted benefit exceeds threshold
•	cap materialized size; if exceeds, abort reopt and continue with original plan
•	use sampling: materialize a sample first to estimate whether reopt is worthwhile

59.5 Adaptive actions: implementation detail

59.5.1 Runtime filters (Bloom filters on join keys)
This is often the highest ROI adaptive technique:
•	build side produces join keys
•	create Bloom filter
•	push it into scans/filters on the probe side to reduce rows early

This can be implemented without full replanning if the executor supports filter injection.

Plan-time model:
•	join operator has a “runtime filter output”
•	downstream scan operators accept a runtime filter input

Telemetry:
•	track filter effectiveness: rows filtered, time saved
•	this becomes training data for enabling runtime filters proactively

59.5.2 Algorithm switching (hash join ↔ nested-loop)
To switch algorithms mid-query, you need either:
•	a join operator that can choose algorithm after seeing actual build size
•	or a strategy wrapper that delays algorithm commit until a threshold is crossed

Example:
•	start building hash table, track size
•	if size exceeds memory threshold early:
•	spill-aware hash join mode
•	or switch to partitioned hash join
•	or switch to nested-loop with index probes if the other side is small

This is more feasible than full join reordering.

59.5.3 Replanning remainder
Full replanning is the hardest but can be done if:
•	you have a stage boundary with materialized intermediate
•	you can call planner with runtime overrides and produce a new plan for remaining query fragment

Design:
•	planner produces a “continuation plan” given a base relation representing materialized intermediate result
•	executor executes continuation plan

This requires careful integration:
•	maintain variable binding semantics
•	ensure duplicates and DISTINCT semantics preserved
•	ensure ordering/LIMIT semantics preserved (or handle separately)

59.6 Adaptive execution safety and audit trail

Every adaptation should record:
•	trigger condition (log ratio, observed vs predicted, uncertainty)
•	action taken
•	cost of adapting (time, memory)
•	estimated benefit and realized benefit

Expose in:
•	query profile output
•	logs (with sampling)
•	metrics (adaptations per minute, success rate)

59.7 How adaptive execution interacts with learning stores

Policies:
•	always record telemetry events normally
•	record adaptation events separately
•	for global store updates:
•	you may update selectivity corrections based on actuals, but label them as “post-adaptation” so you can analyze effect
•	avoid using adaptation-induced performance differences to calibrate baseline cost models unless you separate them

This prevents the cost model from “learning” the cost of a plan that it would never pick without adaptation.

⸻

60) Feature registry evolution meets offline models: compatibility contracts and “safe model loading”

Offline models and micro-models depend on stable feature encodings.

60.1 Model artifact metadata contract

Each model must include:
•	modelId, modelVersion
•	featureSchemaHash
•	engineMajorVersion compatibility range
•	modelType (cardinality, cost, spill, join-choice)
•	operatorFamily scope
•	inferenceBudgetMicros
•	expectedFeatureSet with required/optional flags
•	trainingDataWindow timestamps
•	evaluationSummary (error metrics, regret)
•	tenantScope (if restricted)

60.2 Loader behavior

On engine startup or model refresh:
1.	validate featureSchemaHash matches engine registry
2.	validate engine version compatibility
3.	validate model file checksum and signature if you use signed artifacts
4.	load model into memory, compile into fast structures
5.	run a small sanity inference test (known examples)
6.	activate in SHADOW by default unless config says otherwise

If any step fails:
•	keep model inactive
•	log the reason
•	do not crash

60.3 Runtime gating (per query/node)

Even a compatible model should not always apply.
Gate on:
•	feature completeness (required features present)
•	out-of-distribution detection:
•	if feature values are outside training buckets or unseen categories appear often, reduce confidence
•	prediction delta clamp relative to baseline:
•	if model predicts 100× more rows than baseline, clamp or ignore unless confidence very high
•	expected benefit:
•	apply models only for high-impact nodes to save planning CPU

⸻

61) Micro-model parameter selection: concrete starting values and tuning playbook

The fastest way to stall an “all in” project is endless parameter debates. Start with reasonable defaults and tune empirically.

61.1 Heavy hitters (SpaceSaving)
•	default K per predicate: 256
•	activate when:
•	predicate appears in ≥ 1% of scans, or
•	equality filters on this predicate are common
•	update sampling:
•	sample 1 in 10 values for updates under load
•	eviction:
•	evict sketches with low plan-hit rate and low benefit score

61.2 NDV (HLL)
•	p=12 default
•	activate for predicates participating in joins and group-by keys frequently
•	update:
•	update for all values in moderate predicates (cheap)
•	sample for extremely high-volume scans if needed
•	use:
•	join selectivity and group cardinality estimation

61.3 Range sketches (KLL)
•	target 2–4KB per active sketch
•	activate for predicates used in range filters
•	store separate sketch per datatype family (int/decimal/datetime)
•	use:
•	estimate filter selectivity and maybe join key distribution shape (advanced)

61.4 CS / correlation stats
•	store top M CS patterns:
•	start with M=50k or 100k depending on memory
•	inverted index only for top P predicates:
•	P=500–2000 based on workload
•	rebuild periodically or on drift trigger
•	use:
•	star join existence correlation and conditional probabilities

61.5 Spill model
•	start with threshold EWMA heuristic:
•	simpler, robust
•	add logistic model later if needed

61.6 Clamp defaults (safe apply)
•	maxFactorHard for cardinality corrections: 8×
•	maxFactorSoft derived from confidence
•	stickiness δ: 10% expected improvement required to change plan (tune)

⸻

62) Reference implementation blueprint: modules, APIs, and threading model

This section is a “project map” you can hand to a team and implement without ambiguity.

62.1 Modules (recommended boundaries)
1.	feature-registry
•	defines features, buckets, encoding
•	schema hash computation
•	conformance test utilities
2.	fingerprinting
•	canonicalization of queries/patterns/filters
•	query fingerprint computation
•	plan shape hash computation
•	key builder for all key families and levels
3.	telemetry
•	per-operator counters and timers
•	FeedbackEvent struct
•	per-thread ring buffers
•	telemetry sink interface
4.	feedback-plane
•	flusher thread
•	batch aggregator
•	FeedbackStore interface + backend implementations
•	in-memory hot cache
•	compaction/rebuild manager
•	metrics emission
5.	sketch-plane
•	SketchStore interface
•	sketch implementations (HLL, KLL, SpaceSaving, optional CMS/Bloom)
•	activation controller (benefit scoring, admission policy)
•	eviction manager
•	optional persistence snapshots
6.	estimator-stack
•	baseline estimator adapters
•	micro-model estimator layer
•	online correction layer
•	uncertainty propagation
•	optional offline model layer
•	gating policy interface
7.	cost-model
•	operator family cost models
•	online calibration (RLS/SGD)
•	spill model
•	risk scoring (tail penalties)
8.	planner-integration
•	hooks into cardinality estimator and coster
•	plan annotation (node IDs, predicted rows, keys)
•	explain output generation
9.	offline-pipeline (optional external module)
•	export format writer
•	trainer (outside engine)
•	model registry client/loader
10.	adaptive-execution

	•	checkpoint triggers
	•	runtime override store
	•	reopt region manager
	•	algorithm switch hooks
	•	runtime filter injection

	11.	ops

	•	config flags and hot reload (if supported)
	•	dashboards and structured logs
	•	runbooks and safe-mode boot

62.2 Threading model (safe and predictable)
•	Query execution threads:
•	update per-operator counters
•	emit events into per-thread ring buffers (non-blocking)
•	One telemetry flusher thread:
•	drains ring buffers periodically
•	aggregates and updates FeedbackStore and SketchStore
•	Optional compaction thread:
•	runs rebuild/compaction off-peak
•	coordinates safe store swap
•	Planner threads:
•	perform lookups through hot cache
•	never block on store writes
•	Optional model refresh thread:
•	loads new offline models, validates, swaps into active set atomically

Thread safety principles:
•	planner reads must be lock-free or fine-grained
•	store writes are serialized through flusher
•	caches updated atomically

62.3 Atomic swap patterns

For cache and model swaps:
•	use immutable snapshots or versioned pointers
•	swap references atomically
•	avoid partial updates visible to readers

Example:
•	cache stores AtomicReference<CacheTable>
•	compaction builds new table and swaps reference

⸻

63) Test matrix (expanded): what to test, how, and where failures hide

63.1 Unit tests (fast)
•	feature registry hash stability
•	bucketization boundaries and encoding/decoding
•	canonicalization invariance tests
•	key builder consistency
•	EWMA/variance math
•	RLS update correctness
•	sketch merge correctness (HLL, KLL)
•	clamp logic and gating

63.2 Integration tests (engine-level)
•	run sample workload:
•	verify telemetry events emitted
•	verify store updated and persists across restart
•	verify planner shadow shows adjustments
•	verify off mode produces identical plans
•	verify store mismatch fails closed
•	verify sketch budgets enforced under stress
•	verify compaction rebuild produces a valid store and is crash-safe

63.3 Performance tests
•	microbench: planner lookup hit/miss latency
•	stress: high-QPS telemetry emission and flush
•	soak: long-running test with compaction cycles
•	tail stability: p95/p99 planning time under load

63.4 Correctness tests (semantic)
•	query results identical across modes
•	OPTIONAL and UNION semantics preserved
•	DISTINCT and ORDER BY behavior preserved under any adaptation (if adaptive enabled)

63.5 Adversarial tests
•	skewed datasets
•	sudden distribution change
•	contention injection (simulate load)
•	malicious query patterns (poisoning attempts)
Verify clamps and quarantine work.

⸻

64) A practical “day 1 reference config” (fully featured but safe)

A sample initial deployment configuration philosophy:
•	collect broadly
•	apply narrowly
•	keep budgets tight
•	prioritize explainability

Example policy (conceptual):
•	feedback mode: OBSERVE for all, SHADOW for 5% templates, APPLY_SAFE for 1% templates
•	micro-models: enable NDV sketches for high-impact predicates; keep heavy hitters off until evidence supports
•	CS: build offline, enable in SHADOW first
•	cost calibration: OBSERVE first; apply later
•	offline models: disabled until you have stable export + evaluation pipeline
•	adaptive execution: disabled until planner stack stable

This avoids “we changed everything at once.”

⸻

65) Part 4 delivered the engineering “how”: registry, budgets, operator playbook, rollout architecture, adaptive execution mechanics, and module blueprint.

⸻

**Part 5: Reference Pseudo‑Code, Binary Formats, A/B Analytics Automation, RDF‑Specific Estimation Tricks, and an End‑to‑End Walkthrough**

Parts 1–4 gave you the architecture, the micro-model/cost/offline/adaptive components, and the engineering scaffolding (keys, stores, budgets, rollout). Part 5 goes deeper into implementation details: concrete algorithms, pseudo‑code, and data formats—plus the “how do we prove this helped?” machinery and a worked example.

---

## 66) Reference pseudo‑code for major subsystems

This section is deliberately “code-adjacent.” It’s not tied to a particular language/runtime, but it describes the algorithms and invariants precisely enough to implement.

### 66.1 FeatureRegistry: schema, hashing, and conformance harness

#### 66.1.1 Core data structures

You want a registry that is:

* immutable at runtime (so planners/executors don’t see partial changes),
* fast to query,
* explicit about bucket boundaries.

```java
enum FeatureType { U8, U16, U32, U64, I32, F32, BOOL }

record BucketSpec(
  String name,
  FeatureType type,
  // For numeric buckets:
  double[] boundaries,         // sorted ascending; bucket i = [b[i], b[i+1])
  // For categorical:
  Map<String,Integer> catMap,   // stable mapping for known categories (optional)
  int defaultBucket
) {}

record FeatureDef(
  String name,
  FeatureType outType,
  BucketSpec bucketSpec,
  boolean requiredForKeying,
  boolean exportAllowed,
  String privacyClass,         // SAFE_BUCKET / HASHED / DISALLOWED etc.
  int featureId                // stable small ID used in encoding
) {}

final class FeatureRegistry {
  final int schemaVersion;
  final long schemaHash64;
  final byte[] schemaHash128;
  final FeatureDef[] defsById;               // indexed by featureId
  final Map<String, Integer> idByName;       // for debugging & tooling
}
```

#### 66.1.2 Schema hash computation

Determinism is king. Schema hash must be computed from a canonical serialization of all `FeatureDef`s.

Canonical serialization rules:

* feature defs sorted by `featureId`
* include: name, outType, bucket boundaries, catMap entries sorted by key, defaultBucket, required/export flags, privacyClass

Then:

* `schemaHash128 = hash128(bytes)`
* `schemaHash64 = low64(schemaHash128)` (or separate)

This hash is embedded in:

* feedback store header
* sketch store header
* export files
* offline model manifests

#### 66.1.3 Encoder utilities

At runtime you need fast encoders:

```java
final class FeatureEncoder {
  final FeatureRegistry reg;

  int bucketizeNumeric(String featureName, double value) { ... }
  int bucketizeInt(String featureName, long value) { ... }
  int encodeCategorical(String featureName, String cat) { ... }
}
```

Use binary search on `boundaries` for numeric bucketization. Precompute boundaries in primitive arrays; avoid object overhead.

#### 66.1.4 Conformance tests (planner ↔ executor)

Define a test harness that takes random node contexts and asserts the exact same encoding:

```java
class FeatureConformanceTest {
  void run(int iterations) {
    for i in 1..iterations:
      NodeContext ctx = randomNodeContext();
      KeyStruct k1 = PlannerKeyBuilder.build(ctx);
      ExecutableNode planNode = Planner.attachKey(ctx, k1);
      KeyStruct k2 = ExecutorKeyBuilder.recomputeFromPlanNode(planNode);
      assertEquals(k1, k2);
  }
}
```

Make this part of CI. It catches the “executor didn’t get the field” drift that otherwise poisons the system silently.

---

### 66.2 KeyBuilder and hierarchical key namespaces

#### 66.2.1 Uniform key construction across families and levels

A canonical pattern:

```java
enum KeyFamily { PATTERN, FILTER, JOIN, OPTIONAL, UNION_ARM, PATH, GROUP }
enum KeyLevel  { GLOBAL, GRAPH, PREDICATE, PATTERN_HASH, LEAF }

record KeySeed(KeyFamily fam, KeyLevel lvl, long a, long b, int c, short d, byte e) {}
// a,b,c,d,e are generic slots depending on family+level; avoid allocation via builder.

final class KeyHasher {
  final long salt64a;     // from store header or config
  final long salt64b;
  final byte[] schemaHash128;   // include in hash input

  Key128 hash(KeySeed seed) {
    // bytes = fam|lvl|schemaHash|seed fields (canonical order)
    return hash128(bytes);
  }
}
```

The key insight: **all key hashing flows through one function**, and that function includes the schema hash and a family/level tag.

This prevents cross-level collisions and protects against schema drift.

#### 66.2.2 PatternKey leaf seed layout (example)

For PatternKey leaf:

* a = patternHash
* b = predicateId (or 0 if not applicable)
* c = packed small fields (bindMask, litBucket, graphId, indexTag)

Example packing:

* c (32 bits):

  * bits 0..5: indexTag (0..63)
  * bits 6..17: graphId (0..4095)
  * bits 18..25: litBucket (0..255)
  * bits 26..31: bindMaskLow (0..63) (if bindMask needs more bits, store in d)

Then d = bindMaskHigh or additional features.

The point isn’t the exact bit layout; it’s that it is deterministic, documented, and tested.

---

### 66.3 Ring buffer and telemetry sink: low‑overhead ingestion

A fast telemetry system is mostly about:

* **no allocation** per event,
* **no locks** in the hot path,
* and **bounded loss** under overload.

#### 66.3.1 Per-thread ring buffer

Each worker thread has a fixed array `events[]` and an atomic write index.

```java
final class RingBuffer {
  final FeedbackEvent[] buf;          // fixed size
  final int mask;                     // size is power of 2
  final AtomicLong writePos = new AtomicLong(0);
  volatile long readPos = 0;          // read only by flusher

  boolean tryPublish(FeedbackEvent e) {
    long wp = writePos.getAndIncrement();
    long slot = wp & mask;
    // If flusher is too slow, wp - readPos can exceed capacity:
    if (wp - readPos > buf.length) {
      // drop event (record drop counter)
      return false;
    }
    buf[(int)slot] = e;               // write event
    return true;
  }

  int drainTo(List<FeedbackEvent> out, int maxDrain) {
    long wp = writePos.get();
    int drained = 0;
    while (readPos < wp && drained < maxDrain) {
      FeedbackEvent e = buf[(int)(readPos & mask)];
      if (e != null) out.add(e);
      buf[(int)(readPos & mask)] = null; // help GC / reuse slot if needed
      readPos++;
      drained++;
    }
    return drained;
  }
}
```

Important details:

* Keep `buf` as a struct-of-primitives if you can. In Java, `FeedbackEvent` as an object can cause allocation and GC pressure. Prefer a primitive-packed array or an off-heap structure. If you can’t, reduce event size and sample.
* Dropping events under overload is acceptable as long as you record how many were dropped and do not block query execution.

#### 66.3.2 TelemetrySink interface

Let operators emit into an interface so you can swap sinks:

```java
interface TelemetrySink {
  void emit(FeedbackEvent e);
  void emitCheckpoint(FeedbackEvent e); // optional
}
```

Implementations:

* RingBuffer sink (normal)
* No-op sink (OFF)
* Logging sink (debug)
* Direct in-memory aggregator sink (test)

---

### 66.4 Batch aggregation and store updates: fast, write‑amplification resistant

The flusher should:

* drain all ring buffers,
* aggregate by key,
* update store in one batch transaction,
* update hot cache.

#### 66.4.1 Aggregation map design

Using a generic `HashMap` may be fine, but if you want predictable latency under load, use:

* open-addressed hash map with primitive keys (128-bit key split into two longs)
* value is a `BatchAgg` struct

```java
record Key128(long hi, long lo) {}

final class BatchAgg {
  long count;
  long sumPredRows;
  long sumActRows;
  long sumWallNanos;
  long sumCpuNanos;
  long sumBytesRead;
  long sumSpilledBytes;
  long maxPeakMemBytes;
  // optional: sumLogErr, sumLogErrSq if you compute log errors in flusher
}
```

#### 66.4.2 Flusher loop skeleton

```java
final class TelemetryFlusher implements Runnable {
  final RingBuffer[] buffers;
  final FeedbackStore store;
  final HotCache cache;
  final long flushIntervalMillis;

  public void run() {
    while (!shutdown) {
      sleep(flushIntervalMillis);
      flushOnce();
    }
    flushOnce(); // final flush
  }

  void flushOnce() {
    List<FeedbackEvent> events = new ArrayList<>(expectedBatchSize);
    for (RingBuffer rb : buffers) rb.drainTo(events, MAX_DRAIN_PER_BUFFER);

    if (events.isEmpty()) return;

    AggMap agg = new AggMap(events.size() * 2);

    for (FeedbackEvent e : events) {
      Key128 k = new Key128(e.keyHi, e.keyLo);
      BatchAgg a = agg.getOrCreate(k);
      a.count++;
      a.sumPredRows += e.predictedRows;
      a.sumActRows  += e.actualRows;
      a.sumWallNanos += e.wallNanos;
      a.sumCpuNanos  += e.cpuNanos;
      a.sumBytesRead += e.bytesRead;
      a.sumSpilledBytes += e.spilledBytes;
      a.maxPeakMemBytes = max(a.maxPeakMemBytes, e.peakMemBytes);
    }

    store.updateBatch(agg);          // transactional update
    cache.refreshFromBatch(agg);     // optional: store can return updated stats
  }
}
```

Design choice: compute `logErr` in flusher or in store update?

* If flusher computes logErr, it can store `sumLogErr` and `sumLogErrSq` per key for more robust variance updates.
* If store computes logErr, it needs per-event info; but we aggregated sums, not individual rows, so you’d approximate logErr via aggregated ratio: log(sumAct/sumPred). That’s often fine and cheaper.

For a “fully featured” implementation, you can do both:

* maintain logErr from aggregated ratio for mean drift
* maintain an EWMA of “dispersion proxy” using heuristics based on variability of batch ratios across flushes rather than within a batch

---

### 66.5 FeedbackStore update math: log-space EWMA with shrinkage-ready aggregates

#### 66.5.1 Store value structure

Store must support:

* mean log error
* variance log error
* counts and timestamps
* optional additional fields (planHits, anomaly counters)

```java
record FeedbackStats(
  int n;
  long lastSeenNanos;
  double ewmaLogErr;
  double ewmaLogErr2;    // EWMA of (logErr^2)
  double ewmaLogCost;    // optional
  int anomalyCount;
  int planHits;          // optional
) {}
```

#### 66.5.2 Update from batch aggregate

Let:

* `P = sumPredRows`, `A = sumActRows`
* `logErr = log( A / max(1,P) )` with clamping for extremes
* `α = alpha(n, batchCount)` e.g., α = 1 - exp(-batchCount / τ) or a constant like 0.1

Update:

* `ewmaLogErr = (1-α)*ewmaLogErr + α*logErr`
* `ewmaLogErr2 = (1-α)*ewmaLogErr2 + α*(logErr*logErr)`
* `n += batchCount`
* `lastSeenNanos = now`

Variance estimate:

* `var = max(0, ewmaLogErr2 - ewmaLogErr^2)`

Outlier handling:

* if `abs(logErr) > LOGERR_CAP` then:

  * increment anomalyCount
  * clamp logErr to sign(logErr)*LOGERR_CAP (or ignore update if you prefer)

#### 66.5.3 Confidence function (store-side helper)

Compute an effective confidence that planner can reuse:

* `age = now - lastSeen`
* `nEff = n * exp(-age/halfLife)`
* `confN = 1 - exp(-nEff / nScale)`
* `confVar = 1 / (1 + var/varScale)`
* `confidence = confN * confVar`

Planner may recompute this, but caching it avoids repeated exp operations.

---

### 66.6 HotCache: two-level caching for predictable planner latency

You want the planner’s `get()` to be “fast enough even when disk is sad.”

#### 66.6.1 Two-level cache pattern

* L1: per-planning-session ephemeral map (very small, just during planning)
* L2: shared in-memory LRU cache of `Key128 → FeedbackStats` and derived factor/confidence

Flow:

1. planner checks L1
2. if miss, checks L2
3. if miss, calls store.get(key) (disk)
4. store result inserted into L2 and L1

Pseudo:

```java
final class PlannerLookupContext {
  final Long2ObjectMap<CacheEntry> local = new Long2ObjectOpenHashMap<>();

  CacheEntry get(Key128 k, Store store, HotCache cache) {
    long combined = mix128to64(k);      // local map uses 64-bit; acceptable
    CacheEntry e = local.get(combined);
    if (e != null) return e;

    e = cache.get(k);
    if (e != null) { local.put(combined, e); return e; }

    FeedbackStats s = store.get(k).orElse(null);
    e = CacheEntry.fromStats(s);
    cache.put(k, e);
    local.put(combined, e);
    return e;
  }
}
```

#### 66.6.2 CacheEntry should store derived values

Don’t recompute exp/log all the time. Store:

* `factor = exp(ewmaLogErr)` (clamped maybe)
* `confidence`
* `var`
* `n`, `lastSeen`

Compute them once on cache fill.

#### 66.6.3 Cache invalidation

You don’t need strict coherence. Update L2 entries opportunistically:

* after store updates, flusher calls `cache.refreshFromBatch()`
* refresh either:

  * recompute from updated stats returned by store
  * or mark keys as dirty so next planner miss triggers reload

Given this is a learning system, eventual consistency is fine.

---

### 66.7 Compaction/rebuild algorithm: keep the best, drop the rest

A rebuild-based eviction is robust and easier to reason about than random deletes.

#### 66.7.1 Compaction trigger

Trigger when:

* store size > `maxBytes` × (1 + hysteresis) e.g., 1.2
* or on schedule (daily/weekly) if you want continuous hygiene

#### 66.7.2 Rebuild selection

Algorithm:

1. iterate all records
2. compute utility score
3. keep top K records under target size
4. write to new store

Pseudo (conceptual):

```java
void rebuildStore() {
  List<RecordMeta> metas = new ArrayList<>();

  for (Record r : store.iterateAll()) {
    double score = score(r.stats);
    metas.add(new RecordMeta(r.key, r.stats, score, approxBytes(r)));
  }

  metas.sort(byScoreDescending);

  long bytes = 0;
  NewStore ns = createNewStore();
  for (RecordMeta m : metas) {
    if (bytes + m.bytes > targetBytes) break;
    ns.put(m.key, m.stats);
    bytes += m.bytes;
  }

  ns.flushAndClose();
  atomicallySwapStores(ns, store);
}
```

Important operational detail: doing a full iterate+sort can be heavy. For large stores:

* use a streaming top-K selection (min-heap) rather than sorting all entries
* or sample entries and keep those above a score threshold
* or rebuild by multiple passes (coarse then fine)

A pragmatic approach:

* keep top-K by score via min-heap of size K
* K chosen to fit target bytes based on average record size

---

### 66.8 SketchStore admission/eviction: budget enforcement that actually works

#### 66.8.1 Admission controller

A sketch should be created only if benefit exceeds threshold and budget allows.

Inputs:

* key’s frequency/impact from FeedbackStore
* observed error magnitude
* query pattern prevalence (planHits)
* memory cost of sketch

Output:

* admit yes/no with target sketch size parameter (e.g., HLL p, SpaceSaving K)

Pseudo:

```java
boolean shouldAdmit(SketchKey k, ModelType t, BenefitFeatures f) {
  double benefit = f.freqScore * f.errorScore * f.impactScore;
  if (benefit < config.minBenefit) return false;
  if (sketchStore.bytesUsed + estimatedSketchBytes(t, f) > config.maxBytesTotal) return false;
  return true;
}
```

#### 66.8.2 Eviction policy

Use a weighted LRU:

* base eviction: least recently used in planning
* weight: benefit score and stability
* size-aware: evict large cold sketches first

Maintain for each sketch:

* `bytes`
* `lastUsedNanos`
* `benefitScore`
* `stabilityScore`

Evict until under budget:

* pick sketch with lowest `benefitScore / bytes` and old lastUsed (or a combined score)

This can be implemented with:

* an approximate priority queue updated periodically
* or periodic sweeps rather than per-operation updates (cheaper)

---

### 66.9 EstimatorStack: layering with gating and uncertainty propagation

A clean estimator stack prevents “everything calls everything” architecture rot.

#### 66.9.1 Interfaces

```java
interface Estimator {
  Estimate estimate(NodeContext ctx, PlannerLookupContext lc);
}

final class EstimatorStack implements Estimator {
  final BaselineEstimator baseline;
  final MicroModelEstimator micro;
  final OnlineCorrectionEstimator corr;
  final OfflineModelEstimator offline;  // optional
  final GatingPolicy gate;

  Estimate estimate(NodeContext ctx, PlannerLookupContext lc) {
    Estimate e = baseline.estimate(ctx, lc);

    if (gate.allowMicro(ctx)) e = micro.refine(ctx, e, lc);
    e = corr.adjust(ctx, e, lc);  // correction often applies broadly, but gated internally
    if (gate.allowOffline(ctx, e)) e = offline.refine(ctx, e, lc);

    return e;
  }
}
```

#### 66.9.2 Micro-model refinement example (range filter)

```java
Estimate refineRangeFilter(NodeContext ctx, Estimate base, PlannerLookupContext lc) {
  RangeSketch sk = sketchStore.getRangeSketch(ctx.predicateId, ctx.graphId, ctx.datatypeFamily);
  if (sk == null || sk.confidence() < MIN_CONF) return base;

  double sel = clamp01(sk.cdf(ctx.upper) - sk.cdf(ctx.lower));
  double mean = base.inputRows * sel;

  // uncertainty: combine base var and sketch var conservatively
  double var = max(base.varLogRows, sketchVarToLogVar(sk));
  return base.withMean(mean).withVarLogRows(var).markMicroUsed("rangeSketch");
}
```

#### 66.9.3 Online correction adjustment example

```java
Estimate adjust(NodeContext ctx, Estimate e, PlannerLookupContext lc) {
  if (!config.applyCorrections) return e;

  Key128 leaf = keyBuilder.leafKey(ctx);
  Correction c = correctionComputer.computeHierarchical(ctx, leaf, lc);

  if (!c.shouldApply()) return e;

  double factor = c.factorClampedSoft;
  double newMean = clampRows(e.meanRows * factor, ctx.minRows, ctx.maxRows);

  double newVar = combineVar(e.varLogRows, c.varLogErr); // conservative
  return e.withMean(newMean).withVarLogRows(newVar).markCorrUsed(c);
}
```

Note: `correctionComputer.computeHierarchical` does the multi-level lookups and shrinkage.

---

### 66.10 Online cost calibration pseudo‑code: RLS with robust residual clamp

#### 66.10.1 Operator family models

```java
final class LinearRlsModel {
  double[] beta;
  double[][] P;
  double lambda;        // forgetting factor
  double huberC;        // robustness parameter
  double residScale;    // EWMA of abs residual (for huber)

  double predict(double[] x) { return dot(beta, x); }

  void update(double[] x, double y) {
    double yhat = predict(x);
    double r = y - yhat;

    // Update residual scale
    residScale = 0.99*residScale + 0.01*Math.abs(r);
    double cap = huberC * max(1e-9, residScale);
    double rClamped = clamp(r, -cap, cap);

    // RLS update using rClamped
    // k = P x / (lambda + x^T P x)
    double[] Px = matVec(P, x);
    double denom = lambda + dot(x, Px);
    double[] k = scale(Px, 1.0/denom);

    // beta = beta + k * rClamped
    axpy(beta, k, rClamped);

    // P = (P - k x^T P) / lambda
    // Compute kxT = outer(k, x)
    // P = (P - kxT * P) / lambda  (implement efficiently)
    P = updateP(P, k, x, lambda);
  }
}
```

#### 66.10.2 When to update cost models

Update only if the observation is “trustworthy”:

* operator wall time > threshold
* rows observed > threshold
* system load not extreme (optional)
* not during known GC pause windows (if you can detect)

Then:

* build feature vector x from NodeContext and estimate
* y = observed cpuNanos or wallNanos (depending on model)
* update model

---

## 67) Persistence and export formats (binary layouts, checksums, evolution)

The goal of formats is not aesthetics; it’s operational survivability:

* you can evolve without silent incompatibility,
* you can detect corruption,
* you can recover safely.

### 67.1 Store header format (FeedbackStore and SketchStore)

Define a binary header at the start of each store file:

Fields (example):

* magic bytes: `"FBST"` (feedback store) or `"SKST"` (sketch store)
* format version (u32)
* keySchemaVersion (u32)
* featureSchemaHash128 (16 bytes)
* dictionaryHash64 (8 bytes)
* engineMajorVersion (u16)
* engineMinorVersion (u16)
* createdAtUnixMillis (u64)
* lastCompactedAtUnixMillis (u64)
* salts for hashing (2×u64)
* header CRC32 (u32)

Any mismatch in these fields triggers:

* fail closed (ignore store or purge depending on config)

### 67.2 FeedbackStats record encoding

For compactness and fast IO, encode stats in fixed-size binary, not JSON.

Example record payload layout (fixed 64 bytes, little endian):

* n (u32)
* anomalyCount (u32)
* lastSeenNanos (u64)
* ewmaLogErr (f64)
* ewmaLogErr2 (f64)
* ewmaLogCost (f64) (optional; else 0)
* planHits (u32) (optional)
* reserved/padding (u32)
* record CRC32 (u32) or store-level checksum scheme

Key is stored by the KV backend:

* key is 16 bytes (hash128)
* value is 64 bytes

Fixed sizes simplify memory mapping and reduce parsing overhead.

### 67.3 Sketch serialization format

Sketches must be versioned independently. Store per sketch:

* sketchType (u16) (HLL, KLL, SpaceSaving, etc.)
* sketchVersion (u16)
* sketchKeyHash128 (16 bytes)
* createdAt, lastUpdated, lastUsed
* bytes length (u32)
* payload bytes (compressed if beneficial)
* CRC32 of payload

Important: keep payload self-contained so sketches can be loaded without needing external state. If a sketch depends on parameters (like HLL p), store them in the sketch header.

### 67.4 Export file format for offline training

Goals:

* compact
* easy to parse
* schema versioned
* privacy-safe

You can use:

* columnar formats (Parquet/Arrow) if available, or
* a custom binary row format

For a custom format, define:

* file header with schema hash and feature definitions version ID
* records as:

  * key family and level
  * feature vector in packed bucket IDs (varints)
  * labels: actual rows (log), actual time (log), spilled? etc.
  * weights: observation count, confidence
  * timestamps in coarse buckets

Privacy:

* never export raw literal strings
* export only bucket IDs and hashed identifiers where permitted
* tenant-scoped exports separate by tenantId, and tenantId itself may be anonymized depending on policy

### 67.5 Model artifact packaging

A model artifact bundle should include:

* `manifest.json` (or binary manifest) with:

  * modelId, version, type, operator family
  * feature schema hash
  * required features
  * training window
  * evaluation summary
  * inference budget
  * signature/checksum
* `weights.bin` (compiled representation)
* optional `calibration.bin` (post-training calibration, e.g., isotonic regression)
* optional `notes.txt` for human context

Engine loads model by:

* verifying manifest compatibility
* verifying checksum/signature
* loading weights into fast runtime structures

---

## 68) A/B analytics automation: regret, noise control, and rollback triggers

Fully featured doesn’t just mean the optimizer learns; it means you can prove it learned and prevent it from learning itself into a ditch.

### 68.1 What you log for experiments

For each query execution:

* query fingerprint (canonical)
* tenantId (or anonymized bucket)
* variant/mode (CONTROL/SHADOW/APPLY_SAFE/APPLY_FULL)
* plan shape hash
* planning time
* runtime wall time
* bytes read, spilled bytes, peak memory
* result size (rows returned) if safe
* “applied corrections count” and max factor
* micro-model usage flags (which sketches contributed)
* offline model usage flags and modelId/version
* adaptive execution events (if any)

Keep it structured for aggregation.

### 68.2 Primary outcomes and guard metrics

Primary outcomes:

* p95/p99 runtime (tail)
* worst-1% outliers (max or p99.9)
* spill incidence and spilled bytes
* planning overhead

Guard metrics:

* plan stability (plan changes per fingerprint)
* error metrics: abs(log(actual/pred)) distribution
* clamp frequency: how often corrections were clamped
* anomaly rates

### 68.3 Regret computation strategies

True regret would require:

* runtime of baseline plan and runtime of experimental plan for the same query instance

In production you usually can’t run both. So you use approximations:

**Strategy A: Per-template A/B over time**

* Compare CONTROL and APPLY for the same fingerprint across many executions.
* Use robust statistics (median, trimmed mean).
* This gives *expected* regret/improvement.

**Strategy B: Shadow top‑k candidate plan evaluation in staging**

* In a dedicated environment, for a set of query fingerprints:

  * collect top-k candidate plans (from enumerator)
  * execute a subset of plans to measure actual costs
  * compute regret of chosen plan under each estimator
    This is expensive but gives better insight and helps tune risk penalties.

**Strategy C: Operator-level cost replay**

* Use recorded operator-level actuals and predicted costs to simulate plan scoring.
* Not perfect, but useful for regression detection.

A “fully featured” program typically uses A in production and B/C in staging.

### 68.4 Noise reduction techniques that are feasible

Even without fancy statistics, you can reduce noise by:

* grouping by fingerprint and comparing within group
* separating warm vs cold cache regimes (if you can tag them)
* excluding runs during extreme system load (or analyzing separately)
* using log runtime: `log(time)` reduces skew

If you can do paired comparisons:

* route the same fingerprint deterministically to a variant, so you’re not mixing templates.

### 68.5 Automated rollback triggers

Define triggers as a combination of:

* effect size
* confidence (sample size)
* safety thresholds

Example rollback policy (conceptual):

* In canary group, over last 10 minutes:

  * if p99 runtime > control p99 × 1.25 AND sample count ≥ N
  * OR spilled bytes increase > 2× AND spill count ≥ N
  * OR planning time p95 increases > 1.10×
    Then:
* automatically switch canary from APPLY to SHADOW
* keep OBSERVE on
* record rollback event with diagnostics snapshot

Diagnostics snapshot should include:

* top regressing fingerprints
* plan stability change rates
* most applied correction keys and their factors/confidence
* store and cache hit rates
* any recent schema/model changes

### 68.6 Plan stability analytics and flap detection

For each fingerprint:

* track last plan hash
* count plan changes per hour/day
* detect oscillation between two plans:

  * if plan alternates A↔B frequently, that’s flapping

Mitigation tuning:

* increase stickiness δ
* increase risk penalty weight λ
* clamp factors more aggressively when confidence low
* treat high variance keys as lower confidence

---

## 69) RDF/SPARQL‑specific estimation tricks (beyond CS, NDV, and heavy hitters)

This section is the “extra spice” that makes RDF estimators notably better than generic relational heuristics, without requiring full-blown ML magic.

### 69.1 Predicate multiplicity models (how many objects per subject for p?)

Many RDF predicates have characteristic multiplicity:

* `rdf:type`: often multiple per subject
* `foaf:knows`: potentially many
* `ex:birthDate`: typically exactly one
* `ex:country`: usually one, sometimes zero

Multiplicity matters because:

* in star joins, the cardinality of `?s p ?o` is not just “subjects with p,” but “subjects with p × average multiplicity”
* OPTIONAL multiplicity influences output explosion

Maintain per `(predicate, graph)`:

* `subjectCountWithP` (how many subjects have at least one triple with p)
* `tripleCountForP` (how many triples with p)
  Then:
* average multiplicity `mult = tripleCountForP / subjectCountWithP`

You can store these as baseline stats or micro-model stats updated online.

At plan time:

* if your star join uses existence correlation via CS, combine:

  * estimated subjects satisfying predicate presence constraints
  * multiply by multiplicity for each predicate whose object is projected (carefully; independence assumptions apply)

Uncertainty:

* multiplicity can be heavy-tailed (some subjects have many values). Track variance or at least a “heavy tail indicator”:

  * e.g., ratio of p90 multiplicity to mean multiplicity (can be approximated if you track a small digest of multiplicities)

### 69.2 Subject degree distribution (generalization for star joins)

Instead of only predicate multiplicity, track subject degree distribution for a predicate:

* how many triples per subject for that predicate
  If you maintain a small sketch of degree values per predicate:
* you can estimate tail risk: a star join might explode if you hit high-degree subjects

This helps risk-aware planning:

* avoid plans that join on predicates with heavy degrees early unless bound constraints are selective.

### 69.3 Distinctness ratio (rows vs distinct subjects)

A common confusion in RDF is mixing:

* triple count with predicate p
* number of distinct subjects with p
* number of distinct objects with p

You often want:

* distinct subjects count for star existence
* distinct objects count for join keys and group keys
* triple count for raw cardinality

Track and use all three:

* `countTriples(p)`
* `countSubjects(p)`
* `countObjects(p)` (NDV objects)

Then you can derive:

* average objects per subject = triples/subjects
* average subjects per object = triples/objects (useful for reverse joins)

### 69.4 Type-conditioned stats (rdf:type is a powerful condition)

If your workload frequently constrains `?s rdf:type T`, then type-conditioned stats can dramatically improve estimates:

* within type T:

  * count of subjects
  * predicate presence rates
  * predicate multiplicities
  * object NDV for certain predicates

You don’t need full per-type everything. Start with:

* for top K types (by frequency in workload), maintain:

  * count subjects of type
  * presence probabilities of top predicates (or counts of subjects with predicate given type)
    Then star join estimate becomes:
* subjects of type T satisfying predicates ≈ count(T) × Π P(p_i | T) (or using type-conditioned CS/itemsets)

This is a micro-model tier:

* `TypeKey = (typeId, graphId)`
* `TypePredKey = (typeId, predicateId, graphId)`

Budget it: only for common types.

### 69.5 Language tags and datatype partitions

Literal distributions differ by language tag and datatype:

* `rdfs:label` might have en, fr, etc.
* numeric vs string values behave differently
* date vs datetime behave differently

If filters or joins depend on these:

* bucket by language tag family (top languages + OTHER)
* bucket by datatype family
  Maintain separate sketches per family when high impact.

### 69.6 Join key skew on intermediate variables (approximate but useful)

It’s hard to maintain exact join key distributions for intermediate results. But you can approximate:

* propagate “origin metadata” for variables through the plan:

  * variable ?x came from predicate p on object position
* if join key variable originates from a scan on predicate p, reuse p’s heavy hitter sketch for skew estimation

Then for join selectivity:

* compute long-tail uniform estimate from NDV
* add heavy hitters contribution:

  * estimate for each top value v:

    * leftFreq(v) * rightFreq(v)
      Sum over top values, plus long tail approximation.

This is approximate but can catch the worst skew cases and prevent catastrophic join algorithm choices.

### 69.7 Correlation between filters and predicates

Filters are not independent of predicates. For example:

* string length constraints on labels correlate strongly with certain entity types
* numeric ranges correlate with time periods

A lightweight “all in” approach:

* maintain filter selectivity stats keyed by (predicateId, filterKind, bucket features) instead of filterKind alone
* this requires predicate-aware FilterKey:

  * include predicateId in FilterKey where safe

Gate it carefully to avoid fragmentation.

---

## 70) End‑to‑end walkthrough: how the loop improves a real query over time

This section ties everything together. We’ll walk through one SPARQL-style query that includes:

* a star around `?person`,
* a numeric range filter,
* an OPTIONAL,
* and a join that can spill if misestimated.

### 70.1 Example query (conceptual)

```sparql
SELECT ?person ?name ?birthYear ?countryName
WHERE {
  ?person rdf:type ex:Person .
  ?person ex:name ?name .
  ?person ex:birthYear ?birthYear .
  FILTER (?birthYear >= 1980 && ?birthYear <= 1990)

  OPTIONAL {
    ?person ex:country ?country .
    ?country ex:countryName ?countryName .
  }
}
```

Assume:

* `ex:Person` is common
* `ex:birthYear` exists for many persons but is skewed (more recent years more common)
* `ex:country` is present for many but not all persons; multiplicity mostly 1, but some have multiple countries
* `ex:countryName` is 1:1 for countries

### 70.2 Baseline planner behavior (before feedback)

A naive baseline estimator might do:

* estimate `?person rdf:type ex:Person` returns 10M rows (persons)
* estimate `ex:name` selectivity ~1.0 (1 name per person)
* estimate `ex:birthYear` selectivity ~1.0
* estimate range filter `1980..1990` as 11/100 ≈ 0.11 if it assumes uniform year distribution across a 100-year span
* OPTIONAL estimated as “matches often,” maybe 0.8 match rate, multiplicity 1.0

Baseline predicted rows:

* `type`: 10M persons
* after `birthYear` filter: 1.1M
* OPTIONAL join doesn’t change row count much: ~1.1M

Baseline plan might choose:

* start with `rdf:type` then `birthYear` and filter
* then join name
* then optional country join

But if birthYear distribution is not uniform (say 1980–1990 is actually 0.02, not 0.11), baseline will overestimate the filtered set. This affects:

* join order choice
* join algorithm choice for country join
* memory reservation for hash join

### 70.3 Execution telemetry (first run)

On execution, actuals might be:

* type returns 10M (close)
* birthYear range filter reduces to 200k (not 1.1M)
* OPTIONAL match rate is 0.6 and sometimes multiplicity 1.1 (some multiple countries)
* countryName join is cheap

Telemetry events emitted on close:

* PatternKey for `rdf:type ex:Person`: predicted 10M, actual 10M → logErr ~ 0
* PatternKey for `ex:birthYear ?birthYear` plus FilterKey for range: predicted filter sel 0.11, actual sel 0.02 → logErr negative
* OptionalKey: predicted matchRate 0.8 mult 1.0, actual matchRate 0.6 mult 1.1 → mismatch
* Join node for optional chain: predicted rows and spill risk maybe too high/low depending

Feedback store updates:

* FilterKey (RANGE_NUMERIC, widthBucket=small, datatype=int, graphId): logErr ~ log(0.02/0.11) ≈ -1.704
* Predicate-conditioned FilterKey (if enabled): (predicate=ex:birthYear, range bucket): learns that `birthYear` ranges are not uniform
* OptionalKey for `(ex:country chain)`: learns match rate ~0.6 and multiplicity ~1.1

Range sketch updates:

* RangeSketch for `ex:birthYear` gets sampled values; over time it learns the CDF shape

### 70.4 Second run planning: micro-model + correction starts to bite

Assume after a handful of runs, you have:

* enough observations (n ≥ kMin)
* range sketch confidence moderate-high
* correction store has stable logErr for the range filter

Planner now estimates:

* type ~10M
* range filter selectivity from sketch:

  * `cdf(1990) - cdf(1980)` ≈ 0.02
* online correction factor might further adjust slightly:

  * factor = exp(ewmaLogErr) near 1.0 now because sketch already fixed it, or
  * if sketch is new, correction still provides the 0.18× adjustment (0.02/0.11)

Estimated rows after birthYear filter:

* ~200k instead of ~1.1M

This changes plan choice:

* it may now start with `birthYear` pattern (if index supports it) because it’s far more selective
* or if it must start with type, it may choose a plan that pushes birthYear early
* OPTIONAL chain now sees left input 200k, which is smaller, so it might choose an index nested-loop for country join rather than a hash join with big build side
* memory reservations adjust downward
* spill probability becomes low

OPTIONAL estimates:

* match rate 0.6, multiplicity 1.1
* expected output rows ≈ 200k * ((0.4)*1 + 0.6*1.1) = 200k * (0.4 + 0.66) = 212k

Tail-risk:

* if multiplicity variance suggests heavy tails (some persons with many countries), risk-aware scoring may avoid placing OPTIONAL too early if it could explode; but with 200k it’s manageable.

### 70.5 What changes operationally: fewer outliers, more stable plans

The biggest wins from this feedback loop often look like:

* p99 collapses, not because average improved dramatically, but because catastrophic wrong plans happen less
* fewer spills due to better memory risk estimates
* fewer plan changes once stable evidence accumulates (stickiness)

### 70.6 EXPLAIN (ANALYZE, FEEDBACK) “diff narrative”

A well-designed explain output would show something like:

* Filter node (birthYear range):

  * baseline sel: 0.11
  * micro-model rangeSketch sel: 0.021 (conf=0.78)
  * online correction factor: 0.95 (conf=0.62) (small residual adjustment)
  * final sel: 0.020
  * predicted rows: 200k
  * actual rows (analyzed run): 205k

* OPTIONAL node:

  * baseline matchRate: 0.8
  * learned matchRate: 0.6 (conf=0.70)
  * learned multiplicity: 1.1 (conf=0.55)
  * predicted output rows: 212k
  * actual: 220k

* Country join:

  * algorithm chosen: index nested-loop
  * reason: leftRows small, spillProb(hash) moderate under worst-case; tail-risk favors nlj

### 70.7 The “long-term behavior” after weeks

As distributions drift (new persons added, different birthYear distributions, different country completeness):

* the range sketch updates gradually
* online corrections track residual shifts
* half-life decay ensures old evidence doesn’t dominate forever
* if drift is large, anomaly detectors reduce confidence and the system leans back on broader priors until enough new evidence accumulates

This is the “optimizer gets wiser but doesn’t get cocky” property.

---

## 71) End of Part 5

Part 5 delivered:

* concrete pseudo‑code patterns for the registry, keying, telemetry ingestion, batching, cache, compaction, sketch admission/eviction, estimator stack, and cost calibration
* practical binary format guidelines for store headers, fixed-size stats records, sketch serialization, export files, and model artifacts
* a realistic A/B analytics and rollback automation plan
* RDF/SPARQL-specific estimation improvements beyond the basic micro-model set
* a worked end-to-end example showing how the loop converges

