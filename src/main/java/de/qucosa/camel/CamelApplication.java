package de.qucosa.camel;

import de.qucosa.camel.routebuilders.SitemapFeederRoutes;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CamelApplication {

    public static void main(String[] args) throws Exception {
        final Logger log = LoggerFactory.getLogger(CamelApplication.class);

        final List<Path> readableFiles = Arrays.stream(args)
                .map(Paths::get)
                .filter(path -> {
                    final boolean readable = Files.isReadable(path);
                    if (readable) {
                        log.info(String.format("Using properties from: %s", path));
                    } else {
                        log.warn(String.format("Unreadable properties file: %s", path));
                    }
                    return readable;
                })
                .collect(Collectors.toList());

        final String locations = readableFiles.stream()
                .map(Path::toString)
                .map("file:"::concat)
                .collect(Collectors.joining(","));

        final Main main = new Main();

        main.setPropertyPlaceholderLocations(String.join(",",
                "classpath:default.properties",
                locations));

        main.addRouteBuilder(new SitemapFeederRoutes());

        main.run();
    }
}
