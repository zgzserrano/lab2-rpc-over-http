package client

import client.translator.TranslationRequest
import client.translator.TranslationResponse
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.WebServiceClientException
import org.springframework.ws.client.core.support.WebServiceGatewaySupport

/**
 * A translation client.
 */
class TranslatorClient : WebServiceGatewaySupport() {
    fun translate(translate: TranslationRequest) = webServiceTemplate.marshalSendAndReceive(translate) as TranslationResponse
}

@SpringBootApplication
class Client {

    /**
     * The Jaxb2 marshaller/unmarshaller.
     */
    @Bean
    fun marshaller() = Jaxb2Marshaller().apply {
        contextPath = "client.translator"
    }

    /**
     * Defines a [TranslatorClient].
     */
    @Bean
    fun translatorClient(marshaller: Jaxb2Marshaller) = TranslatorClient().apply {
        defaultUri = "http://localhost:8080/ws"
        unmarshaller = marshaller
        setMarshaller(marshaller)
    }

    /**
     * The code that is run by this client.
     */
    @Bean
    fun lookup(translatorClient: TranslatorClient) = CommandLineRunner {
        val input = "Translate me!"
        val request = TranslationRequest().apply {
            langFrom = "en"
            langTo = "es"
            text = input
        }
        try {
            println("Result of translating [$input] is [${translatorClient.translate(request).translation}]")
        } catch(exception : WebServiceClientException) {
            println("Something wrong is happening with the the server; fix it!")
        }
    }
}

/**
 * The main entry point.
 */
fun main(args: Array<String>) {
    runApplication<Client>(*args)
}
