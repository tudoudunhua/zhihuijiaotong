package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.CarOwner;
import com.zhihui.bishe.repository.CarOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/api/owners")
@CrossOrigin
public class CarOwnerController {

    @Autowired
    private CarOwnerRepository carOwnerRepository;

    /** 根据车牌查询车主信息（用于通知前检查） */
    @GetMapping("/{plateNumber}")
    public ResponseEntity<?> getByPlate(@PathVariable String plateNumber) {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("车牌号不能为空");
        }
        Optional<CarOwner> owner = carOwnerRepository.findByPlateNumber(plateNumber.trim());
        return owner.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 录入/更新车主信息（按车牌 upsert）
     * 前端可以在“通知失败：缺少联系方式”时弹窗补录后再重试通知
     */
    @PostMapping
    public ResponseEntity<?> upsert(@RequestBody CarOwner body) {
        if (body == null) return ResponseEntity.badRequest().body("请求体不能为空");
        if (body.getPlateNumber() == null || body.getPlateNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("车牌号不能为空");
        }

        String plate = body.getPlateNumber().trim();
        CarOwner owner = carOwnerRepository.findByPlateNumber(plate).orElse(new CarOwner());
        owner.setPlateNumber(plate);
        owner.setOwnerName(body.getOwnerName());
        owner.setPhoneNumber(body.getPhoneNumber());
        owner.setAllowSms(body.getAllowSms() != null ? body.getAllowSms() : Boolean.TRUE);
        owner.setAllowPhone(body.getAllowPhone() != null ? body.getAllowPhone() : Boolean.TRUE);
        owner.setAllowMessage(body.getAllowMessage() != null ? body.getAllowMessage() : Boolean.TRUE);
        owner.setPreferredChannel(body.getPreferredChannel());
        owner.setUpdateTime(new Date());

        return ResponseEntity.ok(carOwnerRepository.save(owner));
    }
}

