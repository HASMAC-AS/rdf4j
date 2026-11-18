import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public class GroupedLauncher {
 public static void main(String[] args) {
 var listener=new SummaryGeneratingListener();
 var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectMethod("org.eclipse.rdf4j.sail.lmdb.LmdbMapGrowthReplayLifecycleTest", "compiledGroupedDistinctIsRecheckedAcrossMapGrowth", "java.nio.file.Path")).build();
 LauncherFactory.create().execute(request,listener);
 listener.getSummary().printTo(new java.io.PrintWriter(System.out)); listener.getSummary().printFailuresTo(new java.io.PrintWriter(System.out));
 if(listener.getSummary().getTestsFailedCount()!=0)System.exit(1);
 }
}
