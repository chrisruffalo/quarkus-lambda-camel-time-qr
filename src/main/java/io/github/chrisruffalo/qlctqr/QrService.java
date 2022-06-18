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

    private static final boolean INVERT = false;

    @Inject
    Logger logger;

    private static final String FULL = "█";
    private static final String UPPER_HALF = "▀";
    private static final String LOWER_HALF = "▄";
    private static final String NONE = " ";

    protected String charactersToEntity(final String input) {
        return input.codePoints().mapToObj(codePoint -> String.format("&#%d;", codePoint)).collect(Collectors.joining(""));
    }

    @Override
    public void configure() throws Exception {
        from("direct:generate")
            .to("bean:qr?method=generateQrCode")
            .to("bean:qr?method=turnToAscii")
            .to("bean:qr?method=html")
            ;
    }

    public void generateQrCode(Exchange exchange) {
        final Message requestMessage = exchange.getIn();
        final QRCodeWriter writer = new QRCodeWriter();
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            Object body = requestMessage.getBody();
            if (body == null) {
                body = ZonedDateTime.now().toString();
            }

            final BitMatrix matrix = writer.encode(Objects.toString(body), BarcodeFormat.QR_CODE, 60, 60);
            final MatrixToImageConfig config = new MatrixToImageConfig();
            final BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, config);
            ImageIO.write(image, "png", baos);

            requestMessage.setHeader(Exchange.FILE_NAME, requestMessage.getMessageId() + ".png");
            requestMessage.setBody(new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray())));
        } catch (WriterException | IOException e) {
            logger.error("Could not create QR code", e);
            requestMessage.setBody("{}");
        }
        exchange.setMessage(requestMessage);
    }

    public void turnToAscii(Exchange exchange) {
        final Message in = exchange.getIn();
        StringBuilder sb = new StringBuilder();
        if (in.getBody() instanceof InputStream iostream) {
            try {
                final BufferedImage readImage = ImageIO.read(iostream);
                int width = readImage.getWidth();
                int height = readImage.getHeight();
                int minX = 0;
                int maxX = width;

                boolean inContent = false;
                for (int y = 0; y < height; y += 2) {
                    final StringBuilder line = new StringBuilder();
                    for (int x = minX; x < maxX; x ++) {
                        boolean upper = readImage.getRGB(x, y) == -16777216;
                        boolean lower = readImage.getRGB(x, y+1) == -16777216;
                        if (!inContent && (upper || lower)){
                            minX = x - 3;
                            maxX = width - x + 2;
                            height = height - y + 2;
                            inContent = true;
                            line.append(String.join("", Collections.nCopies(maxX - minX,INVERT ? FULL : NONE))).append(System.lineSeparator());
                            line.append(String.join("", Collections.nCopies(maxX - minX,INVERT ? FULL : NONE))).append(System.lineSeparator());
                            line.append(INVERT ? FULL + FULL + FULL : NONE + NONE + NONE);
                        }
                        if (INVERT) {
                            upper = !upper;
                            lower = !lower;
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
                }
                // it seems that there is always a half-line difference so we are compensating for this seeming half-line off-by-one error in the most obvious way
                if (INVERT) {
                    sb.append(String.join("", Collections.nCopies(maxX - minX, INVERT ? UPPER_HALF : LOWER_HALF))).append(System.lineSeparator());
                }
            } catch (IOException ex) {
                logger.error("Could not read image from inputstream", ex);
            }
        }
        in.setBody(sb.toString());
        exchange.setMessage(in);
    }

    public void html(final Exchange exchange) {
        final Message message = exchange.getIn();
        message.setBody(String.format("<html lang='en'><head></head><body><pre><span style=\"font-family: monospace; font-size: 0.70em;\">%s</span></pre></body></html>", charactersToEntity(message.getBody(String.class))));
        exchange.setMessage(message);
    }

}
