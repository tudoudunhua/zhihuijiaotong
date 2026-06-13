#!/usr/bin/env python3
import random
from datetime import datetime, timedelta


OUTPUT_SQL = r"C:\Users\lisihao\Desktop\smart_traffic_zhengzhou_10080.sql"
OUTPUT_SQL_REFRESH = r"C:\Users\lisihao\Desktop\smart_traffic_zhengzhou_10080_refresh.sql"
TOTAL_ROWS = 10080
SEED = 20260420

DISTRICT_ROADS = {
    "金水区": ["花园路农业路口", "经三路东风路", "中州大道北三环", "东明路红专路"],
    "二七区": ["大学路中原路口", "京广路航海路", "嵩山路长江路", "淮河路兴华街"],
    "中原区": ["建设路嵩山路", "桐柏路陇海路", "秦岭路棉纺路", "中原路伏牛路"],
    "管城回族区": ["紫荆山路", "航海东路未来路", "城东路商城路", "南关街陇海路"],
    "惠济区": ["文化路北段", "开元路天河路", "江山路三全路", "英才街花园口大道"],
    "郑东新区": ["商务内环路", "金水东路东风南路", "农业南路七里河南路", "龙子湖平安大道"],
    "高新区": ["科学大道", "瑞达路梧桐街", "雪松路枫杨街", "长椿路莲花街"],
    "经开区": ["航海东路第八大街", "经北二路经开第五大街", "朝凤路经南三路", "南三环经开第二十大街"],
    "航空港区": ["迎宾大道", "华夏大道郑港六路", "新港大道雍州路", "机场高速南四环"],
    "上街区": ["中心路金华路", "汝南路登封路", "许昌路济源路", "新安路工业路"],
    "中牟县": ["商都大道", "建设路青年路", "解放路官渡大街", "新城大道文通路"],
    "新郑市": ["人民路中华路", "轩辕路龙湖大道", "解放路玉前路", "郑新快速路双湖大道"],
}

VIOLATION_RULES = [
    ("违法停车", 50, 0, "BLUE", 0.18),
    ("未系安全带", 100, 2, "BLUE", 0.13),
    ("超速行驶", 150, 3, "YELLOW", 0.16),
    ("违法变道", 100, 2, "BLUE", 0.14),
    ("占用应急车道", 500, 6, "YELLOW", 0.08),
    ("闯红灯", 300, 6, "RED", 0.12),
    ("酒驾", 1300, 12, "ORANGE", 0.07),
    ("无证驾驶", 800, 12, "ORANGE", 0.05),
    ("逆行", 200, 3, "YELLOW", 0.04),
    ("违反禁令标志", 100, 3, "BLUE", 0.03),
]


def weighted_pick(items):
    r = random.random()
    acc = 0.0
    for item in items:
        acc += item[-1]
        if r <= acc:
            return item
    return items[-1]


def random_plate():
    letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    nums = "0123456789"
    tail = "".join(random.choice(nums) for _ in range(4))
    prefix = random.choice(letters)
    return f"豫{prefix}{tail}"


def build_plate_pool(total_rows):
    """
    构造车牌池，保证存在重复违章车辆：
    - 约 20% 车牌会出现 3~8 次（明显复犯）
    - 约 35% 车牌会出现 2~4 次（一般复犯）
    - 其余为单次
    """
    heavy_count = int(total_rows * 0.20)
    normal_count = int(total_rows * 0.35)
    single_count = total_rows - heavy_count - normal_count

    pool = []

    # 明显复犯车牌
    heavy_plates = [random_plate() for _ in range(max(1, heavy_count // 5))]
    for _ in range(heavy_count):
        pool.append(random.choice(heavy_plates))

    # 一般复犯车牌
    normal_plates = [random_plate() for _ in range(max(1, normal_count // 3))]
    for _ in range(normal_count):
        pool.append(random.choice(normal_plates))

    # 单次/稀疏车牌
    for _ in range(single_count):
        pool.append(random_plate())

    random.shuffle(pool)
    return pool


def random_violation_time():
    start = datetime(2024, 1, 1, 0, 0, 0)
    end = datetime(2026, 4, 15, 23, 59, 59)
    delta = end - start
    sec = random.randint(0, int(delta.total_seconds()))
    t = start + timedelta(seconds=sec)
    hour = t.hour
    # 强化早晚高峰分布
    if random.random() < 0.35:
        hour = random.choice([7, 8, 9, 17, 18, 19])
        t = t.replace(hour=hour, minute=random.randint(0, 59), second=random.randint(0, 59))
    return t


def build_row(idx, plate_number):
    district = random.choice(list(DISTRICT_ROADS.keys()))
    road = random.choice(DISTRICT_ROADS[district])
    violation_type, fine, points, warning_level, _ = weighted_pick(VIOLATION_RULES)
    v_time = random_violation_time()
    create_time = v_time + timedelta(minutes=random.randint(1, 360))

    road_id = random.randint(1, 80)
    device_id = random.randint(11, 80)
    location = f"郑州市{district}{road}"
    img = f"/img/violation_generated_{idx}.jpg"

    dispose_status = random.choices([0, 1, None], weights=[0.52, 0.38, 0.10], k=1)[0]
    dispose_time = None
    dispose_user = None
    if dispose_status == 1:
        dt = v_time + timedelta(hours=random.randint(1, 96))
        dispose_time = dt.strftime("%Y-%m-%d %H:%M:%S")
        dispose_user = random.randint(1, 6)

    return {
        "plate_number": plate_number,
        "violation_type": violation_type,
        "violation_time": v_time.strftime("%Y-%m-%d %H:%M:%S"),
        "fine_amount": fine,
        "points_deducted": points,
        "road_id": road_id,
        "device_id": device_id,
        "violation_location": location,
        "violation_img": img,
        "warning_level": warning_level,
        "dispose_status": dispose_status,
        "dispose_time": dispose_time,
        "dispose_user": dispose_user,
        "create_time": create_time.strftime("%Y-%m-%d %H:%M:%S"),
    }


def sql_value(v):
    if v is None:
        return "NULL"
    if isinstance(v, str):
        return "'" + v.replace("\\", "\\\\").replace("'", "''") + "'"
    return str(v)


def write_sql(path, truncate_first=False):
    random.seed(SEED)
    header = [
        "-- 郑州市违章记录扩展数据（10080条）",
        "-- 生成时间: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "USE `smart_traffic`;",
        "",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
    ]
    cols = (
        "plate_number, violation_type, violation_time, fine_amount, points_deducted, "
        "road_id, device_id, violation_location, violation_img, warning_level, "
        "dispose_status, dispose_time, dispose_user, create_time"
    )

    batch_size = 500
    plate_pool = build_plate_pool(TOTAL_ROWS)
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(header) + "\n")
        if truncate_first:
            f.write("TRUNCATE TABLE `traffic_violation`;\n\n")
        for start in range(1, TOTAL_ROWS + 1, batch_size):
            end = min(start + batch_size - 1, TOTAL_ROWS)
            f.write(f"INSERT INTO `traffic_violation` ({cols}) VALUES\n")
            lines = []
            for i in range(start, end + 1):
                row = build_row(i, plate_pool[i - 1])
                values = ", ".join(
                    sql_value(row[k]) for k in [
                        "plate_number",
                        "violation_type",
                        "violation_time",
                        "fine_amount",
                        "points_deducted",
                        "road_id",
                        "device_id",
                        "violation_location",
                        "violation_img",
                        "warning_level",
                        "dispose_status",
                        "dispose_time",
                        "dispose_user",
                        "create_time",
                    ]
                )
                lines.append(f"({values})")
            f.write(",\n".join(lines))
            f.write(";\n\n")
        f.write("SET FOREIGN_KEY_CHECKS = 1;\n")


if __name__ == "__main__":
    write_sql(OUTPUT_SQL, truncate_first=False)
    write_sql(OUTPUT_SQL_REFRESH, truncate_first=True)
    print(f"SQL file generated (append mode): {OUTPUT_SQL}")
    print(f"SQL file generated (refresh mode): {OUTPUT_SQL_REFRESH}")
    print(f"Rows prepared: {TOTAL_ROWS}")
