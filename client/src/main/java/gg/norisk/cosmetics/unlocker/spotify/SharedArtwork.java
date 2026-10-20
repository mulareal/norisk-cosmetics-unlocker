package gg.norisk.cosmetics.unlocker.spotify;

import gg.norisk.client.v2.modules.spotify.ArtworkCache;
import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Song covers for the nametag. NoRisk uploads the cover to its backend and other clients fetch it from
 * its CDN by id, but that upload is NRC+ only (HTTP 403), so the nametag never got a cover. Instead we
 * keep covers on disk under the same id (SHA-256 of the bytes); ArtworkCache.getRemote then loads ids we
 * know from that file instead of the CDN. {@link ClientHooks#ARTWORK_SHARED} lets addons pass covers on.
 */
public final class SharedArtwork {

    /** Larger covers are re-encoded smaller, so they stay cheap to store and to hand to {@link ClientHooks#ARTWORK_SHARED} listeners. */
    public static final int MAX_BYTES = 20000;

    private static final int[][] SHRINK_STEPS = {{128, 85}, {96, 75}, {64, 70}}; // max size px, JPEG quality %
    private static final Map<String, String> fileUrlById = new ConcurrentHashMap<>();
    private static Path dir;
    private static volatile String ownId;
    private static volatile byte[] ownBytes;

    private SharedArtwork() {
    }

    /** Replacement for SpotifyHud.shareArtwork: stores the cover and returns its id (or null). */
    public static String share(String artworkUrl) {
        byte[] original = decode(artworkUrl);
        if (original == null) return null;
        byte[] bytes = original.length <= MAX_BYTES ? original : shrink(original);
        if (bytes == null) return null;
        String id = ArtworkCache.INSTANCE.artworkIdFor(bytes);
        if (!register(id, bytes)) return null;
        ownBytes = bytes;
        ownId = id;
        ClientHooks.ARTWORK_SHARED.fire(listener -> listener.accept(id, bytes));
        return id;
    }

    /** Id of the local player's current cover, or null. */
    public static String ownId() {
        return ownId;
    }

    /** Bytes of the local player's current cover, or null. */
    public static byte[] ownBytes() {
        return ownBytes;
    }

    /** file: URL of a cover we have locally, or null. */
    public static String localUrl(String id) {
        return id != null ? fileUrlById.get(id) : null;
    }

    /** Stores a cover under its id (own covers and covers received from elsewhere). */
    public static boolean register(String id, byte[] bytes) {
        if (fileUrlById.containsKey(id)) return true;
        try {
            Path file = directory().resolve(id + ".img");
            if (!Files.exists(file)) Files.write(file, displayable(bytes));
            fileUrlById.put(id, file.toUri().toString());
            return true;
        } catch (Throwable t) {
            Log.warn("Could not store song cover: " + t);
            return false;
        }
    }

    /** NoRisk only decodes PNG (Minecraft's NativeImage) and WebP, so store anything else as PNG. */
    private static byte[] displayable(byte[] bytes) throws Exception {
        if (isPng(bytes) || isWebp(bytes)) return bytes;
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) return bytes;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static boolean isPng(byte[] b) {
        return b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private static boolean isWebp(byte[] b) {
        return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static synchronized Path directory() throws Exception {
        if (dir == null) {
            Path path = Path.of(System.getProperty("java.io.tmpdir"), "nrc-unlocker-artwork");
            Files.createDirectories(path);
            try (var old = Files.list(path)) { // covers from earlier sessions
                old.forEach(f -> {
                    try {
                        Files.deleteIfExists(f);
                    } catch (Exception ignored) {
                    }
                });
            }
            dir = path;
        }
        return dir;
    }

    /** Same decoder NoRisk uses (URL, file or base64 artwork string -> bytes). Lives in a NoRisk-bundled jar. */
    private static byte[] decode(String artwork) {
        if (artwork == null || artwork.isBlank()) return null;
        try {
            Class<?> decoder = Class.forName("org.endlesssource.mediainterface.api.ArtworkDecoder", true,
                    SharedArtwork.class.getClassLoader());
            Object result = decoder.getMethod("decodeBytes", String.class).invoke(null, artwork);
            byte[] bytes = result instanceof Optional<?> opt && opt.orElse(null) instanceof byte[] b ? b : null;
            return bytes != null && bytes.length > 0 ? bytes : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Small JPEG of the cover for the network (stored as PNG on arrival); the nametag draws it tiny. */
    private static byte[] shrink(byte[] bytes) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) return null; // format ImageIO can't read
            for (int[] step : SHRINK_STEPS) {
                byte[] jpeg = toJpeg(source, step[0], step[1] / 100f);
                if (jpeg.length <= MAX_BYTES) return jpeg;
            }
        } catch (Throwable t) {
            Log.warn("Could not shrink song cover: " + t);
        }
        return null;
    }

    private static byte[] toJpeg(BufferedImage source, int maxSize, float quality) throws Exception {
        double scale = Math.min(1.0, (double) maxSize / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(scaled, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
