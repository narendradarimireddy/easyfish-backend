package com.easyfish.backend3.controller;

import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/share/product")
public class ProductShareController {

    private static final String STORE_ORIGIN = "https://easyfish.in";
    private final ProductRepository productRepository;

    public ProductShareController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareProduct(@PathVariable Long id, HttpServletRequest request) {
        String productUrl = STORE_ORIGIN + "/product/" + id;
        Optional<Product> productOptional = productRepository.findById(id);

        if (productOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(productUrl))
                    .build();
        }

        Product product = productOptional.get();
        if (!isSocialCrawler(request.getHeader("User-Agent"))) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(productUrl))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        }

        String title = clean(product.getName(), "Easyfish Product");
        String localName = clean(product.getLocalName(), title);
        String packSize = formatPackSize(product);
        String imageUrl = absoluteImage(product.getImageUrl());
        String description = "Local Name: " + localName + " | Pack Size: " + packSize;

        String html = "<!doctype html><html lang=\"en\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<meta name=\"description\" content=\"" + escapeHtml(description) + "\">"
                + "<link rel=\"canonical\" href=\"" + escapeHtml(productUrl) + "\">"
                + "<meta property=\"og:type\" content=\"product\">"
                + "<meta property=\"og:site_name\" content=\"Easyfish\">"
                + "<meta property=\"og:title\" content=\"" + escapeHtml(title) + "\">"
                + "<meta property=\"og:description\" content=\"" + escapeHtml(description) + "\">"
                + "<meta property=\"og:url\" content=\"" + escapeHtml(productUrl) + "\">"
                + "<meta property=\"og:image\" content=\"" + escapeHtml(imageUrl) + "\">"
                + "<meta property=\"og:image:secure_url\" content=\"" + escapeHtml(imageUrl) + "\">"
                + "<meta property=\"og:image:alt\" content=\"" + escapeHtml(title) + "\">"
                + "<meta name=\"twitter:card\" content=\"summary_large_image\">"
                + "<meta name=\"twitter:title\" content=\"" + escapeHtml(title) + "\">"
                + "<meta name=\"twitter:description\" content=\"" + escapeHtml(description) + "\">"
                + "<meta name=\"twitter:image\" content=\"" + escapeHtml(imageUrl) + "\">"
                + "</head><body><a href=\"" + escapeHtml(productUrl) + "\">View " + escapeHtml(title) + " on Easyfish</a></body></html>";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(html);
    }

    private static boolean isSocialCrawler(String userAgent) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        return ua.contains("whatsapp")
                || ua.contains("facebookexternalhit")
                || ua.contains("facebot")
                || ua.contains("twitterbot")
                || ua.contains("telegrambot")
                || ua.contains("linkedinbot")
                || ua.contains("discordbot")
                || ua.contains("slackbot")
                || ua.contains("skypeuripreview")
                || ua.contains("googlebot")
                || ua.contains("bingbot");
    }

    private static String formatPackSize(Product product) {
        String quantity = clean(product.getQuantity(), "500");
        String unit = clean(product.getUnit(), "g");
        if (quantity.toLowerCase(Locale.ROOT).contains(unit.toLowerCase(Locale.ROOT))) {
            return quantity;
        }
        return quantity + " " + unit;
    }

    private static String absoluteImage(String imageUrl) {
        String image = clean(imageUrl, STORE_ORIGIN + "/logo.png");
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image;
        }
        return "https://api.easyfish.in" + (image.startsWith("/") ? image : "/" + image);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
