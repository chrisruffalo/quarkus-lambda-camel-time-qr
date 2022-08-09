package io.github.chrisruffalo.qlctqr;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class QrRest extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        rest("/test")
            .get("/time")
                .param()
                .name("invert")
                .type(RestParamType.query)
                .allowableValues("true", "false")
                .defaultValue("false")
                .endParam()
                .param()
                .name("format")
                .type(RestParamType.query)
                .allowableValues("html", "ascii", "png")
                .defaultValue("png")
                .endParam()
                .to("direct:generate")
            .get("/time.{format}")
                .param()
                .name("invert")
                .type(RestParamType.query)
                .allowableValues("true", "false")
                .defaultValue("false")
                .endParam()
                .param()
                .name("format")
                .type(RestParamType.path)
                .allowableValues("html", "ascii", "png")
                .defaultValue("false")
                .endParam()
                .to("direct:generate");
    }

}
