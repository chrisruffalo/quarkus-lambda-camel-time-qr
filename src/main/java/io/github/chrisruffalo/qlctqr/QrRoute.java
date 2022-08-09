package io.github.chrisruffalo.qlctqr;

import org.apache.camel.builder.RouteBuilder;

public class QrRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:generate")
                .log("generating ${headers.format}")
                .to("direct:qr")
                .choice().when(this.header("format").in("html", "ascii")) // both html and ascii rely on ascii first
                .to("direct:ascii")
                .otherwise()
                .setHeader("Content-Disposition", this.constant("inline"))
                .end()
                .choice().when(this.header("format").isEqualTo("html")) // html needs to get converted to entities and shoved in a html string
                .to("direct:html")
                .end()
                .end()
        ;
    }
}
