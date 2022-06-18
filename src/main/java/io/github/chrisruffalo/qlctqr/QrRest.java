package io.github.chrisruffalo.qlctqr;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class QrRest extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        rest("/time")
            .produces("text/html")
            .get()
                .param()
                .name("format")
                .endParam()
            .to("direct:generate");

    }

}
