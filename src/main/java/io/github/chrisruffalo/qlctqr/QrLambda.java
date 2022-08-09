package io.github.chrisruffalo.qlctqr;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Message;
import org.apache.camel.Produce;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@RegisterForReflection
public class QrLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String API_USE_HELP = "use /time or /time.{format} to request a qr code";

    @Inject
    Logger logger;

    @Produce("direct:generate")
    FluentProducerTemplate generate;

    public APIGatewayProxyResponseEvent error(final int code, final String message) {
        final APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setStatusCode(code);
        responseEvent.setBody(message);
        return responseEvent;
    }

    public APIGatewayProxyResponseEvent handle(APIGatewayProxyRequestEvent input) {
        final String queryPath = input.getPath();
        String format = "png";
        String invert = "false";
        if (queryPath == null || queryPath.isEmpty()) {
            return error(404, API_USE_HELP);
        }

        logger.infof("Handling AWS Proxy request for path %s", queryPath);

        // for some _ridiculous reason_ the query and path parameters are _null_ instead of empty when
        // accessed this way. So what we do is create an empty map and load it up with values if there
        // are any.
        final Map<String,String> params = new HashMap<>();
        if (input.getQueryStringParameters() != null && !input.getQueryStringParameters().isEmpty()) {
            params.putAll(input.getQueryStringParameters());
        }

        // the input body is null because it will be created if missing by the generated
        String body = null;

        // parse
        if (queryPath.endsWith("time") || queryPath.endsWith("epoch")) {
            format = params.getOrDefault("format", format);
        } else if (queryPath.endsWith(".html")) {
            format = "html";
        } else if (queryPath.endsWith(".ascii")) {
            format = "ascii";
        } else if (queryPath.endsWith(".png")) {
            format = "png";
        } else {
            return error(404, API_USE_HELP);
        }

        // if the epoch is requested then set that as the body input for generation
        if (queryPath.contains("/epoch")) {
            body = String.valueOf(System.currentTimeMillis());
        }

        invert = params.getOrDefault("invert", invert);

        final Exchange ex = generate
                .withBody(body)
                .withHeader("format", format.toLowerCase())
                .withHeader("invert", invert.toLowerCase())
                .send();

        final Message message = ex.getIn();

        final APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        final Map<String, String> convertedHeaders = new HashMap<>();
        message.getHeaders().forEach((key, value) -> convertedHeaders.put(key, Objects.toString(value)));

        byte[] responseBody;
        if (message.getBody() instanceof final InputStream is) {
            try (is; final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                is.transferTo(byteArrayOutputStream);
                responseBody = Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());
                convertedHeaders.put("Content-Encoding", "base64"); // signal to browsers that this is something without the default encoding
                responseEvent.setIsBase64Encoded(true);
            } catch (IOException e) {
                return error(500, "error");
            }
        } else {
            responseBody = Objects.toString(message.getBody()).getBytes();
        }

        responseEvent.setHeaders(convertedHeaders);
        responseEvent.setBody(new String(responseBody));
        responseEvent.setStatusCode(200);
        return responseEvent;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return handle(input);
    }

}
