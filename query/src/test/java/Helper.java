import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class Helper {

    public static String convertResourceFileToJson(String fileName) {
        String jsonString = null;
        try(InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)){
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readValue(in,
                    JsonNode.class);
            jsonString = mapper.writeValueAsString(jsonNode);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
        return jsonString;
    }
}
