package com.placefinder.xslt;

import android.content.Context;

import com.placefinder.R;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Created by Dima on 09.04.2017.
 */

public class XSLTConverters {

    public static List<GeocodingResult> getResults(String response){
        List<GeocodingResult> results = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            try {
                Document doc = db.parse(is);
                String message = doc.getDocumentElement()
                        .getTextContent();

                List<String> sp = new ArrayList<>(Arrays.asList(message.split("\n")));
                sp.removeAll(Arrays.asList(""));

                String[] splitted = sp.toArray(new String[sp.size()]);

                for(int i = 0; i < splitted.length; i++){
                    results.add(
                            new GeocodingResult(
                                    splitted[i],
                                    Double.parseDouble(splitted[i + 1]),
                                    Double.parseDouble(splitted[i + 2])));
                    i += 2;
                }
                return results;
            }  catch (IOException e) {
                return null;
                // handle IOException
            } catch (org.xml.sax.SAXException e) {
                e.printStackTrace();
                return null;
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
            return null;
        }
    }

    public static String xsl(Context context, String inXMLString) {
        try {
            // Create transformer factory
            TransformerFactory factory = TransformerFactory.newInstance();

            // Use the factory to create a template containing the xsl file
            Templates template = factory.newTemplates(new StreamSource(context.getResources().openRawResource(R.raw.xslt_geocoding)));


            // Use the template to create a transformer
            Transformer xformer = template.newTransformer();

            // Prepare the input and output files
            Source source = new StreamSource(new StringReader(inXMLString));
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);

            // Apply the xsl file to the source file and write the result to the output file
            xformer.transform(source, result);

            return stringWriter.toString();
        } catch (TransformerConfigurationException e) {
            return null;
            // An error occurred in the XSL file
        } catch (TransformerException e) {
            // An error occurred while applying the XSL file
            // Get location of error in input file
            SourceLocator locator = e.getLocator();
            int col = locator.getColumnNumber();
            int line = locator.getLineNumber();
            String publicId = locator.getPublicId();
            String systemId = locator.getSystemId();
            return null;
        }
    }
}
