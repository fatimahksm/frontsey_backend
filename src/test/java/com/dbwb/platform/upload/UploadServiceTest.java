package com.dbwb.platform.upload;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.upload.entity.UploadedImage;
import com.dbwb.platform.upload.repository.UploadedImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    private UploadService uploadService;
    private UploadProperties properties;

    /**
     * The rows a real repository would hold, so the quota sum is computed from
     * what was actually saved rather than from a number the test hands back.
     * A stub that always answered zero would pass every quota test there is.
     */
    private final List<UploadedImage> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        properties = new UploadProperties();
        properties.setDirectory(tempDir.toString());
        properties.setMaxFileSizeMb(1);
        uploadService = new UploadService(properties, new LocalDiskImageStorage(properties), fakeRepository());
    }

    private UploadedImageRepository fakeRepository() {
        UploadedImageRepository repository = mock(UploadedImageRepository.class);
        when(repository.save(any(UploadedImage.class))).thenAnswer(call -> {
            UploadedImage image = call.getArgument(0);
            stored.add(image);
            return image;
        });
        when(repository.totalBytesForAccount(any())).thenAnswer(call -> stored.stream()
                .filter(image -> image.getAccountId().equals(call.getArgument(0)))
                .mapToLong(UploadedImage::getByteSize)
                .sum());
        return repository;
    }

    /** Real leading bytes for each format - what the service now actually looks at. */
    private static final byte[] PNG_HEADER = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] GIF_HEADER = {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0};
    private static final byte[] WEBP_HEADER = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @Test
    void storesAValidImageAndReturnsItsFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG_HEADER);

        String filename = uploadService.storeImage(file);

        assertThat(filename).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    void acceptsEveryFormatItClaimsTo() throws Exception {
        assertThat(uploadService.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_HEADER))).endsWith(".jpg");
        assertThat(uploadService.storeImage(new MockMultipartFile("file", "a.gif", "image/gif", GIF_HEADER))).endsWith(".gif");
        assertThat(uploadService.storeImage(new MockMultipartFile("file", "a.webp", "image/webp", WEBP_HEADER))).endsWith(".webp");
    }

    // --- the small copy the browser sends alongside (see ImageVariants) ---

    @Test
    void storesTheSmallCopyUnderAKeyDerivedFromTheOriginals() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEADER);
        MockMultipartFile thumbnail = new MockMultipartFile("thumbnail", "photo.jpg", "image/jpeg", JPEG_HEADER);

        String key = uploadService.storeImage(file, thumbnail);

        // The marker on the original is the whole mechanism: it is what tells a
        // client there is a small copy to ask for.
        assertThat(key).contains(ImageVariants.ORIGINAL_MARKER + ".");
        assertThat(ImageVariants.hasThumbnail(key)).isTrue();
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
        assertThat(Files.exists(tempDir.resolve(ImageVariants.thumbnailKey(key)))).isTrue();
    }

    @Test
    void anUploadWithNoSmallCopyIsNotMarkedAsHavingOne() throws Exception {
        // Otherwise every image uploaded before this existed, and every one from
        // a client that does not send a copy, becomes a 404 on every page view.
        String key = uploadService.storeImage(new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEADER));

        assertThat(ImageVariants.hasThumbnail(key)).isFalse();
        assertThat(ImageVariants.thumbnailKey(key)).isEqualTo(key);
    }

    @Test
    void aSmallCopyThatIsNotAnImageIsDroppedRatherThanStored() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEADER);
        byte[] html = "<!doctype html><script>alert(1)</script>".getBytes();
        MockMultipartFile disguised = new MockMultipartFile("thumbnail", "t.jpg", "image/jpeg", html);

        String key = uploadService.storeImage(file, disguised);

        // The photograph is still stored - the owner does not lose their upload
        // over its copy - but nothing unvalidated reached the bucket.
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
        assertThat(ImageVariants.hasThumbnail(key)).isFalse();
        try (var entries = Files.list(tempDir)) {
            assertThat(entries.count()).isEqualTo(1);
        }
    }

    @Test
    void aSmallCopyOverTheSizeLimitIsDroppedToo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEADER);
        byte[] huge = new byte[2 * 1024 * 1024];
        System.arraycopy(JPEG_HEADER, 0, huge, 0, JPEG_HEADER.length);
        MockMultipartFile oversized = new MockMultipartFile("thumbnail", "t.jpg", "image/jpeg", huge);

        String key = uploadService.storeImage(file, oversized);

        assertThat(ImageVariants.hasThumbnail(key)).isFalse();
    }

    @Test
    void theThumbnailKeySwapsOnlyTheMarkerAndKeepsTheExtension() {
        assertThat(ImageVariants.thumbnailKey("abc-def~v.jpg")).isEqualTo("abc-def~400.jpg");
        assertThat(ImageVariants.thumbnailKey("abc-def~v.png")).isEqualTo("abc-def~400.png");
        // No marker, no derived key - never a guess at a URL that was not made.
        assertThat(ImageVariants.thumbnailKey("abc-def.jpg")).isEqualTo("abc-def.jpg");
    }

    @Test
    void refusesAFileThatOnlyClaimsToBeAnImage() {
        // Content-Type is a header the client writes. Trusting it meant an HTML
        // document declared as image/png was stored and then served back from
        // /uploads/**, which is public and unauthenticated.
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "logo.png", "image/png", "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> uploadService.storeImage(disguised))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPEG, PNG, WEBP, or GIF");
    }

    @Test
    void namesTheStoredFileAfterItsRealFormatNotItsClaimedOne() throws Exception {
        // A genuine JPEG announced as a PNG is still a genuine image, so it is
        // stored - but under the extension its bytes justify.
        MockMultipartFile mislabelled = new MockMultipartFile("file", "photo.png", "image/png", JPEG_HEADER);

        assertThat(uploadService.storeImage(mislabelled)).endsWith(".jpg");
    }

    @Test
    void rejectsNonImageContentTypes() {
        MockMultipartFile file = new MockMultipartFile("file", "menu.csv", "text/csv", "name,price\n".getBytes());

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPEG, PNG, WEBP, or GIF");
    }

    @Test
    void rejectsAFileTooShortToIdentify() {
        MockMultipartFile file = new MockMultipartFile("file", "tiny.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPEG, PNG, WEBP, or GIF");
    }

    @Test
    void rejectsFilesLargerThanTheConfiguredLimit() {
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", new byte[2 * 1024 * 1024]);

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("smaller than");
    }

    @Test
    void rejectsAnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    /** ISO-BMFF: four length bytes, "ftyp", then the brand. */
    private static byte[] isoBmff(String brand) {
        byte[] header = new byte[]{0, 0, 0, 0x20, 'f', 't', 'y', 'p', 0, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            header[8 + i] = (byte) brand.charAt(i);
        }
        return header;
    }

    @Test
    void refusesAnIphonePhotoByNameRatherThanReadingOutAList() {
        // Recognised on purpose. Nothing but Safari renders HEIC, so storing it
        // would leave a broken picture on a public page - but "Only JPEG, PNG,
        // WEBP, or GIF" tells the owner nothing about the photo they just took.
        MockMultipartFile heic = new MockMultipartFile("file", "IMG_0421.HEIC", "image/heic", isoBmff("heic"));

        assertThatThrownBy(() -> uploadService.storeImage(heic))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("iPhone photo")
                .hasMessageContaining("converted automatically");
    }

    @Test
    void recognisesTheOtherHeifBrandsToo() {
        for (String brand : new String[]{"heix", "hevc", "mif1", "msf1"}) {
            assertThatThrownBy(() -> uploadService.storeImage(
                    new MockMultipartFile("file", "p." + brand, "image/heif", isoBmff(brand))))
                    .as("brand %s", brand)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("iPhone photo");
        }
    }

    @Test
    void doesNotCallAVideoAnIphonePhoto() {
        // Every MP4 also has "ftyp" at offset 4, so matching that alone would
        // have refused a video with a message about photographs.
        MockMultipartFile video = new MockMultipartFile("file", "clip.mp4", "video/mp4", isoBmff("isom"));

        assertThatThrownBy(() -> uploadService.storeImage(video))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPEG, PNG, WEBP, or GIF")
                .hasMessageNotContaining("iPhone");
    }

    // --- the storage quota (BR: an account's uploads are not unbounded) ---

    /**
     * Nothing metered uploads before this. An account could post a file every
     * few seconds for as long as it existed, and the only ceiling was the rate
     * limit's hundred an hour - a limit on speed, not on total.
     */
    @Test
    void refusesAnUploadThatWouldExceedTheAccountsQuota() {
        UUID account = UUID.randomUUID();
        // A quota of one megabyte and a file size limit of one, so the second
        // upload of a full-size file is the one over the line.
        properties.setQuotaMb(1);
        byte[] nearlyAMegabyte = padded(JPEG_HEADER, 900_000);

        uploadService.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", nearlyAMegabyte), null, account);

        assertThatThrownBy(() -> uploadService.storeImage(
                new MockMultipartFile("file", "b.jpg", "image/jpeg", nearlyAMegabyte), null, account))
                .isInstanceOf(BusinessRuleViolationException.class)
                // One decimal place on purpose: whole megabytes made this read
                // "you have used 0MB" when 900KB of a 1MB quota was gone.
                .hasMessageContaining("past your 1MB of image storage")
                .hasMessageContaining("you have used 0.9MB");
    }

    @Test
    void oneAccountFillingItsQuotaDoesNotAffectAnother() {
        properties.setQuotaMb(1);
        byte[] nearlyAMegabyte = padded(JPEG_HEADER, 900_000);
        UUID first = UUID.randomUUID();
        uploadService.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", nearlyAMegabyte), null, first);

        assertThat(uploadService.storeImage(
                new MockMultipartFile("file", "b.jpg", "image/jpeg", nearlyAMegabyte), null, UUID.randomUUID()))
                .endsWith(".jpg");
    }

    @Test
    void countsTheSmallCopyToo() {
        UUID account = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", padded(JPEG_HEADER, 5_000));
        MockMultipartFile thumbnail = new MockMultipartFile("thumbnail", "photo.jpg", "image/jpeg", padded(JPEG_HEADER, 800));

        uploadService.storeImage(file, thumbnail, account);

        // Two objects in the bucket, so two rows: counting only originals would
        // under-report what the account is storing.
        assertThat(stored).hasSize(2);
        assertThat(stored).extracting(UploadedImage::getByteSize).containsExactly(5_000L, 800L);
    }

    @Test
    void aQuotaOfZeroMeansNoQuota() {
        properties.setQuotaMb(0);
        UUID account = UUID.randomUUID();
        byte[] nearlyAMegabyte = padded(JPEG_HEADER, 900_000);

        uploadService.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", nearlyAMegabyte), null, account);

        assertThat(uploadService.storeImage(
                new MockMultipartFile("file", "b.jpg", "image/jpeg", nearlyAMegabyte), null, account))
                .endsWith(".jpg");
    }

    /**
     * An internal caller with no account - a seeder, or the two-argument
     * overload the older call sites still use - is not metered, and must not
     * be refused for having no quota to spend.
     */
    @Test
    void anUploadWithNoAccountIsNotMetered() {
        properties.setQuotaMb(1);
        byte[] nearlyAMegabyte = padded(JPEG_HEADER, 900_000);

        uploadService.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", nearlyAMegabyte));
        assertThat(uploadService.storeImage(new MockMultipartFile("file", "b.jpg", "image/jpeg", nearlyAMegabyte)))
                .endsWith(".jpg");
        assertThat(stored).isEmpty();
    }

    /** A real header followed by filler, so the bytes are a valid JPEG of a chosen size. */
    private static byte[] padded(byte[] header, int totalSize) {
        byte[] bytes = new byte[totalSize];
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }
}
