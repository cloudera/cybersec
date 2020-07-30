package com.cloudera.cyber.taxii;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mitre.taxii.ContentBindings;
import org.mitre.taxii.client.HttpClient;
import org.mitre.taxii.messages.xml11.*;
import org.mitre.taxii.query.TargetingExpressionInfoType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

@Slf4j
public class TaxiiPoller {
    private final static TaxiiXmlFactory txf = new TaxiiXmlFactory();
    private final static TaxiiXml taxiiXml = txf.createTaxiiXml();
    private final static TransformerFactory tf = TransformerFactory.newInstance();
    private final static ObjectFactory factory = new ObjectFactory();

    private final HttpClient taxiiClient;
    private final KafkaProducer<String, String> producer;

    private TaxiiPoller(Properties properties) {
        Properties kafkaProperties = readKafkaProperties(properties, false);
        kafkaProperties.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getOrDefault("kafka.clientId", "taxii-poller"));
        kafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getOrDefault("kafka.bootstrap-servers", "localhost:9092"));

        kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producer = new KafkaProducer<String, String>(kafkaProperties);

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials((String) properties.get("username"), (String) properties.get("password")));

        HttpClientBuilder cb = HttpClients.custom()
                .setConnectionManagerShared(true)
                .setDefaultCredentialsProvider(credsProvider);

        CloseableHttpClient httpClient = cb.build();

        // Create a Taxii Client with the HttpClient object.
        taxiiClient = new HttpClient(httpClient);

    }

    public static void main(String[] args) throws Exception {
        InputStream input = new FileInputStream(args[0]);

        Properties properties = new Properties();
        properties.load(input);
        input.close();

        String serverUrl = (String) properties.getOrDefault("server", "https://otx.alienvault.com/taxii/discovery");
        String topic = (String) properties.getOrDefault("topic", "stix");
        new TaxiiPoller(properties).run(serverUrl, topic);

    }

    private void run(String serverUrl, String topic) throws URISyntaxException, JAXBException, IOException {
        URI discoverUri = new URI(serverUrl);
        DiscoveryResponse discoveryResponse = discover(discoverUri);

        // figure out what sort of STIX data we will accept
        TargetingExpressionInfoType target = new TargetingExpressionInfoType()
                .withAllowedScopes("STIX_Package/Indicators/Indicator/**")
                .withTargetingExpressionId(ContentBindings.CB_STIX_XML_11);

        discoveryResponse.getServiceInstances().stream()
                .filter(s -> s.isAvailable())
                .filter(s -> s.getServiceType().equals(ServiceTypeEnum.POLL))
                .forEach(poller -> {
                    CollectionInformationRequest fr = factory.createCollectionInformationRequest()
                            .withMessageId(MessageHelper.generateMessageId());
                    CollectionInformationResponse out = (CollectionInformationResponse) sendTaxiiRequest(poller.getAddress(), fr);

                    PollRequest pr = factory.createPollRequest()
                            .withCollectionName(out.getCollections().get(0).getCollectionName())
                            .withMessageId(MessageHelper.generateMessageId())
                            .withExclusiveBeginTimestamp(startTs)
                            .withInclusiveEndTimestamp(endTs)
                            .withPollParameters(new PollParametersType()
                                    .withContentBindings(poller.getContentBindings())
                                    .withAllowAsynch(false)
                                    .withResponseType(ResponseTypeEnum.FULL)
                            );
                    PollResponse prr = (PollResponse) sendTaxiiRequest(poller.getAddress(), pr);
                    prr.getContentBlocks().stream().forEach(b -> {
                        AnyMixedContentType content = b.getContent();
                        for (Object o : content.getContent()) {

                            if (o instanceof Node) {
                                NodeList stix_packages = ((Node) o).getOwnerDocument().getElementsByTagNameNS("http://stix.mitre.org/stix-1", "STIX_Package");
                                for (int i = 0; i < stix_packages.getLength(); i++) {
                                    Node stix_package = stix_packages.item(i);
                                    sendXmlToKafka(topic, stix_package);
                                }
                            }
                        }
                    });

                });
    }

    private void sendXmlToKafka(String topic, Node stix_package) {
        try {
            StringWriter writer = new StringWriter();
            tf.newTransformer().transform(new DOMSource(stix_package), new StreamResult(writer));
            String xmlString = writer.getBuffer().toString();
            sendToKafka(topic, xmlString);
        } catch (TransformerException e) {
            log.error("XML extraction failed", e);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Kafka send failed", e);
            throw new IllegalStateException("Kafka Problem", e);
        }
    }


    private void sendToKafka(String topic, String xmlString) throws InterruptedException, ExecutionException, TimeoutException {
        producer.send(new ProducerRecord<>(topic, null, xmlString)).get(1000, TimeUnit.MILLISECONDS);
    }

    private Object sendTaxiiRequest(String address, Object pr) {
        Object o = null;
        try {
            o = taxiiClient.callTaxiiService(new URI(address), pr);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return o;
    }

    private DiscoveryResponse discover(URI discoverUri) throws JAXBException, IOException {
        DiscoveryRequest dr = factory.createDiscoveryRequest()
                .withMessageId(MessageHelper.generateMessageId());
        return (DiscoveryResponse) taxiiClient.callTaxiiService(discoverUri, dr);
    }
}
