"""Small manually reviewed ports retained from the first source package."""
from __future__ import annotations

QLEVER_PIN='a815b3b71a6c764d927332dcd5505fe6436ad214'

def add(c):
    # These assertions were reviewed at this exact immutable revision. Do not silently
    # apply hard-coded source expectations after someone changes sources.json.
    if c.SOURCES['qlever']['revision']!=QLEVER_PIN:
        c.issue('reviewed_cases.py','Reviewed FILTER/BIND ports require source review for the new QLever revision');return
    definitions={
        'Filter.verifyPredicateIsAppliedCorrectlyOnLazyEvaluation':{
            'query':'SELECT ?x WHERE { VALUES ?x { true true false false true true false false false false true } FILTER(?x) }',
            'expected':c.inline(['x'],[[c.boolterm(True)] for _ in range(5)]),
            'notes':['The source expects a three-row true chunk followed by two one-row true chunks. The query port compares their five-row logical result, not lazy chunk boundaries.']},
        'Filter.verifyPredicateIsAppliedCorrectlyOnNonLazyEvaluation':{
            'query':'SELECT ?x WHERE { VALUES ?x { true true false false true true false false false false true } FILTER(?x) }',
            'expected':c.inline(['x'],[[c.boolterm(True)] for _ in range(5)]),
            'notes':['The native expected table contains five true rows. Fully materialized engine strategy is not an observable RepositoryConnection assertion.']},
        'Bind.computeResult':{
            'query':'SELECT ?a ?b WHERE { '+c.values_clause(['a'],[[1],[2],[3],[4]])+' BIND(?a AS ?b) }',
            'expected':c.inline(['a','b'],[[c.qid(i),c.qid(i)] for i in [1,2,3,4]]),
            'notes':['Opaque native vocabulary IDs are mapped injectively to IRIs; the BIND alias preserves each value.']},
        'Bind.computeResultWithTableWithoutRows':{
            'query':'SELECT ?a ?b WHERE { VALUES ?a { } BIND(?a AS ?b) }',
            'expected':c.inline(['a','b'],[]),
            'notes':['The native one-column empty input becomes a two-column empty output. The projected header is checked even when there are no rows.']},
        'Bind.computeResultWithTableWithoutColumns':{
            'query':'SELECT ?b WHERE { VALUES () { () () } BIND(42 AS ?b) }',
            'expected':c.inline(['b'],[[c.term_from_sparql('42')],[c.term_from_sparql('42')]]),
            'notes':['Two distinct zero-column solution occurrences must remain two result rows after binding the constant. Native numeric IDs are represented by SPARQL integer literals.']},
        'Bind.limitIsPropagated':{
            'query':'SELECT ?a ?b WHERE { VALUES ?a { 0 1 2 } BIND(42 AS ?b) } ORDER BY ?a LIMIT 1 OFFSET 1',
            'expected':c.inline(['a','b'],[[c.term_from_sparql('1'),c.term_from_sparql('42')]]),
            'notes':['ORDER BY makes the native sorted input order explicit before LIMIT/OFFSET. The query tests the resulting (1,42) row, not internal propagation or cloning.']}
    }
    found=set()
    for n in c.NATIVE:
        spec=definitions.get(n['name']) if n['project']=='qlever' else None
        if spec is None:continue
        case=c.add_native_case(n,spec['query'],spec['expected'])
        case['notes']+=spec['notes'];case['reviewedAtRevision']=QLEVER_PIN;found.add(n['name'])
    for missing in definitions.keys()-found:c.issue('reviewed_cases.py','Missing native source declaration: '+missing)
