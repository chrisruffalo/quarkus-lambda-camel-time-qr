package io.github.chrisruffalo.qlctqr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@Named("qr")
@RegisterForReflection
public class QrService extends RouteBuilder {

    @Inject
    Logger logger;

    private static final String FULL = "█";
    private static final String UPPER_HALF = "▀";
    private static final String LOWER_HALF = "▄";
    private static final String NONE = " ";

    @Override
    public void configure() throws Exception {
        from("direct:generate")
            .to("bean:qr?method=generateQrCode")
            .choice().when(this.header("format").in("html", "ascii")) // both html and ascii rely on ascii first
                .to("bean:qr?method=turnToAscii")
                .otherwise()
                    .setHeader("Content-Disposition", this.constant("inline"))
                .end()
                .choice().when(this.header("format").isEqualTo("html")) // html needs to get converted to entities and shoved in a html string
                    .to("bean:qr?method=html")
                .end()
            .end()
            ;
    }

    // originally from http://www.java2s.com/example/java/2d-graphics/invert-black-and-white-bufferedimage.html
    public static void invertBlackAndWhite(BufferedImage image) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                Color c = new Color(image.getRGB(i, j));
                //invert white into black
                if (c.equals(Color.white)) {
                    image.setRGB(i, j, Color.black.getRGB());
                }
                //invert black into white
                else if (c.equals(Color.black)) {
                    image.setRGB(i, j, Color.white.getRGB());
                }
            }
        }
    }

    protected static String charactersToEntity(final String input) {
        return input.codePoints().mapToObj(codePoint -> String.format("&#%d;", codePoint)).collect(Collectors.joining(""));
    }

    public void generateQrCode(Exchange exchange) {
        final Message requestMessage = exchange.getIn();

        int width = 200;
        int height = 200;
        if ("html".equalsIgnoreCase(requestMessage.getHeader("format", String.class))) {
            width = 100;
            height = 100;
        } else if ("ascii".equalsIgnoreCase(requestMessage.getHeader("format", String.class))) {
            width = 60;
            height = 60;
        }

        boolean invert = "true".equalsIgnoreCase(requestMessage.getHeader("invert", String.class));

        final QRCodeWriter writer = new QRCodeWriter();
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            Object body = requestMessage.getBody();
            if (body == null) {
                body = ZonedDateTime.now().toString();
            }

            final BitMatrix matrix = writer.encode(Objects.toString(body), BarcodeFormat.QR_CODE, width, height);
            final MatrixToImageConfig config = new MatrixToImageConfig();
            final BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, config);

            if (invert) {
                invertBlackAndWhite(image);
            }

            // write after inverting
            ImageIO.write(image, "png", baos);

            byte[] bytes = baos.toByteArray();
            requestMessage.setHeader("Content-Length", bytes.length);
            requestMessage.setHeader("Content-Type", "image/x-png");
            requestMessage.setBody(new BufferedInputStream(new ByteArrayInputStream(bytes)));
        } catch (WriterException | IOException e) {
            logger.error("Could not create QR code", e);
            requestMessage.setBody("{}");
        }
        exchange.setMessage(requestMessage);
    }

    public void turnToAscii(Exchange exchange) {
        final Message in = exchange.getIn();
        StringBuilder sb = new StringBuilder();

        boolean invert = "true".equalsIgnoreCase(in.getHeader("invert", String.class));

        if (in.getBody() instanceof final InputStream iostream) {
            try {
                final BufferedImage readImage = ImageIO.read(iostream);
                int width = readImage.getWidth();
                int height = readImage.getHeight();
                int minX = 0;
                int maxX = width;

                boolean doubledY = false;
                boolean inContent = false;
                for (int y = 1; y < height; y += 2) {
                    final StringBuilder line = new StringBuilder();
                    for (int x = minX; x < maxX; x ++) {
                        final Color compare = Color.black;
                        boolean upper = false;
                        boolean lower = false;
                        try {
                            upper = compare.equals(new Color(readImage.getRGB(x, y )));
                            lower = compare.equals(new Color(readImage.getRGB(x, y + 1)));
                        } catch (ArrayIndexOutOfBoundsException ibex) {
                            logger.errorf("index out of bounds at h=%d, w=%d", y, x);
                            continue;
                        }
                        if (!inContent && ((!invert && (upper || lower)) || (invert && (!upper || !lower)))){
                            int spacing = 2;
                            minX = x - spacing;
                            maxX = width - x + (readImage.getHeight() / 30 > 2 ? 2 : 1); // hand tuning nonsense
                            height = height - y + (invert ? 1 : 0);
                            inContent = true;
                            line.append(String.join("", Collections.nCopies(maxX - minX,invert ? FULL : NONE))).append(System.lineSeparator());
                            line.append(String.join("", Collections.nCopies(spacing, invert ? FULL : NONE)));
                        }
                        if (inContent) {
                            String place = NONE;
                            if (upper && lower) {
                                place = FULL;
                            } else if (upper) {
                                place = UPPER_HALF;
                            } else if (lower) {
                                place = LOWER_HALF;
                            }
                            line.append(place);
                        }
                    }
                    if (inContent) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }
                    // repeat one line to make the oddly shaped ascii line up right
                    if (!doubledY && y + 1 > height / 2) {
                        y -= readImage.getHeight() / 30 > 2 ? 2 : 1; // hand tuning doubling
                        doubledY = true;
                    }
                }
            } catch (IOException ex) {
                logger.error("Could not read image from inputstream", ex);
            }
        }
        in.setHeader("Content-Type", "text/plain");
        in.setHeader("Content-Length", sb.toString().getBytes().length);
        in.setBody(sb.toString());
        exchange.setMessage(in);
    }

    public void html(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String outputHtml = String.format("<html lang='en'><head></head><body><pre><span style=\"font-family: monospace; font-size: 0.5em;\">%s</span></pre></body></html>", charactersToEntity(message.getBody(String.class)));
        message.setHeader("Content-Length", outputHtml.getBytes().length);
        message.setHeader("Content-Type", "text/html");
        message.setBody(outputHtml);
        exchange.setMessage(message);
    }

}
