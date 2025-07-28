package com.redhat.chatbot.controller;

import java.util.Objects;

import com.redhat.chatbot.model.Document;
import com.redhat.chatbot.service.DocumentIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentIngestionService documentIngestionService;

    @GetMapping("/upload")
    public String uploadPage() {

        return "upload";
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {

        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");

                return "redirect:/documents/upload";
            }

            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                redirectAttributes.addFlashAttribute("error", "Only PDF files are supported");

                return "redirect:/documents/upload";
            }

            Document document = documentIngestionService.ingestPdf(file);
            redirectAttributes.addFlashAttribute("success",
                    "Document '" + document.getFilename() + "' uploaded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to upload document: " + e.getMessage());
        }

        return "redirect:/documents/upload";
    }

}
