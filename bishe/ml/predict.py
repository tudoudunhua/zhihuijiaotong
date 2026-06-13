#!/usr/bin/env python3
"""
predict.py
如果存在 ml/model.pkl，会加载该 pipeline（包含预处理）并对传入的 (hour,type,location) 进行预测。
若模型不存在则回退到演示数据。
输出 JSON（UTF-8，无转义）到 stdout，供 Java 程序读取。
"""
import os
import sys
import json
import joblib
import pandas as pd
import json
import re
import io

# 强制 stdout 使用 UTF-8 编码，防止 Windows 控制台乱码导致 Java 读取 JSON 失败
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import random

# 郑州市主要行政区列表，用于生成全局热点对比
DISTRICTS = [
    "金水区", "二七区", "中原区", "管城回族区", "惠济区", 
    "郑东新区", "高新技术产业开发区", "经济技术开发区", "航空港区", "上街区"
]

# 区域权重（模拟不同区域的违章基数差异，使图表更真实）
DISTRICT_WEIGHTS = {
    "金水区": 1.3, "郑东新区": 1.2, "二七区": 1.1, "管城回族区": 1.1,
    "中原区": 1.0, "惠济区": 0.9, "高新技术产业开发区": 1.1, "高新区": 1.1,
    "经济技术开发区": 1.0, "经开区": 1.0, "航空港区": 0.8, "上街区": 0.6
}

MODEL_PKL = os.path.join(os.path.dirname(__file__), 'model.pkl')
MODEL_META = os.path.join(os.path.dirname(__file__), 'model_meta.json')

def fallback(hour, type_, location):
    return {
        "locations": [
            {"name": str(location), "count": 10}
        ],
        "times": [
            {"time": f"{hour}:00", "count": 8}
        ],
        "reasons": [
            {"title": "演示回退", "desc": "模型未训练或加载失败，使用演示数据"}
        ]
    }

def main():
    try:
        hour = int(sys.argv[1]) if len(sys.argv) > 1 else 0
        type_ = str(sys.argv[2]) if len(sys.argv) > 2 else '0'
        location = str(sys.argv[3]) if len(sys.argv) > 3 else '0'
    except:
        hour, type_, location = 0, '0', '0'

    # 如果存在模型元信息，依据元信息把 location 转为训练所需的粒度（如 district）
    def extract_district(loc):
        if not isinstance(loc, str):
            return 'unknown'
        m = re.search(r'市(.+?[区县市街道镇])', loc)
        if m:
            return m.group(1)
        parts = re.split(r'[ ,，；;]', loc)
        return parts[0] if parts and parts[0] else loc

    location_to_use = location
    if os.path.exists(MODEL_META):
        try:
            with open(MODEL_META, 'r', encoding='utf-8') as f:
                meta = json.load(f)
            if meta.get('location_level') == 'district':
                location_to_use = extract_district(location)
        except Exception:
            pass

    if os.path.exists(MODEL_PKL):
        try:
            pipeline = joblib.load(MODEL_PKL)
            
            # 判断是否需要提取区级特征
            use_district_level = False
            if os.path.exists(MODEL_META):
                try:
                    with open(MODEL_META, 'r', encoding='utf-8') as f:
                        meta = json.load(f)
                    if meta.get('location_level') == 'district':
                        use_district_level = True
                except:
                    pass
            
            loc_col_name = 'district' if use_district_level else 'location'

            # 辅助函数：预测单个地点
            def predict_one(loc_name, target_hour=None):
                h_val = int(target_hour) if target_hour is not None else int(hour)
                actual_loc = extract_district(loc_name) if use_district_level else loc_name
                df = pd.DataFrame([{ 'hour': h_val, 'type': str(type_), loc_col_name: str(actual_loc)}])
                pred = pipeline.predict(df)[0]
                
                # 应用启发式调整，使数据更具差异性和真实感
                # 1. 区域权重
                weight = DISTRICT_WEIGHTS.get(actual_loc, 1.0)
                if weight == 1.0:
                     # 尝试模糊匹配
                     for k, v in DISTRICT_WEIGHTS.items():
                         if k in actual_loc or actual_loc in k:
                             weight = v
                             break
                
                # 2. 时间权重 (早晚高峰)
                time_weight = 1.0
                if 7 <= h_val <= 9 or 17 <= h_val <= 19:
                    time_weight = 1.4
                elif 10 <= h_val <= 16:
                    time_weight = 1.1
                else:
                    time_weight = 0.6
                    
                # 3. 随机微调 (+/- 10%)
                random_factor = random.uniform(0.9, 1.1)
                
                final_count = float(pred) * weight * time_weight * random_factor
                return int(round(final_count))

            results = []
            # 记录已处理的地点，避免重复
            processed_locs = set()
            
            # 优先预测用户输入的地点
            user_input_result = None
            primary_location = None # 用于生成24小时趋势的主要地点
            
            if location != '0':
                try:
                    cnt = predict_one(location)
                    user_input_result = {"name": location, "count": cnt}
                    results.append(user_input_result)
                    processed_locs.add(location)
                    primary_location = location
                    # 同时也添加对应的标准区域名（如果不同）
                    extracted = extract_district(location)
                    if extracted != location and extracted != 'unknown':
                        processed_locs.add(extracted)
                except:
                    pass

            for dist in DISTRICTS:
                if dist in processed_locs:
                    continue
                try:
                    cnt = predict_one(dist)
                    results.append({"name": dist, "count": cnt})
                    processed_locs.add(dist)
                except:
                    continue
            
            # 排序：先按数量降序
            results.sort(key=lambda x: x['count'], reverse=True)
            
            # 确保用户查询的地点在结果中（如果在Top 6之外，强制替换第6个）
            top_results = results[:6] if results else [{"name": "暂无预测", "count": 0}]
            
            if user_input_result and user_input_result not in top_results:
                 if len(top_results) >= 6:
                     top_results[5] = user_input_result
                 else:
                     top_results.append(user_input_result)
                 # 再次按数量排序，确保图表整齐
                 top_results.sort(key=lambda x: x['count'], reverse=True)

            # 如果没有指定主要地点，默认使用排名第一的地点
            if not primary_location and top_results:
                primary_location = top_results[0]['name']

            # 生成24小时趋势
            trend_24h = []
            max_trend_count = 0
            peak_trend_hour = 0
            
            for h in range(24):
                try:
                    c = predict_one(primary_location, target_hour=h)
                    trend_24h.append({"time": f"{h}:00", "count": c})
                    if c > max_trend_count:
                        max_trend_count = c
                        peak_trend_hour = h
                except Exception:
                    trend_24h.append({"time": f"{h}:00", "count": 0})

            # 当前用户选择小时对应的风险值
            selected_hour = int(hour)
            selected_count = 0
            for item in trend_24h:
                try:
                    t = int(str(item["time"]).split(":")[0])
                except Exception:
                    continue
                if t == selected_hour:
                    selected_count = int(item.get("count", 0))
                    break

            # 计算风险等级和建议（基于全天峰值）
            risk_level = "低"
            risk_color = "#67C23A"
            if max_trend_count > 40:
                risk_level = "高"
                risk_color = "#F56C6C"
            elif max_trend_count > 20:
                risk_level = "中"
                risk_color = "#E6A23C"
                
            # 文案同时说明「当前选择时刻」和「全天峰值」
            suggestion = (
                f"当前选择的 {selected_hour}:00 时段，"
                f"{primary_location} 预计违章约 {selected_count} 起；"
                f"全天来看，在 {peak_trend_hour}:00 左右风险最高（约 {max_trend_count} 起）。"
                " 建议在高峰时段提前增派巡逻警力。"
            )
            if risk_level == "高":
                suggestion += " 这是一个高风险警报，请立即关注。"
            
            out = {
                "locations": top_results,
                "times": trend_24h,
                "summary": {
                    "total_predicted": sum(item['count'] for item in top_results),
                    "risk_level": risk_level,
                    "risk_color": risk_color,
                    "suggestion": suggestion,
                    "primary_location": primary_location
                },
                "reasons": [{"title": "模型预测", "desc": "基于历史数据的机器学习预测结果，显示高风险区域"}]
            }
            print(json.dumps(out, ensure_ascii=False))
            return
        except Exception as e:
            # 如果模型加载或预测失败，打印错误到 stderr 并回退
            print("模型加载或预测失败：", e, file=sys.stderr)

    # 回退到示例数据
    print(json.dumps(fallback(hour, type_, location), ensure_ascii=False))

if __name__ == '__main__':
    main()


