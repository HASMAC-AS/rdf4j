Purpose / Big Picture

Goal in one sentence

Add multi‑level trie indexes backed by LMDB DUPSORT databases to the RDF4J LMDB Store, so that a Leapfrog‑Triejoin‑style join engine can do variable‑by‑variable joins efficiently over RDF4J’s dictionary‑encoded triples.

What this enables
•	A new physical layout of triple data in LMDB that exposes a trie view over subject/predicate/object/context IDs.
•	A clean Java API to treat these LMDB structures as tries over integer IDs, with operations like openPrefix, next, and seek.
•	A path (via a custom EvaluationStrategyFactory) to plug in a Leapfrog Triejoin (LFTJ) implementation for SPARQL basic graph patterns (BGPs).
•	A configurable feature: existing behavior remains the default; the trie indexes and LFTJ engine can be turned on separately for testing/benchmarking.

End‑user observable behavior: same query results, but for suitably shaped queries (dense joins, cliques, complex graph patterns) we aim for significantly smaller intermediate results and better performance.

⸻

Progress

Keep this section updated as you go. Timestamps don’t need to be perfect; ISO dates are fine.
•	(2025‑11‑20) ExecPlan v1 written and handed to implementer.
•	Step 1: Read and map the current LMDB Store code (triple layout, write path, read path).
•	Step 2: Finalize concrete LMDB trie layout and open/close logic.
•	Step 3: Implement write‑side maintenance of trie indexes (insert/delete).
•	Step 4: Implement read‑side trie API (prefix iterators on top of LMDB DUPSORT).
•	Step 5: Implement an in‑memory Leapfrog Triejoin over the trie API.
•	Step 6: Integrate the LFTJ engine via a custom EvaluationStrategyFactory.
•	Step 7: Add tests (correctness + regression), benchmarks, and documentation.
•	Step 8: Run full RDF4J LMDB testsuite with feature enabled and fix issues.
•	Step 9: Capture retrospective (what worked / what to change next).

⸻

Surprises & Discoveries

Fill this in as you bump into reality.
•	Observation: …
•	Evidence: …

Examples of things to record: unexpected LMDB errors, performance cliffs, tricky concurrency behavior, weird GC interactions, or inconsistencies between different RDF4J stores.

⸻

Decision Log

Use this for any non‑obvious design choices you make while implementing.
•	Decision: Represent trie levels as separate LMDB databases per triple‑index order and per level, using DUPSORT with “prefix as key, next‑ID as duplicate value”.
•	Rationale: Simplifies the code and makes each DB’s semantics obvious: “given prefix, enumerate sorted children”.
•	Date/Author: 2025‑11‑20, plan author
•	Decision: Initial scope: apply Leapfrog Triejoin only to pure BGPs (no OPTIONAL/MINUS/UNION/AGGREGATES) and only when all patterns are over the same underlying LMDB Store.
•	Rationale: Keep the first integration small and debuggable; other operators can still be evaluated using the existing strategy.
•	Date/Author: 2025‑11‑20
•	Decision: Feature gated by two independent flags: “maintain trie indexes” and “use LFTJ for BGPs”.
•	Rationale: Lets us:
•	build indexes but still use the old join engine; or
•	test LFTJ on prebuilt data; or
•	turn it off entirely.
•	Date/Author: 2025‑11‑20

Add more as you go.

⸻

Outcomes & Retrospective

Leave this empty for now; fill it when you hit major milestones and when the work is “done enough”.

Things to include later:
•	Did we actually see performance wins? Where?
•	Did the LMDB layout cause issues (DB count limits, map size, write amplification)?
•	Was the LFTJ integration maintainable? Would we change the abstraction boundaries?

⸻

Context and Orientation

1. Where this lives in RDF4J

You’ll be working in the LMDB Sail implementation:
•	Maven artifact: org.eclipse.rdf4j:rdf4j-sail-lmdb.
•	Source directory (in the main RDF4J repo):
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/

Key classes (names are from the Javadoc; open them in your IDE):
•	LmdbStore – the Sail wrapper users instantiate.
•	LmdbStoreConnection – the SailConnection implementation (add/remove statements, evaluate queries).
•	LmdbStoreConfig – store configuration, including triple index orders (e.g., spoc, posc).
•	org.eclipse.rdf4j.sail.lmdb.util.IndexKeyWriters – helpers to build keys for LMDB triple indexes from field sequences like "spoc".
•	org.eclipse.rdf4j.sail.lmdb.Varint – encodes unsigned integers in a variable‑length format where lexicographic byte order matches numeric order.

You will also see internal classes like LmdbSailStore, TripleStore, ValueStore, etc., in that package. They’re not in javadoc, but they are where the real LMDB schema and operations live. The WDQS working paper explicitly points to TripleStore.java in this module as the LMDB triple backend.

2. What the LMDB Store currently does (at a high level)
   •	RDF values (IRIs, literals, bnodes) are stored in a dictionary and referenced by numeric IDs.
   •	RDF statements (triples/quads) are stored by their numeric IDs in at least one LMDB “triple DB”.
   •	By default, LMDB Store uses triple indexes spoc and posc, and you can configure more like ospc via LmdbStoreConfig.setTripleIndexes("spoc,ospc,psoc").
   •	Index keys are ordered by the specified field sequence (e.g., spoc), and the keys are built using varints so that key order matches numeric order.

This is already very close to a trie: each index key is basically a concatenated [s, p, o, c] path encoded into bytes.

3. What a “trie” and Leapfrog Triejoin are in this context

You don’t need to become a database theorist here, but you do need the working picture:
•	A trie (prefix tree) over triples is:
•	Level 1: all distinct values of some variable or component (e.g. all subject IDs).
•	Level 2: for each subject, all predicates that occur with that subject.
•	Level 3: for each (subject, predicate), all objects.
•	Level 4: for each (subject, predicate, object), all contexts.
•	Leapfrog Triejoin (LFTJ) is a join algorithm that:
•	Chooses an order of variables: ?x1, ?x2, …, ?xn.
•	For each variable, it takes several sorted iterators (one per triple pattern that mentions that variable) and intersects them by repeatedly calling seek(value) and next().
•	It does this variable‑by‑variable instead of pattern‑by‑pattern, which gets you “worst‑case optimal” behavior on certain hard joins.

In our setting:
•	Trie levels live in LMDB, using integer IDs from RDF4J’s value & context dictionaries.
•	Leapfrog uses LMDB cursors as its sorted iterators.

You can think of the whole change as:

“Give the join engine a nice, clean integer trie API over LMDB, and then write a join that only does ordered scans and seeks over those tries.”

⸻

Design Overview (what we’re going to build)

1. New LMDB trie layout

For each triple index order configured in LmdbStoreConfig.tripleIndexes (e.g. spoc, posc), we introduce three new LMDB databases (for 4‑component quads):

Given an order X Y Z C (for subject/predicate/object/context in some permutation), we add:
•	Level 1 DB:
•	Key: X ID encoded as a varint.
•	Value (duplicates): Y IDs, each as a varint.
•	LMDB flags: DUPSORT so there can be many Y per X, sorted by Y.
•	Level 2 DB:
•	Key: (X, Y) IDs encoded as varint list.
•	Value (duplicates): Z IDs.
•	Level 3 DB:
•	Key: (X, Y, Z) IDs encoded as varint list.
•	Value (duplicates): C IDs (context IDs).

So for spoc you get:
•	trie_spoc_L1: key = s, dup values = p
•	trie_spoc_L2: key = (s, p), dup values = o
•	trie_spoc_L3: key = (s, p, o), dup values = c

We’ll use RDF4J’s Varint to encode IDs, so that LMDB’s lexicographic ordering of dup values is numeric ordering.

This yields a logical trie:
•	Level 1: all s (or whatever X is for that index).
•	Level 2: for each s, all p (or Y).
•	Level 3: for each (s, p), all o.
•	Level 4: for each (s, p, o), all contexts c.

For each triple index you already have (e.g., spoc, posc), you will now also have three trie DBs.

2. Java API on top of the trie layout

We’ll introduce two conceptual layers:
1.	TrieIndexManager (write + open/close):
•	Knows about:
•	which triple index orders exist (spoc, posc, …),
•	which LMDB DB handles correspond to which levels.
•	Provides write‑side operations:
•	insert(IdQuad q, Transaction txn)
•	delete(IdQuad q, Transaction txn)
2.	TrieCursor / TrieView (read‑side, for joins):
•	Exposes primitive operations over a fixed prefix:
•	openPrefix(IdPrefix prefix, Txn txn)
•	boolean next() – advance to next child ID.
•	boolean seek(long targetId) – “leapfrog”: jump to first child ≥ targetId.
•	long key() – current child ID.
•	boolean atEnd().

The join engine will not know about LMDB or varints; it will only know about:
•	integer IDs (longs),
•	the ability to get “all X for this prefix” and then do sorted iteration/seek.

3. Integration points with RDF4J

We’ll touch three main areas:
1.	Store initialization: where LMDB environments and databases are opened.
•	Extend LmdbSailStore / TripleStore (or equivalent internal class) to open trie DBs with DUPSORT based on LmdbStoreConfig.getTripleIndexes() and a new config flag maintainTrieIndexes.
2.	Write path: where statements are added and removed.
•	In LmdbStoreConnection.addStatementInternal, removeStatementsInternal, clearInternal, etc., after the existing triple DB writes, call into TrieIndexManager.insert/delete using the same LMDB transaction.
3.	Query evaluation:
•	Introduce LmdbWcojEvaluationStrategyFactory and LmdbWcojEvaluationStrategy that:
•	Look at a SPARQL algebra tree,
•	Identify a basic graph pattern (BGP),
•	Build a join plan (variable order + which trie index to use),
•	Use the trie API to run Leapfrog Triejoin and produce a CloseableIteration<BindingSet, QueryEvaluationException>.
•	For everything else, delegate back to the default evaluation strategy.

⸻

Detailed Implementation Plan

Step 1 – Orient yourself in the LMDB module

Goal: Understand where you will add code and how triples are currently encoded and written.

Actions
1.	Open the RDF4J repo and find:
•	core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java
•	LmdbStoreConnection.java
•	LmdbSailStore.java
•	TripleStore.java (and anything else with “Triple” or “SailStore” in this package)
•	ValueStore / ValueStoreRevision (for dictionary IDs)
•	org.eclipse.rdf4j.sail.lmdb.util.IndexKeyWriters and Varint.
2.	Locate where:
•	The LMDB Environment and DBs are opened (inside LmdbSailStore / TripleStore).
•	Triple index orders from LmdbStoreConfig.getTripleIndexes() are parsed and used.
•	Statement add/remove methods in LmdbStoreConnection end up calling into the triple storage.
3.	Draw yourself a tiny diagram (on paper or in a comment) like:

LmdbStore
-> LmdbStoreConnection
-> LmdbSailStore / TripleStore
-> LMDB Environment + DB handles



Success criteria
•	You can answer:
•	“Which class has the LMDB DB handles for triples?”
•	“Where is the code that writes a triple to LMDB?”
•	“How does a SPARQL query’s StatementPattern get turned into LMDB operations?” (This path goes through the evaluation strategy and triple source; just understand roughly.)

⸻

Step 2 – Nail down the LMDB trie schema

Goal: Decide exact DB names, key/value encodings, and how to open them.

Actions
1.	Define a simple internal model for encoded triples:

final class IdQuad {
final long s, p, o, c; // internal IDs, not RDF4J Values
}

You don’t necessarily need a new class if TripleStore already has something equivalent; reuse it if it exists.

	2.	Define a mapping from triple index string to component order:

enum Component { S, P, O, C }

final class IndexOrder {
final String name;     // e.g. "spoc"
final Component[] order; // e.g. [S, P, O, C]
}

A helper to parse String index = "spoc"; into Component[].

	3.	For each index order, define DB names (as Java constants):
Example for index "spoc":

private static String trieL1Name(String index) { return "trie_" + index + "_L1"; }
private static String trieL2Name(String index) { return "trie_" + index + "_L2"; }
private static String trieL3Name(String index) { return "trie_" + index + "_L3"; }

These become named LMDB databases inside the same environment as the triple DB.

	4.	Key/value encoding:
	•	Use Varint.writeUnsigned and Varint.writeListUnsigned to serialize IDs into a ByteBuffer.
	•	Keys:
	•	Level 1: X only (single ID).
	•	Level 2: (X, Y).
	•	Level 3: (X, Y, Z).
	•	Values (duplicates):
	•	Level 1: Y.
	•	Level 2: Z.
	•	Level 3: C.
Use fixed order for lists: follow the actual index order; do not reorder.
5.	LMDB flags:
•	For each trie DB, open with DUPSORT and (if available in the binding) DUPFIXED since value size is constant (encoded varint). The exact flag names depend on the LWJGL LMDB binding; follow how the code currently opens any DUPSORT DBs (if it does). Otherwise check the LMDB docs.

Success criteria
•	You can write (or sketch) a Java method:

void insertSpocTrie(IdQuad quad, Txn txn) {
long s = quad.s, p = quad.p, o = quad.o, c = quad.c;
// encode and put into L1, L2, L3 DBs with duplicates
}


	•	And you know exactly what bytes go where for a given quad.

⸻

Step 3 – Implement TrieIndexManager (write‑side + DB handles)

Goal: Centralize all trie DB handles and write‑side maintenance.

Actions
1.	Create a package‑private class, e.g.:

final class TrieIndexManager {

    static final class TrieDbs {
        final IndexOrder order;
        final Dbi<ByteBuffer> l1Db;
        final Dbi<ByteBuffer> l2Db;
        final Dbi<ByteBuffer> l3Db;
    }

    private final Map<String, TrieDbs> indexesByName = new HashMap<>();

    TrieIndexManager(Environment env, String tripleIndexesCsv) {
        // parse "spoc,posc,..." into IndexOrder instances
        // open L1,L2,L3 DBs for each, with DUPSORT flags
    }

    void insert(IdQuad q, Txn txn) { … }   // writes to all configured trie indexes
    void delete(IdQuad q, Txn txn) { … }
    void close() { … } // if anything explicit is needed
}

Replace Environment, Dbi, Txn with the actual LMDB wrapper types used in the module (likely from LWJGL LMDB).

	2.	Implement insert(IdQuad q, Txn txn):
For each configured TrieDbs:
•	Reorder q into (X, Y, Z, C) according to IndexOrder.
•	Build key and value buffers and call LMDB put() (with MDB_NODUPDATA if you want to avoid duplicate duplicates).
•	Do this for all three levels.
3.	Implement delete(IdQuad q, Txn txn):
•	Reorder as before.
•	For each level:
•	Build key and value buffers.
•	Open a cursor on the DB, use get/del to remove that duplicate.
•	Decide whether you want to clean up empty keys (i.e., if a prefix has no more duplicates at a level). It’s not strictly required, but it keeps the trie “tidier” and may help prefix scans later.
4.	Integrate TrieIndexManager into LmdbSailStore / TripleStore:
•	At store initialization (LmdbStore.initializeInternal → LmdbSailStore constructor, or wherever the triple DB is opened), construct a TrieIndexManager if a new config flag (e.g. maintainTrieIndexes) is true.
•	Store it in a field so that LmdbStoreConnection can reach it (likely via the LmdbSailStore or TripleStore instance it already uses).
5.	Config flag:
•	Extend LmdbStoreConfig with a boolean field maintainTrieIndexes and getter/setter.
•	Also extend the LMDB store schema / config parsing if necessary so that this can be configured in RDF; follow the pattern used for other options.

Success criteria
•	You can construct an LmdbStore with maintainTrieIndexes=true, insert some triples, and then manually inspect the LMDB environment (using mdb_dump or a small Java utility) to see the expected trie DBs and contents.

⸻

Step 4 – Hook trie maintenance into the write path

Goal: Ensure every statement insert/delete keeps the trie in sync with the triple DB.

Actions
1.	In LmdbStoreConnection:
•	Find addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) and removeStatementsInternal(...).
•	Find where those calls eventually become LMDB operations via LmdbSailStore / TripleStore.
•	Identify where you have access to:
•	the LMDB transaction,
•	the internal ID representation (IdQuad) used to write to the triple DB.
2.	For each write:
•	After writing to the triple DB (or in the same method that encodes/writes it), call trieIndexManager.insert(idQuad, txn) if maintainTrieIndexes is true.
•	For deletes, call trieIndexManager.delete(idQuad, txn).
3.	For “bulk” operations:
•	clearInternal (contexts), clearInferred, etc. Either:
•	Implement a bulk path for trie deletion (iterate existing triples and delete), or
•	For the first version, it’s acceptable to fall back to “iterate triples and call delete per triple”, as long as you understand the performance implications.
4.	Transaction semantics:
•	Ensure the trie updates are inside the same LMDB transaction as the triple DB updates, so that failures roll back consistently.
•	If LMDB transactions are managed centrally in LmdbSailStore, try to keep the trie calls as close to where the triple DB put/del calls are as possible.

Success criteria
•	Run existing LMDB Store tests (without LFTJ) with maintainTrieIndexes=true and confirm they pass. Even though the trie is not used for queries yet, writes must still succeed and the database must remain consistent.

⸻

Step 5 – Implement the read‑side trie API

Goal: Create a minimal, efficient Java abstraction over LMDB trie DBs that Leapfrog Triejoin can use without knowing about LMDB internals.

Actions
1.	Design an interface for a single trie level cursor:

interface TrieLevelCursor extends AutoCloseable {
// Position this cursor at the first child of the given prefix.
void openPrefix(IdPrefix prefix, Txn txn) throws LmdbException;

    // Move to next child; return false if there are no more.
    boolean next() throws LmdbException;

    // Jump to the first child >= target; return false if no such child.
    boolean seek(long target) throws LmdbException;

    long key();          // current child ID
    boolean atEnd();     // true if no current child

    @Override
    void close();        // closes underlying LMDB cursor
}

IdPrefix can be a small wrapper around an array of component IDs and a component order.

	2.	Implement a concrete LmdbTrieLevelCursor:
	•	Holds:
	•	A reference to the TrieDbs and the specific Dbi<ByteBuffer> for the level.
	•	An LMDB cursor.
	•	Reusable ByteBuffers for key and value.
	•	openPrefix:
	•	Encode the prefix as the key.
	•	Use LMDB get or cursor get with SET_KEY (or SET_RANGE then check equality) to position at the first duplicate.
	•	next:
	•	Use CURSOR_NEXT_DUP or equivalent to move to next duplicate; if there is no next duplicate, mark atEnd = true.
	•	seek(target):
	•	Use a loop:
	•	Starting from current duplicate (or from the first duplicate if you haven’t started), move forward until value >= target or no more duplicates.
	•	There is no built‑in SET_RANGE on duplicates in LMDB’s base API, so you may need to step duplicates yourself.
	•	For performance, you might later introduce optimizations (binary search over duplicates if you materialize them, etc.), but sequential scanning is acceptable for the first version.
	3.	Add helpers in TrieIndexManager to construct cursors:

TrieLevelCursor openLevelCursor(
String indexName, int level, IdPrefix prefix, Txn txn) {
// returns a cursor for L1/L2/L3 of a given index
}


	4.	Think about error handling:
	•	Wrap LMDB errors in a store‑specific exception (SailException or a subclass) as appropriate.
	•	Ensure close() on the cursor is idempotent and safe to call in finally blocks.

Success criteria
•	Write a small unit test that:
•	Inserts a handful of triples via the normal LMDB store API (with maintainTrieIndexes=true).
•	Opens a level‑1 cursor for a given prefix and iterates all children.
•	Asserts that the child IDs match what you expect, in sorted order.

⸻

Step 6 – Implement Leapfrog Triejoin over the trie API

Goal: Implement an in‑memory LFTJ engine that consumes TrieLevelCursors and produces BindingSets for BGPs.

Here you are not changing RDF4J’s algebra or parser; you’re just adding an alternative way to evaluate BGPs.

6.1 Conceptual model
•	A basic graph pattern (BGP) is a set of triple patterns:

?x1 :winner ?x2 .
?x1 :winner ?x3 .
?x2 :father ?x3 .
...


	•	A variable order is a sequence like [?x1, ?x2, ?x3, ?x4].
	•	For each triple pattern and each variable in it, we can view that triple pattern as a trie that contributes one iterator to the Leapfrog join at each variable.

Our implementation will follow the structure from the SPARQL LFTJ paper (Leapfrog per variable + variable elimination), but adapted to RDF4J and our trie API.

6.2 Data structures
Create a small set of types for the LFTJ engine, in a package like org.eclipse.rdf4j.sail.lmdb.wcoj:
•	WcojBGP – a representation of a basic graph pattern:

final class WcojBGP {
final List<TriplePattern> patterns;
final List<Var> variablesInOrder;
// maybe also store index choices, etc.
}


	•	TriplePattern – a resolved form of StatementPattern:

final class TriplePattern {
final Var subjVarOrNull;
final Var predVarOrNull;
final Var objVarOrNull;
final Var ctxVarOrNull;
final long subjConstIdOrMinus1;
final long predConstIdOrMinus1;
final long objConstIdOrMinus1;
final long ctxConstIdOrMinus1;
final String indexName;     // e.g. "spoc"
final Component[] indexOrder;
}

You will build this from RDF4J’s StatementPattern nodes and the LMDB value store, which maps constants to IDs.

	•	Var – simple wrapper for variable name, plus maybe the position(s) where it appears.

6.3 Variable order
For a first implementation:
•	Use a simple heuristic: order variables by increasing estimated cardinality or degree, or more simply:
•	the variable that appears in the most triple patterns first,
•	tie‑break by position (subject < object < predicate < context),
•	or just subject > object > predicate if you want something easy.

The paper describes more sophisticated selection based on AGM bounds; skip that for now.

Ensure you encapsulate variable ordering in a small method so it can be improved later:

List<Var> chooseVariableOrder(WcojBGP bgp, LmdbSailStore store);

6.4 Trie iterators per variable
For each variable ?x in the order:
•	Collect all triple patterns where ?x appears.
•	For each such pattern:
•	Decide which index order to use (spoc, posc, …) to get a good trie layout for that variable position.
•	Build a prefix for that pattern at the current variable:
•	For variables earlier in the order, we already have their IDs bound.
•	For components that are constants, we know their IDs.
•	For components that are variables later in the order, leave them unconstrained.
•	Use TrieIndexManager.openLevelCursor(indexName, level, prefix, txn) to get a TrieLevelCursor for this variable.

This gives you, for each variable ?x, a list of sorted iterators over the domain of ?x that are compatible with all triple patterns where ?x appears.

6.5 Logical Leapfrog for one variable
Implement a method like:

Iterable<Long> leapfrogOneVariable(List<TrieLevelCursor> cursors)

That returns all ID values of the variable that satisfy all constraints for that variable (i.e., intersection of all iterators).

Algorithm sketch (classic LFTJ behavior):
1.	If any cursor has atEnd()==true after openPrefix, then there are no results for this variable.
2.	Maintain:
•	a list of cursors c[0..k-1],
•	an index i,
•	a current maxKey = max(c[j].key()) over all cursors.
3.	Loop:
•	For j = 0..k-1:
•	If c[j].key() < maxKey:
•	If !c[j].seek(maxKey), then no results.
•	Update maxKey = c[j].key(), restart loop from j=0.
•	When all cursors have key() == maxKey, emit maxKey as one solution for this variable.
•	Then advance one cursor (e.g. c[0].next()); if it’s atEnd, then you’re done; else update maxKey = c[0].key() and repeat.

(You can keep cursors sorted by key() to make the loop more efficient; that’s an optimization.)

6.6 Variable elimination (full LFTJ)
Implement a recursive or iterative method:

void lftjEval(
int varIndex,
WcojBGP bgp,
List<Var> varOrder,
Map<Var, Long> bindings,
Consumer<Map<Var, Long>> output,
Txn txn)

Pseudo‑flow:
1.	Base case: if varIndex == varOrder.size():
•	Emit a copy of bindings.
•	Return.
2.	Let Var v = varOrder.get(varIndex).
3.	Build a list of TrieLevelCursors for v using the current bindings (for earlier vars) and the constants in the triple patterns.
4.	For each value val in leapfrogOneVariable(cursors):
•	Set bindings.put(v, val).
•	Recurse into lftjEval(varIndex + 1, ...).
5.	Clean up cursors at this level.

Within the recursion, building prefixes for later variables is straightforward: use bindings plus constants.

Later, this bindings map will be converted into an RDF4J BindingSet by looking up values for variables that map to actual SPARQL variables (some internal helper variables may be purely structural).

Success criteria
•	Construct a small synthetic BGP directly inside a unit test and run lftjEval over LMDB tries; verify emitted tuples match what RDF4J’s existing join engine returns for the same query.

⸻

Step 7 – Integrate LFTJ via a custom evaluation strategy

Goal: Wire the LFTJ engine into RDF4J’s query evaluation stack for LMDB Store, behind a flag, without breaking other stores.

Actions
1.	Create LmdbWcojEvaluationStrategyFactory in a package like org.eclipse.rdf4j.sail.lmdb:
•	Implement EvaluationStrategyFactory.
•	In createEvaluationStrategy(...), return an instance of your new LmdbWcojEvaluationStrategy.
2.	Create LmdbWcojEvaluationStrategy:
•	Extend DefaultEvaluationStrategy or StrictEvaluationStrategy (depending on what LMDB Store currently uses; Jenkins logs mention StrictEvaluationStrategyFactory being used).
•	Override appropriate methods:
•	Either a general evaluate(TupleExpr expr, BindingSet bindings) and pattern‑match on Join / LeftJoin / StatementPattern etc.,
•	Or more fine‑grained evaluate(Join, ...) method(s), depending on the base class.
3.	BGP detection:
•	Implement a helper that, given a TupleExpr, tries to identify a contiguous join sub‑tree that is purely a BGP:
•	All leaves are StatementPattern nodes.
•	No OPTIONAL/MINUS/UNION/VALUES/AGGREGATES/FILTER/PROPERTY PATH, etc.
•	If detection fails, delegate to super.evaluate(...).
4.	Translation to WcojBGP:
•	Given a list of StatementPatterns in the BGP:
•	Resolve constant terms to internal IDs via the LMDB value store.
•	Collect variable positions and names.
•	Select an index order for each pattern (for now, pick spoc or posc based on which positions are constants; this is already how triple indexes are used, so you can follow that logic).
•	Construct a WcojBGP.
5.	Running LFTJ within the strategy:
•	Inside LmdbWcojEvaluationStrategy, when evaluating that BGP:
•	Open a read‑only LMDB transaction via LmdbSailStore (see how sizeInternal or getStatementsInternal does it and mimic).
•	Call your lftjEval(...) with:
•	a fresh bindings map seeded from incoming BindingSet (for externally fixed variables),
•	a Consumer<Map<Var, Long>> that:
•	converts the internal var→ID map into a BindingSet by:
•	looking up RDF4J Values from IDs via ValueStore,
•	merging with the incoming BindingSet.
•	Wrap this in a CloseableIteration<BindingSet, QueryEvaluationException> that:
•	lazily drives the LFTJ recursion,
•	closes LMDB transaction and trie cursors when iteration ends or is closed.
6.	Feature flag:
•	Extend LmdbStoreConfig with:
•	boolean useWcojForBgp.
•	In LmdbStore.initializeInternal():
•	If config.useWcojForBgp is true, set the EvaluationStrategyFactory on the Sail to LmdbWcojEvaluationStrategyFactory.
•	Keep the default factory otherwise.

Success criteria
•	With maintainTrieIndexes=true and useWcojForBgp=true, simple SPARQL queries whose algebra is a pure BGP should evaluate using your LFTJ engine and give identical results to the baseline.

⸻

Step 8 – Testing & validation

Goal: Ensure correctness and get some first‑order performance signals.

Actions
1.	Unit tests for trie layout:
•	New test class in core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/TrieIndexManagerTest.java.
•	Use LMDB Store directly:
•	Insert a small dataset (10–100 triples) using the standard API.
•	Verify trie contents for each index and level via the trie API.
2.	Unit tests for LFTJ:
•	New test class LmdbWcojEvaluationStrategyTest.
•	Build small in‑memory repositories backed by LMDB:
•	Simple chain, star, and clique patterns.
•	Run queries twice:
•	With useWcojForBgp=false (baseline).
•	With useWcojForBgp=true and maintainTrieIndexes=true.
•	Compare resulting BindingSets ignoring order.
3.	Regression tests:
•	Ensure the existing LMDB testsuite runs with your flags both disabled and enabled:
•	Flags disabled: everything should behave exactly as before.
•	Flags enabled: same results; you might accept minor performance differences.
4.	Smoke performance tests:
•	Use existing benchmark harnesses (e.g., rdf4j-benchmark module or any local scripts) to:
•	Run a set of BGP‑heavy queries on LMDB store with:
•	old join engine,
•	new LFTJ engine.
•	Record query times and intermediate result sizes if feasible.

Success criteria
•	All tests pass.
•	For at least some queries with complex joins (e.g., cliques, long chains), the new engine is not obviously worse, and ideally shows some improvement.

⸻

Step 9 – Documentation & configuration

Goal: Make this feature discoverable and understandable to other developers.

Actions
1.	JavaDoc:
•	Add class‑level JavaDocs to:
•	TrieIndexManager
•	LmdbTrieLevelCursor
•	LmdbWcojEvaluationStrategyFactory
•	LmdbWcojEvaluationStrategy
•	Focus on what the class does and how it is expected to be used, not the full theory of LFTJ.
2.	Config docs:
•	Update The LMDB Store documentation page to mention:
•	New config flags (maintainTrieIndexes, useWcojForBgp).
•	High‑level description of the new join engine: “worst‑case optimal multiway join based on Leapfrog Triejoin”.
3.	Internal design notes:
•	Either:
•	Add a short README.md in core/sail/lmdb describing the trie layout and join integration; or
•	Document it in the project’s existing design docs, depending on team convention.

Success criteria
•	Another developer familiar with RDF4J but not with this plan can read the docs and understand:
•	what the feature does,
•	how to enable it,
•	where to look in the code.

⸻

Step 10 – Fill in Outcomes & Retrospective

After you’ve implemented and tested:
•	Summarize:
•	What worked well.
•	Where the performance gains were, and where they weren’t.
•	Any surprises in LMDB behavior under this pattern (e.g. many small DUPSORT entries).
•	Suggest follow‑ups:
•	Better variable‑order selection.
•	Using trie layout to accelerate other operations (e.g. neighborhood queries).
•	Extending LFTJ to handle OPTIONAL/UNION in a fused way (if interesting).

⸻
