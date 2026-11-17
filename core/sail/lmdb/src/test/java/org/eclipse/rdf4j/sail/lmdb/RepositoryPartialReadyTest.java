/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. Licensed under the Eclipse Distribution License v1.0.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryPartialReadyTest {
 static final String FLAG="rdf4j.lmdb.repository.partialReady";
 static final class Store extends ValueStore implements AutoCloseable {
  Store(File dir)throws Exception {super(dir,new LmdbStoreConfig().setInlineLiterals(false));}
 }
 @Test void skipsOnlyPreparedRootsInMixedRowAndPreservesColdChildren(@TempDir File dir)throws Exception {
  try(Store store=new Store(dir);var scope=store.openValueResolutionScope()) {
   var vf=SimpleValueFactory.getInstance();
   store.startTransaction(true);
   long node=store.storeValue(vf.createBNode("cold"));long datatype=store.storeValue(vf.createIRI("urn:custom:type"));
   long triple=store.storeValue(vf.createTripleTerm(vf.createIRI("urn:s"),vf.createIRI("urn:p"),vf.createLiteral("o")));
   store.commit();store.clearCaches();
   LmdbValue cold,dt,tt;
   try(var activation=scope.activateForeground()){cold=store.getLazyValue(node);dt=store.getLazyValue(datatype);tt=store.getLazyValue(triple);}
   AtomicInteger visits=new AtomicInteger();
   LmdbLiteral ready=new LmdbLiteral(new ValueStoreRevision.Default(store),"ready",XSD.STRING,-1) {
    @Override public void visitValueChildren(Consumer<Value> visitor){visits.incrementAndGet();super.visitValueChildren(visitor);}
   };
   LmdbLiteral custom=new LmdbLiteral(new ValueStoreRevision.Default(store),"custom",(IRI)dt,-1);
   AtomicInteger lazyVisits=new AtomicInteger();
   LmdbLiteral initializedLazy=new LmdbLiteral(cold.getValueStoreRevision(),"owned",XSD.STRING,-1) {
    @Override public void visitValueChildren(Consumer<Value> visitor){lazyVisits.incrementAndGet();super.visitValueChildren(visitor);}
   };
   List<Value> row=List.of(ready,cold,custom,tt,initializedLazy);
   AtomicInteger pulls=new AtomicInteger();
   var source=new IterationWrapper<List<Value>>(new CloseableIteratorIteration<>(List.of(row,row).iterator())) {
    @Override public List<Value> next(){pulls.incrementAndGet();return super.next();}
   };
   try(var result=new RepositoryValueIteration<>(source,(List<Value> r,Consumer<Value> sink)->r.forEach(sink),store,scope,ignored->{})) {
    assertSame(row,result.next());assertEquals(1,pulls.get());
    assertTrue(cold.isInitialized());assertTrue(dt.isInitialized());assertTrue(tt.isInitialized());
    assertEquals(Boolean.getBoolean(FLAG)?1:3,visits.get(),"prepared literal traversed only by readiness check in experimental mode");
    assertEquals(2,lazyVisits.get(),"initialized Lazy root must retain owner capture and wave traversal");
    assertEquals("urn:custom:type",custom.getDatatype().stringValue());
   }
   scope.close();assertEquals("cold",cold.stringValue());assertEquals("ready",ready.getLabel());assertEquals("owned",initializedLazy.getLabel());
   assertEquals(vf.createTripleTerm(vf.createIRI("urn:s"),vf.createIRI("urn:p"),vf.createLiteral("o")),tt);
  }
 }
}
