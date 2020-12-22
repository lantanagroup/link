import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ResourceLoader {
    private FhirContext ctx = FhirContext.forR4();
    private IParser xmlParser = ctx.newXmlParser();

    @Test
    @Ignore
    public void testLoadRRBundlesOntoCQFRuler() throws Exception {
        // This method takes the rr-bundles that you download onto your computer and load them to the CQF-RULER server
        // TODO replace with the path to the bundle files on your computer
        File folder = new File("/Users/briannorris/Downloads/RR_Bundles");
        File[] fileNames = folder.listFiles();

        Arrays.stream(fileNames).forEach(name -> {
            Path path = Paths.get(name.toString());
            String fileName = name.toString().replace("/Users/briannorris/Downloads/RR_Bundles/", "");
            fileName = fileName.replace(".xml", "");
            System.out.println(fileName);
            try {
                putBundle("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir/Bundle/" + fileName, path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    @Ignore
    public void testLoadResources() throws Exception {
        // This method loads the resources onto the fhir.nandina.org server
        // TODO replace the path with the path to your folders locally
        loadResources("/Users/briannorris/Downloads/Resources/steph");
        loadResources("/Users/briannorris/Downloads/Resources/max");
        loadResources("/Users/briannorris/Downloads/Resources/luke");
        loadResources("/Users/briannorris/Downloads/Resources/kenz");
        loadResources("/Users/briannorris/Downloads/Resources/kathy");
        loadResources("/Users/briannorris/Downloads/Resources/jordan");
        loadResources("/Users/briannorris/Downloads/Resources/jeff");
        loadResources("/Users/briannorris/Downloads/Resources/james");
        loadResources("/Users/briannorris/Downloads/Resources/colin");
        loadResources("/Users/briannorris/Downloads/Resources/charles");
        loadResources("/Users/briannorris/Downloads/Resources/cassie");
        loadResources("/Users/briannorris/Downloads/Resources/allison");
    }

    private void loadResources(String folderPath) {
        File folder = new File(folderPath);
        File[] fileNames = folder.listFiles();

        Arrays.stream(fileNames).forEach(name -> {
            Path path = Paths.get(name.toString());

            BufferedReader reader = null;
            try {
                reader = Files.newBufferedReader(path);
                String contents = reader.lines().collect(Collectors.joining());
                String fileName = name.toString().replace("/Users/briannorris/Downloads/RR_Bundles/", "");
                fileName = fileName.replace(".xml", "");

                String resourceType = contents.substring(contents.indexOf("<")+1, contents.indexOf(">"));
                if (resourceType.contains(" ")) {
                    resourceType = resourceType.split(" ")[0];
                } else {
                    resourceType = resourceType;
                }

                String id = StringUtils.substringBetween(contents,"<id value=\"", "\"/>");
                if (null == id) {
                    id = StringUtils.substringBetween(contents,"<id value=\"", "\" />");
                }
                putResource("https://fhir.nandina.org/fhir/" + resourceType + "/" + id, path, resourceType, fileName);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void putResource(String uri, Path path, String resourceType, String fileName) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/xml")
                .PUT(HttpRequest.BodyPublishers.ofFile(path))
                .build();

        try {
            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() > 205) {
                System.out.println("*******************************************************");
                System.out.println(resourceType + " " + fileName + " statusCode: " + response.statusCode());
                System.out.println(response.body());
                System.out.println("*******************************************************");
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void putBundle(String uri, Path path) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/xml")
                .PUT(HttpRequest.BodyPublishers.ofFile(path))
                .build();

        try {
            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.statusCode());


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
