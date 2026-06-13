package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.Camera;
import com.zhihui.bishe.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
@CrossOrigin
public class CameraController {

    @Autowired
    private CameraService cameraService;

    @GetMapping
    public List<Camera> getAll() {
        return cameraService.getAllCameras();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Camera> getById(@PathVariable Long id) {
        Camera c = cameraService.getCameraById(id);
        return c != null ? ResponseEntity.ok(c) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Camera camera) {
        try {
            if (camera == null || camera.getCameraName() == null || camera.getCameraName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("摄像头名称不能为空");
            }
            Camera saved = cameraService.addCamera(camera);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("保存失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Camera> update(@PathVariable Long id, @RequestBody Camera camera) {
        Camera updated = cameraService.updateCamera(id, camera);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return cameraService.deleteCamera(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}


