import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.direct.DirectSender;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

@Category(DirectSender.class)
public class DirectTest {
//  @Test()
//  public void testDirectSend() throws Exception {
//    FhirContext ctx = FhirContext.forR4();
//    JsonProperties config = new JsonProperties();
//    config.setDirect(new HashMap<>());
//    config.getDirect().put(JsonProperties.DIRECT_URL, "<NEED TO SET BEFORE RUNNING>");
//    config.getDirect().put(JsonProperties.DIRECT_USERNAME, "<NEED TO SET BEFORE RUNNING>");
//    config.getDirect().put(JsonProperties.DIRECT_PASSWORD, "<NEED TO SET BEFORE RUNNING>");
//    config.getDirect().put(JsonProperties.DIRECT_TO_ADDRESS, "<NEED TO SET BEFORE RUNNING>");
//
//    DirectSender sender = new DirectSender(config, ctx);
//    sender.sendCSV("test email", "test message", "a csv attachment");
//  }
}
