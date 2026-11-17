/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. Licensed under the Eclipse Distribution License v1.0.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelRuntime;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbBNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class NativeAggregateMemoEligibilityTest {
 private void enabled(Runnable test) {
  String previous=System.setProperty(NativeValueMemo.ENABLED_PROPERTY,"true");
  try{test.run();}finally{if(previous==null)System.clearProperty(NativeValueMemo.ENABLED_PROPERTY);else System.setProperty(NativeValueMemo.ENABLED_PROPERTY,previous);}
 }
 private NativeLmdbQuerySource source(boolean stable) {
  var source=mock(NativeLmdbQuerySource.class);when(source.hasStableValueIds()).thenReturn(stable);
  when(source.lazyValue(anyLong(),anyInt())).thenAnswer(c->new LmdbBNode(null,"value-"+c.getArgument(1),(long)c.getArgument(0)));
  return source;
 }
 @Test void unstableSourceRetainsAggregateCacheAndFullMasks() {enabled(()->{
  var source=source(false);var context=new AggContext(source,false);
  try{
   assertNull(context.valueMemo);
   var first=context.value(7,1);var mixed=context.value(7,5);
   assertSame(first,context.value(7,1));assertSame(mixed,context.value(7,5));assertNotSame(first,mixed);
   verify(source,times(1)).lazyValue(7,1);verify(source,times(1)).lazyValue(7,5);
  }finally{context.close();}
 });}
 @Test void stableSourceStillOwnsMemo() {enabled(()->{
  var source=source(true);var context=new AggContext(source,false);
  try{assertNotNull(context.valueMemo);assertSame(context.value(7,1),context.value(7,1));verify(source,times(1)).lazyValue(7,1);}finally{context.close();}
 });}
 @Test void refusedMemoDoesNotFallBackToStrongAggregateCache() {enabled(()->{
  var source=source(true);var memory=mock(KernelRuntime.MemoryAccount.class);when(memory.tryReserve(anyLong())).thenReturn(false);
  var context=new AggContext(source,false,false,false,memory);
  try{
   assertNotNull(context.valueMemo);assertNotSame(context.value(7,1),context.value(7,1));
   assertEquals("memory",context.valueMemo.disabledReason);verify(memory,times(1)).tryReserve(anyLong());verify(source,times(2)).lazyValue(7,1);
  }finally{context.close();}
 });}
 @Test void nullSourcePrimitiveContextHasNoMemo() {enabled(()->{var context=new AggContext(null,false);try{assertNull(context.valueMemo);}finally{context.close();}});}
}
