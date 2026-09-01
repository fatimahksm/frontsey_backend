package com.dbwb.platform.upload;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores owner-uploaded images (logos, cover photos, gallery, menu items,
 * services) on local disk and hands back the filename the caller needs to
 * build a public URL from. Replaces the earlier "paste an image URL"
 * workflow with a real upload.
 */
@Service
public class UploadService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final UploadProperties properties;
    private final ImageStorage storage;

    public UploadService(UploadProperties properties, ImageStorage storage) {
        this.properties = properties;
        this.storage = storage;
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("No file was uploaded.");
        }
        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleViolationException("Image must be smaller than " + properties.getMaxFileSizeMb() + "MB.");
        }

        try {
            // The declared type decides nothing. It is a header the client
            // writes, so "Content-Type: image/png" on an HTML document used to
            // be enough to get that document stored and served back from
            // /uploads/**, which is public. The bytes decide instead, and the
            // extension comes from what the bytes actually are.
            String detectedType = detectImageType(file);
            if (HEIF_TYPE.equals(detectedType)) {
                // Recognised on purpose, and still refused. Nothing but Safari
                // renders HEIC, so storing one leaves a public page with a
                // broken picture on it - and decoding it here would mean
                // libheif on every deployment host. The browser converts it to
                // JPEG before uploading (lib/images/prepare-upload.ts), so one
                // arriving here means that did not run: an old browser, or a
                // client posting to the API directly. Say which, rather than
                // reading out a list that does not explain anything.
                throw new BusinessRuleViolationException(
                        "That looks like an iPhone photo (HEIC), which browsers cannot display. "
                                + "Upload it from the website and it will be converted automatically.");
            }
            if (detectedType == null) {
                throw new BusinessRuleViolationException("Only JPEG, PNG, WEBP, or GIF images are allowed.");
            }

            // Where it goes is the storage's business - local disk in
            // development, Cloudflare R2 in production. This method's job ends
            // at deciding the bytes are acceptable.
            return storage.store(file.getBytes(), detectedType, EXTENSION_BY_CONTENT_TYPE.get(detectedType));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the uploaded image.", e);
        }
    }

    /**
     * The image format the file's own leading bytes identify, or null if they
     * identify none of the formats we accept.
     *
     * Deliberately a small signature check rather than ImageIO.read: this runs
     * on untrusted input, and handing an arbitrary upload to a full image
     * decoder is a wider attack surface than reading twelve bytes.
     */
    private String detectImageType(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        try (InputStream stream = file.getInputStream()) {
            if (stream.readNBytes(header, 0, header.length) < header.length) {
                return null;
            }
        }
        for (var signature : SIGNATURES.entrySet()) {
            if (signature.getKey().matches(header)) {
                return signature.getValue();
            }
        }
        return null;
    }

    /** A magic-number test: bytes that must match at fixed offsets for a file to be this format. */
    private record Signature(int[] offsets, int[] values) {
        static Signature at(int offset, int... values) {
            int[] offsets = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                offsets[i] = offset + i;
            }
            return new Signature(offsets, values);
        }

        Signature and(int offset, int... values) {
            Signature other = at(offset, values);
            int[] mergedOffsets = new int[offsets.length + other.offsets.length];
            int[] mergedValues = new int[values().length + other.values().length];
            System.arraycopy(offsets, 0, mergedOffsets, 0, offsets.length);
            System.arraycopy(other.offsets, 0, mergedOffsets, offsets.length, other.offsets.length);
            System.arraycopy(values(), 0, mergedValues, 0, values().length);
            System.arraycopy(other.values(), 0, mergedValues, values().length, other.values().length);
            return new Signature(mergedOffsets, mergedValues);
        }

        boolean matches(byte[] header) {
            for (int i = 0; i < offsets.length; i++) {
                if ((header[offsets[i]] & 0xFF) != values[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Recognised so it can be refused with a reason, never stored - see storeImage. */
    private static final String HEIF_TYPE = "image/heif";

    private static final Map<Signature, String> SIGNATURES = new LinkedHashMap<>();

    static {
        SIGNATURES.put(Signature.at(0, 0xFF, 0xD8, 0xFF), "image/jpeg");
        SIGNATURES.put(Signature.at(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), "image/png");
        SIGNATURES.put(Signature.at(0, 0x47, 0x49, 0x46, 0x38), "image/gif");
        // WEBP is a RIFF container: "RIFF" then four length bytes then "WEBP".
        SIGNATURES.put(Signature.at(0, 0x52, 0x49, 0x46, 0x46).and(8, 0x57, 0x45, 0x42, 0x50), "image/webp");
        // HEIC/HEIF is an ISO-BMFF box: four length bytes, then "ftyp", then a
        // four-character brand. The brand is what has to be checked - "ftyp"
        // alone is every MP4 as well, and a video refused as "an iPhone photo"
        // explains nothing. Detected only so the refusal can name it; it is
        // never stored. See storeImage.
        for (String brand : List.of("heic", "heix", "hevc", "hevx", "mif1", "msf1")) {
            SIGNATURES.put(
                    Signature.at(4, 'f', 't', 'y', 'p')
                            .and(8, brand.charAt(0), brand.charAt(1), brand.charAt(2), brand.charAt(3)),
                    HEIF_TYPE);
        }
    }

    /** Where a visitor loads this key from, or null when the caller should build it from the request. */
    public String publicUrl(String key) {
        return storage.publicUrl(key);
    }
}
