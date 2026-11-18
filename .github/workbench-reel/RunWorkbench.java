import org.apache.catalina.startup.Tomcat;
import java.io.File;
public class RunWorkbench {
  public static void main(String[] args) throws Exception {
    String base = new File(".").getCanonicalPath();
    System.setProperty("org.eclipse.rdf4j.appdata.basedir", base + "/runtime/data");
    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(base + "/runtime/tomcat");
    tomcat.setPort(18762);
    tomcat.getConnector().setProperty("address", "127.0.0.1");
    tomcat.addWebapp("/rdf4j-server", base + "/server");
    tomcat.addWebapp("/rdf4j-workbench", base + "/workbench");
    tomcat.start();
    tomcat.getServer().await();
  }
}
