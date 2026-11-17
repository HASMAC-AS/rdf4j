# Conservative partial-ready experiment

Flag (default false): rdf4j.lmdb.repository.partialReady.

In RepositoryValueIteration.prepareNext the existing isPrepared(root) guard is evaluated once during root extraction. Experimental mode retains only roots failing that guard for captureReadyOwners and resolveWaves. No extra list/pass, no read-ahead, no relaxing readiness definition. An initialized Lazy-revision value still fails readiness, literals with cold datatype children still fail, and RDF-star triples still fail. The existing all-ready shortcut already relies on this exact guard, so filtering mixed rows does not introduce a new owner/lifetime assumption.

Hit proof: new RepositoryPartialReadyTest counts visits through a fully ready completed-revision literal in a row also containing cold BNode,custom datatype literal,RDF-star triple and initialized Lazy literal. Ready literal visits fall from3baseline to1experimental; initialized Lazy root remains2visits. First row only is pulled,all cold descendants resolved,values survive scopeclose. Existing repository suite covers cancellation,concurrentclose,sourceclose and map-growth lifetimes.

Evidence:partial-ready-pre.log14pass before production;partial-ready-post.log14pass flagtrue;partial-ready-off.log14pass flagfalse;partial-ready-grouped-correct-flag.log3pass with partialReady and groupedValueWaves true. Earlier partial-ready-grouped.log used a misspelled group property,so it is NOT combined-path evidence. All tests zero failures/errors. No benchmarks run or speedup claim.

Production diff only RepositoryValueIteration.java,8added2removed lines. New test RepositoryPartialReadyTest.java. Existing grouped-wave branch untouched. Parent owns integration with reconstructed memo,benchmarking and publication.
