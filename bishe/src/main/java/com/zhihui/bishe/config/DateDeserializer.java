package com.zhihui.bishe.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateDeserializer extends JsonDeserializer<Date> {
    
    private static final String[] DATE_FORMATS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    };

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String date = jsonParser.getText();
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        
        // 尝试多种日期格式
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(date);
            } catch (ParseException e) {
                // 继续尝试下一个格式
            }
        }
        
        // 如果所有格式都失败，尝试解析为时间戳
        try {
            long timestamp = Long.parseLong(date);
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            throw new IOException("无法解析日期格式: " + date, e);
        }
    }
}







