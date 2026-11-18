import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public class GatedLauncher {
 public static void main(String[] args) {
 var listener=new SummaryGeneratingListener();
 var builder=LauncherDiscoveryRequestBuilder.request();
 for(String method:new String[]{"initiallyCompiledMemoIsRecreatedAcrossUnobservedMapGrowth","replayRestoresCallerContextWhenReopeningFails","compiledGroupedDistinctIsRecheckedAcrossMapGrowth"})
 builder.selectors(DiscoverySelectors.selectMethod("org.eclipse.rdf4j.sail.lmdb.LmdbMapGrowthReplayLifecycleTest", method, "java.nio.file.Path"));
 LauncherFactory.create().execute(builder.build(),listener);
 listener.getSummary().printTo(new java.io.PrintWriter(System.out)); listener.getSummary().printFailuresTo(new java.io.PrintWriter(System.out));
 if(listener.getSummary().getTestsFailedCount()!=0 || listener.getSummary().getTestsSucceededCount()!=3)System.exit(1);
 }
}
