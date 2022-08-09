package io.github.chrisruffalo.qlctqr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Consume;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@RegisterForReflection
public class QrService {

    @Inject
    Logger logger;

    @Inject
    @Location("qr.template")
    Template qr;

    private QRCodeWriter writer;
    private final Map<EncodeHintType, Object> hints = new HashMap<>();

    @PostConstruct
    public void init() {
        writer = new QRCodeWriter();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
    }

    private static final String FULL = "█";
    private static final String UPPER_HALF = "▀";
    private static final String LOWER_HALF = "▄";
    private static final String NONE = " ";

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

    @Consume("direct:qr")
    public void generateQrCode(Exchange exchange) {
        final Message requestMessage = exchange.getIn();

        int width = 200;
        int height = 200;
        if ("html".equalsIgnoreCase(requestMessage.getHeader("format", String.class))) {
            width = 90;
            height = 90;
        } else if ("ascii".equalsIgnoreCase(requestMessage.getHeader("format", String.class))) {
            width = 60;
            height = 60;
        }

        boolean invert = "true".equalsIgnoreCase(requestMessage.getHeader("invert", String.class));

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            Object body = requestMessage.getBody();
            if (body == null) {
                body = ZonedDateTime.now().toString();
            }

            final BitMatrix matrix = this.writer.encode(Objects.toString(body), BarcodeFormat.QR_CODE, width, height, hints);
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

    @Consume("direct:ascii")
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
                int originalHeight = height;

                final Color compare = Color.black;
                boolean inContent = false;
                boolean oddContent = false;
                for (int y = 1; y < height; y += 2) {
                    final StringBuilder line = new StringBuilder();
                    // hand tuning for
                    if ( y + 1 == originalHeight / 2 && originalHeight % 2 == 0) {
                        y = y-1;
                        continue;
                    }

                    for (int x = minX; x < maxX; x ++) {
                        boolean upper = compare.equals(new Color(readImage.getRGB(x, y )));;
                        boolean lower = compare.equals(new Color(readImage.getRGB(x, y + 1)));;

                        if (!inContent && ((!invert && (upper || lower)) || (invert && (!upper || !lower)))){
                            // determine if the content starts on the lower line (requiring a "half" character height)
                            oddContent = (!invert && lower && !upper) || (invert && !lower && upper);

                            int spacing = 4;
                            minX = x - spacing;
                            maxX = width - x +  spacing;
                            height = height - y + 1;
                            inContent = true;
                            line.append(String.join("", Collections.nCopies(maxX - minX,invert ? FULL : NONE))).append(System.lineSeparator());
                            line.append(String.join("", Collections.nCopies(maxX - minX,invert ? FULL : NONE))).append(System.lineSeparator());
                            // what this does is: when the content is "odd" (starts on an odd numbered y value)
                            // it re-aligns the start content on an even (full) character row
                            if (oddContent) {
                                y += 1;
                                x = minX - 1;
                                continue;
                            } else {
                                // otherwise it just adds left spacing (like will happen on every other row) and continues
                                line.append(String.join("", Collections.nCopies(spacing, invert ? FULL : NONE)));
                            }
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
                sb.append(String.join("", Collections.nCopies(maxX - minX,invert ? FULL : NONE))).append(System.lineSeparator());
            } catch (IOException ex) {
                logger.error("Could not read image from inputstream", ex);
            }
        }
        in.setHeader("Content-Type", "text/plain");
        in.setHeader("Content-Length", sb.toString().getBytes().length);
        in.setBody(sb.toString());
        exchange.setMessage(in);
    }

    @Consume("direct:html")
    public void html(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String outputHtml = qr
                .data("qr", charactersToEntity(message.getBody(String.class)).trim())
                .render();
        message.setHeader("Content-Length", outputHtml.getBytes().length);
        message.setHeader("Content-Type", "text/html");
        message.setBody(outputHtml);
        exchange.setMessage(message);
    }

}
