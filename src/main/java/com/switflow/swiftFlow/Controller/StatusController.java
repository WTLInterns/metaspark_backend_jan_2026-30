package com.switflow.swiftFlow.Controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.Response.StatusResponse;
import com.switflow.swiftFlow.Service.StatusService;

@RestController
@RequestMapping("/status")
public class StatusController {

    @Autowired
    private StatusService statusService;

   

   
   
    @PostMapping("/create/{orderId}")
        @PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<StatusResponse> createStatus(
            @PathVariable long orderId, 
            @RequestPart("status") StatusRequest statusRequest,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) throws IOException {
        StatusResponse response = statusService.createStatus(statusRequest, orderId, attachment);
        return ResponseEntity.ok(response);
    }

    // @PostMapping("/create-with-attachment/{orderId}")
    // public ResponseEntity<StatusResponse> createStatusWithAttachment(
    //         @PathVariable long orderId,
    //         @RequestPart("status") StatusRequest statusRequest,
    //         @RequestPart(value = "attachment", required = false) MultipartFile attachment) throws IOException {
    //     StatusResponse response = statusService.createStatusWithAttachment(statusRequest, orderId, attachment);
    //     return ResponseEntity.ok(response);
    // }

    @GetMapping("/order/{orderId}")
            @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<List<StatusResponse>> getStatusesByOrderId(@PathVariable long orderId) {
        List<StatusResponse> responses = statusService.getStatusesByOrderId(orderId);
        return ResponseEntity.ok(responses);
    }
}