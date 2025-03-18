package com.kryeit.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ProductImages {
    private static final String BASE_DIR = "db/product_images/";
    private static final Logger logger = Logger.getLogger(ProductImages.class.getName());

    static {
        createBaseDirectory();
    }

    private static void createBaseDirectory() {
        new File(BASE_DIR).mkdirs();
    }

    public static void uploadImage(String productName, byte[] imageData) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            productDir.mkdirs();
        }

        int nextIndex = (int) Files.list(productDir.toPath()).count() + 1;
        String imageName = nextIndex + ".webp";
        File imageFile = new File(productDir, imageName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(imageData);
            logger.info("Image uploaded successfully: " + imageFile.getAbsolutePath());
        }
    }

    public static void deleteImage(String productName, int imageIndex) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            throw new IOException("Product directory does not exist: " + productName);
        }

        File[] images = productDir.listFiles();
        if (images == null || imageIndex < 1 || imageIndex > images.length) {
            throw new IOException("Invalid image index: " + imageIndex);
        }

        File imageFile = new File(productDir, imageIndex + ".webp");
        if (imageFile.delete()) {
            logger.info("Image deleted successfully: " + imageFile.getName());
            // Rename remaining images to maintain order
            for (int i = imageIndex; i < images.length; i++) {
                File oldFile = new File(productDir, (i + 1) + ".webp");
                File newFile = new File(productDir, i + ".webp");
                if (oldFile.exists()) {
                    Files.move(oldFile.toPath(), newFile.toPath());
                }
            }
        } else {
            throw new IOException("Failed to delete image: " + imageFile.getName());
        }
    }

    public static void deleteAllImages(String productName) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            throw new IOException("Product directory does not exist: " + productName);
        }

        File[] images = productDir.listFiles();
        if (images != null) {
            for (File image : images) {
                if (!image.delete()) {
                    throw new IOException("Failed to delete image: " + image.getName());
                }
            }
        }

        if (productDir.delete()) {
            logger.info("All images deleted and directory removed for product: " + productName);
        } else {
            throw new IOException("Failed to remove product directory: " + productName);
        }
    }

    public static List<String> getImages(String productName) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            throw new IOException("Product directory does not exist: " + productName);
        }

        File[] images = productDir.listFiles();
        if (images == null) {
            return List.of();
        }

        List<String> imageUrls = new ArrayList<>();
        for (File image : images) {
            String imageUrl = "/api/products/images/" + productName + "/" + image.getName();
            imageUrls.add(imageUrl);
        }

        return imageUrls;
    }

    public static void swap(String productName, int i, int j) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            throw new IOException("Product directory does not exist: " + productName);
        }


        File fileI = new File(productDir, i + ".webp");
        File fileJ = new File(productDir, j + ".webp");

        if (!fileI.exists() || !fileJ.exists()) {
            throw new IOException("One or both image files do not exist.");
        }

        File tempFile = new File(productDir, "temp_" + UUID.randomUUID() + ".webp");
        Files.move(fileI.toPath(), tempFile.toPath());
        Files.move(fileJ.toPath(), fileI.toPath());
        Files.move(tempFile.toPath(), fileJ.toPath());
    }

    public static void renameFolder(String productName, String newProductName) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            throw new IOException("Product directory does not exist: " + productName);
        }

        File newProductDir = new File(BASE_DIR + newProductName);
        if (newProductDir.exists()) {
            throw new IOException("New product directory already exists: " + newProductName);
        }

        if (productDir.renameTo(newProductDir)) {
            logger.info("Product directory renamed from " + productName + " to " + newProductName);
        } else {
            throw new IOException("Failed to rename product directory from " + productName + " to " + newProductName);
        }
    }

    public static String getImage(String productName, int index) throws IOException {
        File productDir = new File(BASE_DIR + productName);
        if (!productDir.exists()) {
            throw new IOException("Product directory does not exist: " + productName);
        }

        File imageFile = new File(productDir, index + ".webp");
        if (!imageFile.exists()) {
            throw new IOException("Image file does not exist: " + imageFile.getName());
        }

        return "/api/products/images/" + productName + "/" + imageFile.getName();
    }
}