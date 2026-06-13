package com.zhihui.bishe.service;

import com.zhihui.bishe.model.Camera;
import com.zhihui.bishe.repository.CameraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CameraService {

    @Autowired
    private CameraRepository cameraRepository;

    public List<Camera> getAllCameras() {
        return cameraRepository.findAll();
    }

    public Camera getCameraById(Long id) {
        Optional<Camera> optional = cameraRepository.findById(id);
        return optional.orElse(null);
    }

    public Camera addCamera(Camera camera) {
        return cameraRepository.save(camera);
    }

    public Camera updateCamera(Long id, Camera camera) {
        Optional<Camera> optional = cameraRepository.findById(id);
        if (!optional.isPresent()) {
            return null;
        }
        Camera existing = optional.get();
        existing.setCameraName(camera.getCameraName());
        existing.setLocation(camera.getLocation());
        existing.setIpAddress(camera.getIpAddress());
        existing.setStatus(camera.getStatus());
        existing.setInstallTime(camera.getInstallTime());
        existing.setLastHeartbeat(camera.getLastHeartbeat());
        existing.setRemark(camera.getRemark());
        return cameraRepository.save(existing);
    }

    public boolean deleteCamera(Long id) {
        if (!cameraRepository.existsById(id)) {
            return false;
        }
        cameraRepository.deleteById(id);
        return true;
    }
}


