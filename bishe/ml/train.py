#!/usr/bin/env python3
"""
train.py
训练示例：读取 ml/data.csv（由 prepare_data.py 生成），训练 RandomForest 回归模型预测每组 (hour,type,location) 的违章计数。
会把训练好的 pipeline（含 OneHot 编码）保存为 ml/model.pkl，供 predict.py 使用。
"""
import os
import argparse
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder
from sklearn.compose import ColumnTransformer
import joblib
import json

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DATA_CSV = os.path.join(os.path.dirname(__file__), 'data.csv')
DATA_DISTRICT_CSV = os.path.join(os.path.dirname(__file__), 'data_district.csv')
MODEL_PKL = os.path.join(os.path.dirname(__file__), 'model.pkl')
MODEL_META = os.path.join(os.path.dirname(__file__), 'model_meta.json')

def load_data(path):
    if not os.path.exists(path):
        raise FileNotFoundError(f"训练数据找不到：{path}。请先运行 prepare_data.py，或把数据保存为该路径。")
    return pd.read_csv(path)

def build_and_train(df, n_estimators=100):
    # 根据数据列名选择地点列（full->location, district->district）
    loc_col = 'location' if 'location' in df.columns else ('district' if 'district' in df.columns else 'location')
    # 特征列
    X = df[['hour', 'type', loc_col]].astype({'hour': 'int64', 'type': 'str', loc_col: 'str'})
    y = df['count'].astype('float')

    categorical_features = ['type', loc_col]
    numeric_features = ['hour']

    pre = ColumnTransformer(
        transformers=[
            ('cat', OneHotEncoder(handle_unknown='ignore'), categorical_features)
        ],
        remainder='passthrough'  # hour 直接通过
    )

    model = RandomForestRegressor(n_estimators=n_estimators, random_state=42)
    pipeline = Pipeline([('pre', pre), ('model', model)])
    pipeline.fit(X, y)
    return pipeline

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data', default=DATA_CSV, help='训练数据路径（CSV）')
    parser.add_argument('--location-level', default='full', choices=['full','district'], help='地点粒度：full 或 district')
    parser.add_argument('--n', type=int, default=100, help='随机森林树的数量')
    parser.add_argument('--out', default=MODEL_PKL, help='模型输出路径')
    args = parser.parse_args()

    # 根据 location-level 自动选择数据源（若未指定 data）
    data_path = args.data
    if args.location_level == 'district' and args.data == DATA_CSV:
        data_path = DATA_DISTRICT_CSV

    df = load_data(data_path)
    print(f"读取训练数据：{data_path} （{len(df)} 行）")

    pipeline = build_and_train(df, n_estimators=args.n)
    joblib.dump(pipeline, args.out)
    # 保存模型元信息，供 predict.py 使用
    meta = {
        "location_level": args.location_level
    }
    with open(MODEL_META, 'w', encoding='utf-8') as f:
        json.dump(meta, f, ensure_ascii=False)

    print(f"已训练并保存模型：{args.out}")
    print(f"已写入模型元信息：{MODEL_META}")

if __name__ == '__main__':
    main()


