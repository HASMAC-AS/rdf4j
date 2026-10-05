import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import verify_corpus as verification

class CorpusIntegrityTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
        for name in ['corpus/cases','vendor/jena','reports']:(self.root/name).mkdir(parents=True,exist_ok=True)
        (self.root/'sources.json').write_text(json.dumps({'jena':{'repository':'example/jena','revision':'fixed'}}))
        self.query='SELECT ?x WHERE { VALUES ?x { 1 } }'
        raw=b'original source';(self.root/'vendor/jena/Test.java').write_bytes(raw)
        data=b'<urn:s> <urn:p> <urn:o> .';(self.root/'corpus/data.ttl').write_bytes(data)
        self.case={'id':'example','family':'jena','kind':'evaluation','status':'ready','query':self.query,
          'querySha256':hashlib.sha256(self.query.encode()).hexdigest(),
          'source':{'repository':'example/jena','revision':'fixed','path':'Test.java','sha256':hashlib.sha256(raw).hexdigest()},
          'fixtures':[{'path':'corpus/data.ttl','sha256':hashlib.sha256(data).hexdigest()}],
          'expected':{'kind':'tuple','vars':['x'],'rows':[]}}
        (self.root/'corpus/cases/example.md').write_text('## Provenance\n## Prerequisites\n## Query\n'+self.query+'\n## Expected results\n')
        self.save()
    def tearDown(self):self.temp.cleanup()
    def save(self,records=None):(self.root/'corpus/cases.json').write_text(json.dumps(records if records is not None else[self.case]))
    def test_valid_corpus(self):self.assertTrue(verification.verify(self.root)['passed'])
    def test_changed_query_rejected(self):
        self.case['query']+=' LIMIT 1';self.save()
        with self.assertRaises(RuntimeError):verification.verify(self.root)
    def test_changed_fixture_rejected(self):
        (self.root/'corpus/data.ttl').write_text('changed')
        with self.assertRaises(RuntimeError):verification.verify(self.root)
    def test_missing_document_rejected(self):
        (self.root/'corpus/cases/example.md').unlink()
        with self.assertRaises(RuntimeError):verification.verify(self.root)
    def test_duplicate_ids_rejected(self):
        self.save([self.case,self.case])
        with self.assertRaises(RuntimeError):verification.verify(self.root)
    def test_wrong_source_pin_rejected(self):
        self.case['source']['revision']='other';self.save()
        with self.assertRaises(RuntimeError):verification.verify(self.root)
    def test_stale_document_rejected(self):
        (self.root/'corpus/cases/stale.md').write_text('stale')
        with self.assertRaises(RuntimeError):verification.verify(self.root)

if __name__=='__main__':unittest.main()
