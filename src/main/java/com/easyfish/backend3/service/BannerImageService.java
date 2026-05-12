package com.easyfish.backend3.service;

import com.easyfish.backend3.dto.BannerImageRequest;
import com.easyfish.backend3.dto.BannerImageResponse;
import com.easyfish.backend3.entity.BannerImage;
import com.easyfish.backend3.repository.BannerImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Service
public class BannerImageService {

    private final BannerImageRepository bannerImageRepository;
    private final ImageService imageService;

    public BannerImageService(BannerImageRepository bannerImageRepository, ImageService imageService) {
        this.bannerImageRepository = bannerImageRepository;
        this.imageService = imageService;
    }

    public List<BannerImageResponse> getActiveBanners() {
        return bannerImageRepository.findByActiveTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BannerImageResponse> getAllBanners() {
        return bannerImageRepository.findAll()
                .stream()
                .sorted(Comparator
                        .comparing((BannerImage b) -> b.getSortOrder() == null ? 0 : b.getSortOrder())
                        .thenComparing(BannerImage::getId))
                .map(this::toResponse)
                .toList();
    }

    public BannerImageResponse createBanner(BannerImageRequest request, MultipartFile image) throws IOException {
        long activeCount = bannerImageRepository.findByActiveTrueOrderBySortOrderAscIdAsc().size();
        boolean requestedActive = request.getActive() == null || request.getActive();

        if (requestedActive && activeCount >= 3) {
            throw new RuntimeException("Only 3 active banner images are allowed");
        }

        BannerImage banner = new BannerImage();
        banner.setTitle(request.getTitle());
        banner.setImageUrl(saveBannerImageWithFixedRatio(image));
        banner.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        banner.setActive(requestedActive);

        return toResponse(bannerImageRepository.save(banner));
    }

    public BannerImageResponse createBannerFromUrl(BannerImageRequest request) {
        long activeCount = bannerImageRepository.findByActiveTrueOrderBySortOrderAscIdAsc().size();
        boolean requestedActive = request.getActive() == null || request.getActive();
        if (requestedActive && activeCount >= 3) {
            throw new RuntimeException("Only 3 active banner images are allowed");
        }
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new RuntimeException("Banner imageUrl is required");
        }
        BannerImage banner = new BannerImage();
        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        banner.setActive(requestedActive);
        return toResponse(bannerImageRepository.save(banner));
    }

    public BannerImageResponse updateBanner(Long id, BannerImageRequest request, MultipartFile image) throws IOException {
        BannerImage banner = bannerImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        boolean currentActive = banner.getActive() != null && banner.getActive();
        boolean newActive = request.getActive() == null ? currentActive : request.getActive();

        if (!currentActive && newActive) {
            long activeCount = bannerImageRepository.findByActiveTrueOrderBySortOrderAscIdAsc().size();
            if (activeCount >= 3) {
                throw new RuntimeException("Only 3 active banner images are allowed");
            }
        }

        if (StringUtils.hasText(request.getTitle())) {
            banner.setTitle(request.getTitle());
        }
        if (request.getSortOrder() != null) {
            banner.setSortOrder(request.getSortOrder());
        }
        banner.setActive(newActive);

        if (image != null && !image.isEmpty()) {
            banner.setImageUrl(saveBannerImageWithFixedRatio(image));
        }

        return toResponse(bannerImageRepository.save(banner));
    }

    public BannerImageResponse updateBannerFromUrl(Long id, BannerImageRequest request) {
        BannerImage banner = bannerImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));
        boolean currentActive = banner.getActive() != null && banner.getActive();
        boolean newActive = request.getActive() == null ? currentActive : request.getActive();
        if (!currentActive && newActive) {
            long activeCount = bannerImageRepository.findByActiveTrueOrderBySortOrderAscIdAsc().size();
            if (activeCount >= 3) throw new RuntimeException("Only 3 active banner images are allowed");
        }
        if (StringUtils.hasText(request.getTitle())) banner.setTitle(request.getTitle());
        if (request.getSortOrder() != null) banner.setSortOrder(request.getSortOrder());
        banner.setActive(newActive);
        if (StringUtils.hasText(request.getImageUrl())) banner.setImageUrl(request.getImageUrl());
        return toResponse(bannerImageRepository.save(banner));
    }

    public void deleteBanner(Long id) {
        BannerImage banner = bannerImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));
        bannerImageRepository.delete(banner);
    }

    private BannerImageResponse toResponse(BannerImage banner) {
        return new BannerImageResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getSortOrder(),
                banner.getActive()
        );
    }

    private String saveBannerImageWithFixedRatio(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Banner image is required");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean validExtension = originalName.endsWith(".jpg")
                || originalName.endsWith(".jpeg")
                || originalName.endsWith(".png")
                || originalName.endsWith(".webp");

        if (!validExtension) {
            throw new RuntimeException("Only JPG, JPEG, PNG, WEBP images are allowed");
        }

        BufferedImage original = ImageIO.read(file.getInputStream());

        // WEBP may not decode with ImageIO on some servers. Upload original to S3 in that case.
        if (original == null) {
            return imageService.uploadBannerOriginal(file);
        }

        // Banner is stored in S3 as exact 1750x500 JPG. RDS stores only this S3 URL.
        BufferedImage cropped = cropToRatio(original, 7, 2);
        BufferedImage resized = resize(cropped, 1750, 500);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", output);

        return imageService.uploadBannerBytes(output.toByteArray(), "image/jpeg");
    }

    private BufferedImage cropToRatio(BufferedImage source, int ratioW, int ratioH) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();

        double targetRatio = (double) ratioW / ratioH;
        double srcRatio = (double) srcW / srcH;

        int cropW = srcW;
        int cropH = srcH;

        if (srcRatio > targetRatio) {
            cropW = (int) (srcH * targetRatio);
        } else {
            cropH = (int) (srcW / targetRatio);
        }

        int x = (srcW - cropW) / 2;
        int y = (srcH - cropH) / 2;

        return source.getSubimage(x, y, cropW, cropH);
    }

    private BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = output.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(source, 0, 0, width, height, null);
        g2d.dispose();
        return output;
    }
}
