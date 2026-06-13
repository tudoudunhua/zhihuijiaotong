# ML 训练与预测说明

目录结构：
- ml/
  - prepare_data.py   # 从 violations.xlsx 生成 ml/data.csv
  - train.py          # 训练模型并保存 ml/model.pkl
  - predict.py        # 预测入口（被 Java 调用）
  - requirements.txt  # Python 依赖

快速上手：

1) 安装依赖（建议在虚拟环境中）
```
pip install -r ml/requirements.txt
```

2) 生成训练数据（假定 `violations.xlsx` 位于项目根目录）
```
python ml/prepare_data.py
```

3) 训练模型并保存
```
python ml/train.py --data ml/data.csv --out ml/model.pkl
```

4) 启动后端（Spring Boot），后端会调用 `python ml/predict.py hour type location` 返回 JSON 结果。

说明：
- `prepare_data.py` 会尝试自动识别时间/类型/地点列并按 (hour,type,location) 聚合计数作为训练目标。
- `train.py` 使用 RandomForest 回归预测聚合计数，训练完成后输出 `ml/model.pkl`（包含预处理 pipeline）。
- `predict.py` 如果找不到 `ml/model.pkl` 会回退到演示数据，保证前端可见效果。


