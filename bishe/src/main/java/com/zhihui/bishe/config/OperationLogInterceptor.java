package com.zhihui.bishe.config;

import com.zhihui.bishe.service.OperationLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogService operationLogService;

    public OperationLogInterceptor(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        try {
            int status = response != null ? response.getStatus() : 0;
            operationLogService.record(request, status, ex);
        } catch (Exception ignored) {
            // 日志记录失败不影响业务
        }
    }
}
