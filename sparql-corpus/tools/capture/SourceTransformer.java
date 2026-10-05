package org.hasmac.capture;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Uses javac's parser, rather than a regex, to locate original methods and helper calls. */
public final class SourceTransformer {
    private static final String HOOK = "org.hasmac.capture.Capture.";
    private record Edit(int start, int end, String replacement) {}
    private static String arg(MethodTree m, int i) { return m.getParameters().get(i).getName().toString(); }
    private static String type(MethodTree m, int i) { return m.getParameters().get(i).getType().toString(); }
    private static boolean isTest(MethodTree m) {
        return m.getModifiers().getAnnotations().stream().anyMatch(a -> Set.of("Test", "ParameterizedTest", "RepeatedTest").contains(a.getAnnotationType().toString().replaceFirst("^.*\\.", "")));
    }
    private static String replacement(String owner, MethodTree m) {
        String name=m.getName().toString(); int n=m.getParameters().size();
        if (m.getBody()==null || n==0 || !type(m,0).equals("String")) return null;
        String a=arg(m,0), b=n>1?arg(m,1):"", c=n>2?arg(m,2):"";
        if (owner.equals("LibTestExpr")) {
            if (name.equals("test") && n==2 && type(m,1).equals("NodeValue")) return HOOK+"node("+a+","+b+".asNode(),\"value-datatype\",\"LibTestExpr.test\");";
            if (name.equals("test") && n==2 && type(m,1).startsWith("Predicate")) return HOOK+"predicate("+a+",\"LibTestExpr.test(Predicate)\");";
            if (name.equals("testSameObject") && n==2) return HOOK+"node("+a+","+b+".asNode(),\"term\",\"LibTestExpr.testSameObject\");";
            if (name.equals("testExpr") && n==2) return HOOK+"node("+a+",eval("+b+").asNode(),\"term\",\"LibTestExpr.testExpr\");";
            if (name.equals("testDouble") && n==3 && type(m,1).equals("double")) return HOOK+"floating("+a+","+b+","+c+",\"double-tolerance\",null);";
            if (name.equals("testDouble") && n==3 && type(m,1).equals("Node")) return HOOK+"floating("+a+",NodeValue.makeNode("+b+").getDouble(),"+c+",\"double-node\","+b+");";
            if (name.equals("testError") && n==1) return HOOK+"error("+a+",\"LibTestExpr.testError\");";
            // testSSE evaluates the expected expression twice in the pinned source.
            // It is deliberately not represented as a test of the input expression.
            if (name.equals("testSSE")) return HOOK+"gap(\"LibTestExpr.testSSE does not evaluate its actual input in this source revision\");";
        }
        if (owner.equals("TestSPARQLKeywordFunctions")) {
            if (name.equals("test") && n==2) return HOOK+"node("+a+",NodeFactoryExtra.parseNode("+b+"),\"term\",\"TestSPARQLKeywordFunctions.test\");";
            if (name.equals("testEvalException") && n==1) return HOOK+"error("+a+",\"TestSPARQLKeywordFunctions.testEvalException\");";
        }
        if (owner.equals("TestCastXSD")) {
            if (name.equals("testCast") && n==2) return HOOK+"node("+a+",SSE.parseNode("+b+"),\"term\",\"TestCastXSD.testCast\");";
            if (name.equals("testNoCast") && n==1) return HOOK+"error("+a+",\"TestCastXSD.testNoCast\");";
        }
        if (owner.equals("TestFunctions")) {
            if (name.equals("test") && n==2 && type(m,1).equals("NodeValue")) return HOOK+"node("+a+","+b+".asNode(),\"term\",\"TestFunctions.test\");";
            if (name.equals("test") && n==2 && type(m,1).startsWith("Predicate")) return HOOK+"predicate("+a+",\"TestFunctions.test(Predicate)\");";
            if (name.equals("testEvalException") && n==1) return HOOK+"error("+a+",\"TestFunctions.testEvalException\");";
        }
        return null;
    }
    private static long newlines(String s) { return s.chars().filter(x -> x=='\n').count(); }
    public static void main(String[] args) throws Exception {
        if (args.length!=3) throw new IllegalArgumentException("SourceTransformer <jena-arq-test-root> <output-root> <metadata.jsonl>");
        Path root=Path.of(args[0]), output=Path.of(args[1]);
        List<Path> selected;
        try (var files=Files.walk(root)) {
            selected=files.filter(p->p.toString().endsWith(".java")).filter(p->{
                try {
                    String n=p.getFileName().toString(),s=Files.readString(p);
                    if (Set.of("TestExpressions.java","TestExpressions2.java","TestExpressions3.java").contains(n)) return false;
                    return n.equals("LibTestExpr.java") || Set.of("TestSPARQLKeywordFunctions.java","TestCastXSD.java","TestFunctions.java").contains(n)
                            || n.startsWith("Test") && s.contains("LibTestExpr") && s.contains("@Test");
                } catch(IOException e) { throw new UncheckedIOException(e); }
            }).sorted().toList();
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if (compiler==null) throw new IllegalStateException("JDK compiler required");
        try (StandardJavaFileManager fm=compiler.getStandardFileManager(null,Locale.ROOT,StandardCharsets.UTF_8);
             BufferedWriter meta=Files.newBufferedWriter(Path.of(args[2]),StandardCharsets.UTF_8)) {
            for (Path file:selected) {
                String original=Files.readString(file); List<Edit> edits=new ArrayList<>(); List<Map<String,Object>> methods=new ArrayList<>();
                var units=fm.getJavaFileObjects(file.toFile()); DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
                JavacTask task=(JavacTask)compiler.getTask(null,fm,diagnostics,List.of("-proc:none"),null,units);
                CompilationUnitTree unit=task.parse().iterator().next();
                if (diagnostics.getDiagnostics().stream().anyMatch(d->d.getKind()==Diagnostic.Kind.ERROR)) throw new IllegalStateException("Cannot parse "+file+": "+diagnostics.getDiagnostics());
                SourcePositions positions=Trees.instance(task).getSourcePositions();
                String simple=file.getFileName().toString().replace(".java","");
                String fqcn=unit.getPackageName()+"."+simple;
                new TreeScanner<Void,Void>() {
                    boolean testMethod=false; List<Map<String,Object>> calls;
                    String slice(Tree node) { int a=(int)positions.getStartPosition(unit,node),b=(int)positions.getEndPosition(unit,node);return original.substring(a,b); }
                    @Override public Void visitMethod(MethodTree m,Void unused) {
                        if (isTest(m) && m.getBody()!=null) {
                            testMethod=true; calls=new ArrayList<>();
                            Map<String,Object> info=new LinkedHashMap<>();
                            info.put("name",m.getName().toString());info.put("line",unit.getLineMap().getLineNumber(positions.getStartPosition(unit,m)));
                            info.put("sourceAssertion",slice(m.getBody()));info.put("annotations",m.getModifiers().getAnnotations().stream().map(Object::toString).toList());
                            info.put("parameters",m.getParameters().stream().map(Object::toString).toList());info.put("invocations",calls);methods.add(info);
                            super.visitMethod(m,unused);testMethod=false;calls=null;return null;
                        }
                        String body=replacement(simple,m);
                        if (body!=null) {
                            int a=(int)positions.getStartPosition(unit,m.getBody()),b=(int)positions.getEndPosition(unit,m.getBody());
                            String text="{ "+body+" }";
                            text+="\n".repeat(Math.toIntExact(newlines(original.substring(a,b))-newlines(text)));
                            edits.add(new Edit(a,b,text));return null;
                        }
                        return super.visitMethod(m,unused);
                    }
                    @Override public Void visitMethodInvocation(MethodInvocationTree call,Void unused) {
                        String name=call.getMethodSelect().toString();
                        if (testMethod) {
                            Map<String,Object> info=new LinkedHashMap<>();info.put("name",name);
                            info.put("line",unit.getLineMap().getLineNumber(positions.getStartPosition(unit,call)));
                            info.put("arguments",call.getArguments().stream().map(this::slice).toList());calls.add(info);
                        }
                        if (name.equals("assertThrows") || name.equals("Assertions.assertThrows")) {
                            edits.add(new Edit((int)positions.getStartPosition(unit,call.getMethodSelect()),(int)positions.getEndPosition(unit,call.getMethodSelect()),HOOK+"expectThrows"));
                        }
                        return super.visitMethodInvocation(call,unused);
                    }
                }.scan(unit,null);
                edits.sort(Comparator.comparingInt(Edit::start).reversed());StringBuilder transformed=new StringBuilder(original);int previous=original.length();
                for (Edit edit:edits) { if (edit.end()>previous) throw new IllegalStateException("Overlapping source edits: "+file);transformed.replace(edit.start(),edit.end(),edit.replacement());previous=edit.start(); }
                Path target=output.resolve(root.relativize(file));Files.createDirectories(target.getParent());Files.writeString(target,transformed,StandardCharsets.UTF_8);
                Map<String,Object> info=new LinkedHashMap<>();info.put("class",fqcn);info.put("file",root.relativize(file).toString().replace(File.separatorChar,'/'));
                info.put("helperEdits",edits.size());info.put("methods",methods);meta.write(Capture.json(info));meta.newLine();
            }
        }
        System.out.println("CAPTURE_SOURCE_CLASSES "+selected.size());
    }
}
