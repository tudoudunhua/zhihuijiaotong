#!/usr/bin/env python3
"""
prepare_data.py
从项目根目录下的 `violations.xlsx` 读取违章原始记录，尝试自动识别时间/类型/地点列，
并按 (hour, type, location) 聚合计数，保存为 `ml/data.csv` 供训练使用。
"""
import os
import argparse
import pandas as pd
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DEFAULT_XLSX = os.path.join(ROOT, 'violations.xlsx')
DEFAULT_CSV = os.path.join(ROOT, 'traffic_violation.csv')
OUTPUT_CSV = os.path.join(os.path.dirname(__file__), 'data.csv')

COMMON_TIME_COLS = ['warningtime', 'time', 'date', 'occurtime', 'occur_time', 'timestamp', 'violation_time', 'create_time']
COMMON_TYPE_COLS = ['type', 'violationtype', 'violation_type', 'violation', 'eventtype']
COMMON_LOCATION_COLS = ['location', 'place', 'address', '地点', 'site', 'violation_location']

def find_column(cols, candidates):
    lower = {c.lower(): c for c in cols}
    for cand in candidates:
        if cand in lower:
            return lower[cand]
    return None

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--input', default=None, help='输入数据文件（支持 .csv 或 .xlsx）')
    args = parser.parse_args()

    # 决策输入文件：优先使用 --input，其次项目根的 traffic_violation.csv，再次 violations.xlsx
    input_path = args.input
    if not input_path:
        if os.path.exists(DEFAULT_CSV):
            input_path = DEFAULT_CSV
        elif os.path.exists(DEFAULT_XLSX):
            input_path = DEFAULT_XLSX

    if not input_path or not os.path.exists(input_path):
        print(f"未找到数据文件: {input_path}")
        print("请把原始违章数据放到项目根目录并命名为 traffic_violation.csv 或 violations.xlsx，或通过 --input 指定路径。")
        return

    # 读取文件：根据扩展名选择读取器
    if input_path.lower().endswith('.csv'):
        df = pd.read_csv(input_path, encoding='utf-8')
    else:
        df = pd.read_excel(input_path, engine='openpyxl')
    cols = df.columns.tolist()
    print("发现列：", cols)

    time_col = find_column(cols, COMMON_TIME_COLS)
    type_col = find_column(cols, COMMON_TYPE_COLS)
    location_col = find_column(cols, COMMON_LOCATION_COLS)

    # 尝试提取 hour 列
    if time_col:
        try:
            # 支持日/月/年格式（如 16/5/2024 14:00:00）
            dt = pd.to_datetime(df[time_col], errors='coerce', dayfirst=True)
            df['hour'] = dt.dt.hour.fillna(0).astype(int)
            print(f"使用时间列 `{time_col}` 提取 hour")
        except Exception as e:
            print("解析时间列失败：", e)
            df['hour'] = 0
    else:
        print("未识别到时间列，填充 hour=0")
        df['hour'] = 0

    # 类型与地点列
    if type_col:
        df['type'] = df[type_col].astype(str)
        print(f"使用类型列 `{type_col}`")
    else:
        print("未识别到类型列，统一设为 'unknown'")
        df['type'] = 'unknown'

    if location_col:
        df['location'] = df[location_col].astype(str)
        print(f"使用地点列 `{location_col}`")
    else:
        print("未识别到地点列，统一设为 'unknown'")
        df['location'] = 'unknown'

    # 聚合统计：按 hour/type/location 计数
    agg = df.groupby(['hour', 'type', 'location']).size().reset_index(name='count')
    agg.to_csv(OUTPUT_CSV, index=False, encoding='utf-8-sig')
    print(f"已生成训练数据：{OUTPUT_CSV} （共 {len(agg)} 条聚合行）")
    # 额外生成按行政区域（district）聚合的数据，方便训练更粗粒度的模型
    def extract_district(loc):
        if not isinstance(loc, str):
            return 'unknown'
        # 尝试匹配形如 "郑州市二七区..." 或 "...市二七区..."
        m = re.search(r'市(.+?[区县市街道镇])', loc)
        if m:
            return m.group(1)
        # 退化：尝试提取到第一个空格或逗号之前的短串
        parts = re.split(r'[ ,，；;]', loc)
        return parts[0] if parts and parts[0] else loc

    df['district'] = df['location'].apply(extract_district)
    agg_district = df.groupby(['hour', 'type', 'district']).size().reset_index(name='count')
    out_district = os.path.join(os.path.dirname(__file__), 'data_district.csv')
    agg_district.to_csv(out_district, index=False, encoding='utf-8-sig')
    print(f"已生成按行政区聚合训练数据：{out_district} （共 {len(agg_district)} 条聚合行）")

if __name__ == '__main__':
    main()


