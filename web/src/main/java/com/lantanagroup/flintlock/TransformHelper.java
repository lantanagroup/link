package com.lantanagroup.flintlock;

import org.springframework.util.ResourceUtils;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;

public class TransformHelper {
    static final String FHIR_2_CSV_XSLT = "classpath:fhir2csv.xslt";
    private Transformer fhir2csv;

    public TransformHelper() throws FileNotFoundException, TransformerConfigurationException {
        TransformerFactory transFact = TransformerFactory.newInstance();

        File fixXsltFile = ResourceUtils.getFile(FHIR_2_CSV_XSLT);
        Source fixXsltSource = new javax.xml.transform.stream.StreamSource(fixXsltFile);
        this.fhir2csv = transFact.newTransformer(fixXsltSource);
        this.fhir2csv.setErrorListener(new TransformErrorListener());
    }

    public String convert(String inputXml) throws TransformerException, FileNotFoundException {
        StringWriter sw = new StringWriter();
        Source xmlSource = new StreamSource(new StringReader(inputXml));
        Result result = new javax.xml.transform.stream.StreamResult(sw);
        fhir2csv.transform(xmlSource, result);
        return sw.toString();
    }
}

class TransformErrorListener implements ErrorListener {
    @Override
    public void warning(TransformerException exception) throws TransformerException {

    }

    @Override
    public void error(TransformerException exception) throws TransformerException {

    }

    @Override
    public void fatalError(TransformerException exception) throws TransformerException {

    }
}