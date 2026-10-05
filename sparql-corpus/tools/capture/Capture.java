package org.hasmac.capture;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.*;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.function.Executable;

/**
 * Source-helper argument recorder. It never evaluates an actual expression to manufacture
 * an expected result. The original Java computes only helper arguments/expected constants.
 * This program performs extraction, not verification of the original Jena tests.
 */
public final class Capture {
    private static final class Context {
        String owner, method, invocation, expectedException; int ordinal; boolean completed;
        final List<Map<String,Object>> events=new ArrayList<>();
        final List<String> gaps=new ArrayList<>();
    }
    private static final ThreadLocal<Context> CURRENT=new ThreadLocal<>();
    private static BufferedWriter output,audit;
    private Capture() {}
    public static void gap(String reason) { Context c=CURRENT.get();if(c!=null)c.gaps.add(reason); }
    public static <T extends Throwable> T expectThrows(Class<T> type,Executable body) {
        Context c=Objects.requireNonNull(CURRENT.get(),"No extraction context");String previous=c.expectedException;c.expectedException=type.getName();
        try {body.execute();} catch(Throwable problem) {c.gaps.add("Uncaptured exception inside expected-exception wrapper: "+problem);}
        finally {c.expectedException=previous;} return null;
    }
    public static <T extends Throwable> T expectThrows(Class<T> type,Executable body,String message) {return expectThrows(type,body);}
    public static <T extends Throwable> T expectThrows(Class<T> type,Executable body,Supplier<String> message) {return expectThrows(type,body);}
    public static void node(String expression,Node expected,String mode,String helper) {
        Map<String,Object> gold=new LinkedHashMap<>();gold.put("mode",mode);gold.put("term",term(expected));record(expression,gold,helper);
    }
    public static void floating(String expression,double expected,double delta,String mode,Node expectedNode) {
        Map<String,Object> gold=new LinkedHashMap<>();gold.put("mode",mode);gold.put("number",Double.toString(expected));gold.put("delta",Double.toString(delta));
        if(expectedNode!=null)gold.put("term",term(expectedNode));record(expression,gold,"LibTestExpr.testDouble");
    }
    public static void error(String expression,String helper) {record(expression,new LinkedHashMap<>(Map.of("mode","unbound")),helper);}
    public static void predicate(String expression,String helper) {record(expression,new LinkedHashMap<>(Map.of("mode","predicate-source")),helper);}
    private static void record(String expression,Map<String,Object> gold,String helper) {
        Context c=Objects.requireNonNull(CURRENT.get(),"Helper invoked outside recorded test");
        if(c.expectedException!=null) {
            gold.clear();gold.put("mode",c.expectedException.endsWith("QueryParseException")?"syntax-negative":
                    c.expectedException.endsWith("ExprEvalException")||c.expectedException.endsWith("ARQException")?"unbound":"unsupported-exception");
            gold.put("exception",c.expectedException);
        }
        Map<String,Object> event=new LinkedHashMap<>();event.put("class",c.owner);event.put("method",c.method);event.put("invocation",c.invocation);event.put("ordinal",c.ordinal++);
        event.put("expression",expression);event.put("expected",gold);event.put("helper",helper);
        event.put("prefixes",new TreeMap<>(ARQConstants.getGlobalPrefixMap().getNsPrefixMap()));
        for(StackTraceElement frame:Thread.currentThread().getStackTrace()) {
            if(frame.getClassName().equals(c.owner)&&frame.getMethodName().equals(c.method)) {event.put("callLine",frame.getLineNumber());break;}
        }
        c.events.add(event);
    }
    private static Map<String,Object> term(Node n) {
        Map<String,Object> value=new LinkedHashMap<>();
        if(n.isURI()) {value.put("type","uri");value.put("value",n.getURI());}
        else if(n.isBlank()) {value.put("type","bnode");value.put("value",n.getBlankNodeLabel());}
        else if(n.isLiteral()) {
            value.put("type","literal");value.put("value",n.getLiteralLexicalForm());
            String lang=n.getLiteralLanguage();if(lang!=null&&!lang.isEmpty())value.put("xml:lang",lang);
            else value.put("datatype",Objects.requireNonNullElse(n.getLiteralDatatypeURI(),"http://www.w3.org/2001/XMLSchema#string"));
            if(n.getLiteralBaseDirection()!=null)value.put("direction",n.getLiteralBaseDirection().toString());
        } else {
            try {var t=n.getTriple();value.put("type","triple");value.put("value",Map.of("subject",term(t.getSubject()),"predicate",term(t.getPredicate()),"object",term(t.getObject())));}
            catch(UnsupportedOperationException ex) {value.put("type","unsupported");value.put("value",n.toString());}
        }
        return value;
    }
    public static String json(Object value) {
        if(value==null)return "null";
        if(value instanceof String s) {
            StringBuilder b=new StringBuilder("\"");
            for(int i=0;i<s.length();i++) {char c=s.charAt(i);switch(c) {
                case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");
                default->{if(c<32||Character.isSurrogate(c))b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else b.append(c);}
            }}return b.append('"').toString();
        }
        if(value instanceof Boolean||value instanceof Number)return value.toString();
        if(value instanceof Map<?,?> map)return map.entrySet().stream().map(e->json(e.getKey().toString())+":"+json(e.getValue())).collect(Collectors.joining(",","{","}"));
        if(value instanceof Iterable<?> it) {List<String> cells=new ArrayList<>();for(Object v:it)cells.add(json(v));return String.join(",",cells).transform(s->"["+s+"]");}
        throw new IllegalArgumentException("Not JSON: "+value.getClass());
    }
    private static boolean annotated(Method m,String name) {return Arrays.stream(m.getAnnotations()).anyMatch(a->a.annotationType().getSimpleName().equals(name));}
    private static void lifecycle(Class<?> type,Object instance,String annotation,boolean reverse) throws Exception {
        List<Class<?>> chain=new ArrayList<>();for(Class<?> c=type;c!=null&&c!=Object.class;c=c.getSuperclass())chain.add(c);
        if(!reverse)Collections.reverse(chain);
        for(Class<?> c:chain)for(Method m:c.getDeclaredMethods())if(annotated(m,annotation)) {m.setAccessible(true);m.invoke(Modifier.isStatic(m.getModifiers())?null:instance);}
    }
    private static List<Object[]> parameters(Class<?> owner,Method m) throws Exception {
        if(m.getParameterCount()==0)return List.<Object[]>of(new Object[0]);
        for(Annotation a:m.getAnnotations()) {
            if(a.annotationType().getSimpleName().equals("MethodSource")) {
                String[] names=(String[])a.annotationType().getMethod("value").invoke(a);if(names.length==0)names=new String[]{m.getName()};
                List<Object[]> result=new ArrayList<>();
                for(String name:names) {
                    Class<?> providerClass=owner;String method=name;
                    if(name.contains("#")) {providerClass=Class.forName(name.substring(0,name.indexOf('#')));method=name.substring(name.indexOf('#')+1);}
                    Method provider=providerClass.getDeclaredMethod(method);provider.setAccessible(true);Object values=provider.invoke(null);
                    List<?> rows;
                    if(values instanceof Stream<?> stream) {try(stream){rows=stream.toList();}}
                    else if(values instanceof Iterable<?> iterable) {List<Object> list=new ArrayList<>();iterable.forEach(list::add);rows=list;}
                    else if(values instanceof Object[] array)rows=Arrays.asList(array);else throw new IllegalArgumentException("Unsupported method source "+values);
                    for(Object row:rows) {
                        if(row instanceof Object[] args)result.add(args);
                        else if(row instanceof org.junit.jupiter.params.provider.Arguments args)result.add(args.get());
                        else result.add(new Object[]{row});
                    }
                }return result;
            }
        }
        throw new IllegalArgumentException("Parameter source not yet captured: "+m);
    }
    private static void extract(Class<?> owner,Method m,Object[] args,int invocation) throws Exception {
        Context c=new Context();c.owner=m.getDeclaringClass().getName();c.method=m.getName();c.invocation=Integer.toString(invocation);CURRENT.set(c);
        Object instance=null;
        try {
            Constructor<?> constructor=owner.getDeclaredConstructor();constructor.setAccessible(true);instance=constructor.newInstance();
            lifecycle(owner,instance,"BeforeEach",false);m.setAccessible(true);m.invoke(instance,args);c.completed=true;
        } catch(InvocationTargetException e) {c.gaps.add("Original method did not complete during capture: "+e.getCause());}
        catch(Throwable e) {c.gaps.add("Extraction invocation failed: "+e);}
        finally {
            if(instance!=null)try{lifecycle(owner,instance,"AfterEach",true);}catch(Throwable e){c.gaps.add("AfterEach failure: "+e);}
            for(Map<String,Object> event:c.events) {event.put("methodCompleted",c.completed);event.put("captureGaps",c.gaps);output.write(json(event));output.newLine();}
            audit.write(json(Map.of("class",c.owner,"method",c.method,"invocation",c.invocation,"captured",c.events.size(),"completed",c.completed,"gaps",c.gaps)));audit.newLine();
            CURRENT.remove();
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length<3)throw new IllegalArgumentException("Capture <events.jsonl> <audit.jsonl> <classes.txt>");
        Locale.setDefault(Locale.ROOT);TimeZone.setDefault(TimeZone.getTimeZone("UTC"));JenaSystem.init();NodeValue.VerboseWarnings=false;
        try(BufferedWriter out=Files.newBufferedWriter(Path.of(args[0]),StandardCharsets.UTF_8);BufferedWriter log=Files.newBufferedWriter(Path.of(args[1]),StandardCharsets.UTF_8)) {
            output=out;audit=log;
            for(String className:Files.readAllLines(Path.of(args[2]),StandardCharsets.UTF_8)) {
                if(className.isBlank()||className.endsWith(".LibTestExpr"))continue;
                try {
                    Class<?> owner=Class.forName(className);lifecycle(owner,null,"BeforeAll",false);
                    List<Method> tests=new ArrayList<>();for(Class<?> type=owner;type!=null&&type!=Object.class;type=type.getSuperclass())
                        for(Method m:type.getDeclaredMethods())if(annotated(m,"Test")||annotated(m,"ParameterizedTest")||annotated(m,"RepeatedTest"))tests.add(m);
                    tests.sort(Comparator.comparing(Method::getName));
                    for(Method m:tests) {
                        if(annotated(m,"Disabled")) {audit.write(json(Map.of("class",className,"method",m.getName(),"captured",0,"gaps",List.of("Disabled upstream test"))));audit.newLine();continue;}
                        try {List<Object[]> values=parameters(owner,m);for(int i=0;i<values.size();i++)extract(owner,m,values.get(i),i);}
                        catch(Throwable e) {audit.write(json(Map.of("class",className,"method",m.getName(),"captured",0,"gaps",List.of(e.toString()))));audit.newLine();}
                    }
                    lifecycle(owner,null,"AfterAll",true);
                } catch(Throwable e) {audit.write(json(Map.of("class",className,"captured",0,"gaps",List.of("Class extraction failed: "+e))));audit.newLine();}
                output.flush();audit.flush();
            }
        }
    }
}
