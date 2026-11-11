package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.configuration.CloudinaryProperties;
import com.pfm.personalfinancemanager.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CloudinaryFileStorageServiceTest {

    private MockRestServiceServer mockServer;
    private CloudinaryFileStorageService storageService;

    @BeforeEach
    void setUp() {
        CloudinaryProperties props = new CloudinaryProperties();
        props.setCloudName("demo-cloud");
        props.setApiKey("123456");
        props.setApiSecret("test-secret");
        props.setBaseFolder("pfm");
        props.setUploadBaseUrl("https://api.cloudinary.com/v1_1");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        storageService = new CloudinaryFileStorageService(props, builder.build());
    }

    @Test
    void storeImage_ShouldUploadValidPngAndReturnSecureUrl() {
        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", validPng);

        String expectedUrl = "https://res.cloudinary.com/demo-cloud/image/upload/v1700000000/pfm/avatars/u1_abc.png";
        mockServer.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"secure_url\":\"" + expectedUrl + "\",\"public_id\":\"pfm/avatars/u1_abc\"}",
                        MediaType.APPLICATION_JSON
                ));

        String actualUrl = storageService.storeImage(file, "avatars", "u1", 5 * 1024 * 1024);

        assertThat(actualUrl).isEqualTo(expectedUrl);
        mockServer.verify();
    }

    @Test
    void storeImage_ShouldRejectInvalidMagicBytesBeforeCallingCloudinary() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-a-real-image".getBytes()
        );

        assertThatThrownBy(() -> storageService.storeImage(fakeImage, "avatars", "u1", 5 * 1024 * 1024))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không phải là ảnh hợp lệ");

        mockServer.verify();
    }

    @Test
    void storeImage_ShouldWrapCloudinaryServerFailureInBusinessException() {
        byte[] validJpg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", validJpg);

        mockServer.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> storageService.storeImage(file, "attachments", "tx1", 5 * 1024 * 1024))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cloudinary");

        mockServer.verify();
    }

    @Test
    void deleteByPublicUrl_ShouldExtractPublicIdAndCallDestroyEndpoint() {
        String url = "https://res.cloudinary.com/demo-cloud/image/upload/v1762850000/pfm/attachments/tx1_uuid.jpg";

        mockServer.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        storageService.deleteByPublicUrl(url);

        assertThat(storageService.extractPublicIdFromUrl(url)).isEqualTo("pfm/attachments/tx1_uuid");
        mockServer.verify();
    }

    @Test
    void deleteByPublicUrl_ShouldIgnoreNonCloudinaryUrl() {
        storageService.deleteByPublicUrl("/uploads/avatars/old.png");
        mockServer.verify();
    }

    @Test
    void generateSignature_ShouldProduceExpectedSha1Digest() {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("public_id", "sample_id");
        params.put("timestamp", "1315060510");

        String sig = CloudinaryFileStorageService.generateSignature(params, "abcd");
        assertThat(sig).isEqualTo("4e93d7dd005e2b7123350063a0139f986ba0302f");
    }
}
