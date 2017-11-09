import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.File;
import java.net.URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Calling {

    public static final String ACCOUNT_SID = "SID";
    public static final String AUTH_TOKEN = "TOKEN";

    /**
     * Texts the DestinationPhone from the registered phone number with Twilio
     * (TwilioAuthorizedCallerPhone), and sends the message that is specified in
     * textMessage
     *
     * @param DestinationPhone
     * @param textMessage
     * @param TwilioAuthorizedCallerPhone
     */
    public static void textPhone(String DestinationPhone, String textMessage, String TwilioAuthorizedCallerPhone) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message = Message.creator(
                new PhoneNumber(DestinationPhone),
                new PhoneNumber(TwilioAuthorizedCallerPhone),
                textMessage).create();
    }

    /**
     * Calls the DestinationPhone from the registered phone number with Twilio
     * (TwilioAuthorizedCallerPhone), and speaks the message that is specified
     * in XML file stored in XMLPath
     *
     * @param DestinationPhone
     * @param XMLpath
     * @param TwilioAuthorizedCallerPhone
     */
    public static void callPhone(String DestinationPhone, String XMLpath, String TwilioAuthorizedCallerPhone) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        try {
            Call.creator(new PhoneNumber(DestinationPhone), new PhoneNumber(TwilioAuthorizedCallerPhone),
                    new URI(XMLpath)).create();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates XML file
     *
     * @param s Message
     * @param fileLocation location where the XML file will be created
     */
    public static void createXML(String s, String fileLocation) {
        try {
            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder
                    = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument(); //
            // root element
            Element rootElement = doc.createElement("Response"); // 
            doc.appendChild(rootElement);

            //  supercars element
            Element supercar = doc.createElement("Say");
            rootElement.appendChild(supercar);

            // setting attribute to element
            Attr attr = doc.createAttribute("voice");
            attr.setValue("alice");
            supercar.setAttributeNode(attr);
            supercar.appendChild(doc.createTextNode(s));

            // write the content into xml file
            TransformerFactory transformerFactory
                    = TransformerFactory.newInstance();
            Transformer transformer
                    = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result
                    = new StreamResult(new File(fileLocation));
            transformer.transform(source, result);
            // Output to console for testing
//            StreamResult consoleResult
//                    = new StreamResult(System.out);
//            transformer.transform(source, consoleResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
