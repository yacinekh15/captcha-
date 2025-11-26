package com.captcha.api;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private DefaultKaptcha captchaProducer;

    // In-memory cache for CAPTCHA codes
    private Map<String, String> captchaCache = new ConcurrentHashMap<>();

    @GetMapping("/new")
    public Map<String, String> newCaptcha() throws Exception {
        String captchaText = captchaProducer.createText();
        BufferedImage captchaImage = captchaProducer.createImage(captchaText);
        
        String captchaId = UUID.randomUUID().toString();
        captchaCache.put(captchaId, captchaText);
        
        // Save image to temp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        String imagePath = tempDir + "/" + captchaId + ".png";
        ImageIO.write(captchaImage, "png", new java.io.File(imagePath));
        
        Map<String, String> response = new HashMap<>();
        response.put("captchaId", captchaId);
        response.put("imageUrl", "/captcha/image/" + captchaId);
        return response;
    }

    @GetMapping(value = "/image/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getImage(@PathVariable String id) throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String imagePath = tempDir + "/" + id + ".png";
        return Files.readAllBytes(Paths.get(imagePath));
    }

    @PostMapping("/verify")
    public Map<String, Boolean> verify(@RequestBody Map<String, String> data) {
        String captchaId = data.get("id");
        String userAnswer = data.get("answer");
        
        boolean isValid = captchaCache.containsKey(captchaId) 
            && captchaCache.get(captchaId).equalsIgnoreCase(userAnswer);
        
        if (isValid) {
            captchaCache.remove(captchaId);
        }
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", isValid);
        return response;
    }
}
