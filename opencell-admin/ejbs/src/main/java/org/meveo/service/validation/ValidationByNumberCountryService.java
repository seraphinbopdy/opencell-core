package org.meveo.service.validation;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Objects;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Stateless
public class ValidationByNumberCountryService {
    
    @Inject private ParamBeanFactory paramBeanFactory;
    protected static Logger log = LoggerFactory.getLogger(ValidationByNumberCountryService.class);
    
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    
    public boolean getValByValNbCountryCode(String valNb, String countryCode) {
        ParamBean instanceParamBean = paramBeanFactory.getInstance();
        try {            
            HttpClient client = HttpClient.newHttpClient();
            String data = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:ec.europa.eu:taxud:vies:services:checkVat:types\">\r\n"
                    + "   <soapenv:Header/>\r\n"
                    + "   <soapenv:Body>\r\n"
                    + "      <urn:checkVat>\r\n"
                    + "         <urn:countryCode>" + countryCode + "</urn:countryCode>\r\n"
                    + "         <urn:vatNumber>" + valNb + "</urn:vatNumber>\r\n"
                    + "      </urn:checkVat>\r\n"
                    + "   </soapenv:Body>\r\n"
                    + "</soapenv:Envelope>";
            String uri = instanceParamBean.getProperty("checkVatService.api", "http://ec.europa.eu/taxation_customs/vies/services/checkVatService");
            int maxRetries = Integer.valueOf(instanceParamBean.getProperty("checkVatService.api.maxRetries", "3"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(BodyPublishers.ofString(data))
                    .build();
            
            int attempt = 0;
            Exception lastException = null;
            while (attempt <= maxRetries) {
                try {
                    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                    	String responseStr = response.body();
                    	return parseXml(responseStr);
                    } else if (attempt < maxRetries) {
                        Thread.sleep(RETRY_DELAY.toMillis());
                    }
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(RETRY_DELAY.toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("getValByValNbCountryCode validation interrupted error ", e.getMessage());
                            throw new BusinessException(ie.getMessage());
                        }
                    }
                }
                attempt++;
            }
            
            throw new BusinessException("Validation failed after " + maxRetries + " attempts", 
                    Objects.requireNonNullElse(lastException, 
                    new RuntimeException("Unknown error")));
            
        } catch (Exception e) {
            log.error("getValByValNbCountryCode error ", e.getMessage());
            throw new BusinessException(e.getMessage());
        }
        
    }
    
    private static Document convertStringToXMLDocument(String xmlString) 
    {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();       
        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try
        {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();            
            //Parse the content to Document object
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            return doc;
        } 
        catch (Exception e) 
        {
            log.error("convertStringToXMLDocument error ", e.getMessage());
        }
        return null;
    }

    public boolean parseXml(String xmlStr) 
    {
      //Use method to convert XML string content to XML Document object
      Document doc = convertStringToXMLDocument(xmlStr);
      String valueValideNodeStr = ""; 

      NodeList docNodes = doc.getFirstChild().getChildNodes();
      if (docNodes.getLength() > 1) {
          Node bodyNode = docNodes.item(1);
          if(bodyNode.getNodeName().contains("Body")) {           
              NodeList bodyNodes = bodyNode.getChildNodes();
              if (bodyNodes.getLength() > 0) {
                  Node checkVatResponseNode = bodyNodes.item(0);
                  if(checkVatResponseNode.getNodeName().contains("checkVatResponse")) {             
                      NodeList checkVatResponseNodes = checkVatResponseNode.getChildNodes();
                      if (checkVatResponseNodes.getLength() > 3) {
                          Node validNode = checkVatResponseNodes.item(3);                         
                          if(validNode.getNodeName().contains("valid")) {             
                              valueValideNodeStr = validNode.getFirstChild().getNodeValue(); 
                          }
                      }                   
                  }  
              }                       
          }   
      }
      boolean valueValideNodeBoolean = false;
      if("true".equals(valueValideNodeStr)) {
          valueValideNodeBoolean = true;
      }
      
      return valueValideNodeBoolean;
    }
}
