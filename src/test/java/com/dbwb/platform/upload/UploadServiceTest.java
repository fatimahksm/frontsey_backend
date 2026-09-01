package com.dbwb.platform.upload;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir.toString());
        properties.setMaxFileSizeMb(1);
        uploadService = new UploadService(properties, new LocalDiskImageStorage(properties));
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
}
