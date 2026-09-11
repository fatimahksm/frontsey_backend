package com.dbwb.platform.upload;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.upload.entity.UploadedImage;
import com.dbwb.platform.upload.repository.UploadedImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final UploadedImageRepository uploadedImages;

    public UploadService(UploadProperties properties, ImageStorage storage, UploadedImageRepository uploadedImages) {
        this.properties = properties;
        this.storage = storage;
        this.uploadedImages = uploadedImages;
    }

    public String storeImage(MultipartFile file) {
        return storeImage(file, null, null);
    }

    public String storeImage(MultipartFile file, MultipartFile thumbnail) {
        return storeImage(file, thumbnail, null);
    }

    /**
     * Stores an image and, when the caller sends one, the small copy that goes
     * with it under a key derived from the original's - see ImageVariants.
     *
     * The thumbnail is validated exactly as strictly as the original. It
     * arrives on the same request from the same untrusted client, and a
     * caller that can put arbitrary bytes in the bucket by calling them a
     * thumbnail has the same hole the magic-byte check was added to close.
     *
     * A thumbnail that fails validation is dropped rather than failing the
     * whole upload: the original is fine, the page will simply load the full
     * image where it would have loaded a small one, and refusing the owner's
     * photograph over its shrunken copy would be the worse trade.
     */
    public String storeImage(MultipartFile file, MultipartFile thumbnail, UUID accountId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("No file was uploaded.");
        }
        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleViolationException("Image must be smaller than " + properties.getMaxFileSizeMb() + "MB.");
        }

        requireRoomInQuota(accountId, file.getSize());

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
            String extension = EXTENSION_BY_CONTENT_TYPE.get(detectedType);
            byte[] smallCopy = acceptableThumbnail(thumbnail, maxBytes);
            if (smallCopy == null) {
                String onlyKey = storage.store(file.getBytes(), detectedType, extension);
                record(accountId, onlyKey, file.getSize(), detectedType);
                return onlyKey;
            }

            // The marker goes in the original's key, not just the copy's: it is
            // the original's URL that gets stored and later read, and the
            // marker is how a client knows a small copy exists to ask for.
            String key = storage.store(file.getBytes(), detectedType, ImageVariants.ORIGINAL_MARKER + extension);
            storage.storeAt(ImageVariants.thumbnailKey(key), smallCopy, "image/jpeg");
            record(accountId, key, file.getSize(), detectedType);
            // The small copy is its own object costing its own bytes, so it is
            // its own row. Counting only originals would under-report what an
            // account is actually storing.
            record(accountId, ImageVariants.thumbnailKey(key), smallCopy.length, "image/jpeg");
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the uploaded image.", e);
        }
    }

    /**
     * Refuses an upload that would put the account past its storage quota.
     *
     * Checked against the original's size before anything is written, so the
     * refusal costs no bucket writes. The thumbnail is not added in here: it
     * is a fraction of the original and may yet be dropped as unacceptable, so
     * counting it before it exists would refuse uploads that fit.
     *
     * A null account is an internal caller with nothing to bill - a seeder or
     * a test - and is not metered.
     */
    private void requireRoomInQuota(UUID accountId, long incomingBytes) {
        if (accountId == null || properties.getQuotaMb() <= 0) {
            return;
        }
        long quotaBytes = properties.getQuotaMb() * 1024L * 1024L;
        long used = uploadedImages.totalBytesForAccount(accountId);
        if (used + incomingBytes > quotaBytes) {
            throw new BusinessRuleViolationException(
                    "This photo would take you past your " + properties.getQuotaMb()
                            + "MB of image storage - you have used " + megabytes(used)
                            + "MB. Delete some photos you no longer use, or contact support to raise the limit.");
        }
    }

    /**
     * Megabytes to one decimal place. Whole megabytes made the message
     * contradict itself: 900KB against a 1MB quota read "you have used 0MB",
     * which tells an owner their storage is both full and empty.
     */
    private static String megabytes(long bytes) {
        return String.format("%.1f", bytes / (1024.0 * 1024.0));
    }

    /** Notes a stored object against the account paying for it; skipped for callers with no account. */
    private void record(UUID accountId, String key, long byteSize, String contentType) {
        if (accountId != null) {
            uploadedImages.save(new UploadedImage(accountId, key, byteSize, contentType));
        }
    }

    /**
     * The thumbnail's bytes if they are a JPEG we would have accepted on its
     * own, and null in every other case - absent, empty, oversized, or not
     * actually an image. Null means "store the original alone", never an error.
     *
     * JPEG only, because that is what the browser encodes a downscale as, and
     * narrowing it here means one fewer format that can reach the bucket by a
     * path the main upload does not take.
     */
    private byte[] acceptableThumbnail(MultipartFile thumbnail, long maxBytes) throws IOException {
        if (thumbnail == null || thumbnail.isEmpty() || thumbnail.getSize() > maxBytes) {
            return null;
        }
        return "image/jpeg".equals(detectImageType(thumbnail)) ? thumbnail.getBytes() : null;
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
