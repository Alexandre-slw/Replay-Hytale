package gg.alexandre.replay.util;

import com.hypixel.hytale.common.semver.Semver;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateUtil {

    private static final String UPDATE_URL = "https://update.alexandre.gg";
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
            "Replay-([0-9]+(?:\\.[0-9]+)*)(?:-.*)?\\.jar"
    );

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static Instant lastChecked = Instant.MIN;

    @Nonnull
    public static CompletableFuture<Optional<Semver>> getUpdateAsync(@Nonnull Semver currentVersion) {
        if (lastChecked.plus(Duration.ofHours(1)).isAfter(Instant.now())) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        lastChecked = Instant.now();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPDATE_URL))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return Optional.<Semver>empty();
                    }

                    Optional<Semver> latestVersion = extractVersion(response.body());

                    if (latestVersion.isEmpty()) {
                        return Optional.<Semver>empty();
                    }

                    Semver latest = latestVersion.get();

                    return latest.compareTo(currentVersion) > 0 ? Optional.of(latest) : Optional.<Semver>empty();
                })
                .exceptionally(_ -> Optional.empty());
    }

    @Nonnull
    private static Optional<Semver> extractVersion(@Nonnull String json) {
        Matcher matcher = FILE_NAME_PATTERN.matcher(json);

        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(Semver.fromString(matcher.group(1)));
    }

}